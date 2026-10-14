package com.lifeos.app.core.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifeos.app.core.di.ServiceLocator
import com.lifeos.app.core.util.NotificationHelper

/**
 * Fires exactly one reminder notification for either a Task or a Habit,
 * then exits — scheduling is per-item (see ReminderScheduler), not a
 * recurring poll, so reminders are precise and battery-friendly.
 */
class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val type = inputData.getString(KEY_TYPE) ?: return Result.failure()
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val locator = ServiceLocator.get(applicationContext)

        return try {
            when (type) {
                TYPE_TASK -> {
                    val task = locator.taskRepository.getById(id) ?: return Result.success()
                    if (!task.isCompleted && !task.isDeleted) {
                        NotificationHelper.showReminder(
                            applicationContext,
                            notificationId = id.hashCode(),
                            title = "Task reminder",
                            body = task.title
                        )
                    }
                }
                TYPE_HABIT -> {
                    val habit = locator.habitRepository.getById(id) ?: return Result.success()
                    if (!habit.isArchived) {
                        NotificationHelper.showReminder(
                            applicationContext,
                            notificationId = id.hashCode(),
                            title = "Habit reminder",
                            body = "Time for: ${habit.name}"
                        )
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_ID = "id"
        const val TYPE_TASK = "task"
        const val TYPE_HABIT = "habit"
    }
}
