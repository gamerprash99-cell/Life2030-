package com.lifeos.app.core.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Notification-listener access — the special app-access grant that lets
 * [com.lifeos.app.core.transactions.TransactionCaptureService] see payment
 * notifications.
 *
 * This is deliberately *not* modelled as a runtime permission. It has no
 * `requestPermissions` equivalent: the only honest way to ask is to send the
 * user to the system screen, so the app asks in words and opens the screen, and
 * reports what it finds when it comes back.
 *
 * The check is a package-level grant rather than "is this exact component
 * bound", which is what the platform exposes without private APIs. It is the
 * same signal Play's own pre-install checks use.
 */
object NotificationAccess {

    /**
     * True when the user has granted notification access to this app's package.
     *
     * @return false when the platform cannot answer, rather than throwing: an
     *   unanswerable question must not look like a grant.
     */
    fun isGranted(context: Context): Boolean = runCatching {
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }.getOrDefault(false)

    /** The screen where notification access is granted and revoked. */
    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Deep link to this app's own entry on that screen, which skips the list the
     * user would otherwise have to scroll. The per-component form only exists
     * from API 30, so older devices get the list instead of an intent that
     * resolves to nothing.
     */
    fun appSettingsIntent(context: Context): Intent {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, com.lifeos.app.core.transactions.TransactionCaptureService::class.java)
                        .flattenToString()
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (detail.resolveActivity(context.packageManager) != null) return detail
        }
        return settingsIntent()
    }
}