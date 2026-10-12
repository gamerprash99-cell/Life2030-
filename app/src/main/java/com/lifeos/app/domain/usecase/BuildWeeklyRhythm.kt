package com.lifeos.app.domain.usecase

import com.lifeos.app.core.util.HabitStatsCalculator
import com.lifeos.app.core.util.toSchedule
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import java.time.LocalDate

/**
 * Builds the seven-day rhythm for the seven days ending on [today].
 *
 * Pure and framework-free, so Home and Habits & Routines state the *same*
 * statistic from the *same* rule instead of each keeping a private copy that can
 * quietly disagree.
 *
 * The rule is the app's existing one: a day counts as done only when at least one
 * habit was scheduled for it **and** every habit scheduled that day reached its
 * goal. A day with nothing scheduled is never counted as a miss, so the strip
 * never implies a failure the schedule never asked for. Every input is a real
 * persisted completion, so the strip cannot report a number the database cannot
 * support.
 *
 * @param todayCompletions is the day-today slice, passed separately so today's
 *   cell reflects the live value even when the caller only queried the week.
 */
fun buildWeeklyRhythm(
    habits: List<HabitEntity>,
    todayCompletions: List<HabitCompletionEntity>,
    weekCompletions: List<HabitCompletionEntity>,
    today: LocalDate
): List<DayCheck> {
    val todayEpochDay = today.toEpochDay()
    val weekStart = todayEpochDay - 6
    val completionsByDay = weekCompletions.groupBy { it.dateEpochDay }
    val todayByHabit = todayCompletions.associateBy { it.habitId }
    val scheduled = habits.map { habit -> habit to habit.toSchedule() }

    return (0L..6L).map { offset ->
        val day = weekStart + offset
        val expected = scheduled.filter { (_, schedule) -> HabitStatsCalculator.isScheduled(schedule, day) }
        val dayDone = expected.isNotEmpty() && expected.all { (habit, _) ->
            val completion = if (day == todayEpochDay) {
                todayByHabit[habit.id]
            } else {
                completionsByDay[day]?.firstOrNull { it.habitId == habit.id }
            }
            completion != null && completion.progressCount >= habit.goalCount
        }
        DayCheck(epochDay = day, isDone = dayDone, isToday = day == todayEpochDay)
    }
}
