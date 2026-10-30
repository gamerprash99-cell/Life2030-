package com.lifeos.app.core.util

import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitFrequency
import com.lifeos.app.data.db.entities.HabitOutcome
import com.lifeos.app.data.db.entities.HabitScheduleType
import com.lifeos.app.data.db.entities.HabitTrackingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * The schedule a habit is expected to be performed on, reduced to plain data
 * so [HabitStatsCalculator] stays free of storage/Android dependencies.
 *
 * [frequency] is the pre-v7 representation and is only consulted when [type]
 * is [HabitScheduleType.LEGACY]. Keeping both means existing DAILY / WEEKLY /
 * CUSTOM rows retain their exact old meaning while new habits use the richer
 * schedule types.
 */
data class HabitSchedule(
    val frequency: HabitFrequency,
    val customDays: Set<DayOfWeek>,
    val startEpochDay: Long,
    val type: HabitScheduleType = HabitScheduleType.LEGACY,
    val monthDays: Set<Int> = emptySet(),
    val timesPerPeriod: Int = 0
)

/** Single shared builder for [HabitSchedule] from a stored habit. */
fun HabitEntity.toSchedule(): HabitSchedule {
    val weekdays = HabitStatsCalculator.parseCustomDays(
        daysOfWeekCsv?.takeIf { it.isNotBlank() } ?: customDaysCsv
    )
    return HabitSchedule(
        frequency = frequency,
        customDays = weekdays,
        startEpochDay = startDateEpochDay,
        type = scheduleType,
        monthDays = HabitStatsCalculator.parseMonthDays(daysOfMonthCsv),
        timesPerPeriod = (timesPerPeriod ?: 0).coerceAtLeast(0)
    )
}

/** True when this completion represents a successful scheduled day. */
fun HabitEntity.isCompletionDone(completion: HabitCompletionEntity): Boolean {
    if (completion.outcome == HabitOutcome.SLIPPED) return false
    val target = completion.targetCount.takeIf { it > 0 } ?: goalCount.coerceAtLeast(1)
    return completion.progressCount >= target
}

/** The target a completion is measured against (snapshot first, then habit). */
fun HabitEntity.completionTarget(completion: HabitCompletionEntity?): Int =
    completion?.targetCount?.takeIf { it > 0 } ?: goalCount.coerceAtLeast(1)

/**
 * Pure arithmetic for habit analytics (streaks, completion percentages).
 * No storage or Android dependencies, so it can be unit-tested directly.
 */
object HabitStatsCalculator {

    /** True when [epochDay] is a day the habit is scheduled to be performed. */
    fun isScheduled(schedule: HabitSchedule, epochDay: Long): Boolean {
        if (epochDay < schedule.startEpochDay) return false
        val date = LocalDate.ofEpochDay(epochDay)
        return when (schedule.type) {
            HabitScheduleType.LEGACY -> when (schedule.frequency) {
                HabitFrequency.DAILY -> true
                // "Weekly" means once a week, on the same weekday the habit started.
                HabitFrequency.WEEKLY ->
                    date.dayOfWeek == LocalDate.ofEpochDay(schedule.startEpochDay).dayOfWeek
                HabitFrequency.CUSTOM -> date.dayOfWeek in schedule.customDays
            }
            HabitScheduleType.EVERY_DAY -> true
            HabitScheduleType.WEEKDAYS -> date.dayOfWeek in schedule.customDays
            HabitScheduleType.MONTH_DAYS -> date.dayOfMonth in schedule.monthDays
            // Flexible period schedules accept a check-in on any day. Whether a
            // particular day is still *due* is period-state dependent and is
            // answered by [isDue].
            HabitScheduleType.TIMES_PER_WEEK,
            HabitScheduleType.TIMES_PER_MONTH -> true
        }
    }

    /**
     * True when [epochDay] is a day the user can still act on: fixed schedules
     * are due on every scheduled day; an N-per-week/month habit stops being due
     * once its target for that period has already been met.
     */
    fun isDue(
        schedule: HabitSchedule,
        epochDay: Long,
        doneDays: Set<Long>
    ): Boolean {
        if (!isScheduled(schedule, epochDay)) return false
        return when (schedule.type) {
            HabitScheduleType.TIMES_PER_WEEK,
            HabitScheduleType.TIMES_PER_MONTH -> {
                val target = schedule.timesPerPeriod
                if (target <= 0) return false
                if (doneDays.contains(epochDay)) return true
                val (start, end) = periodBounds(schedule, epochDay)
                val doneBefore = doneDays.count { it in start until epochDay }
                doneBefore < target
            }
            else -> true
        }
    }

    /** Number of successful days in the period containing [epochDay]. */
    fun completedInPeriod(schedule: HabitSchedule, doneDays: Set<Long>, epochDay: Long): Int {
        if (schedule.type != HabitScheduleType.TIMES_PER_WEEK &&
            schedule.type != HabitScheduleType.TIMES_PER_MONTH
        ) return 0
        val (start, end) = periodBounds(schedule, epochDay)
        return doneDays.count { it in start..end }
    }

    /**
     * Parses the persisted weekday CSV into weekdays. Accepts ISO day numbers
     * ("1".."7", Monday..Sunday) or names/short names ("MON", "monday"). Unknown
     * tokens are ignored.
     */
    fun parseCustomDays(csv: String?): Set<DayOfWeek> {
        if (csv.isNullOrBlank()) return emptySet()
        return csv.split(',')
            .mapNotNull { raw ->
                val token = raw.trim()
                if (token.isEmpty()) return@mapNotNull null
                token.toIntOrNull()?.let { number ->
                    runCatching { DayOfWeek.of(number) }.getOrNull()
                } ?: runCatching {
                    DayOfWeek.valueOf(token.uppercase())
                }.getOrNull() ?: DayOfWeek.entries.firstOrNull {
                    it.name.startsWith(token.uppercase()) && token.length >= 3
                }
            }
            .toSet()
    }

    /** Parses a day-of-month CSV; values outside 1..31 are ignored. */
    fun parseMonthDays(csv: String?): Set<Int> {
        if (csv.isNullOrBlank()) return emptySet()
        return csv.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..31 }
            .toSet()
    }

    /**
     * Consecutive *scheduled* days through [today] on which the habit was done.
     *
     * If today is scheduled but not yet completed, it is treated as still in
     * progress and does not break the streak. Unscheduled days are skipped.
     * For N-per-week/month habits, consecutive successful periods are counted
     * instead of calendar days; the current period never breaks the streak
     * while it is still in progress.
     */
    fun currentStreak(doneDays: Set<Long>, today: Long, schedule: HabitSchedule): Int {
        if (today < schedule.startEpochDay) return 0
        return when (schedule.type) {
            HabitScheduleType.TIMES_PER_WEEK -> periodStreak(doneDays, today, schedule, weekly = true)
            HabitScheduleType.TIMES_PER_MONTH -> periodStreak(doneDays, today, schedule, weekly = false)
            else -> {
                var cursor = today
                if (isScheduled(schedule, cursor) && !doneDays.contains(cursor)) cursor--
                var streak = 0
                while (cursor >= schedule.startEpochDay) {
                    if (isScheduled(schedule, cursor)) {
                        if (doneDays.contains(cursor)) streak++ else break
                    }
                    cursor--
                }
                streak
            }
        }
    }

    /** Longest run of consecutive scheduled done-days up to [toEpochDay]. */
    fun longestStreak(doneDays: Set<Long>, schedule: HabitSchedule, toEpochDay: Long): Int {
        if (doneDays.isEmpty()) return 0
        return when (schedule.type) {
            HabitScheduleType.TIMES_PER_WEEK, HabitScheduleType.TIMES_PER_MONTH -> {
                val weekly = schedule.type == HabitScheduleType.TIMES_PER_WEEK
                val target = schedule.timesPerPeriod
                if (target <= 0) return 0
                var periodStart = if (weekly) {
                    startOfWeek(LocalDate.ofEpochDay(doneDays.min()))
                } else {
                    LocalDate.ofEpochDay(doneDays.min()).withDayOfMonth(1)
                }
                val lastDate = LocalDate.ofEpochDay(toEpochDay)
                var longest = 0
                var running = 0
                while (!periodStart.isAfter(lastDate)) {
                    val end = if (weekly) periodStart.plusDays(6) else {
                        YearMonth.from(periodStart).atEndOfMonth()
                    }
                    val done = doneDays.count {
                        it >= periodStart.toEpochDay() && it <= end.toEpochDay()
                    }
                    if (done >= target) {
                        running++
                        longest = maxOf(longest, running)
                    } else if (!end.isAfter(lastDate)) {
                        running = 0
                    }
                    periodStart = if (weekly) periodStart.plusWeeks(1) else periodStart.plusMonths(1)
                }
                longest
            }
            else -> {
                val firstDone = doneDays.min()
                var longest = 0
                var running = 0
                var day = firstDone
                while (day <= toEpochDay) {
                    if (isScheduled(schedule, day)) {
                        if (doneDays.contains(day)) {
                            running++
                            longest = maxOf(longest, running)
                        } else {
                            running = 0
                        }
                    }
                    day++
                }
                longest
            }
        }
    }

    /**
     * Number of expected occurrences in [fromEpochDay]..[toEpochDay].
     * Fixed schedules count scheduled days; N-per-period schedules count the
     * target per period, capped by the days actually available in the range.
     */
    fun scheduledDayCount(schedule: HabitSchedule, fromEpochDay: Long, toEpochDay: Long): Int {
        if (toEpochDay < fromEpochDay) return 0
        return when (schedule.type) {
            HabitScheduleType.TIMES_PER_WEEK,
            HabitScheduleType.TIMES_PER_MONTH -> {
                val target = schedule.timesPerPeriod
                if (target <= 0) return 0
                val weekly = schedule.type == HabitScheduleType.TIMES_PER_WEEK
                var cursor = if (weekly) startOfWeek(LocalDate.ofEpochDay(fromEpochDay))
                else LocalDate.ofEpochDay(fromEpochDay).withDayOfMonth(1)
                var total = 0
                while (cursor.toEpochDay() <= toEpochDay) {
                    val end = if (weekly) cursor.plusDays(6) else {
                        YearMonth.from(cursor).atEndOfMonth()
                    }
                    val availableStart = maxOf(cursor.toEpochDay(), fromEpochDay)
                    val availableEnd = minOf(end.toEpochDay(), toEpochDay)
                    val available = (availableEnd - availableStart + 1).coerceAtLeast(0)
                    total += minOf(target.toLong(), available).toInt()
                    cursor = if (weekly) cursor.plusWeeks(1) else cursor.plusMonths(1)
                }
                total
            }
            else -> {
                var count = 0
                var day = maxOf(fromEpochDay, schedule.startEpochDay)
                while (day <= toEpochDay) {
                    if (isScheduled(schedule, day)) count++
                    day++
                }
                count
            }
        }
    }

    /**
     * Successful occurrences in [fromEpochDay]..[toEpochDay]. For flexible
     * schedules extra check-ins beyond the period target do not inflate the
     * percentage above 100.
     */
    fun completedOccurrences(
        schedule: HabitSchedule,
        doneDays: Set<Long>,
        fromEpochDay: Long,
        toEpochDay: Long
    ): Int {
        if (toEpochDay < fromEpochDay) return 0
        return when (schedule.type) {
            HabitScheduleType.TIMES_PER_WEEK,
            HabitScheduleType.TIMES_PER_MONTH -> {
                val target = schedule.timesPerPeriod
                if (target <= 0) return 0
                val weekly = schedule.type == HabitScheduleType.TIMES_PER_WEEK
                var cursor = if (weekly) startOfWeek(LocalDate.ofEpochDay(fromEpochDay))
                else LocalDate.ofEpochDay(fromEpochDay).withDayOfMonth(1)
                var total = 0
                while (cursor.toEpochDay() <= toEpochDay) {
                    val end = if (weekly) cursor.plusDays(6) else {
                        YearMonth.from(cursor).atEndOfMonth()
                    }
                    val availableStart = maxOf(cursor.toEpochDay(), fromEpochDay)
                    val availableEnd = minOf(end.toEpochDay(), toEpochDay)
                    val done = doneDays.count { it in availableStart..availableEnd }
                    total += minOf(done, target)
                    cursor = if (weekly) cursor.plusWeeks(1) else cursor.plusMonths(1)
                }
                total
            }
            else -> doneDays.count { day ->
                day in fromEpochDay..toEpochDay &&
                    isScheduled(schedule, day) &&
                    day >= schedule.startEpochDay
            }
        }
    }

    /** Percentage denominator for streak labels: day, week or month. */
    fun streakUnit(schedule: HabitSchedule): String = when (schedule.type) {
        HabitScheduleType.TIMES_PER_WEEK -> "week"
        HabitScheduleType.TIMES_PER_MONTH -> "month"
        else -> "day"
    }

    /** Whole-number completion percentage, guarded against divide-by-zero. */
    fun completionPercent(completedDays: Int, daysElapsed: Int): Int =
        if (daysElapsed > 0) (completedDays * 100 / daysElapsed).coerceIn(0, 100) else 0

    /** Consecutive days through [today] (inclusive) on which the habit was done. */
    fun currentStreak(doneDays: Set<Long>, today: Long): Int {
        var streak = 0
        var cursor = today
        while (doneDays.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }

    /** Longest run of consecutive done-days anywhere in [doneDays]. */
    fun longestStreak(doneDays: Set<Long>): Int {
        var longest = 0
        var running = 0
        var prevDay: Long? = null
        for (day in doneDays.sorted()) {
            running = if (prevDay != null && day == prevDay + 1) running + 1 else 1
            longest = maxOf(longest, running)
            prevDay = day
        }
        return longest
    }

    // ---- period helpers ---------------------------------------------------

    private fun startOfWeek(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

    private fun periodBounds(schedule: HabitSchedule, epochDay: Long): Pair<Long, Long> {
        val date = LocalDate.ofEpochDay(epochDay)
        return if (schedule.type == HabitScheduleType.TIMES_PER_WEEK) {
            val start = startOfWeek(date)
            start.toEpochDay() to start.plusDays(6).toEpochDay()
        } else {
            val start = date.withDayOfMonth(1)
            start.toEpochDay() to YearMonth.from(date).atEndOfMonth().toEpochDay()
        }
    }

    private fun periodStreak(
        doneDays: Set<Long>,
        today: Long,
        schedule: HabitSchedule,
        weekly: Boolean
    ): Int {
        val target = schedule.timesPerPeriod
        if (target <= 0) return 0
        val (currentStart, _) = periodBounds(schedule, today)
        var periodStart = if (weekly) {
            startOfWeek(LocalDate.ofEpochDay(currentStart))
        } else {
            LocalDate.ofEpochDay(currentStart).withDayOfMonth(1)
        }
        // If the current period is already complete count it; otherwise treat it
        // as still in progress and start from the previous period.
        val currentEnd = if (weekly) periodStart.plusDays(6) else {
            YearMonth.from(periodStart).atEndOfMonth()
        }
        val currentDone = doneDays.count {
            it >= periodStart.toEpochDay() && it <= currentEnd.toEpochDay()
        }
        if (currentDone < target) {
            periodStart = if (weekly) periodStart.minusWeeks(1) else periodStart.minusMonths(1)
        }
        var streak = 0
        while (periodStart.toEpochDay() + (if (weekly) 6 else 0) >= schedule.startEpochDay) {
            val end = if (weekly) periodStart.plusDays(6) else {
                YearMonth.from(periodStart).atEndOfMonth()
            }
            val done = doneDays.count {
                it >= periodStart.toEpochDay() && it <= end.toEpochDay()
            }
            if (done >= target) {
                streak++
                periodStart = if (weekly) periodStart.minusWeeks(1) else periodStart.minusMonths(1)
            } else {
                break
            }
        }
        return streak
    }
}
