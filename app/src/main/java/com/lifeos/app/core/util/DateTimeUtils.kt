package com.lifeos.app.core.util

import java.time.Instant
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

    // DateTimeFormatter is immutable and thread-safe; building one per call
    // showed up in list rows (tasks, timeline, memory list) on every row.
    private val MINUTE_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")
    private val FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy")

    fun today(): LocalDate = LocalDate.now()

    /** Current local time as minutes since midnight (0..1439). */
    fun nowMinutesOfDay(): Int {
        val now = LocalTime.now()
        return now.hour * 60 + now.minute
    }

    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun LocalTime.toMinutesSinceMidnight(): Int = this.hour * 60 + this.minute

    fun minutesToLocalTime(minutes: Int): LocalTime = LocalTime.of(minutes / 60, minutes % 60)

    fun formatMinutes(minutes: Int): String {
        val t = minutesToLocalTime(minutes)
        return t.format(MINUTE_FORMATTER)
    }

    fun formatFullDate(date: LocalDate): String =
        date.format(FULL_DATE_FORMATTER)

    fun formatDayOfWeek(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()

    /** Short day-of-week name for compact date chips and charts, e.g. "Mon". */
    fun shortDayName(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    fun greeting(hour: Int = LocalDateTime.now().hour): String = when (hour) {
        in 4..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    fun startOfMonthEpochDay(date: LocalDate = today()): Long =
        date.withDayOfMonth(1).toEpochDay()

    fun endOfMonthEpochDay(date: LocalDate = today()): Long =
        date.withDayOfMonth(date.lengthOfMonth()).toEpochDay()

    fun zoneId(): ZoneId = ZoneId.systemDefault()

    /**
     * Local wall-clock minute of day (0..1439) for a UTC-epoch-millis instant,
     * resolved through the system zone. Replaces ad-hoc `hour*60+minute` math
     * so offsets/DST never move an event across a day boundary.
     */
    fun minutesOfDay(epochMillis: Long): Int =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId()).toLocalTime().toMinutesSinceMidnight()

    /**
     * Inclusive local-day range in UTC-epoch-millis from the start of
     * [startEpochDay] to the end of [endEpochDay]. Centralises the boundary
     * math so callers never write `end*86_400_000L` (UTC-midnight, wrong
     * day for non-UTC zones).
     */
    fun dayRangeMillis(startEpochDay: Long, endEpochDay: Long): Pair<Long, Long> =
        startOfLocalDayMillis(startEpochDay) to endOfLocalDayMillis(endEpochDay)

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
