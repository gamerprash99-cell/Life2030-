package com.lifeos.app.data.repository

import android.content.Context
import com.lifeos.app.core.reminders.ReminderScheduler
import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.data.db.dao.TaskDao
import com.lifeos.app.data.db.entities.RepeatRule
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.data.db.entities.TaskPriority
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao, private val appContext: Context) {

    fun observeForDay(epochDay: Long): Flow<List<TaskEntity>> = dao.observeForDay(epochDay)
    fun observeOverdue(todayEpochDay: Long): Flow<List<TaskEntity>> = dao.observeOverdue(todayEpochDay)
    fun observeAll(): Flow<List<TaskEntity>> = dao.observeAll()
    fun observeCountForDay(epochDay: Long): Flow<Int> = dao.observeCountForDay(epochDay)
    fun observeCompletedCountForDay(epochDay: Long): Flow<Int> = dao.observeCompletedCountForDay(epochDay)

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

    /**
     * Bulk-approve AI-extracted tasks (Section 8/10). Every task created here
     * originates from an explicit user tap on [CREATE TASKS] — never silent,
     * per Rule #9 ("AI-generated tasks must be reviewable").
     */
    suspend fun createFromAiExtraction(
        titles: List<String>,
        dueDateEpochDay: Long?,
        sourceType: String,
        sourceId: String
    ): List<String> = titles.map { title ->
        createTask(
            title = title,
            dueDateEpochDay = dueDateEpochDay,
            sourceType = sourceType,
            sourceId = sourceId
        )
    }

    suspend fun setCompleted(id: String, completed: Boolean) {
        dao.setCompleted(id, completed, if (completed) System.currentTimeMillis() else null, System.currentTimeMillis())
        if (completed) ReminderScheduler.cancelTaskReminder(appContext, id)
    }

    suspend fun reschedule(id: String, newEpochDay: Long) = dao.reschedule(id, newEpochDay, System.currentTimeMillis())

    /** Keep for tomorrow — Section 10 "TASK NOT COMPLETED" flow shortcut. */
    suspend fun keepForTomorrow(id: String, todayEpochDay: Long) = reschedule(id, todayEpochDay + 1)

    suspend fun delete(id: String) {
        dao.softDelete(id, System.currentTimeMillis())
        ReminderScheduler.cancelTaskReminder(appContext, id)
    }

    suspend fun search(query: String): List<TaskEntity> {
        if (query.isBlank()) return emptyList()
        return dao.search(query)
    }

    suspend fun countCompletedBetween(startMillis: Long, endMillis: Long): Int =
        dao.countCompletedBetween(startMillis, endMillis)

    suspend fun getAllForBackup(): List<TaskEntity> = dao.getAllForBackup()
    suspend fun restoreFromBackup(tasks: List<TaskEntity>) = tasks.forEach { dao.upsert(it) }
}
