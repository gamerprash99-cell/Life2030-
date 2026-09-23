package com.lifeos.app.core.di

import android.content.Context
import com.lifeos.app.core.util.SettingsStore
import com.lifeos.app.data.db.AppDatabase
import com.lifeos.app.data.repository.BackupRepository
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.ReminderRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.domain.usecase.BuildTimelineUseCase
import com.lifeos.app.domain.usecase.GetHomeSummaryUseCase

/**
 * A single, simple, hand-written DI container. Deliberately not Hilt/Dagger —
 * for a scaffold of this size, manual DI is far less likely to break the
 * build (no KSP/annotation-processor version coupling) while still keeping
 * every dependency created in exactly one place. Swap for Hilt later if the
 * team prefers, without touching any ViewModel signatures.
 */
class ServiceLocator private constructor(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Lazily opened so constructing the container never touches the main
     * thread with SQLCipher's native-library load, the Keystore passphrase
     * read, or the Room builder. Whether the database is stuck is irrelevant:
     * the actual open runs on a background thread at startup (see
     * `LifeOSApplication`), and nothing touches [AppDatabase] until
     * `databaseReady` has emitted.
     *
     * `by lazy` is synchronized by default, so a concurrent first access from
     * two threads still initializes exactly once.
     */
    private val database by lazy { AppDatabase.getInstance(appContext) }

    val settingsStore = SettingsStore(appContext)

    /** Created before the task/habit repositories, which delegate their alarm wiring to it. */
    val reminderRepository = ReminderRepository(database.reminderDao(), appContext)

    val taskRepository = TaskRepository(database.taskDao(), reminderRepository)
    val habitRepository = HabitRepository(database.habitDao(), database.habitCompletionDao(), reminderRepository)
    val expenseRepository = ExpenseRepository(database.expenseDao())
    val diaryRepository = DiaryRepository(database.diaryDao())

    val backupRepository = BackupRepository(
        database, taskRepository, habitRepository, expenseRepository, diaryRepository, reminderRepository
    )

    val buildTimelineUseCase = BuildTimelineUseCase(
        taskRepository, habitRepository, expenseRepository, diaryRepository
    )
    val getHomeSummaryUseCase = GetHomeSummaryUseCase(taskRepository, habitRepository, expenseRepository, buildTimelineUseCase)

    companion object {
        @Volatile private var INSTANCE: ServiceLocator? = null

        fun get(context: Context): ServiceLocator =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServiceLocator(context).also { INSTANCE = it }
            }
    }
}
