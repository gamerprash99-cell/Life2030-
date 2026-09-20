package com.lifeos.app.core.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Central place for date/time conversions between Room-friendly primitives
 * (epoch day / minutes-since-midnight / epoch millis) and java.time types,
 * so screens never do this math themselves.
 */
object DateTimeUtils {

    fun today(): LocalDate = LocalDate.now()

    fun nowEpochMillis(): Long = System.currentTimeMillis()

    /** Current local time as minutes since midnight (0..1439). */
    fun nowMinutesOfDay(): Int {
        val now = LocalTime.now()
        return now.hour * 60 + now.minute
    }

    fun LocalDate.toEpochDayLong(): Long = this.toEpochDay()

    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun LocalTime.toMinutesSinceMidnight(): Int = this.hour * 60 + this.minute

    fun minutesToLocalTime(minutes: Int): LocalTime = LocalTime.of(minutes / 60, minutes % 60)

    /** Whole-number percentage of the current local day that has already elapsed (0..100). */
    fun dayProgressPercent(now: LocalTime = LocalTime.now()): Int {
        val minutes = now.hour * 60 + now.minute
        return (minutes * 100 / 1440).coerceIn(0, 100)
    }

    fun formatMinutes(minutes: Int): String {
        val t = minutesToLocalTime(minutes)
        return t.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    fun formatFullDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

    fun formatDayOfWeek(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()

    fun greeting(hour: Int = LocalDateTime.now().hour): String = when (hour) {
        in 4..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    fun startOfWeekEpochDay(date: LocalDate = today()): Long =
        date.minusDays((date.dayOfWeek.value - 1).toLong()).toEpochDay()

    fun endOfWeekEpochDay(date: LocalDate = today()): Long =
        startOfWeekEpochDay(date) + 6

    fun startOfMonthEpochDay(date: LocalDate = today()): Long =
        date.withDayOfMonth(1).toEpochDay()

    fun endOfMonthEpochDay(date: LocalDate = today()): Long =
        date.withDayOfMonth(date.lengthOfMonth()).toEpochDay()

    fun zoneId(): ZoneId = ZoneId.systemDefault()

    /**
     * UTC-epoch-millis boundary at the *local* start of [epochDay]. Using this
     * instead of `epochDay * 86_400_000L` is essential: the raw multiplication
     * treats midnight as UTC, so any user not on UTC would see notes/tasks
     * fall into the wrong calendar day (and DST days would be off by an hour).
     */
    fun startOfLocalDayMillis(epochDay: Long): Long =
        epochDayToLocalDate(epochDay).atStartOfDay(zoneId()).toInstant().toEpochMilli()

    /** Exclusive local-midnight boundary that begins the day after [epochDay]. */
    fun endOfLocalDayMillis(epochDay: Long): Long =
        epochDayToLocalDate(epochDay + 1).atStartOfDay(zoneId()).toInstant().toEpochMilli()
}
