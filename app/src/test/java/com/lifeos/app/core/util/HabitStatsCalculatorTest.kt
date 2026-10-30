package com.lifeos.app.core.util

import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitFrequency
import com.lifeos.app.data.db.entities.HabitOutcome
import com.lifeos.app.data.db.entities.HabitScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitStatsCalculatorTest {

    private fun day(year: Int, month: Int, dom: Int): Long = LocalDate.of(year, month, dom).toEpochDay()

    private fun schedule(
        frequency: HabitFrequency,
        start: Long = day(2024, 1, 1),
        customDays: Set<DayOfWeek> = emptySet()
    ) = HabitSchedule(frequency, customDays, start)

    @Test
    fun `current streak counts consecutive days through today`() {
        val today = day(2024, 1, 10)
        val done = setOf(day(2024, 1, 10), day(2024, 1, 9), day(2024, 1, 8))
        assertEquals(3, HabitStatsCalculator.currentStreak(done, today))
    }

    @Test
    fun `current streak resets on a missed day`() {
        val today = day(2024, 1, 10)
        val done = setOf(day(2024, 1, 10), day(2024, 1, 9), day(2024, 1, 7))
        assertEquals(2, HabitStatsCalculator.currentStreak(done, today))
    }

    @Test
    fun `current streak is zero when today is not done`() {
        val today = day(2024, 1, 10)
        val done = setOf(day(2024, 1, 9), day(2024, 1, 8), day(2024, 1, 7))
        assertEquals(0, HabitStatsCalculator.currentStreak(done, today))
    }

    @Test
    fun `current streak is zero on empty history`() {
        assertEquals(0, HabitStatsCalculator.currentStreak(emptySet(), day(2024, 1, 10)))
    }

    @Test
    fun `longest streak finds the best run across gaps`() {
        val done = setOf(
            day(2024, 1, 1), day(2024, 1, 2), day(2024, 1, 3),
            day(2024, 1, 5),
            day(2024, 1, 10), day(2024, 1, 11), day(2024, 1, 12), day(2024, 1, 13)
        )
        assertEquals(4, HabitStatsCalculator.longestStreak(done))
    }

    @Test
    fun `longest streak is zero on empty history`() {
        assertEquals(0, HabitStatsCalculator.longestStreak(emptySet()))
    }

    @Test
    fun `longest streak handles single day`() {
        assertEquals(1, HabitStatsCalculator.longestStreak(setOf(day(2024, 1, 1))))
    }

    @Test
    fun `completion percent is integer floor`() {
        assertEquals(50, HabitStatsCalculator.completionPercent(1, 2))
        assertEquals(33, HabitStatsCalculator.completionPercent(1, 3))
        assertEquals(100, HabitStatsCalculator.completionPercent(3, 3))
    }

    @Test
    fun `completion percent guards divide by zero`() {
        assertEquals(0, HabitStatsCalculator.completionPercent(5, 0))
    }

    // ---- schedule-aware behavior -------------------------------------------

    @Test
    fun `daily schedule treats every day on or after start as scheduled`() {
        val s = schedule(HabitFrequency.DAILY)
        assertFalse(HabitStatsCalculator.isScheduled(s, day(2023, 12, 31)))
        assertTrue(HabitStatsCalculator.isScheduled(s, day(2024, 1, 1)))
        assertTrue(HabitStatsCalculator.isScheduled(s, day(2024, 6, 1)))
    }

    @Test
    fun `weekly schedule only matches the start weekday`() {
        // 2024-01-01 is a Monday.
        val s = schedule(HabitFrequency.WEEKLY)
        assertTrue(HabitStatsCalculator.isScheduled(s, day(2024, 1, 8)))
        assertFalse(HabitStatsCalculator.isScheduled(s, day(2024, 1, 9)))
    }

    @Test
    fun `custom schedule matches only configured weekdays`() {
        val s = schedule(HabitFrequency.CUSTOM, customDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        assertTrue(HabitStatsCalculator.isScheduled(s, day(2024, 1, 3)))   // Wed
        assertFalse(HabitStatsCalculator.isScheduled(s, day(2024, 1, 2)))  // Tue
    }

    @Test
    fun `parse custom days accepts iso numbers and names`() {
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), HabitStatsCalculator.parseCustomDays("1,wed"))
        assertEquals(setOf(DayOfWeek.FRIDAY), HabitStatsCalculator.parseCustomDays("Friday"))
        assertTrue(HabitStatsCalculator.parseCustomDays(null).isEmpty())
        assertTrue(HabitStatsCalculator.parseCustomDays("bogus,99").isEmpty())
    }

    @Test
    fun `mwf streak ignores off days and counts scheduled completions`() {
        val s = schedule(HabitFrequency.CUSTOM, customDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        // Mon 2024-01-08, Wed 01-10, Fri 01-12 completed; today Sun 01-14.
        val done = setOf(day(2024, 1, 8), day(2024, 1, 10), day(2024, 1, 12))
        assertEquals(3, HabitStatsCalculator.currentStreak(done, day(2024, 1, 14), s))
    }

    @Test
    fun `unscheduled today does not break an in-progress streak`() {
        val s = schedule(HabitFrequency.CUSTOM, customDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        val done = setOf(day(2024, 1, 8), day(2024, 1, 10)) // Mon + Wed
        // Today is Thu 01-11 (unscheduled): streak should remain 2, not reset.
        assertEquals(2, HabitStatsCalculator.currentStreak(done, day(2024, 1, 11), s))
    }

    @Test
    fun `scheduled today not yet done does not break the streak`() {
        val s = schedule(HabitFrequency.DAILY)
        val done = setOf(day(2024, 1, 8), day(2024, 1, 9))
        // Today 01-10 is scheduled but incomplete: keep yesterday's run.
        assertEquals(2, HabitStatsCalculator.currentStreak(done, day(2024, 1, 10), s))
    }

    @Test
    fun `longest streak counts only scheduled days`() {
        val s = schedule(HabitFrequency.CUSTOM, customDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        val done = setOf(
            day(2024, 1, 1), day(2024, 1, 3), day(2024, 1, 5), // Mon/Wed/Fri
            day(2024, 1, 8), day(2024, 1, 10), // Mon/Wed - continues across the weekend
            day(2024, 1, 12), day(2024, 1, 15) // Fri done, then Monday 01-15 missed
        )
        // Scheduled days run consecutively regardless of unscheduled days between
        // them, so the best run is 01-01, 01-03, 01-05, 01-08, 01-10, 01-12 = 6.
        assertEquals(6, HabitStatsCalculator.longestStreak(done, s, day(2024, 1, 14)))
    }

    @Test
    fun `scheduled day count counts scheduled days in range`() {
        val s = schedule(HabitFrequency.CUSTOM, customDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        // 2024-01-01 (Mon) .. 2024-01-14 (Sun): Mon/Wed/Fri = 6 days.
        assertEquals(6, HabitStatsCalculator.scheduledDayCount(s, day(2024, 1, 1), day(2024, 1, 14)))
        assertEquals(0, HabitStatsCalculator.scheduledDayCount(s, day(2024, 1, 14), day(2024, 1, 1)))
    }

    @Test
    fun `toSchedule maps a stored habit to its schedule`() {
        val habit = com.lifeos.app.data.db.entities.HabitEntity(
            id = "h1",
            name = "Run",
            icon = "🏃",
            frequency = HabitFrequency.CUSTOM,
            customDaysCsv = "2,4,6", // Tue, Thu, Sat (ISO day numbers)
            startDateEpochDay = day(2024, 1, 1),
            createdAt = 1L,
            updatedAt = 1L
        )

        val s = habit.toSchedule()
        assertEquals(HabitFrequency.CUSTOM, s.frequency)
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), s.customDays)
        assertEquals(habit.startDateEpochDay, s.startEpochDay)
    }

    // ---- permanent Habits schedule modes -----------------------------------

    private fun typedSchedule(
        type: HabitScheduleType,
        weekdays: Set<DayOfWeek> = emptySet(),
        monthDays: Set<Int> = emptySet(),
        times: Int = 0,
        start: Long = day(2024, 1, 1)
    ) = HabitSchedule(
        frequency = HabitFrequency.DAILY,
        customDays = weekdays,
        startEpochDay = start,
        type = type,
        monthDays = monthDays,
        timesPerPeriod = times
    )

    @Test
    fun `every day schedule includes weekends`() {
        val s = typedSchedule(HabitScheduleType.EVERY_DAY)
        assertTrue(HabitStatsCalculator.isScheduled(s, day(2024, 1, 6))) // Sat
        assertTrue(HabitStatsCalculator.isScheduled(s, day(2024, 1, 7))) // Sun
        assertFalse(HabitStatsCalculator.isScheduled(s, day(2023, 12, 31)))
    }

    @Test
    fun `weekday schedule ignores unconfigured days`() {
        val s = typedSchedule(
            HabitScheduleType.WEEKDAYS,
            weekdays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
        )
        assertTrue(HabitStatsCalculator.isScheduled(s, day(2024, 1, 2)))
        assertFalse(HabitStatsCalculator.isScheduled(s, day(2024, 1, 3)))
    }

    @Test
    fun `month day schedule matches configured dates`() {
        val s = typedSchedule(HabitScheduleType.MONTH_DAYS, monthDays = setOf(1, 15))
        assertTrue(HabitStatsCalculator.isScheduled(s, day(2024, 2, 1)))
        assertTrue(HabitStatsCalculator.isScheduled(s, day(2024, 2, 15)))
        assertFalse(HabitStatsCalculator.isScheduled(s, day(2024, 2, 2)))
    }

    @Test
    fun `month day schedule silently skips dates a short month does not have`() {
        val s = typedSchedule(HabitScheduleType.MONTH_DAYS, monthDays = setOf(30, 31))
        // February 2024 has 29 days, so neither configured date exists.
        assertEquals(0, HabitStatsCalculator.scheduledDayCount(s, day(2024, 2, 1), day(2024, 2, 29)))
        assertEquals(1, HabitStatsCalculator.scheduledDayCount(s, day(2024, 4, 1), day(2024, 4, 30)))
    }

    @Test
    fun `times per week is due until the weekly target is met`() {
        val s = typedSchedule(HabitScheduleType.TIMES_PER_WEEK, times = 3)
        val monday = day(2024, 1, 8)
        assertTrue(HabitStatsCalculator.isDue(s, monday, emptySet()))
        assertTrue(HabitStatsCalculator.isDue(s, day(2024, 1, 10), setOf(monday, day(2024, 1, 9))))
        assertFalse(
            HabitStatsCalculator.isDue(
                s,
                day(2024, 1, 11),
                setOf(monday, day(2024, 1, 9), day(2024, 1, 10))
            )
        )
        // The next week starts fresh.
        assertTrue(HabitStatsCalculator.isDue(s, day(2024, 1, 15), setOf(monday, day(2024, 1, 9), day(2024, 1, 10))))
    }

    @Test
    fun `times per week counts completions inside the calendar week`() {
        val s = typedSchedule(HabitScheduleType.TIMES_PER_WEEK, times = 3)
        val done = setOf(day(2024, 1, 8), day(2024, 1, 10), day(2024, 1, 12), day(2024, 1, 15))
        assertEquals(3, HabitStatsCalculator.completedInPeriod(s, done, day(2024, 1, 12)))
        assertEquals(1, HabitStatsCalculator.completedInPeriod(s, done, day(2024, 1, 15)))
    }

    @Test
    fun `times per week streak counts consecutive successful weeks`() {
        val s = typedSchedule(HabitScheduleType.TIMES_PER_WEEK, times = 2)
        val done = setOf(
            // Week of 1 Jan: met
            day(2024, 1, 1), day(2024, 1, 4),
            // Week of 8 Jan: met
            day(2024, 1, 8), day(2024, 1, 11),
            // Week of 15 Jan: only one so far, still in progress
            day(2024, 1, 16)
        )
        assertEquals(2, HabitStatsCalculator.currentStreak(done, day(2024, 1, 17), s))
        assertEquals(2, HabitStatsCalculator.longestStreak(done, s, day(2024, 1, 17)))
        assertEquals("week", HabitStatsCalculator.streakUnit(s))
    }

    @Test
    fun `times per month is due only while the month target is unmet`() {
        val s = typedSchedule(HabitScheduleType.TIMES_PER_MONTH, times = 2)
        val done = setOf(day(2024, 3, 4), day(2024, 3, 20))
        assertFalse(HabitStatsCalculator.isDue(s, day(2024, 3, 25), done))
        assertTrue(HabitStatsCalculator.isDue(s, day(2024, 4, 1), done))
        assertEquals("month", HabitStatsCalculator.streakUnit(s))
    }

    @Test
    fun `month streak counts consecutive successful months`() {
        val s = typedSchedule(HabitScheduleType.TIMES_PER_MONTH, times = 2)
        val done = setOf(
            day(2024, 1, 3), day(2024, 1, 20),
            day(2024, 2, 5), day(2024, 2, 21),
            day(2024, 3, 6) // March still in progress
        )
        assertEquals(2, HabitStatsCalculator.currentStreak(done, day(2024, 3, 10), s))
        assertEquals(2, HabitStatsCalculator.longestStreak(done, s, day(2024, 3, 10)))
    }

    @Test
    fun `flexible schedules do not inflate completion percentage past the target`() {
        val s = typedSchedule(HabitScheduleType.TIMES_PER_WEEK, times = 2)
        val done = setOf(
            day(2024, 1, 8), day(2024, 1, 9), day(2024, 1, 10), day(2024, 1, 11)
        )
        assertEquals(2, HabitStatsCalculator.completedOccurrences(s, done, day(2024, 1, 8), day(2024, 1, 14)))
        assertEquals(2, HabitStatsCalculator.scheduledDayCount(s, day(2024, 1, 8), day(2024, 1, 14)))
    }

    @Test
    fun `scheduled day count is capped by the days available in a partial week`() {
        val s = typedSchedule(HabitScheduleType.TIMES_PER_WEEK, times = 5)
        // Only Monday and Tuesday fall inside this range.
        assertEquals(2, HabitStatsCalculator.scheduledDayCount(s, day(2024, 1, 8), day(2024, 1, 9)))
    }

    @Test
    fun `completion helpers honour slipped outcomes and target snapshots`() {
        val habit = HabitEntity(
            id = "h1",
            name = "Read",
            icon = "📚",
            goalCount = 5,
            startDateEpochDay = day(2024, 1, 1),
            createdAt = 1L,
            updatedAt = 1L
        )
        val partial = HabitCompletionEntity("h1", day(2024, 1, 1), 3, 1L, targetCount = 5)
        val done = HabitCompletionEntity("h1", day(2024, 1, 2), 5, 2L, targetCount = 5)
        val slipped = HabitCompletionEntity("h1", day(2024, 1, 3), 9, 3L, HabitOutcome.SLIPPED, targetCount = 5)
        val legacy = HabitCompletionEntity("h1", day(2024, 1, 4), 5, 4L, targetCount = 0)

        assertFalse(habit.isCompletionDone(partial))
        assertTrue(habit.isCompletionDone(done))
        assertFalse(habit.isCompletionDone(slipped))
        assertTrue(habit.isCompletionDone(legacy))
        assertEquals(5, habit.completionTarget(legacy))
        assertEquals(5, habit.completionTarget(partial))
    }
}
