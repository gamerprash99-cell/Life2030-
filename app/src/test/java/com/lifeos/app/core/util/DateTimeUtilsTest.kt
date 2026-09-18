package com.lifeos.app.core.util

import com.lifeos.app.core.util.DateTimeUtils.startOfMonthEpochDay
import com.lifeos.app.core.util.DateTimeUtils.toMinutesSinceMidnight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class DateTimeUtilsTest {

    @Test
    fun `greeting by time of day`() {
        assertEquals("Good Morning", DateTimeUtils.greeting(9))
        assertEquals("Good Afternoon", DateTimeUtils.greeting(13))
        assertEquals("Good Evening", DateTimeUtils.greeting(19))
        assertEquals("Good Night", DateTimeUtils.greeting(23))
    }

    @Test
    fun `start and end of month cover the full month`() {
        val sample = LocalDate.of(2024, 2, 15)
        val start = startOfMonthEpochDay(sample)
        val end = DateTimeUtils.endOfMonthEpochDay(sample)
        assertEquals(LocalDate.of(2024, 2, 1).toEpochDay(), start)
        assertEquals(LocalDate.of(2024, 2, 29).toEpochDay(), end)
    }

    @Test
    fun `epoch day round trips through local date`() {
        val date = LocalDate.of(2025, 6, 3)
        assertEquals(date, DateTimeUtils.epochDayToLocalDate(date.toEpochDay()))
    }

    @Test
    fun `minutes to local time`() {
        assertEquals(LocalTime.of(14, 5), DateTimeUtils.minutesToLocalTime(845))
        assertEquals(LocalTime.MIDNIGHT, DateTimeUtils.minutesToLocalTime(0))
        assertEquals(LocalTime.of(23, 59), DateTimeUtils.minutesToLocalTime(1439))
    }

    @Test
    fun `local time to minutes`() {
        assertEquals(845, LocalTime.of(14, 5).toMinutesSinceMidnight())
        assertEquals(0, LocalTime.MIDNIGHT.toMinutesSinceMidnight())
    }

    @Test
    fun `local day boundaries are local midnight and contiguous`() {
        val epochDay = LocalDate.of(2024, 10, 27).toEpochDay() // a DST transition day in many zones
        val start = DateTimeUtils.startOfLocalDayMillis(epochDay)
        val end = DateTimeUtils.endOfLocalDayMillis(epochDay)

        assertEquals(LocalTime.MIDNIGHT, Instant.ofEpochMilli(start).atZone(DateTimeUtils.zoneId()).toLocalTime())
        assertTrue(end > start)
        // end of this day is exactly the start of the next
        assertEquals(DateTimeUtils.startOfLocalDayMillis(epochDay + 1), end)
    }

    @Test
    fun `now minutes of day is within a valid range`() {
        val minutes = DateTimeUtils.nowMinutesOfDay()
        assertTrue(minutes in 0..1439)
    }
}