package com.lifeos.app.data.repository

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.HabitStatsCalculator
import com.lifeos.app.core.util.toSchedule
import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.data.db.dao.HabitCompletionDao
import com.lifeos.app.data.db.dao.HabitDao
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitFrequency
import com.lifeos.app.data.db.entities.ReminderRepeatType
import com.lifeos.app.domain.model.HabitAnalytics
import com.lifeos.app.domain.model.HeatmapCell
import com.lifeos.app.domain.model.HeatmapIntensity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class HabitRepository(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao,
    private val reminderRepository: ReminderRepository
) {
    fun observeAll(): Flow<List<HabitEntity>> = habitDao.observeAll()
    fun observeById(id: String): Flow<HabitEntity?> = habitDao.observeById(id)
    fun observeCompletion(habitId: String, epochDay: Long): Flow<HabitCompletionEntity?> =
        completionDao.observe(habitId, epochDay)
    fun observeAllForDay(epochDay: Long): Flow<List<HabitCompletionEntity>> = completionDao.observeAllForDay(epochDay)
    fun observeAllInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<HabitCompletionEntity>> =
        completionDao.observeAllInRange(startEpochDay, endEpochDay)
    fun observeAllForHabit(habitId: String): Flow<List<HabitCompletionEntity>> = completionDao.observeAllForHabit(habitId)

    suspend fun getById(id: String): HabitEntity? = habitDao.getById(id)

    suspend fun createHabit(
        name: String,
        icon: String,
        category: String? = null,
        frequency: HabitFrequency = HabitFrequency.DAILY,
        customDaysCsv: String? = null,
        goalCount: Int = 1,
        reminderEpochMillis: Long? = null,
        reminderRepeatType: ReminderRepeatType = ReminderRepeatType.DAILY,
        startDateEpochDay: Long = DateTimeUtils.today().toEpochDay()
    ): String {
        val id = IdGenerator.newId()
        val now = System.currentTimeMillis()
        habitDao.upsert(
            HabitEntity(
                id = id, name = name, icon = icon, category = category, frequency = frequency,
                customDaysCsv = customDaysCsv, goalCount = goalCount, reminderEpochMillis = reminderEpochMillis,
                startDateEpochDay = startDateEpochDay, createdAt = now, updatedAt = now
            )
        )
        // Reconcile the reminders mirror + exact alarm in one place.
        habitDao.getById(id)?.let { reminderRepository.syncReminderForHabit(it, reminderRepeatType) }
        return id
    }

    suspend fun archive(id: String) {
        habitDao.archive(id, System.currentTimeMillis())
        reminderRepository.deleteForHabit(id)
    }

    suspend fun delete(id: String) {
        completionDao.deleteForHabit(id)
        habitDao.delete(id)
        reminderRepository.deleteForHabit(id)
    }

    /**
     * Updates (or clears, when [reminderEpochMillis] is null) a habit's reminder
     * time and repeat cadence. Used by the Habit detail screen.
     */
    suspend fun setReminder(habitId: String, reminderEpochMillis: Long?, repeatType: ReminderRepeatType) {
        val habit = habitDao.getById(habitId) ?: return
        val updated = habit.copy(
            reminderEpochMillis = reminderEpochMillis,
            updatedAt = System.currentTimeMillis()
        )
        habitDao.upsert(updated)
        reminderRepository.syncReminderForHabit(updated, repeatType)
    }

    /** Increments today's progress by one tap (or sets explicit count for goal-based habits). */
    suspend fun logProgress(habitId: String, epochDay: Long, progressCount: Int) {
        completionDao.upsert(
            HabitCompletionEntity(
                habitId = habitId,
                dateEpochDay = epochDay,
                progressCount = progressCount,
                completedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearProgress(habitId: String, epochDay: Long) = completionDao.clear(habitId, epochDay)

    /** Computes real analytics from the completions table — never hardcoded (Rule #12). */
    suspend fun computeAnalytics(habit: HabitEntity, today: LocalDate = DateTimeUtils.today()): HabitAnalytics {
        val allCompletions = completionDao.getForHabitInRange(habit.id, habit.startDateEpochDay, today.toEpochDay())
        return analyticsFor(habit, allCompletions, today)
    }

    /**
     * Computes analytics for every habit from a single pass over the completions
     * table, instead of one full-history query per habit. Used by the Home
     * summary so the dashboard renders without a fan-out of Room queries.
     */
    suspend fun computeAnalyticsBatch(
        habits: List<HabitEntity>,
        today: LocalDate = DateTimeUtils.today()
    ): Map<String, HabitAnalytics> {
        if (habits.isEmpty()) return emptyMap()
        val allCompletions = completionDao.getAllForBackup()
        val todayEpochDay = today.toEpochDay()
        return habits.associate { habit ->
            habit.id to analyticsFor(
                habit,
                allCompletions.filter { it.habitId == habit.id && it.dateEpochDay in habit.startDateEpochDay..todayEpochDay },
                today
            )
        }
    }

    private fun analyticsFor(
        habit: HabitEntity,
        allCompletions: List<HabitCompletionEntity>,
        today: LocalDate
    ): HabitAnalytics {
        val monthStart = DateTimeUtils.startOfMonthEpochDay(today)
        val monthEnd = DateTimeUtils.endOfMonthEpochDay(today)
        val monthCompletions = allCompletions.filter { it.dateEpochDay in monthStart..monthEnd }

        val doneDays = allCompletions.filter { it.progressCount >= habit.goalCount }.map { it.dateEpochDay }.toSet()
        val schedule = habit.toSchedule()
        val todayEpochDay = today.toEpochDay()

        // Streaks and monthly completion are schedule-aware: only days the habit
        // is actually expected on count towards (or break) progress.
        val currentStreak = HabitStatsCalculator.currentStreak(doneDays, todayEpochDay, schedule)
        val longestStreak = HabitStatsCalculator.longestStreak(doneDays, schedule, todayEpochDay)

        val scheduledDaysElapsed = HabitStatsCalculator.scheduledDayCount(
            schedule, monthStart, minOf(todayEpochDay, monthEnd)
        )
        val monthDoneScheduled = monthCompletions.count {
            it.progressCount >= habit.goalCount && HabitStatsCalculator.isScheduled(schedule, it.dateEpochDay)
        }
        val completionPercent = HabitStatsCalculator.completionPercent(monthDoneScheduled, scheduledDaysElapsed)

        return HabitAnalytics(
            habitId = habit.id,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            completionsThisMonth = monthDoneScheduled,
            totalDaysThisMonth = scheduledDaysElapsed,
            completionPercentThisMonth = completionPercent,
            missedDaysThisMonth = (scheduledDaysElapsed - monthDoneScheduled).coerceAtLeast(0),
            totalCompletionsAllTime = doneDays.size
        )
    }

    /** GitHub-style heatmap data (Section 13) for an arbitrary [startEpochDay]..[endEpochDay] window. */
    suspend fun computeHeatmap(habit: HabitEntity, startEpochDay: Long, endEpochDay: Long): List<HeatmapCell> {
        val completions = completionDao.getForHabitInRange(habit.id, startEpochDay, endEpochDay)
            .associateBy { it.dateEpochDay }
        val schedule = habit.toSchedule()
        val todayEpochDay = DateTimeUtils.today().toEpochDay()

        return (startEpochDay..endEpochDay).map { day ->
            val completion = completions[day]
            val intensity = when {
                day > todayEpochDay -> HeatmapIntensity.NO_DATA
                // Days the habit is not scheduled for are neutral, not "missed".
                !HabitStatsCalculator.isScheduled(schedule, day) -> HeatmapIntensity.NO_DATA
                completion == null || completion.progressCount == 0 -> HeatmapIntensity.MISSED
                completion.progressCount < habit.goalCount -> HeatmapIntensity.PARTIAL
                completion.progressCount == habit.goalCount -> HeatmapIntensity.COMPLETED
                else -> HeatmapIntensity.EXCEPTIONAL // progressCount > goalCount
            }
            HeatmapCell(
                epochDay = day,
                intensity = intensity,
                progressCount = completion?.progressCount ?: 0,
                goalCount = habit.goalCount
            )
        }
    }

    suspend fun getAllForBackup(): List<HabitEntity> = habitDao.getAllForBackup()
    suspend fun getAllCompletionsForBackup(): List<HabitCompletionEntity> = completionDao.getAllForBackup()
    suspend fun restoreFromBackup(habits: List<HabitEntity>, completions: List<HabitCompletionEntity>) {
        habits.forEach { habitDao.upsert(it) }
        completions.forEach { completionDao.upsert(it) }
    }
}
