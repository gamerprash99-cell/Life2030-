package com.lifeos.app

import android.app.Application
import com.lifeos.app.core.di.ServiceLocator
import com.lifeos.app.core.util.NotificationHelper
import com.lifeos.app.core.util.StartupTrace
import com.lifeos.app.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Startup state of the encrypted database. Nothing in the UI that reads a
 * repository may render before [Ready]: the DI container is built lazily (it
 * never touches the database), then the heavy open (SQLCipher native load +
 * Keystore passphrase + Room build + one-time PBKDF2 key derivation) runs on a
 * background thread, and only then is the main UI composed.
 */
sealed interface DatabaseInit {
    data object Initializing : DatabaseInit
    data object Ready : DatabaseInit
    data class Error(val cause: Throwable) : DatabaseInit
}

class LifeOSApplication : Application() {

    /**
     * Never null once [onCreate] returns — constructing the container is cheap
     * because the database is opened lazily on a background thread (see
     * [DatabaseInit] and [ServiceLocator]).
     */
    var serviceLocator: ServiceLocator? = null
        private set

    var initializationError: Throwable? = null
        private set

    private val _databaseState = MutableStateFlow<DatabaseInit>(DatabaseInit.Initializing)
    val databaseState: StateFlow<DatabaseInit> = _databaseState.asStateFlow()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        StartupTrace.section("lifeos:Application.onCreate") {

            // Cheap: builds the DI container. Every ServiceLocator member is
            // `by lazy`, so this cannot touch SQLCipher, the Keystore or Room —
            // see ServiceLocator's comment for the bug this replaced.
            val locator = runCatching {
                StartupTrace.section("lifeos:di.build") { ServiceLocator.get(this) }
            }
                .onSuccess { serviceLocator = it }
                .onFailure { initializationError = it; _databaseState.value = DatabaseInit.Error(it) }
                .getOrNull()
            if (locator == null) return@section

            // Notification channel creation is a binder round-trip; never block the
            // main thread on it during startup.
            appScope.launch { runCatching { NotificationHelper.ensureChannel(this@LifeOSApplication) } }

            // Pre-load SettingsStore's DataStore value cache so the first composition
            // reads are served from memory instead of a first-frame file read (which
            // would otherwise flash onboarding or the app-lock screen).
            appScope.launch { runCatching { locator.settingsStore.warmUp() } }

            // Heavy: SQLCipher native load, Keystore passphrase, Room build and the
            // one-time PBKDF2 open. The UI stays on the native splash until this
            // completes, then databaseState flips to Ready.
            launchDatabaseOpen()
        }
    }

    private fun launchDatabaseOpen() {
        appScope.launch(Dispatchers.IO) {
            StartupTrace.beginAsync("lifeos:db.open")
            try {
                AppDatabase.warmUpOpen(this@LifeOSApplication)
                // Flip to Ready first so the native splash can dismiss and the
                // UI can draw. Re-arming surviving reminders is then done
                // NON-blockingly — it must never delay first content
                // (BootReceiver already covers reboot / time-change / update).
                _databaseState.value = DatabaseInit.Ready
                serviceLocator?.let { locator ->
                    appScope.launch { runCatching { locator.reminderRepository.rebuildAllActive() } }
                }
            } catch (t: Throwable) {
                initializationError = t
                _databaseState.value = DatabaseInit.Error(t)
            } finally {
                StartupTrace.endAsync("lifeos:db.open")
            }
        }
    }

    /** Retries initialization, e.g. after the user returns from system settings. */
    fun retryInitialization(): Boolean {
        if (serviceLocator == null) {
            val ok = runCatching { ServiceLocator.get(this) }
                .onSuccess { serviceLocator = it; initializationError = null; launchDatabaseOpen() }
                .onFailure { initializationError = it; _databaseState.value = DatabaseInit.Error(it) }
                .isSuccess
            return ok
        }
        _databaseState.value = DatabaseInit.Initializing
        launchDatabaseOpen()
        return true
    }
}