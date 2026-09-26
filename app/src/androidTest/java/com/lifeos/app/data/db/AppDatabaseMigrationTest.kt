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
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO notes (id, title, contentJson, searchableText, folder, tagsCsv, pinned, favorite, archived, trashed, createdAt, updatedAt, isDeleted) " +
                    "VALUES ('n1', 't', '[]', 't', NULL, '', 0, 0, 0, 0, 0, 0, 0)"
            )
            execSQL(
                "INSERT INTO captures (id, type, filePath, caption, dateEpochDay, timeMinutes, createdAt) " +
                    "VALUES ('c1', 'PHOTO', '/nonexistent/f.jpg', NULL, 0, 0, 0)"
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

    private companion object {
        const val TEST_DB = "migration-test"
    }
}