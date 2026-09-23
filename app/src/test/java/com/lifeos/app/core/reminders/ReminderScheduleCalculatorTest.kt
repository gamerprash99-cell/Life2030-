package com.lifeos.app.core.reminders

import com.lifeos.app.data.db.entities.ReminderEntity
import com.lifeos.app.data.db.entities.ReminderRepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pure-JVM tests for the reminder recurrence math (no Android dependencies).
 *
 * The harness's clock is real wall-clock time in the host zone, so these tests
 * compute expectations from the same anchored instant using java.time — the
 * same way the production code does — rather than hard-coding UTC timestamps
 * that would drift across zones/DST.
 */
class ReminderScheduleCalculatorTest {

    private val zone = ZoneId.systemDefault()
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun toText(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(fmt)

    @Test
    fun once_hasNoNextOccurrence() {
        assertNull(
            ReminderScheduleCalculator.nextOccurrenceMillis(ReminderRepeatType.ONCE, null, at(2026, 1, 1, 9, 0))
        )
    }

    @Test
    fun daily_advancesExactlyOneDay() {
        val start = at(2026, 1, 31, 9, 30) // end of month, exercises rollover
        val next = ReminderScheduleCalculator.nextOccurrenceMillis(ReminderRepeatType.DAILY, null, start)!!
        assertEquals("2026-02-01 09:30", toText(next))
    }

    @Test
    fun weekdays_advancesToNextWeekdayAcrossWeekend() {
        // Friday 2026-01-30 -> Monday 2026-02-02 (weekend skipped).
        val friday = at(2026, 1, 30, 8, 15)
        val next = ReminderScheduleCalculator.nextOccurrenceMillis(
            ReminderRepeatType.WEEKDAYS, ReminderEntity.WEEKDAYS_DEFAULT_CSV, friday
        )!!
        assertEquals("2026-02-02 08:15", toText(next))
    }

    @Test
    fun weekdays_respectsCustomDaySet() {
        // Mon/Wed/Fri set; Friday 2026-01-30 -> Monday 2026-02-02.
        val friday = at(2026, 1, 30, 8, 15)
        val next = ReminderScheduleCalculator.nextOccurrenceMillis(
            ReminderRepeatType.WEEKDAYS, "1,3,5", friday
        )!!
        assertEquals("2026-02-02 08:15", toText(next))
    }

    @Test
    fun weekdays_withEmptyOrInvalidCsvUsesDefaults() {
        val friday = at(2026, 1, 30, 8, 15)
        val next = ReminderScheduleCalculator.nextOccurrenceMillis(ReminderRepeatType.WEEKDAYS, "junk", friday)!!
        assertEquals("2026-02-02 08:15", toText(next))
    }

    @Test
    fun weekdays_withWeekendOnlySetSkipsToNextWeekend() {
        // Sat/Sun set; Saturday 2026-01-31 -> Sunday 2026-02-01.
        val saturday = at(2026, 1, 31, 10, 0)
        val next = ReminderScheduleCalculator.nextOccurrenceMillis(
            ReminderRepeatType.WEEKDAYS, "6,7", saturday
        )!!
        assertEquals("2026-02-01 10:00", toText(next))
    }

    @Test
    fun parseWeekdaySet_defaultsToMonFri() {
        assertEquals(setOf(1, 2, 3, 4, 5), ReminderScheduleCalculator.parseWeekdaySet(null))
        assertEquals(setOf(1, 2, 3, 4, 5), ReminderScheduleCalculator.parseWeekdaySet(""))
        assertEquals(setOf(2, 4), ReminderScheduleCalculator.parseWeekdaySet("2,4"))
        // Out-of-range values are dropped.
        assertEquals(setOf(1), ReminderScheduleCalculator.parseWeekdaySet("1,9,0"))
    }
}