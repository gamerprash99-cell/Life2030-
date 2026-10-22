package com.lifeos.app.core.reminders

import android.app.AlarmManager
import android.content.Context
import android.os.Build

/**
 * Projects the coalesced [AlarmEvent] set straight onto real `AlarmManager`
 * alarms — no reminder-level bookkeeping, no per-reminder PendingIntents.
 *
 * Scheduling ladder (exact-when-permitted):
 *  - exact access granted (or pre-S Android): `setAlarmClock`. Alarm-clock
 *    alarms fire exactly on time even in deep Doze (the system exits Doze to
 *    deliver them), are exempt from the "once per 9 minutes per app" limit,
 *    and are permitted to start a foreground service from the background.
 *  - exact access denied on API 31+: `setAndAllowWhileIdle` — the permission-
 *    free, Doze-aware degrade; never throws when exact is refused.
 *
 * One alarm is armed per *event* (a unique trigger time). Re-projecting is
 * idempotent: triggers that left the projection are cancelled, everything that
 * remains is (re)armed with refreshed snapshot extras via FLAG_UPDATE_CURRENT.
 */
object ReminderScheduler {

    private const val PREFS = "lifeos_reminders"
    private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    private const val KEY_SCHEDULED_EVENTS = "scheduled_events"

    fun areRemindersEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REMINDERS_ENABLED, true)

    fun setRemindersEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
        if (!enabled) cancelAll(context)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    /**
     * Reconciles the armed set with [events]: cancels every previously armed
     * event trigger that is no longer desired, then arms/refreshes each event.
     */
    fun reproject(context: Context, events: List<AlarmEvent>) {
        if (!areRemindersEnabled(context)) {
            cancelAll(context)
            return
        }
        val desired = events.mapTo(mutableSetOf()) { it.id }
        val armed = scheduledEvents(context)
        (armed - desired).forEach { cancel(context, it) }
        events.forEach { scheduleEvent(context, it) }
    }

    fun scheduleEvent(context: Context, event: AlarmEvent) {
        if (!areRemindersEnabled(context)) return
        val triggerAt = event.triggerAtEpochMillis
        if (triggerAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = AlarmIntents.eventPendingIntent(context, event)
        if (canScheduleExactAlarms(context)) {
            try {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(
                        triggerAt,
                        AlarmIntents.alarmClockShowPendingIntent(context, event)
                    ),
                    pendingIntent
                )
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
        addScheduledEvent(context, event.id)
    }

    /** Cancels the armed alarm and drops the event's scheduled-book entry. */
    fun cancel(context: Context, eventKey: String) {
        val triggerAt = triggerAtFromKey(eventKey) ?: return
        // Cancelling the broadcast alarm also removes its setAlarmClock entry.
        context.getSystemService(AlarmManager::class.java)
            .cancel(AlarmIntents.eventPendingIntent(context, AlarmEvent(triggerAt, emptyList())))
        removeScheduledEvent(context, eventKey)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        scheduledEvents(context).forEach { eventKey ->
            triggerAtFromKey(eventKey)?.let { triggerAt ->
                alarmManager.cancel(AlarmIntents.eventPendingIntent(context, AlarmEvent(triggerAt, emptyList())))
            }
        }
        clearScheduledEvents(context)
    }

    private fun triggerAtFromKey(eventKey: String): Long? =
        eventKey.removePrefix(AlarmEvent.ID_PREFIX).toLongOrNull()

    private fun scheduledEvents(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_SCHEDULED_EVENTS, emptySet()).orEmpty()

    private fun addScheduledEvent(context: Context, eventKey: String) {
        prefs(context).edit()
            .putStringSet(KEY_SCHEDULED_EVENTS, scheduledEvents(context) + eventKey)
            .apply()
    }

    private fun removeScheduledEvent(context: Context, eventKey: String) {
        prefs(context).edit()
            .putStringSet(KEY_SCHEDULED_EVENTS, scheduledEvents(context) - eventKey)
            .apply()
    }

    private fun clearScheduledEvents(context: Context) {
        prefs(context).edit().remove(KEY_SCHEDULED_EVENTS).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}