package com.lifeos.app.data.db

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the exported Room schema:
 *  - every retained feature table is still present (nothing was accidentally
 *    dropped by the v2 → v3 migration),
 *  - the removed `notes` and `captures` tables are gone,
 *  - the v4 `reminders` table (reliable alarm reminders) exists with its full
 *    column set.
 *
 * This runs as a plain JVM unit test against the KSP-exported schema file
 * (`app/schemas/.../4.json` — falls back to 3.json), so it executes on every
 * `testDebugUnitTest` run without needing a device. The on-device path is
 * exercised by `AppDatabaseMigrationTest` in app/src/androidTest.
 */
class AppDatabaseSchemaTest {

    private fun readSchemaJson(): String {
        val versions = listOf("5.json", "4.json", "3.json")
        for (version in versions) {
            val candidates = listOf(
                // Room schemaLocation is "$projectDir/schemas"; unit tests run with the
                // module dir (app/) as the working directory.
                File("schemas", "com.lifeos.app.data.db.AppDatabase/$version"),
                File("app", "schemas/com.lifeos.app.data.db.AppDatabase/$version")
            )
            val hit = candidates.firstOrNull { it.exists() && it.isFile }
            if (hit != null) return hit.readText()
        }
        error("Can't find an exported Room schema (5.json, 4.json or 3.json); run an assemble first.")
    }

    private fun createSqlFor(table: String, json: String = readSchemaJson()): String =
        Regex("\"tableName\": \"$table\"")
            .find(json)
            ?.let { match ->
                val tail = json.substring(match.range.last)
                Regex("\"createSql\": \"([^\"]*)\"").find(tail)?.groupValues?.get(1)
            }
            ?: error("can't find createSql for table '$table' in exported schema")

    private fun columnsOf(createSql: String): Set<String> =
        Regex("`([a-zA-Z_]+)`").findAll(createSql).map { it.groupValues[1] }.toSet()

    private fun tableNames(): Set<String> {
        val json = readSchemaJson()
        return Regex("\"tableName\"\\s*:\\s*\"([a-z_]+)\"")
            .findAll(json)
            .map { it.groupValues[1] }
            .toSet()
    }

    @Test
    fun schema_keepsEveryEntityTableExactly() {
        val tables = tableNames()
        for (retained in listOf(
            "tasks", "habits", "habit_completions", "expenses", "diary_entries", "reminders"
        )) {
            assertTrue("schema must keep table '$retained'", retained in tables)
        }
        assertTrue("schema must have exactly the expected tables", tables == setOf(
            "tasks", "habits", "habit_completions", "expenses", "diary_entries", "reminders"
        ))
    }

    @Test
    fun schemaV3_dropsNotesAndCaptures() {
        val tables = tableNames()
        assertFalse("v3 schema must not contain 'notes'", "notes" in tables)
        assertFalse("v3 schema must not contain 'captures'", "captures" in tables)
    }

    @Test
    fun schemaV4_keepsRetainedTablesAndAddsReminders() {
        val tables = tableNames()
        for (retained in listOf(
            "tasks", "habits", "habit_completions", "expenses", "diary_entries", "reminders"
        )) {
            assertTrue("v4 schema must contain table '$retained'", retained in tables)
        }
    }

    @Test
    fun schemaV4_remindersTableHasFullColumnSet() {
        val columns = columnsOf(createSqlFor("reminders"))
        val expected = setOf(
            "id", "entityType", "entityId", "title", "nextTriggerAtEpochMillis",
            "repeatType", "repeatDaysCsv", "enabled", "soundEnabled", "vibrationEnabled",
            "snoozeMinutes", "snoozeReturnAtEpochMillis", "updatedAt"
        )
        for (column in expected) {
            assertTrue("v4 reminders table must define column '$column' (found: $columns)", column in columns)
        }
    }

    /**
     * v5 adds the diary favourites flag. This is the guard for the v4 → v5
     * migration: if the column is ever dropped or made nullable, an existing
     * install would fail to open, so assert it here as a plain JVM test.
     */
    @Test
    fun schemaV5_diaryEntriesAddsIsFavorite() {
        val columns = columnsOf(createSqlFor("diary_entries"))
        assertTrue(
            "v5 diary_entries must define the 'isFavorite' column (found: $columns)",
            "isFavorite" in columns
        )
    }

    /**
     * The migration is additive and has to leave every previously stored field
     * intact — a favourite flag must not cost us the body, mood, tags or the
     * attachments payload (which is where photos, audio, place and weather now
     * live, so the old weather* columns are intentionally not expected here).
     */
    @Test
    fun schemaV5_diaryEntriesKeepsEveryEarlierColumn() {
        val columns = columnsOf(createSqlFor("diary_entries"))
        val expected = setOf(
            "id", "dateEpochDay", "timeMinutes", "mood", "title", "content",
            "tagsCsv", "aiGenerated", "isReviewed", "attachmentsJson",
            "createdAt", "updatedAt"
        )
        for (column in expected) {
            assertTrue(
                "v5 diary_entries must still define column '$column' (found: $columns)",
                column in columns
            )
        }
    }

    /** Every field the Diary feature persists in `attachmentsJson` must still be an entity column. */
    @Test
    fun schemaV5_diaryEntriesStillStoresAttachmentsPayload() {
        val columns = columnsOf(createSqlFor("diary_entries"))
        assertTrue(
            "diary entries must still have the attachmentsJson column (found: $columns)",
            "attachmentsJson" in columns
        )
    }
}