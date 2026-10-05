package com.lifeos.app.core.di

import android.content.Context
import com.lifeos.app.core.location.DeviceLocationProvider
import com.lifeos.app.core.util.SettingsStore
import com.lifeos.app.data.db.AppDatabase
import com.lifeos.app.data.repository.BackupRepository
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.ReminderRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.data.repository.OfflineOnlyWeatherSource
import com.lifeos.app.data.repository.WeatherRepository
import com.lifeos.app.data.repository.WeatherRepositoryImpl
import com.lifeos.app.domain.usecase.BuildTimelineUseCase
import com.lifeos.app.domain.usecase.CaptureTransactionUseCase
import com.lifeos.app.domain.usecase.GetDiaryCalendarUseCase
import com.lifeos.app.domain.usecase.GetDiaryInsightsUseCase
import com.lifeos.app.domain.usecase.GetHomeSummaryUseCase
import com.lifeos.app.domain.usecase.SearchDiaryEntriesUseCase

/**
 * A single, simple, hand-written DI container. Deliberately not Hilt/Dagger —
 * for a scaffold of this size, manual DI is far less likely to break the
 * build (no KSP/annotation-processor version coupling) while still keeping
 * every dependency created in exactly one place. Swap for Hilt later if the
 * team prefers, without touching any ViewModel signatures.
 */
class ServiceLocator private constructor(context: Context) {

    /**
     * The application context, for the few collaborators that need one (the
     * diary recorder writes into app-private storage). Exposed rather than
     * private so a ViewModel factory can build them without the container
     * having to own short-lived, per-screen instances.
     */
    val appContext: Context = context.applicationContext

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
    private val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

    /**
     * STARTUP: every member below MUST stay `by lazy`.
     *
     * `ServiceLocator.get()` is called on the main thread from
     * `LifeOSApplication.onCreate()`, before the first frame. These used to be
     * eager `val`s, and because each of them dereferences [database] (directly,
     * or via a repository that already holds a DAO) the very first assignment
     * forced `AppDatabase.getInstance()` on the main thread — SQLCipher native
     * library load, Keystore load + AES/GCM passphrase unwrap, SharedPreferences
     * read and the Room build, all before `setContent`. That was the dominant
     * cost of the multi-second splash.
     *
     * Lazy also means a feature nobody opens never pays for its repository:
     * nothing is created until a screen actually asks for it, and
     * `LifeOSApplication.launchDatabaseOpen` performs the single warm-up on a
     * background coroutine.
     */
    val settingsStore: SettingsStore by lazy { SettingsStore(appContext) }

    /** Created before the task/habit repositories, which delegate their alarm wiring to it. */
    val reminderRepository: ReminderRepository by lazy { ReminderRepository(database.reminderDao(), appContext) }

    val taskRepository: TaskRepository by lazy { TaskRepository(database.taskDao(), reminderRepository) }
    val habitRepository: HabitRepository by lazy {
        HabitRepository(database.habitDao(), database.habitCompletionDao(), reminderRepository)
    }
    val expenseRepository: ExpenseRepository by lazy { ExpenseRepository(database.expenseDao()) }
    val diaryRepository: DiaryRepository by lazy { DiaryRepository(database.diaryDao()) }

    /**
     * Payment-notification capture. Resolved the same way as everything else —
     * lazily, on whichever thread asks — because a listener service can be bound
     * by the system long before the app's own UI has ever run.
     */
    val captureTransactionUseCase: CaptureTransactionUseCase by lazy {
        CaptureTransactionUseCase(expenseRepository)
    }

    /**
     * Weather for diary entries. Backed by [OfflineOnlyWeatherSource] today:
     * this build declares no INTERNET permission, so the repository reports an
     * honest "unavailable" rather than a fabricated reading. Swapping in an
     * approved source is a one-line change here.
     */
    val weatherRepository: WeatherRepository = WeatherRepositoryImpl(OfflineOnlyWeatherSource())

    /**
     * Real device location for diary entries, via platform `LocationManager`
     * and `Geocoder` only — no Play Services, so the offline-first and
     * privacy-first guarantees hold. Held by the container because it is
     * stateless apart from holding the system service handle.
     */
    val deviceLocationProvider: DeviceLocationProvider by lazy { DeviceLocationProvider(appContext) }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(
            database, taskRepository, habitRepository, expenseRepository, diaryRepository, reminderRepository
        )
    }

    val buildTimelineUseCase: BuildTimelineUseCase by lazy {
        BuildTimelineUseCase(
            taskRepository, habitRepository, expenseRepository, diaryRepository
        )
    }
    val getHomeSummaryUseCase: GetHomeSummaryUseCase by lazy {
        GetHomeSummaryUseCase(taskRepository, habitRepository, expenseRepository, buildTimelineUseCase)
    }

    // ---- Diary search, calendar and local insights ----------------------
    //
    // Stateless objects over [diaryRepository], all `by lazy` like everything
    // else above: the Diary screens that use them are not the startup path, and
    // nothing should touch the database until a screen asks for it.

    val searchDiaryEntriesUseCase: SearchDiaryEntriesUseCase by lazy {
        SearchDiaryEntriesUseCase(diaryRepository)
    }

    val getDiaryCalendarUseCase: GetDiaryCalendarUseCase by lazy {
        GetDiaryCalendarUseCase(diaryRepository)
    }

    /**
     * Local insights. Reads only rows already in Room and computes on
     * `Dispatchers.Default`; there is no network collaborator to configure,
     * which is why this build declares no `INTERNET` permission and needs no
     * API key.
     */
    val getDiaryInsightsUseCase: GetDiaryInsightsUseCase by lazy {
        GetDiaryInsightsUseCase(diaryRepository)
    }

    companion object {
        @Volatile private var INSTANCE: ServiceLocator? = null

        fun get(context: Context): ServiceLocator =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServiceLocator(context).also { INSTANCE = it }
            }
    }
}
