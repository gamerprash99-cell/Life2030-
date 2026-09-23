package com.lifeos.app.data.db

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the exported v3 Room schema:
 *  - every retained feature table is still present (nothing was accidentally
 *    dropped by the v2 → v3 migration),
 *  - the removed `notes` and `captures` tables are gone.
 *
 * This runs as a plain JVM unit test against the KSP-exported schema file
 * (`app/schemas/.../3.json`), so it executes on every `testDebugUnitTest`
 * run without needing a device. The on-device path is exercised by
 * `AppDatabaseMigrationTest` in app/src/androidTest.
 */
class AppDatabaseSchemaTest {

    private fun readSchemaJson(): String {
        val candidates = listOf(
            // Room schemaLocation is "$projectDir/schemas"; unit tests run with the
            // module dir (app/) as the working directory.
            File("schemas", "com.lifeos.app.data.db.AppDatabase/3.json"),
            File("app", "schemas/com.lifeos.app.data.db.AppDatabase/3.json")
        )
        return candidates.firstOrNull { it.exists() && it.isFile }?.readText()
            ?: error("Can't find exported 3.json schema; run an assemble first.")
    }

    private fun tableNames(): Set<String> {
        val json = readSchemaJson()
        return Regex("\"tableName\"\\s*:\\s*\"([a-z_]+)\"")
            .findAll(json)
            .map { it.groupValues[1] }
            .toSet()
    }

    @Test
    fun schemaV3_keepsEveryRetainedTable() {
        val tables = tableNames()
        for (retained in listOf(
            "tasks", "habits", "habit_completions", "expenses", "diary_entries"
        )) {
            assertTrue("v3 schema must keep table '$retained'", retained in tables)
        }
        assertTrue("v3 schema must have exactly the retained tables", tables == setOf(
            "tasks", "habits", "habit_completions", "expenses", "diary_entries"
        ))
    }

    @Test
    fun schemaV3_dropsNotesAndCaptures() {
        val tables = tableNames()
        assertFalse("v3 schema must not contain 'notes'", "notes" in tables)
        assertFalse("v3 schema must not contain 'captures'", "captures" in tables)
    }
}