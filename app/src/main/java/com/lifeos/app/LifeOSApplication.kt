package com.lifeos.app

import android.app.Application
import com.lifeos.app.core.di.ServiceLocator
import com.lifeos.app.core.util.NotificationHelper
import com.lifeos.app.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LifeOSApplication : Application() {

    /**
     * Null when the encrypted database could not be opened because its key
     * material is unavailable. In that case the app shows a recovery screen
     * instead of crashing, and — critically — the database is left untouched
     * (see core/security/DatabasePassphraseProvider.kt).
     */
    var serviceLocator: ServiceLocator? = null
        private set

    var initializationError: Throwable? = null
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        runCatching { ServiceLocator.get(this) }
            .onSuccess { serviceLocator = it }
            .onFailure { initializationError = it }
        NotificationHelper.ensureChannel(this)
        // SQLCipher cold-open does a one-time key derivation; warm it on a
        // background thread so the first Home query renders without a stall.
        runCatching { AppDatabase.warmUpOpen(this, appScope) }
    }

    /** Retries initialization, e.g. after the user returns from system settings. */
    fun retryInitialization(): Boolean {
        if (serviceLocator != null) return true
        return runCatching { ServiceLocator.get(this) }
            .onSuccess { serviceLocator = it; initializationError = null }
            .onFailure { initializationError = it }
            .isSuccess
    }
}
