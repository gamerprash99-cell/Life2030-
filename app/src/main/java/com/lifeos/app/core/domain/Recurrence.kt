package com.lifeos.app.core.domain

import java.time.LocalDate

/**
 * Habit / task recurrence.
 *
 * Stored as one of these [Recurrence] ids plus a JSON-free parameter string
 * (for `interval`, the parameter is the interval count; for `daysOfWeek`, a
 * comma list such as `MON,WED,FRI`). Keeping it as a plain string means no
 * serialization dependency and trivial SQL inspection.
 */
enum class Recurrence(val id: String, val label: String) {
    NONE("none", "Does not repeat"),
    DAILY("daily", "Every day"),
    EVERY_N_DAYS("every_n_days", "Every N days"),
    WEEKLY("weekly", "Every week"),
    DAYS_OF_WEEK("days_of_week", "On selected weekdays"),
    MONTHLY("monthly", "Every month"),
    YEARLY("yearly", "Every year");

    companion object {
        fun fromId(id: String?): Recurrence =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}

object RecurrenceRules {
    private val DAY_ORDER = listOf(
        "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN",
    )

    fun dayCodes(): List<String> = DAY_ORDER

    /** Next occurrence strictly after [from], or null for non-repeating series. */
    fun next(from: LocalDate, recurrence: Recurrence, parameter: String?): LocalDate? =
        when (recurrence) {
            Recurrence.NONE -> null
            Recurrence.DAILY -> from.plusDays(1)
            Recurrence.EVERY_N_DAYS -> {
                val n = (parameter?.toIntOrNull() ?: 1).coerceIn(1, 365)
                from.plusDays(n.toLong())
            }
            Recurrence.WEEKLY -> from.plusWeeks(1)
            Recurrence.DAYS_OF_WEEK -> nextSelectedDay(from, parameter)
            Recurrence.MONTHLY -> from.plusMonths(1)
            Recurrence.YEARLY -> from.plusYears(1)
        }

    /** Previous occurrence on or before [date], used for streak backfill. */
    fun previousOnOrBefore(date: LocalDate, recurrence: Recurrence, parameter: String?): LocalDate? =
        when (recurrence) {
            Recurrence.NONE -> date
            Recurrence.DAILY -> date
            Recurrence.EVERY_N_DAYS -> date
            Recurrence.WEEKLY -> {
                val anchor = date.minusWeeks(1)
                if (anchor.dayOfWeek == date.dayOfWeek) anchor else date.withDayOfMonth(1)
                    .let { if (it <= date) it else date }
            }
            Recurrence.DAYS_OF_WEEK -> {
                val days = parseDays(parameter)
                var cursor = date
                repeat(7) {
                    if (cursor.dayOfWeek.name.take(3) in days) return cursor
                    cursor = cursor.minusDays(1)
                }
                null
            }
            Recurrence.MONTHLY -> date.minusMonths(1)
            Recurrence.YEARLY -> date.minusYears(1)
        }

    /**
     * Whether a series with these rules should have an occurrence on [date],
     * given the series started on [startDate].
     */
    fun occursOn(date: LocalDate, startDate: LocalDate, recurrence: Recurrence, parameter: String?): Boolean {
        if (date.isBefore(startDate)) return false
        return when (recurrence) {
            Recurrence.NONE -> date == startDate
            Recurrence.DAILY -> true
            Recurrence.EVERY_N_DAYS -> {
                val n = (parameter?.toIntOrNull() ?: 1).coerceIn(1, 365)
                val elapsed = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
                elapsed % n == 0L
            }
            Recurrence.WEEKLY ->
                java.time.temporal.ChronoUnit.DAYS.between(startDate, date) % 7 == 0L &&
                    date.dayOfWeek == startDate.dayOfWeek
            Recurrence.DAYS_OF_WEEK ->
                date.dayOfWeek.name.take(3) in parseDays(parameter)
            Recurrence.MONTHLY -> date.dayOfMonth == startDate.dayOfMonth ||
                (date.dayOfMonth == date.lengthOfMonth() && startDate.dayOfMonth > date.lengthOfMonth())
            Recurrence.YEARLY ->
                date.month == startDate.month && date.dayOfMonth == startDate.dayOfMonth
        }
    }

    /** All weekday codes in a run of `MON,WED,FRI`, validated against [DAY_ORDER]. */
    fun parseDays(parameter: String?): Set<String> {
        if (parameter.isNullOrBlank()) return emptySet()
        return parameter.split(',')
            .map { it.trim().uppercase() }
            .filter { it in DAY_ORDER }
            .toSet()
    }

    fun formatDays(days: Collection<String>): String =
        DAY_ORDER.filter { it in days }.joinToString(",")

    private fun nextSelectedDay(from: LocalDate, parameter: String?): LocalDate? {
        val days = parseDays(parameter)
        if (days.isEmpty()) return null
        var cursor = from.plusDays(1)
        repeat(7) {
            if (cursor.dayOfWeek.name.take(3) in days) return cursor
            cursor = cursor.plusDays(1)
        }
        return null
    }
}
