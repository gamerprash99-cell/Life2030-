package com.lifeos.app.domain.usecase

import com.lifeos.app.core.util.HabitStatsCalculator
import com.lifeos.app.core.util.isCompletionDone
import com.lifeos.app.core.util.toSchedule
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import java.time.LocalDate

/**
 * Builds the seven-day rhythm for the seven days ending on [today].
 *
 * Pure and framework-free, so Home and Habits state the *same* statistic from
 * the *same* rule instead of each keeping a private copy that can quietly
 * disagree.
 *
 * A day counts as done only when at least one habit was due for it **and** every
 * habit due that day reached its target. Unscheduled days are neutral. For
 * N-per-week/month habits a day stops being due once the period target is met,
 * so extra rest days are never counted as misses.
 */
fun buildWeeklyRhythm(
    habits: List<HabitEntity>,
    todayCompletions: List<HabitCompletionEntity>,
    weekCompletions: List<HabitCompletionEntity>,
    today: LocalDate
): List<DayCheck> {
    val todayEpochDay = today.toEpochDay()
    val weekStart = todayEpochDay - 6
    val scheduled = habits.map { habit -> habit to habit.toSchedule() }
    val successByDay = weekCompletions
        .filter { completion -> habits.firstOrNull { it.id == completion.habitId }?.isCompletionDone(completion) == true }
        .groupBy { it.dateEpochDay }
        .mapValues { (_, rows) -> rows.map { it.habitId }.toSet() }
    val todayByHabit = todayCompletions.associateBy { it.habitId }

    // Successful days must stay per habit: an N-per-week habit may have met
    // its own target while another habit in the same week has not.
    val doneDaysByHabit = mutableMapOf<String, MutableSet<Long>>()
    return (0L..6L).map { offset ->
        val day = weekStart + offset
        val doneForDay = if (day == todayEpochDay) {
            todayByHabit.values
                .filter { completion -> habits.firstOrNull { it.id == completion.habitId }?.isCompletionDone(completion) == true }
                .map { it.habitId }
                .toSet()
        } else {
            successByDay[day].orEmpty()
        }
        doneForDay.forEach { habitId ->
            doneDaysByHabit.getOrPut(habitId) { mutableSetOf() }.add(day)
        }

        val expected = scheduled.filter { (habit, schedule) ->
            HabitStatsCalculator.isDue(
                schedule,
                day,
                doneDaysByHabit[habit.id].orEmpty()
            )
        }
        val dayDone = expected.isNotEmpty() && expected.all { (habit, _) ->
            val completion = if (day == todayEpochDay) {
                todayByHabit[habit.id]
            } else {
                weekCompletions.firstOrNull { it.habitId == habit.id && it.dateEpochDay == day }
            }
            completion != null && habit.isCompletionDone(completion)
        }
        DayCheck(epochDay = day, isDone = dayDone, isToday = day == todayEpochDay)
    }
}
