package com.lifeos.app.core.reminders

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules a single, precise WorkManager job per reminder rather than a
 * recurring poll — cheaper on battery and exact to the minute the user
 * picked. Call [scheduleTaskReminder]/[scheduleHabitReminder] whenever a
 * reminder time is set or changed; call the matching cancel function when
 * it's cleared or the item is deleted/completed.
 *
 * A global "reminders enabled" flag (mirrored in plain SharedPreferences so
 * scheduling code can read it synchronously) lets the Settings toggle silence
 * everything at once. See SettingsViewModel.setRemindersEnabled.
 */
object ReminderScheduler {

    private const val PREFS = "lifeos_reminders"
    private const val KEY_ENABLED = "reminders_enabled"
    const val TAG_REMINDERS = "lifeos_reminders"

    private fun workNameForTask(taskId: String) = "task-reminder-$taskId"
    private fun workNameForHabit(habitId: String) = "habit-reminder-$habitId"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun areRemindersEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setRemindersEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) cancelAllReminders(context)
    }

    /** Cancels every reminder job LifeOS has scheduled. */
    fun cancelAllReminders(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_REMINDERS)
    }

    fun scheduleTaskReminder(context: Context, taskId: String, triggerAtEpochMillis: Long) {
        schedule(context, workNameForTask(taskId), ReminderWorker.TYPE_TASK, taskId, triggerAtEpochMillis)
    }

    fun scheduleHabitReminder(context: Context, habitId: String, triggerAtEpochMillis: Long) {
        schedule(context, workNameForHabit(habitId), ReminderWorker.TYPE_HABIT, habitId, triggerAtEpochMillis)
    }

    fun cancelTaskReminder(context: Context, taskId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workNameForTask(taskId))
    }

    fun cancelHabitReminder(context: Context, habitId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workNameForHabit(habitId))
    }

    private fun schedule(context: Context, uniqueWorkName: String, type: String, id: String, triggerAtEpochMillis: Long) {
        // Respect the global toggle even if a caller forgets to check it.
        if (!areRemindersEnabled(context)) return
        val delayMillis = (triggerAtEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val data = Data.Builder()
            .putString(ReminderWorker.KEY_TYPE, type)
            .putString(ReminderWorker.KEY_ID, id)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(TAG_REMINDERS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, request)
    }
}
