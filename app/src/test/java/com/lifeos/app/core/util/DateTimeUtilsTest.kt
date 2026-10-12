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

    @Test
    fun `minutesOfDay converts local wall-clock via the system zone`() {
        val date = LocalDate.of(2024, 10, 27) // DST transition day in many zones
        val midnight = DateTimeUtils.startOfLocalDayMillis(date.toEpochDay())
        assertEquals(0, DateTimeUtils.minutesOfDay(midnight))
        assertEquals(720, DateTimeUtils.minutesOfDay(midnight + 12 * 60 * 60 * 1000L))
        assertEquals(1439, DateTimeUtils.minutesOfDay(DateTimeUtils.endOfLocalDayMillis(date.toEpochDay()) - 60_000L))
    }

    @Test
    fun `dayRangeMillis spans day-count days with local-midnight boundaries`() {
        val startDay = LocalDate.of(2024, 6, 15).toEpochDay() // no DST transition anywhere in June
        val endDay = startDay + 2
        val (start, end) = DateTimeUtils.dayRangeMillis(startDay, endDay)

        assertEquals(DateTimeUtils.startOfLocalDayMillis(startDay), start)
        assertEquals(DateTimeUtils.endOfLocalDayMillis(endDay), end)
        // exactly three local days of wall-clock span
        assertEquals(3 * 86_400_000L, end - start)
    }

    @Test
    fun `dayRangeMillis stays on local midnight even across a DST day`() {
        val dstDay = LocalDate.of(2024, 10, 27).toEpochDay() // fall-back/spring-forward in many zones
        val (start, end) = DateTimeUtils.dayRangeMillis(dstDay, dstDay)

        assertEquals(LocalTime.MIDNIGHT, Instant.ofEpochMilli(start).atZone(DateTimeUtils.zoneId()).toLocalTime())
        assertEquals(LocalTime.MIDNIGHT, Instant.ofEpochMilli(end).atZone(DateTimeUtils.zoneId()).toLocalTime())
        assertTrue(end > start)
        // contiguous with the local boundaries of that single day
        assertEquals(DateTimeUtils.startOfLocalDayMillis(dstDay), start)
        assertEquals(DateTimeUtils.endOfLocalDayMillis(dstDay), end)
    }
}