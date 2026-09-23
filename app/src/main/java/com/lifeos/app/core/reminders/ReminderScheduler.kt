package com.lifeos.app.core.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.lifeos.app.data.db.entities.ReminderEntity

/**
 * Projects enabled [ReminderEntity] rows onto real `AlarmManager` alarms.
 *
 * Scheduling is **exact-when-permitted**: LifeOS asks for Android's
 * `SCHEDULE_EXACT_ALARM` special access (user-enabled in the system
 * "Alarms & reminders" page via Settings / the task-reminder permission gate),
 * and arms each reminder with `setExactAndAllowWhileIdle(RTC_WAKEUP)` whenever
 * that access is granted or not required (API < 31). When exact access is
 * denied (or revoked mid-flight), the scheduler gracefully falls back to the
 * permission-free, Doze-aware `setAndAllowWhileIdle` — so reminders still
 * deliver (possibly batched, e.g. once-per-~15-min in Doze), nothing ever
 * throws a `SecurityException`, and no reminder is left as a silent zombie.
 *
 * A single alarm is scheduled per reminder; when it fires [AlarmReceiver]
 * either re-arms the next occurrence (DAILY/WEEKDAYS) or disables the
 * reminder (ONCE). Re-indexing after reboot / package update / time change is
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
     * Shared helper for the Settings state readout and the task-reminder
     * permission gate. Exact alarms are always available below Android 12
     * (the special access simply doesn't exist there); from Android 12 on,
     * exact scheduling requires the user's `SCHEDULE_EXACT_ALARM` grant.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    /**
     * Arms (or re-arms) the alarm for [reminder]. The alarm is armed at the
     * snooze-return time when the user is snoozing, otherwise at the next real
     * occurrence. Uses `setExactAndAllowWhileIdle` when exact scheduling is
     * permitted (or not required), otherwise falls back to the permission-free
     * `setAndAllowWhileIdle` — both are Doze-aware, and the fallback guarantees
     * this entry point never throws for a missing exact-alarm grant. Using the
     * same PendingIntent identity as [cancel] guarantees a re-schedule replaces
     * any previously armed alarm for this reminder.
     */
    fun schedule(context: Context, reminder: ReminderEntity) {
        if (!areRemindersEnabled(context) || !reminder.enabled) return
        val triggerAt = reminder.snoozeReturnAtEpochMillis ?: reminder.nextTriggerAtEpochMillis
        if (triggerAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntentFor(context, reminder.id)
        if (canScheduleExactAlarms(context)) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } catch (_: SecurityException) {
                // Access was revoked between the check and the call — fall back
                // to the permission-free inexact API rather than failing.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            // Permission-free fallback: reminders still deliver, possibly with
            // the system's normal (Doze-aware) batching.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
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