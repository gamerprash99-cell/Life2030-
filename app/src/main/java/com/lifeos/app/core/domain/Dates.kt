package com.lifeos.app.core.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields

/**
 * Local-date helpers.
 *
 * Rule from the spec: every local date (diary day, habit completion, task due date,
 * expense date) is stored and compared as an ISO `yyyy-MM-dd` string, never as a
 * timestamp. Timestamps are reserved for instants (createdAt/updatedAt/deletedAt),
 * which are stored as epoch milliseconds UTC.
 *
 * Using a plain string key keeps "today" stable across time zones and makes
 * equality/grouping trivial in SQL.
 */
object Dates {
    val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private const val PATTERN_HUMAN = "d MMM yyyy"
    private const val PATTERN_HUMAN_LONG = "EEEE d MMMM yyyy"

    fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate = LocalDate.now(zone)

    /** `2026-10-20` */
    fun iso(date: LocalDate): String = date.format(ISO)

    /** `2026-10-20`; throws if the input is malformed. */
    fun parse(iso: String): LocalDate = LocalDate.parse(iso, ISO)

    fun nowMillis(): Long = System.currentTimeMillis()

    fun toEpochMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun toLocalDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun human(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern(PATTERN_HUMAN))

    fun humanLong(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern(PATTERN_HUMAN_LONG))

    fun dayOfWeek(date: LocalDate): DayOfWeek = date.dayOfWeek

    /** First day of the week for the given locale-sensitive week fields. */
    fun firstDayOfWeek(): DayOfWeek = WeekFields.ISO.firstDayOfWeek

    /** Inclusive day count between two dates: `daysBetween(friday, sunday) == 3`. */
    fun daysBetweenInclusive(start: LocalDate, end: LocalDate): Long =
        ChronoUnit.DAYS.between(start, end) + 1

    fun isToday(iso: String?, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        iso != null && parseOrNull(iso) == today(zone)

    fun parseOrNull(iso: String?): LocalDate? = try {
        if (iso.isNullOrBlank()) null else LocalDate.parse(iso, ISO)
    } catch (_: Exception) {
        null
    }

    fun atStartOfDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDateTime()

    /** Human relative label used in lists: Today / Yesterday / 12 Mar 2026. */
    fun relative(iso: String?, zone: ZoneId = ZoneId.systemDefault()): String {
        val date = parseOrNull(iso) ?: return ""
        val now = today(zone)
        return when (date) {
            now -> "Today"
            now.minusDays(1) -> "Yesterday"
            now.plusDays(1) -> "Tomorrow"
            else -> human(date)
        }
    }
}
