package com.lifeos.app.data.repository

import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.NoteEntity
import com.lifeos.app.data.db.entities.TaskEntity
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
    val notes: List<NoteEntity>,
    val tasks: List<TaskEntity>,
    val habits: List<HabitEntity>,
    val habitCompletions: List<HabitCompletionEntity>,
    val expenses: List<ExpenseEntity>,
    val diaryEntries: List<DiaryEntity>,
    val captures: List<CaptureEntity>
)

class BackupRepository(
    private val noteRepo: NoteRepository,
    private val taskRepo: TaskRepository,
    private val habitRepo: HabitRepository,
    private val expenseRepo: ExpenseRepository,
    private val diaryRepo: DiaryRepository,
    private val captureRepo: CaptureRepository
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun buildBackup(appVersion: String): LifeOSBackup = LifeOSBackup(
        exportedAtEpochMillis = System.currentTimeMillis(),
        appVersion = appVersion,
        notes = noteRepo.getAllForBackup(),
        tasks = taskRepo.getAllForBackup(),
        habits = habitRepo.getAllForBackup(),
        habitCompletions = habitRepo.getAllCompletionsForBackup(),
        expenses = expenseRepo.getAllForBackup(),
        diaryEntries = diaryRepo.getAllForBackup(),
        captures = captureRepo.getAllForBackup()
    )

    /** Writes the export to app-private external files dir; caller shares it via a share sheet. */
    suspend fun exportToFile(directory: File, appVersion: String): File {
        val backup = buildBackup(appVersion)
        val text = json.encodeToString(backup)
        val file = File(directory, "lifeos-backup-${backup.exportedAtEpochMillis}.json")
        file.writeText(text)
        return file
    }

    suspend fun importFromFile(file: File) {
        val backup = json.decodeFromString(LifeOSBackup.serializer(), file.readText())
        restore(backup)
    }

    suspend fun restore(backup: LifeOSBackup) {
        noteRepo.restoreFromBackup(backup.notes)
        taskRepo.restoreFromBackup(backup.tasks)
        habitRepo.restoreFromBackup(backup.habits, backup.habitCompletions)
        expenseRepo.restoreFromBackup(backup.expenses)
        diaryRepo.restoreFromBackup(backup.diaryEntries)
        captureRepo.restoreFromBackup(backup.captures)
    }
}
