package com.lifeos.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * On-device/emulator tests of the Room migrations.
 *
 * The production database is SQLCipher-backed, but this test runs the migration
 * against a plain SQLite database (via `FrameworkSQLiteOpenHelperFactory`) so
 * MigrationTestHelper can validate it. It seeds a v2 database with a row in
 * both dropped tables, runs MIGRATION_2_3, and asserts the dropped tables are
 * really gone while every retained table survives. A second test runs the v3 →
 * v4 migration and confirms the `reminders` table (and its indexes) appear.
 *
 * Run with: `./gradlew :app:assembleDebugAndroidTest` and an emulator/device
 * (`./gradlew :app:connectedDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate2To3_dropsNotesAndCapturesKeepsEverythingElse() {
        // Create a v2 database and seed both tables that v3 must drop.
        // Column lists mirror app/schemas/.../2.json exactly so INSERTs succeed
        // on the strict NOT NULL schema (no defaults enforced).
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO notes (id, title, contentJson, plainTextForSearch, folder, tagsCsv, isPinned, isFavorite, isArchived, isDeleted, createdAt, updatedAt, attachmentsJson) " +
                    "VALUES ('n1', 't', '[]', 't', NULL, '', 0, 0, 0, 0, 0, 0, '[]')"
            )
            execSQL(
                "INSERT INTO captures (id, type, filePath, thumbnailPath, caption, transcript, mood, tagsCsv, dateEpochDay, timeMinutes, createdAt) " +
                    "VALUES ('c1', 'PHOTO', '/nonexistent/f.jpg', NULL, NULL, NULL, NULL, '', 0, 0, 0)"
            )
            close()
        }

        // Run the migration; validateDroppedTables = true makes Room verify
        // that only the declared dropped tables were removed. The helper hands
        // back the migrated database for direct inspection.
        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.MIGRATION_2_3)

        db.use { db ->
            val tables = mutableSetOf<String>()
            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table'"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0))
                }
            }
            assert(tables.containsAll(setOf("tasks", "habits", "habit_completions", "expenses", "diary_entries"))) {
                "retained tables missing after migration: $tables"
            }
            assert(!tables.contains("notes")) { "notes table still present" }
            assert(!tables.contains("captures")) { "captures table still present" }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4_addsRemindersTableAndIndexes() {
        // Seed an empty v3 database, then migrate it forward.
        helper.createDatabase(TEST_DB, 3).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, AppDatabase.MIGRATION_3_4)

        db.use { db ->
            val tables = mutableSetOf<String>()
            val indexes = mutableSetOf<String>()
            db.query(
                "SELECT name, type FROM sqlite_master WHERE type IN ('table', 'index') AND name NOT LIKE 'sqlite_%'"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    when (cursor.getString(1)) {
                        "table" -> tables.add(name)
                        "index" -> indexes.add(name)
                    }
                }
            }
            assert(tables.contains("reminders")) { "reminders table missing after migration: $tables" }
            for (index in listOf(
                "index_reminders_entityType",
                "index_reminders_entityId",
                "index_reminders_nextTriggerAtEpochMillis",
                "index_reminders_enabled"
            )) {
                assert(indexes.contains(index)) { "missing index $index after v3→v4 migration: $indexes" }
            }
            // Every column the entity expects must exist (Room validates shape, and
            // running the SELECT proves the created column set is SELECTable).
            db.query("SELECT * FROM reminders").close()
        }
    }

    /**
     * v4 → v5 adds the diary favourites flag. The interesting property is that
     * it is *additive over real data*: a row written before the migration must
     * still be readable afterwards, and must come back with a non-favourite
     * default rather than NULL (which would blow up the non-null Kotlin field).
     */
    @Test
    @Throws(IOException::class)
    fun migrate4To5_addsIsFavoriteAndKeepsExistingEntries() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                "INSERT INTO diary_entries (id, dateEpochDay, timeMinutes, mood, title, content, tagsCsv, " +
                    "aiGenerated, isReviewed, attachmentsJson, createdAt, updatedAt) " +
                    "VALUES ('d1', 20000, 510, 'JOY', 'A good walk', 'Body text kept.', 'outdoors', " +
                    "0, 0, '[]', 1, 1)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, AppDatabase.MIGRATION_4_5)

        db.use { db ->
            db.query("SELECT isFavorite FROM diary_entries WHERE id = 'd1'").use { cursor ->
                assert(cursor.moveToFirst()) { "pre-existing diary entry vanished in v4→v5" }
                assert(!cursor.isNull(0)) { "isFavorite must be backfilled to 0, not NULL" }
                assert(cursor.getInt(0) == 0) { "existing entries must default to not-favourite" }
            }
            // The stored content must be untouched by the additive migration.
            db.query("SELECT content FROM diary_entries WHERE id = 'd1'").use { cursor ->
                assert(cursor.moveToFirst())
                assert(cursor.getString(0) == "Body text kept.") { "entry content was altered by the migration" }
            }
            // And the new column must be writable.
            db.execSQL("UPDATE diary_entries SET isFavorite = 1 WHERE id = 'd1'")
        }
    }

    /**
     * v6 → v7 is the permanent Habits migration. It must be purely additive
     * over populated data: an existing habit and its completions keep every
     * original value, while the new habit columns arrive with defaults that the
     * Kotlin model considers valid (BUILD / TICK / LEGACY / no colour / timeline
     * on), and the two checklist tables are created empty.
     */
    @Test
    @Throws(IOException::class)
    fun migrate6To7_keepsHabitsAndCompletionsAndAddsPermanentHabitFields() {
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(
                "INSERT INTO habits (id, name, icon, category, frequency, customDaysCsv, goalCount, " +
                    "reminderEpochMillis, startDateEpochDay, isArchived, createdAt, updatedAt) " +
                    "VALUES ('h1', 'Read', 'book', 'Growth', 'CUSTOM', '1,3,5', 2, 123456, 19000, 0, 1, 2)"
            )
            execSQL(
                "INSERT INTO habit_completions (habitId, dateEpochDay, progressCount, completedAtEpochMillis) " +
                    "VALUES ('h1', 19001, 2, 1700000000000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, AppDatabase.MIGRATION_6_7)

        db.use { db ->
            db.query(
                "SELECT name, icon, category, frequency, customDaysCsv, goalCount, startDateEpochDay, " +
                    "description, colorArgb, habitType, trackingMode, scheduleType, daysOfWeekCsv, " +
                    "daysOfMonthCsv, timesPerPeriod, showOnTimeline FROM habits WHERE id = 'h1'"
            ).use { cursor ->
                assert(cursor.moveToFirst()) { "pre-existing habit vanished in v6→v7" }
                assert(cursor.getString(0) == "Read") { "habit name changed" }
                assert(cursor.getString(1) == "book") { "habit icon changed" }
                assert(cursor.getString(2) == "Growth") { "habit category changed" }
                assert(cursor.getString(3) == "CUSTOM") { "legacy frequency changed" }
                assert(cursor.getString(4) == "1,3,5") { "legacy custom days changed" }
                assert(cursor.getInt(5) == 2) { "legacy goal count changed" }
                assert(cursor.getLong(6) == 19000L) { "start date changed" }
                assert(cursor.isNull(7)) { "description must default to NULL" }
                assert(cursor.getLong(8) == 0L) { "colorArgb must default to 0 (use Iris)" }
                assert(cursor.getString(9) == "BUILD") { "habitType must default to BUILD" }
                assert(cursor.getString(10) == "TICK") { "trackingMode must default to TICK" }
                assert(cursor.getString(11) == "LEGACY") { "scheduleType must default to LEGACY" }
                assert(cursor.isNull(12)) { "daysOfWeekCsv must default to NULL" }
                assert(cursor.isNull(13)) { "daysOfMonthCsv must default to NULL" }
                assert(cursor.isNull(14)) { "timesPerPeriod must default to NULL" }
                assert(cursor.getInt(15) == 1) { "showOnTimeline must default to 1" }
            }

            db.query(
                "SELECT progressCount, outcome, targetCount FROM habit_completions " +
                    "WHERE habitId = 'h1' AND dateEpochDay = 19001"
            ).use { cursor ->
                assert(cursor.moveToFirst()) { "pre-existing completion vanished in v6→v7" }
                assert(cursor.getInt(0) == 2) { "completion progress changed" }
                assert(cursor.getString(1) == "DONE") { "outcome must default to DONE" }
                assert(cursor.getInt(2) == 0) { "targetCount must default to 0 (legacy snapshot)" }
            }

            // The new tables exist, are writable, and start empty.
            db.query("SELECT COUNT(*) FROM habit_checklist_items").use { cursor ->
                assert(cursor.moveToFirst() && cursor.getInt(0) == 0)
            }
            db.query("SELECT COUNT(*) FROM habit_checklist_checks").use { cursor ->
                assert(cursor.moveToFirst() && cursor.getInt(0) == 0)
            }
            db.execSQL(
                "INSERT INTO habit_checklist_items (id, habitId, label, position, isArchived, createdAt) " +
                    "VALUES ('i1', 'h1', 'Warm up', 0, 0, 3)"
            )
            db.execSQL(
                "INSERT INTO habit_checklist_checks (itemId, habitId, dateEpochDay, isChecked, checkedAtEpochMillis, labelSnapshot) " +
                    "VALUES ('i1', 'h1', 19001, 1, 4, 'Warm up')"
            )
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}