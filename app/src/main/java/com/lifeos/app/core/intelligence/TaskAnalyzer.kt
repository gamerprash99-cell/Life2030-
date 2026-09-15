package com.lifeos.app.core.intelligence

import com.lifeos.app.data.db.entities.TaskPriority
import com.lifeos.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.first

/**
 * Rule-based/statistical task analysis. Reads through TaskRepository only —
 * no direct DAO access, per the architecture rule that the intelligence
 * layer must go through the existing repository layer.
 */
class TaskAnalyzer(private val taskRepository: TaskRepository) {

    /** Analyzes all tasks whose due date falls in [startEpochDay]..[endEpochDay]. */
    suspend fun analyzePeriod(startEpochDay: Long, endEpochDay: Long, todayEpochDay: Long): TaskAnalysis {
        val allTasks = taskRepository.observeAll().first()
        val inPeriod = allTasks.filter { task ->
            val due = task.dueDateEpochDay ?: return@filter false
            due in startEpochDay..endEpochDay && !task.isDeleted
        }
        val completed = inPeriod.count { it.isCompleted }
        val overdue = allTasks.count { task ->
            !task.isCompleted && !task.isDeleted && (task.dueDateEpochDay ?: Long.MAX_VALUE) < todayEpochDay
        }
        val highPriorityOpen = allTasks.count { it.priority == TaskPriority.HIGH && !it.isCompleted && !it.isDeleted }
        val rate = if (inPeriod.isNotEmpty()) (completed * 100) / inPeriod.size else 0

        return TaskAnalysis(
            totalInPeriod = inPeriod.size,
            completedInPeriod = completed,
            completionRatePercent = rate,
            overdueCount = overdue,
            highPriorityOpenCount = highPriorityOpen
        )
    }
}
