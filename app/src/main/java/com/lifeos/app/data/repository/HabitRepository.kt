package com.lifeos.app.data.repository

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.HabitStatsCalculator
import com.lifeos.app.core.util.completionTarget
import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.core.util.isCompletionDone
import com.lifeos.app.core.util.toSchedule
import com.lifeos.app.data.db.dao.HabitChecklistDao
import com.lifeos.app.data.db.dao.HabitCompletionDao
import com.lifeos.app.data.db.dao.HabitDao
import com.lifeos.app.data.db.entities.HabitChecklistCheckEntity
import com.lifeos.app.data.db.entities.HabitChecklistItemEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitFrequency
import com.lifeos.app.data.db.entities.HabitOutcome
import com.lifeos.app.data.db.entities.HabitScheduleType
import com.lifeos.app.data.db.entities.HabitTrackingMode
import com.lifeos.app.data.db.entities.HabitType
import com.lifeos.app.data.db.entities.ReminderRepeatType
import com.lifeos.app.domain.model.HabitAnalytics
import com.lifeos.app.domain.model.HeatmapCell
import com.lifeos.app.domain.model.HeatmapIntensity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import kotlin.math.min

/**
 * A complete, validated habit definition used by the create/edit flow. Keeping
 * it separate from Room's [HabitEntity] means the UI can never accidentally
 * overwrite `id`, `createdAt` or archive state while editing.
 */
data class HabitDraft(
    val name: String,
    val description: String? = null,
    val icon: String = "🔥",
    val colorArgb: Long = 0L,
    val category: String? = null,
    val habitType: HabitType = HabitType.BUILD,
    val trackingMode: HabitTrackingMode = HabitTrackingMode.TICK,
    val scheduleType: HabitScheduleType = HabitScheduleType.EVERY_DAY,
    val daysOfWeekCsv: String? = null,
    val daysOfMonthCsv: String? = null,
    val timesPerPeriod: Int? = null,
    val goalCount: Int = 1,
    val reminderEpochMillis: Long? = null,
    val reminderRepeatType: ReminderRepeatType = ReminderRepeatType.DAILY,
    val showOnTimeline: Boolean = true,
    val startDateEpochDay: Long = DateTimeUtils.today().toEpochDay(),
    val checklistItems: List<HabitChecklistDraft> = emptyList(),
    /** Preserves pre-v7 frequency when editing a LEGACY habit. */
    val frequencyOverride: HabitFrequency? = null,
    val legacyCustomDaysCsv: String? = null
)

data class HabitChecklistDraft(
    val id: String? = null,
    val label: String,
    val position: Int = 0
)

class HabitRepository(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao,
    private val checklistDao: HabitChecklistDao,
    private val reminderRepository: ReminderRepository
) {
    fun observeAll(): Flow<List<HabitEntity>> = habitDao.observeAll()
    fun observeArchived(): Flow<List<HabitEntity>> = habitDao.observeArchived()
    fun observeCompletion(habitId: String, epochDay: Long): Flow<HabitCompletionEntity?> =
        completionDao.observe(habitId, epochDay)
    fun observeAllForDay(epochDay: Long): Flow<List<HabitCompletionEntity>> =
        completionDao.observeAllForDay(epochDay)
    fun observeAllInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<HabitCompletionEntity>> =
        completionDao.observeAllInRange(startEpochDay, endEpochDay)
    fun observeAllCompletions(): Flow<List<HabitCompletionEntity>> =
        completionDao.observeAllCompletions()
    fun observeChecklistItems(habitId: String): Flow<List<HabitChecklistItemEntity>> =
        checklistDao.observeActiveItems(habitId)
    fun observeAllChecklistItems(): Flow<List<HabitChecklistItemEntity>> =
        checklistDao.observeAllActiveItems()
    fun observeChecklistChecks(habitId: String, epochDay: Long): Flow<List<HabitChecklistCheckEntity>> =
        checklistDao.observeChecks(habitId, epochDay)

    suspend fun getById(id: String): HabitEntity? = habitDao.getById(id)
    suspend fun getChecklistItems(habitId: String): List<HabitChecklistItemEntity> =
        checklistDao.getActiveItems(habitId)
    suspend fun getChecklistChecks(habitId: String, epochDay: Long): List<HabitChecklistCheckEntity> =
        checklistDao.getChecksInRange(habitId, epochDay, epochDay)
    suspend fun getReminderRepeat(habitId: String): ReminderRepeatType? =
        reminderRepository.repeatForHabit(habitId)
    suspend fun getCompletions(habitId: String, start: Long, end: Long): List<HabitCompletionEntity> =
        completionDao.getForHabitInRange(habitId, start, end)

    /** Compatibility overload for call sites that predate the draft model. */
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
    ): String = createHabit(
        HabitDraft(
            name = name,
            icon = icon,
            category = category,
            scheduleType = HabitScheduleType.LEGACY,
            goalCount = goalCount,
            reminderEpochMillis = reminderEpochMillis,
            reminderRepeatType = reminderRepeatType,
            startDateEpochDay = startDateEpochDay,
            frequencyOverride = frequency,
            legacyCustomDaysCsv = customDaysCsv
        )
    )

    suspend fun createHabit(draft: HabitDraft): String {
        val id = IdGenerator.newId()
        val now = System.currentTimeMillis()
        val entity = draft.toEntity(id = id, createdAt = now, updatedAt = now)
        habitDao.upsert(entity)
        saveChecklistTemplate(id, draft.checklistItems)
        reminderRepository.syncReminderForHabit(entity, draft.reminderRepeatType)
        return id
    }

    suspend fun updateHabit(id: String, draft: HabitDraft) {
        val existing = habitDao.getById(id) ?: return
        val updated = draft.toEntity(
            id = existing.id,
            createdAt = existing.createdAt,
            updatedAt = System.currentTimeMillis()
        ).copy(isArchived = existing.isArchived)
        habitDao.upsert(updated)
        saveChecklistTemplate(id, draft.checklistItems)
        // Only checklist habits derive their day row from template items.
        // Reconciling a Tick/Count habit here would clear today's real
        // completion because it has no checklist template at all.
        if (updated.trackingMode == HabitTrackingMode.CHECKLIST) {
            reconcileChecklistCompletion(id, DateTimeUtils.today().toEpochDay())
        }
        reminderRepository.syncReminderForHabit(updated, draft.reminderRepeatType)
    }

    /**
     * Saves the checklist template without rewriting history:
     *  - unchanged items keep their id and every historical check row,
     *  - new items get stable ids,
     *  - removed items are archived instead of deleted, so past checks remain
     *    readable and old completion snapshots stay honest.
     */
    suspend fun saveChecklistTemplate(habitId: String, drafts: List<HabitChecklistDraft>) {
        val existing = checklistDao.getAllItems(habitId)
        val keptIds = mutableSetOf<String>()
        drafts.forEachIndexed { index, draft ->
            val label = draft.label.trim()
            if (label.isEmpty()) return@forEachIndexed
            val existingMatch = draft.id?.let { candidate ->
                existing.firstOrNull { it.id == candidate }
            }
            if (existingMatch != null) {
                keptIds += existingMatch.id
                checklistDao.upsertItem(
                    existingMatch.copy(label = label, position = index, isArchived = false)
                )
            } else {
                val id = IdGenerator.newId()
                keptIds += id
                checklistDao.upsertItem(
                    HabitChecklistItemEntity(
                        id = id,
                        habitId = habitId,
                        label = label,
                        position = index,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
        existing.filter { it.id !in keptIds && !it.isArchived }
            .forEach { checklistDao.archiveItem(it.id) }
    }

    suspend fun archive(id: String) {
        habitDao.archive(id, System.currentTimeMillis())
        reminderRepository.deleteForHabit(id)
    }

    suspend fun unarchive(id: String) {
        habitDao.unarchive(id, System.currentTimeMillis())
        habitDao.getById(id)?.let { reminderRepository.syncReminderForHabit(it) }
    }

    suspend fun delete(id: String) {
        completionDao.deleteForHabit(id)
        checklistDao.deleteChecksForHabit(id)
        checklistDao.deleteItemsForHabit(id)
        habitDao.delete(id)
        reminderRepository.deleteForHabit(id)
    }

    suspend fun setReminder(habitId: String, reminderEpochMillis: Long?, repeatType: ReminderRepeatType) {
        val habit = habitDao.getById(habitId) ?: return
        val updated = habit.copy(reminderEpochMillis = reminderEpochMillis, updatedAt = System.currentTimeMillis())
        habitDao.upsert(updated)
        reminderRepository.syncReminderForHabit(updated, repeatType)
    }

    // ---- Logging ----------------------------------------------------------

    /** Tick / "stayed on track": writes an explicit successful day. */
    suspend fun setTick(habitId: String, epochDay: Long, done: Boolean) {
        if (!done) {
            completionDao.clear(habitId, epochDay)
            return
        }
        val habit = habitDao.getById(habitId) ?: return
        val target = if (habit.trackingMode == HabitTrackingMode.COUNT) {
            habit.goalCount.coerceAtLeast(1)
        } else 1
        completionDao.upsert(
            HabitCompletionEntity(
                habitId = habitId,
                dateEpochDay = epochDay,
                progressCount = target,
                completedAtEpochMillis = System.currentTimeMillis(),
                outcome = HabitOutcome.DONE,
                targetCount = target
            )
        )
    }

    /** Explicit Quit-habit slip: recorded, never inferred from a blank day. */
    suspend fun setSlipped(habitId: String, epochDay: Long) {
        completionDao.upsert(
            HabitCompletionEntity(
                habitId = habitId,
                dateEpochDay = epochDay,
                progressCount = 0,
                completedAtEpochMillis = System.currentTimeMillis(),
                outcome = HabitOutcome.SLIPPED,
                targetCount = 1
            )
        )
    }

    /** Count mode: clamps at zero and deletes the row when the count is undone. */
    suspend fun setCount(habitId: String, epochDay: Long, count: Int) {
        if (count <= 0) {
            completionDao.clear(habitId, epochDay)
            return
        }
        val habit = habitDao.getById(habitId) ?: return
        val target = habit.goalCount.coerceAtLeast(1)
        completionDao.upsert(
            HabitCompletionEntity(
                habitId = habitId,
                dateEpochDay = epochDay,
                progressCount = count,
                completedAtEpochMillis = System.currentTimeMillis(),
                outcome = HabitOutcome.DONE,
                targetCount = target
            )
        )
    }

    /** Checklist mode: persists one item check, then reconciles the day. */
    suspend fun setChecklistItemChecked(
        habitId: String,
        epochDay: Long,
        itemId: String,
        checked: Boolean
    ) {
        val item = checklistDao.getItem(itemId) ?: return
        if (item.habitId != habitId) return
        checklistDao.upsertCheck(
            HabitChecklistCheckEntity(
                itemId = itemId,
                habitId = habitId,
                dateEpochDay = epochDay,
                isChecked = checked,
                checkedAtEpochMillis = System.currentTimeMillis(),
                labelSnapshot = item.label
            )
        )
        reconcileChecklistCompletion(habitId, epochDay)
    }

    /**
     * Reconciles the daily checklist completion with the active template. The
     * target is snapshotted so later template edits cannot rewrite history.
     */
    suspend fun reconcileChecklistCompletion(habitId: String, epochDay: Long) {
        val activeItems = checklistDao.getActiveItems(habitId)
        val checks = checklistDao.getChecksInRange(habitId, epochDay, epochDay)
            .filter { it.isChecked }
        val activeIds = activeItems.mapTo(mutableSetOf()) { it.id }
        val checked = checks.count { it.itemId in activeIds }
        if (activeItems.isEmpty() || checked == 0) {
            completionDao.clear(habitId, epochDay)
            return
        }
        completionDao.upsert(
            HabitCompletionEntity(
                habitId = habitId,
                dateEpochDay = epochDay,
                progressCount = checked,
                completedAtEpochMillis = System.currentTimeMillis(),
                outcome = HabitOutcome.DONE,
                targetCount = activeItems.size
            )
        )
    }

    /** Legacy compatibility: increments progress and clamps to a sane maximum. */
    suspend fun logProgress(habitId: String, epochDay: Long, progressCount: Int) {
        val habit = habitDao.getById(habitId) ?: return
        if (habit.trackingMode == HabitTrackingMode.COUNT) {
            setCount(habitId, epochDay, progressCount)
        } else {
            setTick(habitId, epochDay, progressCount > 0)
        }
    }

    suspend fun clearProgress(habitId: String, epochDay: Long) = completionDao.clear(habitId, epochDay)

    // ---- Analytics --------------------------------------------------------

    suspend fun computeAnalytics(habit: HabitEntity, today: LocalDate = DateTimeUtils.today()): HabitAnalytics {
        val allCompletions = completionDao.getForHabitInRange(habit.id, habit.startDateEpochDay, today.toEpochDay())
        return analyticsFor(habit, allCompletions, today)
    }

    /**
     * Computes analytics for every habit from a single pass over the completions
     * table, instead of one full-history query per habit.
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

    /** Pure analytics over an already-loaded completion list (no DB access). */
    fun analyticsFor(
        habit: HabitEntity,
        allCompletions: List<HabitCompletionEntity>,
        today: LocalDate
    ): HabitAnalytics {
        val schedule = habit.toSchedule()
        val todayEpochDay = today.toEpochDay()
        val doneDays = allCompletions.filter { habit.isCompletionDone(it) }
            .mapTo(mutableSetOf()) { it.dateEpochDay }

        val monthStart = DateTimeUtils.startOfMonthEpochDay(today)
        val monthEnd = DateTimeUtils.endOfMonthEpochDay(today)
        val weekStart = DateTimeUtils.startOfWeekEpochDay(today)
        val weekEnd = DateTimeUtils.endOfWeekEpochDay(today)

        val currentStreak = HabitStatsCalculator.currentStreak(doneDays, todayEpochDay, schedule)
        val longestStreak = HabitStatsCalculator.longestStreak(doneDays, schedule, todayEpochDay)
        val streakUnit = HabitStatsCalculator.streakUnit(schedule)

        val monthExpected = HabitStatsCalculator.scheduledDayCount(schedule, monthStart, minOf(todayEpochDay, monthEnd))
        val monthDone = HabitStatsCalculator.completedOccurrences(schedule, doneDays, monthStart, minOf(todayEpochDay, monthEnd))

        val weekExpected = HabitStatsCalculator.scheduledDayCount(schedule, weekStart, minOf(todayEpochDay, weekEnd))
        val weekDone = HabitStatsCalculator.completedOccurrences(schedule, doneDays, weekStart, minOf(todayEpochDay, weekEnd))

        return HabitAnalytics(
            habitId = habit.id,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            completionsThisMonth = monthDone,
            totalDaysThisMonth = monthExpected,
            completionPercentThisMonth = HabitStatsCalculator.completionPercent(monthDone, monthExpected),
            missedDaysThisMonth = (monthExpected - monthDone).coerceAtLeast(0),
            totalCompletionsAllTime = doneDays.size,
            completionsThisWeek = weekDone,
            totalDaysThisWeek = weekExpected,
            completionPercentThisWeek = HabitStatsCalculator.completionPercent(weekDone, weekExpected),
            streakUnit = streakUnit
        )
    }

    /** GitHub-style heatmap data for an arbitrary inclusive day window. */
    suspend fun computeHeatmap(habit: HabitEntity, startEpochDay: Long, endEpochDay: Long): List<HeatmapCell> {
        val completions = completionDao.getForHabitInRange(habit.id, startEpochDay, endEpochDay)
        return heatmapFor(habit, completions, startEpochDay, endEpochDay)
    }

    /** Pure heatmap projection over already-loaded completions (no DB access). */
    fun heatmapFor(
        habit: HabitEntity,
        completionsForRange: List<HabitCompletionEntity>,
        startEpochDay: Long,
        endEpochDay: Long,
        todayEpochDay: Long = DateTimeUtils.today().toEpochDay()
    ): List<HeatmapCell> {
        val completions = completionsForRange.associateBy { it.dateEpochDay }
        val schedule = habit.toSchedule()
        val doneDays = completions.values.filter { habit.isCompletionDone(it) }
            .mapTo(mutableSetOf()) { it.dateEpochDay }

        return (startEpochDay..endEpochDay).map { day ->
            val completion = completions[day]
            val target = habit.completionTarget(completion)
            val intensity = when {
                day > todayEpochDay -> HeatmapIntensity.NO_DATA
                !HabitStatsCalculator.isScheduled(schedule, day) -> HeatmapIntensity.NO_DATA
                completion?.outcome == HabitOutcome.SLIPPED -> HeatmapIntensity.MISSED
                // A flexible N-per-week habit is not "missed" on a day that was
                // never due because the period target was already met.
                !HabitStatsCalculator.isDue(schedule, day, doneDays) &&
                    completion == null -> HeatmapIntensity.NO_DATA
                completion == null || completion.progressCount == 0 -> HeatmapIntensity.MISSED
                completion.progressCount < target -> HeatmapIntensity.PARTIAL
                completion.progressCount == target -> HeatmapIntensity.COMPLETED
                else -> HeatmapIntensity.EXCEPTIONAL
            }
            HeatmapCell(
                epochDay = day,
                intensity = intensity,
                progressCount = completion?.progressCount ?: 0,
                goalCount = target
            )
        }
    }

    // ---- Backup -----------------------------------------------------------

    suspend fun getAllForBackup(): List<HabitEntity> = habitDao.getAllForBackup()
    suspend fun getAllCompletionsForBackup(): List<HabitCompletionEntity> = completionDao.getAllForBackup()
    suspend fun getAllChecklistItemsForBackup(): List<HabitChecklistItemEntity> = checklistDao.getAllItemsForBackup()
    suspend fun getAllChecklistChecksForBackup(): List<HabitChecklistCheckEntity> = checklistDao.getAllChecksForBackup()

    suspend fun restoreFromBackup(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        checklistItems: List<HabitChecklistItemEntity> = emptyList(),
        checklistChecks: List<HabitChecklistCheckEntity> = emptyList()
    ) {
        habits.forEach { habitDao.upsert(it) }
        completions.forEach { completionDao.upsert(it) }
        checklistItems.forEach { checklistDao.upsertItem(it) }
        checklistChecks.forEach { checklistDao.upsertCheck(it) }
    }

    // ---- mapping ----------------------------------------------------------

    private fun HabitDraft.toEntity(id: String, createdAt: Long, updatedAt: Long): HabitEntity {
        val weekdays = daysOfWeekCsv?.takeIf { it.isNotBlank() }
        val monthDays = daysOfMonthCsv?.takeIf { it.isNotBlank() }
        val legacyFrequency = when (scheduleType) {
            HabitScheduleType.EVERY_DAY -> HabitFrequency.DAILY
            HabitScheduleType.WEEKDAYS -> HabitFrequency.CUSTOM
            HabitScheduleType.LEGACY, HabitScheduleType.MONTH_DAYS,
            HabitScheduleType.TIMES_PER_WEEK, HabitScheduleType.TIMES_PER_MONTH -> HabitFrequency.DAILY
        }
        return HabitEntity(            id = id,
            name = name.trim(),
            icon = icon.ifBlank { "🔥" },
            category = category?.takeIf { it.isNotBlank() },
            frequency = frequencyOverride ?: legacyFrequency,
            customDaysCsv = if (scheduleType == HabitScheduleType.WEEKDAYS) {
                weekdays
            } else {
                legacyCustomDaysCsv
            },
            goalCount = goalCount.coerceAtLeast(1),
            reminderEpochMillis = reminderEpochMillis,
            startDateEpochDay = startDateEpochDay,
            createdAt = createdAt,
            updatedAt = updatedAt,
            description = description?.trim()?.takeIf { it.isNotBlank() },
            colorArgb = colorArgb,
            habitType = habitType,
            trackingMode = trackingMode,
            scheduleType = scheduleType,
            daysOfWeekCsv = weekdays,
            daysOfMonthCsv = monthDays,
            timesPerPeriod = timesPerPeriod?.takeIf { it > 0 },
            showOnTimeline = showOnTimeline
        )
    }
}
