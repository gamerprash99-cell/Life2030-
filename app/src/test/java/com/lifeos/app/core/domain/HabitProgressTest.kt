package com.lifeos.app.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Habit streak + progress math. Streak definitions live in HabitProgress and are
 * the app's most rule-dense logic, so every branch is exercised here.
 */
class HabitProgressTest {

    private val today = LocalDate.of(2026, 3, 10) // Tuesday

    private fun daily(dates: Set<LocalDate>) = HabitProgress.streaks(
        completedDates = dates,
        scheduledPredicate = { true },
        today = today,
    )

    @Test
    fun `no completions means no streak`() {
        val result = daily(emptySet())
        assertEquals(0, result.current)
        assertEquals(0, result.best)
    }

    @Test
    fun `current streak counts back from today`() {
        val dates = setOf(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, daily(dates).current)
    }

    @Test
    fun `today pending does not break the streak`() {
        val dates = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3))
        assertEquals(3, daily(dates).current)
    }

    @Test
    fun `a missed scheduled day breaks the current streak`() {
        val dates = setOf(today, today.minusDays(1), today.minusDays(3))
        assertEquals(2, daily(dates).current)
    }

    @Test
    fun `best streak survives after the current streak resets`() {
        val dates = setOf(
            today.minusDays(10), today.minusDays(9), today.minusDays(8), today.minusDays(7),
            today,
        )
        val result = daily(dates)
        assertEquals(1, result.current)
        assertEquals(4, result.best)
    }

    @Test
    fun `unscheduled days do not break a weekly streak`() {
        // Weekly habit: only Mondays are scheduled.
        val monday = LocalDate.of(2026, 3, 9) // a Monday; today is Tuesday
        val completed = setOf(monday, monday.minusWeeks(1), monday.minusWeeks(2))
        val result = HabitProgress.streaks(
            completedDates = completed,
            scheduledPredicate = { it.dayOfWeek == java.time.DayOfWeek.MONDAY },
            today = today,
        )
        assertEquals(3, result.current)
        assertEquals(3, result.best)
    }

    @Test
    fun `progress ratio is clamped to one`() {
        assertEquals(1f, HabitProgress.progressRatio(10, 3))
        assertEquals(0f, HabitProgress.progressRatio(0, 3))
    }

    @Test
    fun `progress ratio handles a zero target`() {
        assertEquals(0f, HabitProgress.progressRatio(0, 0))
        assertEquals(1f, HabitProgress.progressRatio(1, 0))
    }

    @Test
    fun `progress percent rounds to whole numbers`() {
        assertEquals(50, HabitProgress.progressPercent(1, 2))
        assertEquals(33, HabitProgress.progressPercent(1, 3))
        assertEquals(100, HabitProgress.progressPercent(3, 3))
    }

    @Test
    fun `heatmap spans the requested number of weeks ending today`() {
        val cells = HabitProgress.heatmap(today, weeks = 5) { 0 to 1 }
        // 5 weeks = 35 days, but the grid starts on a Monday, so it can exceed 35.
        assertTrue(cells.size >= 35)
        assertEquals(today, cells.last().date)
        assertTrue(cells.first().date.dayOfWeek == java.time.DayOfWeek.MONDAY)
    }

    @Test
    fun `heatmap clamps the week window to the supported range`() {
        val clampedLow = HabitProgress.heatmap(today, weeks = 1) { 0 to 1 }
        val clampedHigh = HabitProgress.heatmap(today, weeks = 100) { 0 to 1 }
        assertTrue(clampedLow.size >= 5 * 7)
        assertTrue(clampedHigh.size >= 26 * 7)
    }

    @Test
    fun `heatmap intensity reflects the completion ratio`() {
        val cells = HabitProgress.heatmap(today, weeks = 5) { date ->
            when (date) {
                today -> 1 to 1     // complete -> 4
                today.minusDays(1) -> 0 to 1 // nothing -> 0
                today.minusDays(2) -> 1 to 2 // half -> 2
                today.minusDays(3) -> 3 to 4 // 75% -> 3
                today.minusDays(4) -> 1 to 4 // 25% -> 1
                else -> 0 to 1
            }
        }
        val byDate = cells.associateBy { it.date }
        assertEquals(4, byDate.getValue(today).intensity)
        assertEquals(0, byDate.getValue(today.minusDays(1)).intensity)
        assertEquals(2, byDate.getValue(today.minusDays(2)).intensity)
        assertEquals(3, byDate.getValue(today.minusDays(3)).intensity)
        assertEquals(1, byDate.getValue(today.minusDays(4)).intensity)
    }

    @Test
    fun `heatmap never includes a future day`() {
        val cells = HabitProgress.heatmap(today, weeks = 12) { 1 to 1 }
        assertFalse(cells.any { it.date.isAfter(today) })
    }
}
