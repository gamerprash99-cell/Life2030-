package com.lifeos.app.core.util

import com.lifeos.app.data.db.entities.RepeatRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RepeatRuleCalculatorTest {

    private fun day(year: Int, month: Int, dom: Int): Long = LocalDate.of(year, month, dom).toEpochDay()

    @Test
    fun `none never schedules a next occurrence`() {
        val today = day(2024, 1, 10)
        assertNull(RepeatRuleCalculator.nextOccurrence(RepeatRule.NONE, null, today, today))
    }

    @Test
    fun `daily bumps by one day`() {
        val today = day(2024, 1, 10)
        assertEquals(today + 1, RepeatRuleCalculator.nextOccurrence(RepeatRule.DAILY, null, today, today))
    }

    @Test
    fun `weekly bumps by seven days`() {
        val due = day(2024, 1, 10)
        assertEquals(due + 7, RepeatRuleCalculator.nextOccurrence(RepeatRule.WEEKLY, null, due, due))
    }

    @Test
    fun `monthly clamps short months`() {
        val jan31 = day(2024, 1, 31)
        assertEquals(day(2024, 2, 29), RepeatRuleCalculator.nextOccurrence(RepeatRule.MONTHLY, null, jan31, jan31))
    }

    @Test
    fun `custom days picks the next matched weekday`() {
        val sundayJan7_leap2024 = day(2024, 1, 7) // Sunday
        assertEquals(day(2024, 1, 8), RepeatRuleCalculator.nextOccurrence(RepeatRule.CUSTOM_DAYS, "1,3,5", sundayJan7_leap2024, sundayJan7_leap2024))
    }

    @Test
    fun `custom days wraps to next week when no day remains`() {
        val saturday = day(2024, 1, 6) // Saturday
        assertEquals(day(2024, 1, 8), RepeatRuleCalculator.nextOccurrence(RepeatRule.CUSTOM_DAYS, "1", saturday, saturday))
    }

    @Test
    fun `custom days ignores invalid csv entries`() {
        val sunday = LocalDate.of(2019, 3, 3).toEpochDay()
        assertEquals(
            LocalDate.of(2019, 3, 4).toEpochDay(),
            RepeatRuleCalculator.nextOccurrence(RepeatRule.CUSTOM_DAYS, "1,banana,99,x", sunday, sunday)
        )
    }

    @Test
    fun `custom days with empty csv returns null`() {
        val today = day(2024, 1, 10)
        assertNull(RepeatRuleCalculator.nextOccurrence(RepeatRule.CUSTOM_DAYS, "", today, today))
    }

    @Test
    fun `overdue task schedules from today not from the stale due date`() {
        val today = day(2024, 1, 10)
        val staleDue = day(2024, 1, 1)
        assertEquals(today + 1, RepeatRuleCalculator.nextOccurrence(RepeatRule.DAILY, null, staleDue, today))
    }

    @Test
    fun `future due date preserves the original schedule`() {
        val today = day(2024, 1, 1)
        val futureDue = day(2024, 1, 10)
        assertEquals(futureDue + 7, RepeatRuleCalculator.nextOccurrence(RepeatRule.WEEKLY, null, futureDue, today))
    }
}