package com.lifeos.app.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Central notification handling for Task/Habit reminders (Section 9/11).
 * All reminders originate from data the user themselves set (a due date, a
 * habit reminder time) — LifeOS never notifies about anything the user
 * didn't explicitly schedule.
 */
object NotificationHelper {
    const val CHANNEL_ID = "lifeos_reminders"
    private const val CHANNEL_NAME = "LifeOS Reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for your tasks and habits"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showReminder(context: Context, notificationId: Int, title: String, body: String) {
        ensureChannel(context)
        val hasPostPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            PermissionManager.hasPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        if (!hasPostPermission) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // swap for a branded icon asset
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasPostPermission) {
                notify(notificationId, notification)
            }
        }
    }
}
