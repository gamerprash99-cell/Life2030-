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
        val versions = listOf("4.json", "3.json")
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
        error("Can't find an exported Room schema (4.json or 3.json); run an assemble first.")
    }

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
        val json = readSchemaJson()
        // Room writes entity CREATE statements with a `${TABLE_NAME}` placeholder;
        // grab the reminders statement specifically (its body contains entityType).
        val remindersCreateSql = Regex("\"createSql\": \"CREATE TABLE[^\"]*entityType[^\"]*\"")
            .find(json)?.value
            ?: error("can't find reminders createSql in exported schema")
        val columns = Regex("`([a-zA-Z_]+)`")
            .findAll(remindersCreateSql)
            .map { it.groupValues[1] }
            .toSet()
        val expected = setOf(
            "id", "entityType", "entityId", "title", "nextTriggerAtEpochMillis",
            "repeatType", "repeatDaysCsv", "enabled", "soundEnabled", "vibrationEnabled",
            "snoozeMinutes", "snoozeReturnAtEpochMillis", "updatedAt"
        )
        for (column in expected) {
            assertTrue("v4 reminders table must define column '$column' (found: $columns)", column in columns)
        }
    }
}