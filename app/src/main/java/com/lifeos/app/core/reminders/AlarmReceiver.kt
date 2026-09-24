package com.lifeos.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.lifeos.app.core.di.ServiceLocator
import com.lifeos.app.core.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives every fired coalesced alarm (explicit PendingIntent only — no
 * intent filter, so no other app can trigger it) and delivers the event in
 * two steps:
 *
 *  1. **Present instantly from extras** — start [AlarmPlaybackService], which
 *     owns the audible ring/vibration and posts one high-priority notification
 *     with a full-screen intent. Nothing here touches the database, so the
 *     alarm fires immediately even on a cold start in deep Doze (starting a
 *     foreground service from a `setAlarmClock` alarm is platform-permitted).
 *  2. **Advance the state machine** — `goAsync()` keeps the broadcast alive
 *     while the repository runs [handleEventFired] for every reminder in the
 *     event and re-projects the whole armed alarm set.
 *
 * If the foreground-service start itself is refused, the notification fallback
 * (with the event's own sound flags) still posts in this step 1 branch, so the
 * user is always alerted exactly once.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = AlarmEventCodec.from(intent) ?: return
        val appContext = context.applicationContext
        val notificationId = event.id.hashCode()

        val serviceStarted = runCatching {
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, AlarmPlaybackService::class.java).apply {
                    action = AlarmIntents.ACTION_ALARM_EVENT
                    AlarmEventCodec.into(this, event)
                }
            )
        }.isSuccess
        if (!serviceStarted) {
            NotificationHelper.showReminder(
                context = appContext,
                notificationId = notificationId,
                title = AlarmStrings.titleOf(appContext, event),
                body = AlarmStrings.bodyOf(appContext, event),
                fullScreenIntent = AlarmIntents.fullScreenPendingIntent(appContext, event),
                sound = event.anySoundEnabled,
                vibration = event.anyVibrationEnabled
            )
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ServiceLocator.get(appContext)
                    .reminderRepository
                    .handleEventFired(event.items.map { it.reminderId }.toSet())
            } catch (_: Exception) {
                // Never crash the broadcast for a delivery hiccup; the next
                // occurrence (if any) re-arms on the next projection pass.
            } finally {
                pending.finish()
            }
        }
    }
}