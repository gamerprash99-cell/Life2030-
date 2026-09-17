package com.lifeos.app.core.util

/**
 * Pure arithmetic for habit analytics (streaks, completion percentages).
 * No storage or Android dependencies, so it can be unit-tested directly.
 * See HabitRepository.computeAnalytics() for the production call site.
 */
object HabitStatsCalculator {

    /** Consecutive days through [today] (inclusive) on which the habit was done. */
    fun currentStreak(doneDays: Set<Long>, today: Long): Int {
        var streak = 0
        var cursor = today
        while (doneDays.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }

    /** Longest run of consecutive done-days anywhere in [doneDays]. */
    fun longestStreak(doneDays: Set<Long>): Int {
        var longest = 0
        var running = 0
        var prevDay: Long? = null
        for (day in doneDays.sorted()) {
            running = if (prevDay != null && day == prevDay + 1) running + 1 else 1
            longest = maxOf(longest, running)
            prevDay = day
        }
        return longest
    }

    /** Whole-number completion percentage, guarded against divide-by-zero. */
    fun completionPercent(completedDays: Int, daysElapsed: Int): Int =
        if (daysElapsed > 0) (completedDays * 100) / daysElapsed else 0
}