package com.lifeos.app.core.domain

import java.time.LocalDate

/**
 * Habit streak + progress math — the app's most rule-dense pure logic, so it lives
 * free of Android imports and is unit tested directly (see HabitStreakTest).
 *
 * Definitions (documented in README "Behavior rules"):
 * - A habit is *completed on* a date when a completion row exists for that ISO date.
 * - **Current streak**: consecutive eligible days ending today (or yesterday when
 *   today is not yet complete) on which the habit was completed. Days the habit is
 *   not scheduled (e.g. a weekly habit's off days) do not break the streak.
 * - **Best streak**: the longest run of consecutive *scheduled* days completed,
 *   computed over the whole history.
 * - **Progress** (spec formula): `completedToday / targetForToday`, clamped to 1,
 *   where the target for today is 1 for binary habits and the numeric parameter for
 *   count-based habits.
 */
object HabitProgress {

    data class StreakResult(
        val current: Int,
        val best: Int,
    )

    /**
     * @param scheduledDates every date the habit was scheduled up to [today],
     *   newest first is not required; must cover the completion history window.
     * @param completedDates the set of ISO dates with a completion.
     */
    fun streaks(
        completedDates: Set<LocalDate>,
        scheduledPredicate: (LocalDate) -> Boolean,
        today: LocalDate,
    ): StreakResult {
        if (completedDates.isEmpty()) return StreakResult(0, 0)

        // Current streak: walk back from today. Today may be pending without
        // breaking the streak, but anything before it must be scheduled+completed.
        var current = 0
        var cursor = today
        if (cursor !in completedDates && scheduledPredicate(cursor)) {
            cursor = cursor.minusDays(1)
        }
        var skippedUnscheduledTail = false
        while (true) {
            if (!scheduledPredicate(cursor)) {
                // Unscheduled days are invisible to the streak.
                cursor = cursor.minusDays(1)
                skippedUnscheduledTail = true
                if (current == 0 && skippedUnscheduledTail) {
                    // keep scanning; a scheduled day may still start the streak
                }
                if (java.time.temporal.ChronoUnit.DAYS.between(cursor, today) > 366) break
                continue
            }
            if (cursor in completedDates) {
                current++
                cursor = cursor.minusDays(1)
            } else {
                break
            }
        }

        // Best streak: sweep the sorted union of scheduled+completed days.
        val relevant = (completedDates.filter { scheduledPredicate(it) }).sorted()
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (date in relevant) {
            run = if (previous != null &&
                java.time.temporal.ChronoUnit.DAYS.between(previous, date) == 1L
            ) {
                run + 1
            } else {
                1
            }
            if (run > best) best = run
            previous = date
        }
        return StreakResult(current = current, best = maxOf(best, current))
    }

    /**
     * Today's progress in `0f..1f` for the progress ring.
     * `completedToday` may exceed the target; the result is clamped to 1.
     */
    fun progressRatio(completedToday: Int, targetToday: Int): Float {
        if (targetToday <= 0) return if (completedToday > 0) 1f else 0f
        val ratio = completedToday.toFloat() / targetToday.toFloat()
        return ratio.coerceIn(0f, 1f)
    }

    /** Percentage for display, rounded half-up, 0..100. */
    fun progressPercent(completedToday: Int, targetToday: Int): Int =
        Math.round(progressRatio(completedToday, targetToday) * 100f)

    /**
     * Heatmap cells for the last [weeks] weeks (spec: 5–26 weeks), one intensity
     * 0..4 per day based on completion ratio that day.
     */
    data class HeatmapCell(
        val date: LocalDate,
        val intensity: Int,
        val completed: Int,
        val target: Int,
    )

    fun heatmap(
        today: LocalDate,
        weeks: Int,
        ratioFor: (LocalDate) -> Pair<Int, Int>,
    ): List<HeatmapCell> {
        val weeksCoerced = weeks.coerceIn(5, 26)
        val start = today.minusDays((weeksCoerced * 7L - 1))
            .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val cells = mutableListOf<HeatmapCell>()
        var cursor = start
        while (!cursor.isAfter(today)) {
            val (completed, target) = ratioFor(cursor)
            val ratio = if (target <= 0) 0f else completed.toFloat() / target
            val intensity = when {
                target <= 0 -> 0
                completed == 0 -> 0
                ratio >= 1f -> 4
                ratio >= 0.75f -> 3
                ratio >= 0.5f -> 2
                else -> 1
            }
            cells += HeatmapCell(cursor, intensity, completed, target)
            cursor = cursor.plusDays(1)
        }
        return cells
    }
}
