package com.lifeos.app.data.repository

import android.content.Context
import com.lifeos.app.core.reminders.ReminderScheduler
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.data.db.dao.HabitCompletionDao
import com.lifeos.app.data.db.dao.HabitDao
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitFrequency
import com.lifeos.app.domain.model.HabitAnalytics
import com.lifeos.app.domain.model.HeatmapCell
import com.lifeos.app.domain.model.HeatmapIntensity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class HabitRepository(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao,
    private val appContext: Context
) {
    fun observeAll(): Flow<List<HabitEntity>> = habitDao.observeAll()
    fun observeById(id: String): Flow<HabitEntity?> = habitDao.observeById(id)
    fun observeCompletion(habitId: String, epochDay: Long): Flow<HabitCompletionEntity?> =
        completionDao.observe(habitId, epochDay)
    fun observeAllForDay(epochDay: Long): Flow<List<HabitCompletionEntity>> = completionDao.observeAllForDay(epochDay)
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
        if (reminderEpochMillis != null && reminderEpochMillis > System.currentTimeMillis()) {
            ReminderScheduler.scheduleHabitReminder(appContext, id, reminderEpochMillis)
        }
        return id
    }

    suspend fun archive(id: String) {
        habitDao.archive(id, System.currentTimeMillis())
        ReminderScheduler.cancelHabitReminder(appContext, id)
    }

    suspend fun delete(id: String) {
        habitDao.delete(id)
        ReminderScheduler.cancelHabitReminder(appContext, id)
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
        val monthStart = DateTimeUtils.startOfMonthEpochDay(today)
        val monthEnd = DateTimeUtils.endOfMonthEpochDay(today)
        val allCompletions = completionDao.getForHabitInRange(habit.id, habit.startDateEpochDay, today.toEpochDay())
        val monthCompletions = allCompletions.filter { it.dateEpochDay in monthStart..monthEnd }

        val doneDays = allCompletions.filter { it.progressCount >= habit.goalCount }.map { it.dateEpochDay }.toSet()

        var currentStreak = 0
        var cursor = today.toEpochDay()
        while (doneDays.contains(cursor)) {
            currentStreak++
            cursor--
        }

        var longestStreak = 0
        var running = 0
        var prevDay: Long? = null
        for (day in doneDays.sorted()) {
            running = if (prevDay != null && day == prevDay + 1) running + 1 else 1
            longestStreak = maxOf(longestStreak, running)
            prevDay = day
        }

        val daysElapsedThisMonth = (minOf(today.toEpochDay(), monthEnd) - monthStart + 1).toInt()
        val monthDoneCount = monthCompletions.count { it.progressCount >= habit.goalCount }
        val completionPercent = if (daysElapsedThisMonth > 0) (monthDoneCount * 100) / daysElapsedThisMonth else 0

        return HabitAnalytics(
            habitId = habit.id,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            completionsThisMonth = monthDoneCount,
            totalDaysThisMonth = daysElapsedThisMonth,
            completionPercentThisMonth = completionPercent,
            missedDaysThisMonth = daysElapsedThisMonth - monthDoneCount,
            totalCompletionsAllTime = doneDays.size
        )
    }

    /** GitHub-style heatmap data (Section 13) for an arbitrary [startEpochDay]..[endEpochDay] window. */
    suspend fun computeHeatmap(habit: HabitEntity, startEpochDay: Long, endEpochDay: Long): List<HeatmapCell> {
        val completions = completionDao.getForHabitInRange(habit.id, startEpochDay, endEpochDay)
            .associateBy { it.dateEpochDay }

        return (startEpochDay..endEpochDay).map { day ->
            val completion = completions[day]
            val intensity = when {
                day > DateTimeUtils.today().toEpochDay() -> HeatmapIntensity.NO_DATA
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
