package com.lifeos.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HabitStatsCalculatorTest {

    private fun day(year: Int, month: Int, dom: Int): Long = LocalDate.of(year, month, dom).toEpochDay()

    @Test
    fun `current streak counts consecutive days through today`() {
        val today = day(2024, 1, 10)
        val done = setOf(day(2024, 1, 10), day(2024, 1, 9), day(2024, 1, 8))
        assertEquals(3, HabitStatsCalculator.currentStreak(done, today))
    }

    @Test
    fun `current streak resets on a missed day`() {
        val today = day(2024, 1, 10)
        val done = setOf(day(2024, 1, 10), day(2024, 1, 9), day(2024, 1, 7))
        assertEquals(2, HabitStatsCalculator.currentStreak(done, today))
    }

    @Test
    fun `current streak is zero when today is not done`() {
        val today = day(2024, 1, 10)
        val done = setOf(day(2024, 1, 9), day(2024, 1, 8), day(2024, 1, 7))
        assertEquals(0, HabitStatsCalculator.currentStreak(done, today))
    }

    @Test
    fun `current streak is zero on empty history`() {
        assertEquals(0, HabitStatsCalculator.currentStreak(emptySet(), day(2024, 1, 10)))
    }

    @Test
    fun `longest streak finds the best run across gaps`() {
        val done = setOf(
            day(2024, 1, 1), day(2024, 1, 2), day(2024, 1, 3),
            day(2024, 1, 5),
            day(2024, 1, 10), day(2024, 1, 11), day(2024, 1, 12), day(2024, 1, 13)
        )
        assertEquals(4, HabitStatsCalculator.longestStreak(done))
    }

    @Test
    fun `longest streak is zero on empty history`() {
        assertEquals(0, HabitStatsCalculator.longestStreak(emptySet()))
    }

    @Test
    fun `longest streak handles single day`() {
        assertEquals(1, HabitStatsCalculator.longestStreak(setOf(day(2024, 1, 1))))
    }

    @Test
    fun `completion percent is integer floor`() {
        assertEquals(50, HabitStatsCalculator.completionPercent(1, 2))
        assertEquals(33, HabitStatsCalculator.completionPercent(1, 3))
        assertEquals(100, HabitStatsCalculator.completionPercent(3, 3))
    }

    @Test
    fun `completion percent guards divide by zero`() {
        assertEquals(0, HabitStatsCalculator.completionPercent(5, 0))
    }
}