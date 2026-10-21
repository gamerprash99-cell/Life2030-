package com.lifeos.app.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Recurrence rules: the contract that decides when a habit or recurring task is
 * "due". These are pure functions, so they are covered directly here.
 */
class RecurrenceRulesTest {

    private val monday = LocalDate.of(2026, 3, 2) // a Monday

    @Test
    fun `none never repeats`() {
        assertNull(RecurrenceRules.next(monday, Recurrence.NONE, null))
    }

    @Test
    fun `daily advances one day`() {
        assertEquals(monday.plusDays(1), RecurrenceRules.next(monday, Recurrence.DAILY, null))
    }

    @Test
    fun `every n days honours the interval`() {
        assertEquals(
            monday.plusDays(3),
            RecurrenceRules.next(monday, Recurrence.EVERY_N_DAYS, "3"),
        )
    }

    @Test
    fun `every n days falls back to one when parameter is missing or invalid`() {
        assertEquals(monday.plusDays(1), RecurrenceRules.next(monday, Recurrence.EVERY_N_DAYS, null))
        assertEquals(monday.plusDays(1), RecurrenceRules.next(monday, Recurrence.EVERY_N_DAYS, "abc"))
    }

    @Test
    fun `monthly and yearly advance by calendar unit`() {
        assertEquals(monday.plusMonths(1), RecurrenceRules.next(monday, Recurrence.MONTHLY, null))
        assertEquals(monday.plusYears(1), RecurrenceRules.next(monday, Recurrence.YEARLY, null))
    }

    @Test
    fun `days of week picks the next selected weekday`() {
        // Monday -> next WED
        val next = RecurrenceRules.next(monday, Recurrence.DAYS_OF_WEEK, "MON,WED,FRI")
        assertEquals(LocalDate.of(2026, 3, 4), next)
    }

    @Test
    fun `days of week with no valid days returns null`() {
        assertNull(RecurrenceRules.next(monday, Recurrence.DAYS_OF_WEEK, "XYZ"))
    }

    @Test
    fun `occursOn never fires before the start date`() {
        assertFalse(
            RecurrenceRules.occursOn(
                date = monday.minusDays(1),
                startDate = monday,
                recurrence = Recurrence.DAILY,
                parameter = null,
            ),
        )
    }

    @Test
    fun `occursOn for every n days respects elapsed days`() {
        assertTrue(
            RecurrenceRules.occursOn(monday.plusDays(4), monday, Recurrence.EVERY_N_DAYS, "2"),
        )
        assertFalse(
            RecurrenceRules.occursOn(monday.plusDays(3), monday, Recurrence.EVERY_N_DAYS, "2"),
        )
    }

    @Test
    fun `occursOn for weekly requires the same weekday`() {
        val twoWeeks = monday.plusWeeks(2)
        assertTrue(RecurrenceRules.occursOn(twoWeeks, monday, Recurrence.WEEKLY, null))
        assertFalse(RecurrenceRules.occursOn(twoWeeks.plusDays(1), monday, Recurrence.WEEKLY, null))
    }

    @Test
    fun `occursOn for none fires only on the start date`() {
        assertTrue(RecurrenceRules.occursOn(monday, monday, Recurrence.NONE, null))
        assertFalse(RecurrenceRules.occursOn(monday.plusDays(1), monday, Recurrence.NONE, null))
    }

    @Test
    fun `parseDays validates and normalizes weekday codes`() {
        assertEquals(setOf("MON", "WED", "FRI"), RecurrenceRules.parseDays(" mon , wed ,fri "))
        assertEquals(emptySet<String>(), RecurrenceRules.parseDays(null))
        assertEquals(setOf("SUN"), RecurrenceRules.parseDays("sun,xyz"))
    }

    @Test
    fun `formatDays emits canonical order regardless of input order`() {
        assertEquals("MON,WED,FRI", RecurrenceRules.formatDays(listOf("FRI", "MON", "WED")))
    }

    @Test
    fun `previousOnOrBefore finds the last selected weekday`() {
        // From Thursday back to WED
        val thursday = LocalDate.of(2026, 3, 5)
        assertEquals(
            LocalDate.of(2026, 3, 4),
            RecurrenceRules.previousOnOrBefore(thursday, Recurrence.DAYS_OF_WEEK, "MON,WED,FRI"),
        )
    }
}
