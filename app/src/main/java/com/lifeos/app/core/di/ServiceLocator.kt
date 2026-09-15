package com.lifeos.app.core.di

import android.content.Context
import com.lifeos.app.core.ai.AiRepository
import com.lifeos.app.core.intelligence.LifeOSIntelligenceEngine
import com.lifeos.app.core.security.AppLockManager
import com.lifeos.app.core.util.SettingsStore
import com.lifeos.app.data.db.AppDatabase
import com.lifeos.app.data.repository.BackupRepository
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.NoteRepository
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
    private val database = AppDatabase.getInstance(appContext)

    val settingsStore = SettingsStore(appContext)
    val appLockManager = AppLockManager(appContext)

    val noteRepository = NoteRepository(database.noteDao())
    val taskRepository = TaskRepository(database.taskDao(), appContext)
    val habitRepository = HabitRepository(database.habitDao(), database.habitCompletionDao(), appContext)
    val expenseRepository = ExpenseRepository(database.expenseDao())
    val diaryRepository = DiaryRepository(database.diaryDao())
    val captureRepository = CaptureRepository(database.captureDao())

    val backupRepository = BackupRepository(
        noteRepository, taskRepository, habitRepository, expenseRepository, diaryRepository, captureRepository
    )

    // "Ask LifeOS AI" runs entirely on-device — see core/intelligence/LifeOSIntelligenceEngine.kt.
    // No API key, no network client, no external endpoint anywhere in this dependency graph.
    private val intelligenceEngine = LifeOSIntelligenceEngine(
        noteRepository, taskRepository, habitRepository, diaryRepository, expenseRepository, captureRepository
    )
    val aiRepository = AiRepository(intelligenceEngine)

    val getHomeSummaryUseCase = GetHomeSummaryUseCase(taskRepository, habitRepository, expenseRepository)
    val buildTimelineUseCase = BuildTimelineUseCase(
        noteRepository, taskRepository, habitRepository, expenseRepository, diaryRepository, captureRepository
    )

    companion object {
        @Volatile private var INSTANCE: ServiceLocator? = null

        fun get(context: Context): ServiceLocator =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServiceLocator(context).also { INSTANCE = it }
            }
    }
}
