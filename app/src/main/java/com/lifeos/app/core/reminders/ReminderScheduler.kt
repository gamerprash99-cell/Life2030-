package com.lifeos.app.core.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.lifeos.app.data.db.entities.ReminderEntity

/**
 * Projects enabled [ReminderEntity] rows onto real `AlarmManager` exact alarms.
 *
 * Why AlarmManager and not WorkManager: WorkManager defers work under Doze and
 * app-standby bucketing, so a reminder scheduled "now + 2h" can arrive 10s of
 * minutes late or not at all while the app is backgrounded. AlarmManager with
 * `setAlarmClock` is the OS mechanism alarm/clock apps use — it wakes the device,
 * is delivered close to the requested instant, and (with
 * `SCHEDULE_EXACT_ALARM`) is exempt from Doze.
 *
 * A single foreground-style exact alarm is scheduled per reminder; when it fires
 * [AlarmReceiver] either re-arms the next occurrence (DAILY/WEEKDAYS) or disables
 * the reminder (ONCE). Re-indexing after reboot / package update / time change is
 * handled by [BootReceiver] + `LifeOSApplication` re-arm, so a rebooted or
 * re-installed app never silently loses a reminder.
 */
object ReminderScheduler {

    private const val PREFS = "lifeos_reminders"
    private const val KEY_ENABLED = "reminders_enabled"
    private const val KEY_SCHEDULED_IDS = "scheduled_ids"

    fun areRemindersEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setRemindersEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) cancelAll(context)
    }

    /**
     * Arms (or re-arms) the exact alarm for [reminder]. The alarm is armed at the
     * snooze-return time when the user is snoozing, otherwise at the next real
     * occurrence. Using the same PendingIntent identity as [cancel] guarantees a
     * re-schedule replaces any previously armed alarm for this reminder.
     */
    fun schedule(context: Context, reminder: ReminderEntity) {
        if (!areRemindersEnabled(context) || !reminder.enabled) return
        val triggerAt = reminder.snoozeReturnAtEpochMillis ?: reminder.nextTriggerAtEpochMillis
        if (triggerAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntentFor(context, reminder.id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // No exact-alarm permission: fall back to a non-exact but still
            // Doze-aware alarm rather than dropping the reminder entirely.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            // setAlarmClock is exempt from Doze, shows the alarm-clock indicator,
            // and is the strongest priority the platform offers. The PendingIntent
            // doubles as the "show" intent so the system can surface the alarm UI.
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, pendingIntent), pendingIntent)
        }
        addScheduledId(context, reminder.id)
    }

    fun cancel(context: Context, reminderId: String) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(pendingIntentFor(context, reminderId))
        removeScheduledId(context, reminderId)
    }

    /** Cancels every alarm LifeOS has armed (used by the global reminder toggle). */
    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        scheduledIds(context).forEach { alarmManager.cancel(pendingIntentFor(context, it)) }
        prefs(context).edit().remove(KEY_SCHEDULED_IDS).apply()
    }

    /**
     * AlarmManager has no "cancel by tag", so the scheduler keeps a plain
     * SharedPreferences set of every id it has armed. The set is write-through on
     * [schedule]/[cancel] and only used for a global teardown — the actual alarm
     * state always lives in AlarmManager (single source of truth).
     */
    private fun scheduledIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_SCHEDULED_IDS, emptySet()).orEmpty()

    private fun addScheduledId(context: Context, id: String) {
        val updated = scheduledIds(context) + id
        prefs(context).edit().putStringSet(KEY_SCHEDULED_IDS, updated).apply()
    }

    private fun removeScheduledId(context: Context, id: String) {
        val updated = scheduledIds(context) - id
        prefs(context).edit().putStringSet(KEY_SCHEDULED_IDS, updated).apply()
    }

    /**
     * Distinct per reminder. The data URI (rather than just extras) participates
     * in PendingIntent `filterEquals`, so two reminders can never collide even if
     * their hashes do; `FLAG_UPDATE_CURRENT` refreshes the extras on re-arm.
     */
    private fun pendingIntentFor(context: Context, reminderId: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REMINDER
            data = Uri.parse("lifeos://reminder/$reminderId")
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}