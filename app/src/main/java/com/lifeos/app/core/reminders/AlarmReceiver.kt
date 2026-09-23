package com.lifeos.app.core.reminders

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.lifeos.app.core.di.ServiceLocator
import com.lifeos.app.core.util.NotificationHelper
import com.lifeos.app.data.db.entities.ReminderEntity
import com.lifeos.app.data.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives every fired reminder alarm (explicit PendingIntent only — no intent
 * filter) and presents it: runs the repository state machine, posts a high
 * priority alarm notification (carrying a full-screen intent when permitted),
 * and — when the app is in the foreground — surfaces the full-screen alarm UI
 * directly so the alarm starts the moment it fires.
 *
 * `goAsync()` keeps the broadcast alive while the (usually already-open) Room
 * database is read on an IO thread.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val locator = ServiceLocator.get(context.applicationContext)
                locator.reminderRepository.handleFired(reminderId)
                val reminder = locator.reminderRepository.getById(reminderId)
                if (reminder != null && reminder.enabled) {
                    present(context.applicationContext, reminder)
                }
            } catch (_: Exception) {
                // Never crash the broadcast for a delivery hiccup; the next
                // occurrence (if any) will re-arm normally.
            } finally {
                pending.finish()
            }
        }
    }

    private fun present(context: Context, reminder: ReminderEntity) {
        val fullScreenIntent = fullScreenIntent(context, reminder.id)
        NotificationHelper.showReminder(
            context = context,
            notificationId = reminder.id.hashCode(),
            title = if (reminder.entityType == ReminderRepository.TYPE_TASK) "Task reminder" else "Habit reminder",
            body = reminder.title,
            fullScreenIntent = fullScreenIntent,
            sound = reminder.soundEnabled,
            vibration = reminder.vibrationEnabled
        )
        try {
            // Foreground: launch the alarm UI immediately. Background/Doze: the
            // system surfaces the full-screen intent or falls back to a heads-up.
            context.startActivity(
                Intent(context, AlarmFullScreenActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(EXTRA_REMINDER_ID, reminder.id)
                    .putExtra(EXTRA_TITLE, reminder.title)
                    .putExtra(EXTRA_ENTITY_TYPE, reminder.entityType)
                    .putExtra(EXTRA_SOUND_ENABLED, reminder.soundEnabled)
                    .putExtra(EXTRA_VIBRATION_ENABLED, reminder.vibrationEnabled)
            )
        } catch (_: RuntimeException) {
            // Background-activity-start restricted (normal on Android 10+);
            // the notification (with its full-screen intent) is the delivery path.
        }
    }

    private fun fullScreenIntent(context: Context, reminderId: String): PendingIntent {
        val intent = Intent(context, AlarmFullScreenActivity::class.java).apply {
            data = Uri.parse("lifeos://alarm/$reminderId")
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_REMINDER = "com.lifeos.app.action.REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ENTITY_TYPE = "entity_type"
        const val EXTRA_SOUND_ENABLED = "sound_enabled"
        const val EXTRA_VIBRATION_ENABLED = "vibration_enabled"
    }
}