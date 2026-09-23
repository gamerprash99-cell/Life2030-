package com.lifeos.app.data.repository

import com.lifeos.app.data.db.AppDatabase
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.TaskEntity
import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * "Your life. Your data." — Section 2/19/59. A complete, human-readable JSON
 * export of everything LifeOS stores locally, written to app-private storage
 * so it can then be shared/saved by the user explicitly (never uploaded
 * automatically). Restore is idempotent (upsert by primary key).
 */
@Serializable
data class LifeOSBackup(
    val exportedAtEpochMillis: Long,
    val appVersion: String,
    val tasks: List<TaskEntity>,
    val habits: List<HabitEntity>,
    val habitCompletions: List<HabitCompletionEntity>,
    val expenses: List<ExpenseEntity>,
    val diaryEntries: List<DiaryEntity>,
    val formatVersion: Int = LifeOSBackup.CURRENT_FORMAT_VERSION
) {
    companion object {
        /**
         * Bump whenever the serialized shape changes incompatibly so imports
         * can be rejected clearly instead of restoring garbage silently. v1
         * backups written by older LifeOS builds contained "notes" and
         * "captures" keys; those keys are simply ignored on import now
         * (ignoreUnknownKeys), so old export files remain restorable.
         */
        const val CURRENT_FORMAT_VERSION = 1

        /** Hard cap so a giant/corrupt import can't exhaust memory on read. */
        const val MAX_BACKUP_BYTES = 100L * 1024 * 1024

        fun isOversized(sizeBytes: Long): Boolean = sizeBytes > MAX_BACKUP_BYTES
    }
}

/**
 * Validates a decoded backup before it is written into the database. Returns an
 * English error message the UI can surface, or null when the backup is safe to
 * restore.
 */
fun validateBackup(backup: LifeOSBackup): String? = when {
    backup.formatVersion < 1 ->
        "This file is not a valid LifeOS backup."
    backup.formatVersion > LifeOSBackup.CURRENT_FORMAT_VERSION ->
        "This backup was created by a newer version of LifeOS. Update the app first."
    else -> null
}

class BackupRepository(
    private val database: AppDatabase,
    private val taskRepo: TaskRepository,
    private val habitRepo: HabitRepository,
    private val expenseRepo: ExpenseRepository,
    private val diaryRepo: DiaryRepository
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun buildBackup(appVersion: String): LifeOSBackup = LifeOSBackup(
        exportedAtEpochMillis = System.currentTimeMillis(),
        appVersion = appVersion,
        tasks = taskRepo.getAllForBackup(),
        habits = habitRepo.getAllForBackup(),
        habitCompletions = habitRepo.getAllCompletionsForBackup(),
        expenses = expenseRepo.getAllForBackup(),
        diaryEntries = diaryRepo.getAllForBackup(),
        formatVersion = LifeOSBackup.CURRENT_FORMAT_VERSION
    )

    /** Writes the export to app-private external files dir; caller shares it via a share sheet. */
    suspend fun exportToFile(directory: File, appVersion: String): File {
        val backup = buildBackup(appVersion)
        val text = json.encodeToString(backup)
        val file = File(directory, "lifeos-backup-${backup.exportedAtEpochMillis}.json")
        file.writeText(text)
        return file
    }

    /**
     * Decodes and validates a backup file before restoring. Throws with a
     * user-facing message when the file is oversized, not a LifeOS backup, or
     * was written by a newer app version.
     */
    suspend fun importFromFile(file: File) {
        if (LifeOSBackup.isOversized(file.length())) {
            throw IllegalArgumentException("This backup file is too large to restore.")
        }
        val backup = try {
            json.decodeFromString(LifeOSBackup.serializer(), file.readText())
        } catch (e: Exception) {
            throw IllegalArgumentException("This file is not a valid LifeOS backup.", e)
        }
        val error = validateBackup(backup)
        if (error != null) throw IllegalArgumentException(error)
        restore(backup)
    }

    suspend fun restore(backup: LifeOSBackup) = database.withTransaction {
        taskRepo.restoreFromBackup(backup.tasks)
        habitRepo.restoreFromBackup(backup.habits, backup.habitCompletions)
        expenseRepo.restoreFromBackup(backup.expenses)
        diaryRepo.restoreFromBackup(backup.diaryEntries)
    }
}
