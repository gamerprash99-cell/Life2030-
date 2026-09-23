package com.lifeos.app.data.repository

import android.content.Context
import com.lifeos.app.core.reminders.ReminderScheduler
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.core.util.RepeatRuleCalculator
import com.lifeos.app.data.db.dao.TaskDao
import com.lifeos.app.data.db.entities.RepeatRule
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.data.db.entities.TaskPriority
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao, private val appContext: Context) {

    fun observeForDay(epochDay: Long): Flow<List<TaskEntity>> = dao.observeForDay(epochDay)
    fun observeOverdue(
        todayEpochDay: Long,
        nowMinutes: Int = DateTimeUtils.nowMinutesOfDay()
    ): Flow<List<TaskEntity>> = dao.observeOverdue(todayEpochDay, nowMinutes)
    fun observeAll(): Flow<List<TaskEntity>> = dao.observeAll()
    fun observeCountForDay(epochDay: Long): Flow<Int> = dao.observeCountForDay(epochDay)
    fun observeCompletedCountForDay(epochDay: Long): Flow<Int> = dao.observeCompletedCountForDay(epochDay)

    /** Completed tasks whose completion timestamp falls inside [startMillis, endMillis] (Timeline source). */
    suspend fun getCompletedBetween(startMillis: Long, endMillis: Long): List<TaskEntity> =
        dao.getCompletedBetween(startMillis, endMillis)

    suspend fun getById(id: String): TaskEntity? = dao.getById(id)

    suspend fun createTask(
        title: String,
        description: String? = null,
        dueDateEpochDay: Long? = null,
        dueTimeMinutes: Int? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
        category: String? = null,
        reminderEpochMillis: Long? = null,
        repeatRule: RepeatRule = RepeatRule.NONE,
        repeatDaysCsv: String? = null,
        sourceType: String? = null,
        sourceId: String? = null
    ): String {
        val id = IdGenerator.newId()
        val now = System.currentTimeMillis()
        dao.upsert(
            TaskEntity(
                id = id,
                title = title,
                description = description,
                dueDateEpochDay = dueDateEpochDay,
                dueTimeMinutes = dueTimeMinutes,
                priority = priority,
                category = category,
                reminderEpochMillis = reminderEpochMillis,
                repeatRule = repeatRule,
                repeatDaysCsv = repeatDaysCsv,
                sourceType = sourceType,
                sourceId = sourceId,
                createdAt = now,
                updatedAt = now
            )
        )
        if (reminderEpochMillis != null && reminderEpochMillis > System.currentTimeMillis()) {
            ReminderScheduler.scheduleTaskReminder(appContext, id, reminderEpochMillis)
        }
        return id
    }

    suspend fun setCompleted(id: String, completed: Boolean) {
        dao.setCompleted(id, completed, if (completed) System.currentTimeMillis() else null, System.currentTimeMillis())
        if (completed) {
            ReminderScheduler.cancelTaskReminder(appContext, id)
            spawnNextOccurrence(id)
        }
    }

    /**
     * Completing a repeating task creates its next occurrence (daily/weekly/
     * monthly/custom-days). The new task keeps the same details but does not
     * inherit the reminder, so a fresh reminder must be set deliberately.
     */
    private suspend fun spawnNextOccurrence(id: String) {
        val task = dao.getById(id) ?: return
        val rule = task.repeatRule ?: return
        if (rule == RepeatRule.NONE) return

        val today = DateTimeUtils.today().toEpochDay()
        val nextDay = RepeatRuleCalculator.nextOccurrence(
            rule = rule,
            customDaysCsv = task.repeatDaysCsv,
            currentDueEpochDay = task.dueDateEpochDay ?: today,
            todayEpochDay = today
        ) ?: return

        createTask(
            title = task.title,
            description = task.description,
            dueDateEpochDay = nextDay,
            dueTimeMinutes = task.dueTimeMinutes,
            priority = task.priority,
            category = task.category,
            repeatRule = rule,
            repeatDaysCsv = task.repeatDaysCsv,
            sourceType = task.sourceType,
            sourceId = task.sourceId
        )
    }

    suspend fun reschedule(id: String, newEpochDay: Long) = dao.reschedule(id, newEpochDay, System.currentTimeMillis())

    /** Keep for tomorrow — Section 10 "TASK NOT COMPLETED" flow shortcut. */
    suspend fun keepForTomorrow(id: String, todayEpochDay: Long) = reschedule(id, todayEpochDay + 1)

    suspend fun delete(id: String) {
        dao.softDelete(id, System.currentTimeMillis())
        ReminderScheduler.cancelTaskReminder(appContext, id)
    }

    /** Re-registers WorkManager jobs for all future reminders (e.g. after reminders are re-enabled). */
    suspend fun rescheduleAllReminders() {
        val now = System.currentTimeMillis()
        dao.getAllForBackup()
            .filter { !it.isDeleted && !it.isCompleted && it.reminderEpochMillis != null && it.reminderEpochMillis > now }
            .forEach { ReminderScheduler.scheduleTaskReminder(appContext, it.id, it.reminderEpochMillis!!) }
    }

    suspend fun getAllForBackup(): List<TaskEntity> = dao.getAllForBackup()
    suspend fun restoreFromBackup(tasks: List<TaskEntity>) = tasks.forEach { dao.upsert(it) }
}
