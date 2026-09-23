package com.lifeos.app.core.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifeos.app.core.security.PinHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lifeos_settings")

enum class AppLockType { NONE, PIN }

/** Result of an App Lock unlock attempt, used to drive lockout UX. */
sealed interface PinAttemptResult {
    data object Success : PinAttemptResult
    data class Incorrect(val attemptsRemaining: Int) : PinAttemptResult
    data class LockedOut(val remainingMillis: Long) : PinAttemptResult
}

/**
 * Result of a recovery-answer verification attempt. The recovery answer is the
 * bypass for the PIN lockout, so it is throttled at least as strictly as the
 * PIN itself (see [SettingsStore.attemptRecoveryAnswer]).
 */
sealed interface RecoveryAttemptResult {
    data object Success : RecoveryAttemptResult
    data class Incorrect(val attemptsRemaining: Int) : RecoveryAttemptResult
    data class LockedOut(val remainingMillis: Long) : RecoveryAttemptResult
}

/**
 * Central app settings — Section 59 (Settings screen). No settings are
 * ever synced off-device except through the explicit Backup/Export flow.
 *
 * SECURITY NOTE: the App PIN and recovery answer are NEVER stored in
 * plaintext — only a random salt and the derived hash of each (see
 * core/security/PinHasher.kt). This class never exposes the raw PIN or
 * recovery answer back out; only verification is possible. Wrong PIN
 * attempts are throttled with an escalating lockout after N failures so a
 * short numeric PIN cannot be brute-forced through repeated tapping.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val AUTO_LOCK_ENABLED = booleanPreferencesKey("auto_lock_enabled")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_PHOTO_URI = stringPreferencesKey("profile_photo_uri")

        val APP_LOCK_TYPE = stringPreferencesKey("app_lock_type")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val RECOVERY_QUESTION = stringPreferencesKey("recovery_question")
        val RECOVERY_ANSWER_SALT = stringPreferencesKey("recovery_answer_salt")
        val RECOVERY_ANSWER_HASH = stringPreferencesKey("recovery_answer_hash")

        val PIN_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        val PIN_LOCKOUT_UNTIL = longPreferencesKey("pin_lockout_until")
        val RECOVERY_FAILED_ATTEMPTS = intPreferencesKey("recovery_failed_attempts")
        val RECOVERY_LOCKOUT_UNTIL = longPreferencesKey("recovery_lockout_until")
    }

    companion object {
        /** LifeOS App Lock uses exactly this many digits. */
        const val PIN_LENGTH = 4

        private const val MAX_ATTEMPTS = 5
        private const val RECOVERY_MAX_ATTEMPTS = 3
        private const val BASE_LOCKOUT_MS = 30_000L
        private const val MAX_LOCKOUT_MS = 16 * 60 * 1000L
    }

    val darkThemeEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_THEME_ENABLED] ?: false }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    val autoLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_LOCK_ENABLED] ?: true }
    val remindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.REMINDERS_ENABLED] ?: true }
    val profileName: Flow<String?> = context.dataStore.data.map { it[Keys.PROFILE_NAME] }
    val profilePhotoUri: Flow<String?> = context.dataStore.data.map { it[Keys.PROFILE_PHOTO_URI] }

    /**
     * Reads the settings file once so subsequent collectors get a cached value
     * instead of paying the first file read (and any false onboarding/lock
     * flash) during the first composition. Call from a background scope at
     * startup.
     */
    suspend fun warmUp() {
        context.dataStore.data.first()
    }

    val appLockType: Flow<AppLockType> = context.dataStore.data.map {
        when (it[Keys.APP_LOCK_TYPE]) {
            AppLockType.PIN.name -> AppLockType.PIN
            // A previously configured biometric lock is intentionally treated as
            // disabled after the biometric option was removed. It cannot silently
            // become a PIN because no PIN secret exists for that old configuration.
            else -> AppLockType.NONE
        }
    }
    val recoveryQuestion: Flow<String?> = context.dataStore.data.map { it[Keys.RECOVERY_QUESTION] }

    suspend fun setDarkThemeEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.DARK_THEME_ENABLED] = enabled }

    suspend fun setOnboardingComplete(complete: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    suspend fun setAutoLockEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.AUTO_LOCK_ENABLED] = enabled }
    suspend fun setRemindersEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }

    suspend fun setProfileName(name: String) = context.dataStore.edit {
        if (name.isBlank()) it.remove(Keys.PROFILE_NAME) else it[Keys.PROFILE_NAME] = name.trim()
    }

    /** Persists the profile photo content URI (already granted persistable read access by the picker). */
    suspend fun setProfilePhotoUri(uri: String?) = context.dataStore.edit {
        if (uri.isNullOrBlank()) it.remove(Keys.PROFILE_PHOTO_URI) else it[Keys.PROFILE_PHOTO_URI] = uri
    }

    /**
     * Enables PIN App Lock with a mandatory recovery question, so a forgotten
     * PIN doesn't lock the user out permanently. The PIN must be exactly
     * [PIN_LENGTH] digits.
     */
    suspend fun enablePinLock(pin: String, recoveryQuestion: String, recoveryAnswer: String) {
        require(pin.length == PIN_LENGTH && pin.all(Char::isDigit)) {
            "App Lock PIN must be exactly $PIN_LENGTH digits."
        }
        val pinHash = PinHasher.hash(pin)
        val answerHash = PinHasher.hash(recoveryAnswer.trim().lowercase())
        context.dataStore.edit {
            it[Keys.APP_LOCK_TYPE] = AppLockType.PIN.name
            it[Keys.PIN_SALT] = pinHash.saltBase64
            it[Keys.PIN_HASH] = pinHash.hashBase64
            it[Keys.RECOVERY_QUESTION] = recoveryQuestion
            it[Keys.RECOVERY_ANSWER_SALT] = answerHash.saltBase64
            it[Keys.RECOVERY_ANSWER_HASH] = answerHash.hashBase64
            it[Keys.PIN_FAILED_ATTEMPTS] = 0
            it.remove(Keys.PIN_LOCKOUT_UNTIL)
            it[Keys.RECOVERY_FAILED_ATTEMPTS] = 0
            it.remove(Keys.RECOVERY_LOCKOUT_UNTIL)
        }
    }

    /** Disables App Lock entirely, wiping any stored PIN/recovery data. */
    suspend fun disableAppLock() = context.dataStore.edit {
        it[Keys.APP_LOCK_TYPE] = AppLockType.NONE.name
        it.remove(Keys.PIN_SALT); it.remove(Keys.PIN_HASH)
        it.remove(Keys.RECOVERY_QUESTION); it.remove(Keys.RECOVERY_ANSWER_SALT); it.remove(Keys.RECOVERY_ANSWER_HASH)
        it.remove(Keys.PIN_FAILED_ATTEMPTS); it.remove(Keys.PIN_LOCKOUT_UNTIL)
        it.remove(Keys.RECOVERY_FAILED_ATTEMPTS); it.remove(Keys.RECOVERY_LOCKOUT_UNTIL)
    }

    /**
     * Verify the PIN with brute-force throttling. Each failure is recorded;
     * after [MAX_ATTEMPTS] failures the check refuses to run until the current
     * lockout window (which grows on every repeated failure) has elapsed.
     */
    suspend fun attemptPinUnlock(enteredPin: String): PinAttemptResult {
        val prefs = context.dataStore.data.first()
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs[Keys.PIN_LOCKOUT_UNTIL] ?: 0L

        if (now < lockoutUntil) {
            return PinAttemptResult.LockedOut(lockoutUntil - now)
        }

        val salt = prefs[Keys.PIN_SALT] ?: return PinAttemptResult.Incorrect(0)
        val hash = prefs[Keys.PIN_HASH] ?: return PinAttemptResult.Incorrect(0)

        if (PinHasher.verify(enteredPin, salt, hash)) {
            context.dataStore.edit {
                it[Keys.PIN_FAILED_ATTEMPTS] = 0
                it.remove(Keys.PIN_LOCKOUT_UNTIL)
            }
            return PinAttemptResult.Success
        }

        val failures = (prefs[Keys.PIN_FAILED_ATTEMPTS] ?: 0) + 1
        val duration = LockoutPolicy.lockoutDurationMillis(failures, MAX_ATTEMPTS, BASE_LOCKOUT_MS, MAX_LOCKOUT_MS)
        if (duration > 0L) {
            val until = now + duration
            context.dataStore.edit {
                it[Keys.PIN_FAILED_ATTEMPTS] = failures
                it[Keys.PIN_LOCKOUT_UNTIL] = until
            }
            return PinAttemptResult.LockedOut(duration)
        }

        context.dataStore.edit { it[Keys.PIN_FAILED_ATTEMPTS] = failures }
        return PinAttemptResult.Incorrect(LockoutPolicy.attemptsRemaining(failures, MAX_ATTEMPTS))
    }

    /**
     * Verify the recovery answer with the same escalating lockout policy the
     * PIN uses (fewer attempts, since this answer is the PIN's bypass). A
     * lockout on either secret does not by itself lock the other, but a failed
     * recovery attempt here is throttled so the answer cannot be brute-forced
     * once the PIN lockout has driven a user to this screen.
     */
    suspend fun attemptRecoveryAnswer(enteredAnswer: String): RecoveryAttemptResult {
        val prefs = context.dataStore.data.first()
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs[Keys.RECOVERY_LOCKOUT_UNTIL] ?: 0L

        if (now < lockoutUntil) {
            return RecoveryAttemptResult.LockedOut(lockoutUntil - now)
        }

        val salt = prefs[Keys.RECOVERY_ANSWER_SALT] ?: return RecoveryAttemptResult.Incorrect(0)
        val hash = prefs[Keys.RECOVERY_ANSWER_HASH] ?: return RecoveryAttemptResult.Incorrect(0)

        if (PinHasher.verify(enteredAnswer.trim().lowercase(), salt, hash)) {
            context.dataStore.edit {
                it[Keys.RECOVERY_FAILED_ATTEMPTS] = 0
                it.remove(Keys.RECOVERY_LOCKOUT_UNTIL)
            }
            return RecoveryAttemptResult.Success
        }

        val failures = (prefs[Keys.RECOVERY_FAILED_ATTEMPTS] ?: 0) + 1
        val duration = LockoutPolicy.lockoutDurationMillis(failures, RECOVERY_MAX_ATTEMPTS, BASE_LOCKOUT_MS, MAX_LOCKOUT_MS)
        if (duration > 0L) {
            val until = now + duration
            context.dataStore.edit {
                it[Keys.RECOVERY_FAILED_ATTEMPTS] = failures
                it[Keys.RECOVERY_LOCKOUT_UNTIL] = until
            }
            return RecoveryAttemptResult.LockedOut(duration)
        }

        context.dataStore.edit { it[Keys.RECOVERY_FAILED_ATTEMPTS] = failures }
        return RecoveryAttemptResult.Incorrect(LockoutPolicy.attemptsRemaining(failures, RECOVERY_MAX_ATTEMPTS))
    }
}