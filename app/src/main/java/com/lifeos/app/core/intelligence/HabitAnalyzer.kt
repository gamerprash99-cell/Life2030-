package com.lifeos.app.core.intelligence

import com.lifeos.app.data.repository.HabitRepository
import kotlinx.coroutines.flow.first

/**
 * Wraps HabitRepository's existing computeAnalytics() (already real streak
 * math, not duplicated here) into the Intelligence Engine's summary shape.
 */
class HabitAnalyzer(private val habitRepository: HabitRepository) {

    /** Summarizes every active habit, sorted by current streak (most consistent first). */
    suspend fun summarizeAll(): List<HabitAnalysisSummary> {
        val habits = habitRepository.observeAll().first().filter { !it.isArchived }
        return habits.map { habit ->
            val analytics = habitRepository.computeAnalytics(habit)
            HabitAnalysisSummary(
                habitId = habit.id,
                habitName = habit.name,
                currentStreak = analytics.currentStreak,
                longestStreak = analytics.longestStreak,
                completionPercentThisMonth = analytics.completionPercentThisMonth
            )
        }.sortedByDescending { it.currentStreak }
    }
}
