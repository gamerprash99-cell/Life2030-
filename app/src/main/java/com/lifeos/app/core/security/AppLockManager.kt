package com.lifeos.app.core.security

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * App Lock — biometric path. Uses the device's own biometric/PIN
 * enrollment via Android's official BiometricPrompt API; LifeOS never
 * stores or sees the actual fingerprint/face data — Android's secure
 * biometric hardware handles matching, and only a success/failure result
 * is returned to this app.
 *
 * IMPORTANT PLATFORM REALITY (documented here so it's never overstated in
 * the UI): Android does not allow apps to register their own, separate
 * biometric enrollment — BIOMETRIC_WEAK/DEVICE_CREDENTIAL always verifies
 * against whatever fingerprint/face/PIN is enrolled at the OS level for
 * this device. This is true for every Android app that uses BiometricPrompt
 * (banking apps, password managers, etc.), not a LifeOS-specific gap. A
 * user who wants a LifeOS-only secret that's independent of "anyone who
 * can unlock this phone" should choose the separate App PIN option
 * instead (see AppLockType.PIN, core/util/SettingsStore.kt) — the Setup
 * screen explains this distinction to the user directly.
 */
class AppLockManager(private val context: Context) {

    /** Returns true only when a biometric is actually enrolled and usable. */
    fun isBiometricAvailable(): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    /** Opens the Android-managed biometric enrollment screen when supported. */
    fun openBiometricEnrollment(activity: FragmentActivity): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return false
        return runCatching {
            activity.startActivity(Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            })
            true
        }.getOrDefault(false)
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onFailed()
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock LifeOS")
            .setSubtitle("Your personal data is protected")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(promptInfo)
    }
}
