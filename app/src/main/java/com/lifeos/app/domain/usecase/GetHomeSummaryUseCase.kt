package com.lifeos.app.domain.usecase

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** Everything the Home dashboard (Section 5) needs, assembled live from real data. */
data class HomeSummary(
    val greeting: String,
    val dateLabel: String,
    val tasksToday: List<TaskEntity>,
    val tasksCompletedToday: Int,
    val tasksTotalToday: Int,
    val habitsToday: List<HabitSummaryRow>,
    val todaySpend: Double,
    val overdueTaskCount: Int
)

data class HabitSummaryRow(
    val habit: HabitEntity,
    val progressCount: Int,
    val goalCount: Int,
    val isDone: Boolean
)

class GetHomeSummaryUseCase(
    private val taskRepo: TaskRepository,
    private val habitRepo: HabitRepository,
    private val expenseRepo: ExpenseRepository
) {
    operator fun invoke(today: LocalDate = DateTimeUtils.today()): Flow<HomeSummary> {
        val epochDay = today.toEpochDay()
        return combine(
            taskRepo.observeForDay(epochDay),
            taskRepo.observeOverdue(epochDay),
            habitRepo.observeAll(),
            habitRepo.observeAllForDay(epochDay),
            expenseRepo.observeTotalForDay(epochDay)
        ) { tasksToday, overdue, habits, completions, spend ->
            val completionsByHabit = completions.associateBy { it.habitId }
            val habitRows = habits.map { habit ->
                val completion = completionsByHabit[habit.id]
                val progress = completion?.progressCount ?: 0
                HabitSummaryRow(
                    habit = habit,
                    progressCount = progress,
                    goalCount = habit.goalCount,
                    isDone = progress >= habit.goalCount
                )
            }
            HomeSummary(
                greeting = DateTimeUtils.greeting(),
                dateLabel = "${DateTimeUtils.formatDayOfWeek(today)}, ${DateTimeUtils.formatFullDate(today)}",
                tasksToday = tasksToday,
                tasksCompletedToday = tasksToday.count { it.isCompleted },
                tasksTotalToday = tasksToday.size,
                habitsToday = habitRows,
                todaySpend = spend,
                overdueTaskCount = overdue.size
            )
        }
    }
}
