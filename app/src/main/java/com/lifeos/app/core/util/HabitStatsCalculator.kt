package com.lifeos.app.core.util

import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitFrequency
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The schedule a habit is expected to be performed on, reduced to plain data
 * so [HabitStatsCalculator] stays free of storage/Android dependencies.
 */
data class HabitSchedule(
    val frequency: HabitFrequency,
    val customDays: Set<DayOfWeek>,
    val startEpochDay: Long
)

/** Single shared builder for [HabitSchedule] from a stored habit. */
fun HabitEntity.toSchedule(): HabitSchedule = HabitSchedule(
    frequency = frequency,
    customDays = HabitStatsCalculator.parseCustomDays(customDaysCsv),
    startEpochDay = startDateEpochDay
)

/**
 * Pure arithmetic for habit analytics (streaks, completion percentages).
 * No storage or Android dependencies, so it can be unit-tested directly.
 * See HabitRepository.computeAnalytics() for the production call site.
 */
object HabitStatsCalculator {

    /** True when [epochDay] is a day the habit is scheduled to be done. */
    fun isScheduled(schedule: HabitSchedule, epochDay: Long): Boolean {
        if (epochDay < schedule.startEpochDay) return false
        return when (schedule.frequency) {
            HabitFrequency.DAILY -> true
            // "Weekly" means once a week, on the same weekday the habit started.
            HabitFrequency.WEEKLY ->
                LocalDate.ofEpochDay(epochDay).dayOfWeek ==
                    LocalDate.ofEpochDay(schedule.startEpochDay).dayOfWeek
            HabitFrequency.CUSTOM ->
                LocalDate.ofEpochDay(epochDay).dayOfWeek in schedule.customDays
        }
    }

    /**
     * Parses the persisted `customDaysCsv` into weekdays. Accepts ISO day
     * numbers ("1".."7", Monday..Sunday) or names/short names ("MON", "monday").
     * Unknown tokens are ignored.
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

    /**
     * Consecutive *scheduled* days through [today] on which the habit was done.
     *
     * If today is scheduled but not yet completed, it is treated as still in
     * progress and does not break the streak. Days the habit is not scheduled
     * for are skipped entirely, so a Mon/Wed/Fri habit is not penalised for
     * being "missed" on Tuesday.
     */
    fun currentStreak(doneDays: Set<Long>, today: Long, schedule: HabitSchedule): Int {
        if (today < schedule.startEpochDay) return 0
        var cursor = today
        if (isScheduled(schedule, cursor) && !doneDays.contains(cursor)) cursor--
        var streak = 0
        while (cursor >= schedule.startEpochDay) {
            if (isScheduled(schedule, cursor)) {
                if (doneDays.contains(cursor)) streak++ else break
            }
            cursor--
        }
        return streak
    }

    /** Longest run of consecutive *scheduled* done-days, up to and including [toEpochDay]. */
    fun longestStreak(doneDays: Set<Long>, schedule: HabitSchedule, toEpochDay: Long): Int {
        val firstDone = doneDays.minOrNull() ?: return 0
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
        return longest
    }

    /** Number of scheduled days in the inclusive range [fromEpochDay]..[toEpochDay]. */
    fun scheduledDayCount(schedule: HabitSchedule, fromEpochDay: Long, toEpochDay: Long): Int {
        if (toEpochDay < fromEpochDay) return 0
        var count = 0
        var day = fromEpochDay
        while (day <= toEpochDay) {
            if (isScheduled(schedule, day)) count++
            day++
        }
        return count
    }

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

    /** Whole-number completion percentage, guarded against divide-by-zero. */
    fun completionPercent(completedDays: Int, daysElapsed: Int): Int =
        if (daysElapsed > 0) (completedDays * 100) / daysElapsed else 0
}
