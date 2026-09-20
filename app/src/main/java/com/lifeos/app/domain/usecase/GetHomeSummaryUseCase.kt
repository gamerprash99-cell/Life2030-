package com.lifeos.app.domain.usecase

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.HabitSchedule
import com.lifeos.app.core.util.HabitStatsCalculator
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.domain.model.TimelineItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** Everything the Home dashboard needs, assembled live from real persisted data. */
data class HomeSummary(
    val greeting: String,
    val dateLabel: String,
    val dayStatusLabel: String,
    val tasksToday: List<TaskEntity>,
    val tasksCompletedToday: Int,
    val tasksTotalToday: Int,
    val focusTask: TaskEntity?,
    val focusTaskIsDone: Boolean,
    val habitsToday: List<HabitSummaryRow>,
    val weeklyConsistency: List<DayCheck>,
    val weeklyDoneDays: Int,
    val todaySpend: Double,
    val overdueTaskCount: Int,
    val recentActivity: List<TimelineItem>
)

data class HabitSummaryRow(
    val habit: HabitEntity,
    val progressCount: Int,
    val goalCount: Int,
    val isDone: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionPercent: Int
)

/** One cell of the 7-day weekly habit consistency row. */
data class DayCheck(
    val epochDay: Long,
    val isDone: Boolean,
    val isToday: Boolean
)

class GetHomeSummaryUseCase(
    private val taskRepo: TaskRepository,
    private val habitRepo: HabitRepository,
    private val expenseRepo: ExpenseRepository,
    private val buildTimeline: BuildTimelineUseCase
) {
    @Suppress("UNCHECKED_CAST")
    operator fun invoke(today: LocalDate = DateTimeUtils.today()): Flow<HomeSummary> {
        val epochDay = today.toEpochDay()
        val weekStart = epochDay - 6
        return combine(
            taskRepo.observeForDay(epochDay),
            taskRepo.observeOverdue(epochDay),
            habitRepo.observeAll(),
            habitRepo.observeAllForDay(epochDay),
            expenseRepo.observeTotalForDay(epochDay),
            habitRepo.observeAllInRange(weekStart, epochDay)
        ) { args ->
            assemble(
                today = today,
                weekStart = weekStart,
                epochDay = epochDay,
                tasksToday = args[0] as List<TaskEntity>,
                overdueTasks = args[1] as List<TaskEntity>,
                habits = args[2] as List<HabitEntity>,
                todayCompletions = args[3] as List<HabitCompletionEntity>,
                spend = args[4] as Double,
                weekCompletions = args[5] as List<HabitCompletionEntity>
            )
        }
    }

    private suspend fun assemble(
        today: LocalDate,
        weekStart: Long,
        epochDay: Long,
        tasksToday: List<TaskEntity>,
        overdueTasks: List<TaskEntity>,
        habits: List<HabitEntity>,
        todayCompletions: List<HabitCompletionEntity>,
        spend: Double,
        weekCompletions: List<HabitCompletionEntity>
    ): HomeSummary {
        val todayProgressByHabit = todayCompletions.associateBy { it.habitId }
        val weekProgressByHabit = weekCompletions.groupBy { it.habitId }

        val habitRows = habits.map { habit ->
            val progress = todayProgressByHabit[habit.id]?.progressCount ?: 0
            val analytics = runCatching { habitRepo.computeAnalytics(habit, today) }.getOrNull()
            HabitSummaryRow(
                habit = habit,
                progressCount = progress,
                goalCount = habit.goalCount,
                isDone = progress >= habit.goalCount,
                currentStreak = analytics?.currentStreak ?: 0,
                longestStreak = analytics?.longestStreak ?: 0,
                completionPercent = analytics?.completionPercentThisMonth ?: 0
            )
        }
        val habitsDoneToday = habitRows.count { it.isDone }

        // A calendar day counts as a full consistency day when every active habit
        // scheduled on it was completed. Non-scheduled days are never counted.
        val scheduled = habits.map { habit -> habit to scheduleOf(habit) }
        val days = (0L..6L).map { offset ->
            val day = weekStart + offset
            val expected = scheduled.filter { (_, schedule) -> HabitStatsCalculator.isScheduled(schedule, day) }
            val dayDone = expected.isNotEmpty() && expected.all { (habit, _) ->
                val completion = weekProgressByHabit[habit.id]?.firstOrNull { it.dateEpochDay == day }
                completion != null && completion.progressCount >= habit.goalCount
            }
            DayCheck(epochDay = day, isDone = dayDone, isToday = day == epochDay)
        }

        // Focus Now: the highest-priority incomplete task of today, or the most
        // recently completed task when everything is already done (existing
        // priority/focus semantics — tasks are served by the DAO in that order).
        val pendingFocus = tasksToday.firstOrNull { !it.isCompleted }
        val focusTask = pendingFocus ?: tasksToday.lastOrNull { it.isCompleted }
        val focusTaskIsDone = pendingFocus == null && tasksToday.isNotEmpty()

        val totalGoals = habitRows.size + tasksToday.size
        val doneGoals = habitsDoneToday + tasksToday.count { it.isCompleted }
        val dayStatusLabel = when {
            totalGoals == 0 -> "Day starting"
            doneGoals * 2 >= totalGoals -> "Day on track"
            doneGoals > 0 -> "Building momentum"
            else -> "Fresh start"
        }

        return HomeSummary(
            greeting = DateTimeUtils.greeting(),
            dateLabel = "${DateTimeUtils.formatDayOfWeek(today)}, ${DateTimeUtils.formatFullDate(today)}",
            dayStatusLabel = dayStatusLabel,
            tasksToday = tasksToday,
            tasksCompletedToday = tasksToday.count { it.isCompleted },
            tasksTotalToday = tasksToday.size,
            focusTask = focusTask,
            focusTaskIsDone = focusTaskIsDone,
            habitsToday = habitRows,
            weeklyConsistency = days,
            weeklyDoneDays = days.count { it.isDone },
            todaySpend = spend,
            overdueTaskCount = overdueTasks.size,
            recentActivity = runCatching { buildTimeline(epochDay) }.getOrDefault(emptyList())
        )
    }

    private fun scheduleOf(habit: HabitEntity) = HabitSchedule(
        frequency = habit.frequency,
        customDays = HabitStatsCalculator.parseCustomDays(habit.customDaysCsv),
        startEpochDay = habit.startDateEpochDay
    )
}