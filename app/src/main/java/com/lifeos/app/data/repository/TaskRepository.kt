package com.lifeos.app.data.repository

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.core.util.RepeatRuleCalculator
import com.lifeos.app.data.db.dao.TaskDao
import com.lifeos.app.data.db.entities.RepeatRule
import com.lifeos.app.data.db.entities.ReminderEntity
import com.lifeos.app.data.db.entities.ReminderRepeatType
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.data.db.entities.TaskPriority
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val dao: TaskDao,
    private val reminderRepository: ReminderRepository
) {

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

    /** The task's reminder row (used by the task-card reminder editor for its initial repeat cadence). */
    suspend fun getReminderFor(taskId: String): ReminderEntity? =
        reminderRepository.getById(ReminderEntity.idFor(ReminderRepository.TYPE_TASK, taskId))

    /**
     * Updates (or clears, when [reminderEpochMillis] is null) a task's reminder
     * time and repeat cadence — the task equivalent of
     * [HabitRepository.setReminder], sharing the same scheduling path
     * (`syncReminderForTask`), so editing/time-changes/clearing replace the
     * alarm for the same stable id and can never leave a duplicate behind.
     */
    suspend fun setReminder(taskId: String, reminderEpochMillis: Long?, repeatType: ReminderRepeatType) {
        val task = dao.getById(taskId) ?: return
        val updated = task.copy(
            reminderEpochMillis = reminderEpochMillis,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsert(updated)
        reminderRepository.syncReminderForTask(updated, repeatType)
    }

    suspend fun createTask(
        title: String,
        description: String? = null,
        dueDateEpochDay: Long? = null,
        dueTimeMinutes: Int? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
        category: String? = null,
        reminderEpochMillis: Long? = null,
        reminderRepeatType: ReminderRepeatType = ReminderRepeatType.ONCE,
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
        // Reconcile the reminders mirror + exact alarm in one place.
        dao.getById(id)?.let { reminderRepository.syncReminderForTask(it, reminderRepeatType) }
        return id
    }

    suspend fun setCompleted(id: String, completed: Boolean) {
        dao.setCompleted(id, completed, if (completed) System.currentTimeMillis() else null, System.currentTimeMillis())
        if (completed) {
            // A completed task no longer needs reminding (a repeating task's next
            // occurrence is spawned below without inheriting the reminder).
            reminderRepository.deleteForTask(id)
            spawnNextOccurrence(id)
        } else {
            dao.getById(id)?.let { reminderRepository.syncReminderForTask(it) }
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
        reminderRepository.deleteForTask(id)
    }

    suspend fun getAllForBackup(): List<TaskEntity> = dao.getAllForBackup()
    suspend fun restoreFromBackup(tasks: List<TaskEntity>) = tasks.forEach { dao.upsert(it) }
}
