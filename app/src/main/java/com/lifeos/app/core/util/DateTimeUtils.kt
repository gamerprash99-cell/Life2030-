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

    fun LocalDate.toEpochDayLong(): Long = this.toEpochDay()

    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun LocalTime.toMinutesSinceMidnight(): Int = this.hour * 60 + this.minute

    fun minutesToLocalTime(minutes: Int): LocalTime = LocalTime.of(minutes / 60, minutes % 60)

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
}
