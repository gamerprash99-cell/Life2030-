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
 */
object ReminderScheduler {

    private fun workNameForTask(taskId: String) = "task-reminder-$taskId"
    private fun workNameForHabit(habitId: String) = "habit-reminder-$habitId"

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
        val delayMillis = (triggerAtEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val data = Data.Builder()
            .putString(ReminderWorker.KEY_TYPE, type)
            .putString(ReminderWorker.KEY_ID, id)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, request)
    }
}
