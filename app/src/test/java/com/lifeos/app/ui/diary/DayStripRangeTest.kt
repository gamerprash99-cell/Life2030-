package com.lifeos.app.ui.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The date strip's range is the one piece of it that can be wrong without
 * looking wrong: a strip that silently loses its upper or lower bound still
 * scrolls, it just scrolls to the wrong place. These tests pin the bounds.
 */
class DayStripRangeTest {

    private val today = LocalDate.of(2026, 9, 26).toEpochDay()

    @Test
    fun `range ends on today so there is no tomorrow to journal`() {
        assertEquals(today, dayStripRange(today).last())
    }

    @Test
    fun `range starts exactly one history window back`() {
        assertEquals(today - HISTORY_DAYS, dayStripRange(today).first())
    }

    @Test
    fun `range is ascending and contiguous with no gaps or repeats`() {
        val days = dayStripRange(today)
        assertEquals(days.sorted(), days)
        assertEquals(days.size, days.distinct().size)
        days.zipWithNext { earlier, later -> assertEquals(1L, later - earlier) }
    }

    @Test
    fun `range is inclusive of both ends and a known size`() {
        val days = dayStripRange(today)
        assertEquals(HISTORY_DAYS.toInt() + 1, days.size)
        assertTrue(today - 1L in days)
        assertTrue(today - HISTORY_DAYS in days)
    }

    @Test
    fun `range never contains a day after today`() {
        assertFalse(dayStripRange(today).any { it > today })
    }

    @Test
    fun `a narrower history window is honoured`() {
        val days = dayStripRange(today, historyDays = 3)
        assertEquals(listOf(today - 3, today - 2, today - 1, today), days)
    }

    @Test
    fun `the strip shows five dates at a time`() {
        assertEquals(5, VISIBLE_DATES)
    }

    @Test
    fun `the history window is bounded to a year rather than unlimited`() {
        // Guards the intent, not just the arithmetic: if this is ever widened
        // without thought, the test says so.
        assertTrue(HISTORY_DAYS in 1..366)
    }
}
