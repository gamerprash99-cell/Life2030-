package com.lifeos.app.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Central notification handling for Task/Habit reminders (Section 9/11).
 * All reminders originate from data the user themselves set (a due date, a
 * habit reminder time) — LifeOS never notifies about anything the user
 * didn't explicitly schedule.
 *
 * The reminders channel is HIGH importance (so alarms surface as heads-up even
 * when the full-screen intent cannot be shown) but silent by default: the
 * audible alarm is driven per-notification — [showReminder] attaches the alarm
 * tone/vibration only when the reminder's own sound/vibration flags are on, and
 * the in-app full-screen alarm UI plays the sustained ring.
 */
object NotificationHelper {
    const val CHANNEL_ID = "lifeos_reminders"
    private const val CHANNEL_NAME = "LifeOS Reminders"

    fun ensureChannel(context: Context) {
        // minSdk is 26, so notification channels are always available.
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for your tasks and habits"
            // Sound/vibration are driven per-notification so the alarm UI can
            // sustain the ring without a duplicate channel-level beep.
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Posts a task/habit reminder notification. When [fullScreenIntent] is set,
     * the system promotes it to a full-screen alarm when permitted (and the
     * target API/device supports it), otherwise it degrades to a heads-up.
     */
    fun showReminder(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        fullScreenIntent: PendingIntent? = null,
        sound: Boolean = true,
        vibration: Boolean = true
    ) {
        ensureChannel(context)
        val hasPostPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            PermissionManager.hasPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        if (!hasPostPermission) return

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // swap for a branded icon asset
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
        fullScreenIntent?.let { builder.setFullScreenIntent(it, true) }
        if (sound) {
            builder.setSound(alarmToneUri(context))
        } else {
            builder.setSilent(true)
        }
        if (vibration) {
            builder.setVibrate(longArrayOf(0, 500, 400, 500))
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission was revoked between the check above and the post.
        }
    }

    private fun alarmToneUri(context: Context): Uri? =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
}