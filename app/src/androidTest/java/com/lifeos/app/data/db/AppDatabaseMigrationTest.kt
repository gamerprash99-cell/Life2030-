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
 * On-device/emulator test of the v2 → v3 migration.
 *
 * The production database is SQLCipher-backed, but this test runs the migration
 * against a plain SQLite database (via `FrameworkSQLiteOpenHelperFactory`) so
 * MigrationTestHelper can validate it. It seeds a v2 database with a row in
 * both dropped tables, runs MIGRATION_2_3, and asserts the dropped tables are
 * really gone while every retained table survives.
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

    private companion object {
        const val TEST_DB = "migration-test"
    }
}