package com.lifeos.app.core.data

import com.lifeos.app.core.database.LifeOsDatabase
import com.lifeos.app.core.database.entity.DiaryEntryEntity
import com.lifeos.app.core.database.entity.ExpenseEntity
import com.lifeos.app.core.database.entity.HabitCompletionEntity
import com.lifeos.app.core.database.entity.HabitEntity
import com.lifeos.app.core.database.entity.LifeEventEntity
import com.lifeos.app.core.database.entity.TaskEntity
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.HabitProgress
import com.lifeos.app.core.domain.Ids
import com.lifeos.app.core.domain.LifeEventType
import com.lifeos.app.core.domain.Money
import com.lifeos.app.core.domain.Priority
import com.lifeos.app.core.domain.Recurrence
import com.lifeos.app.core.domain.RecurrenceRules
import com.lifeos.app.core.domain.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Tracking features: Tasks, Habits, Expenses, Diary.
 * One repository per aggregate keeps each surface small; shared trash/timeline
 * behaviour is expressed the same way in each method (see NoteRepository for the
 * canonical lifecycle commentary).
 */
class TrackingRepositories(private val db: LifeOsDatabase) {

    // ---------------------------------------------------------------- Tasks

    private val taskDao get() = db.taskDao()

    fun observeOpenTasks(): Flow<List<TaskEntity>> = taskDao.observeOpen()
    fun observeDoneTasks(): Flow<List<TaskEntity>> = taskDao.observeDone()
    fun observeTasksFor(date: String): Flow<List<TaskEntity>> = taskDao.observeForDate(date)
    fun observeTaskTrash(): Flow<List<TaskEntity>> = taskDao.observeTrash()

    suspend fun createTask(
        title: String,
        notes: String = "",
        dueDate: String? = null,
        priority: Priority = Priority.NONE,
        recurrence: Recurrence = Recurrence.NONE,
        recurrenceParameter: String? = null,
    ): String {
        val now = Dates.nowMillis()
        val id = Ids.new()
        taskDao.upsert(
            TaskEntity(
                id = id,
                title = title.trim(),
                notes = notes.trim(),
                dueDate = dueDate?.takeIf { it.isNotBlank() },
                priority = priority.code,
                recurrence = recurrence.id,
                recurrenceParameter = recurrenceParameter,
                createdAt = now,
                updatedAt = now,
            ),
        )
        logEvent(LifeEventType.TASK_CREATED, title.trim(), sourceType = "tasks", sourceId = id, at = now)
        return id
    }

    suspend fun setTaskDone(id: String, done: Boolean) {
        val now = Dates.nowMillis()
        taskDao.setStatus(
            id = id,
            status = if (done) TaskStatus.DONE.code else TaskStatus.OPEN.code,
            completedAt = if (done) now else null,
            updatedAt = now,
        )
        if (done) {
            taskDao.getById(id)?.let {
                logEvent(LifeEventType.TASK_COMPLETED, it.title, sourceType = "tasks", sourceId = id, at = now)
                maybeSpawnRecurringTask(it, now)
            }
        }
    }

    /** Completing a recurring task creates its next occurrence (spec). */
    private suspend fun maybeSpawnRecurringTask(task: TaskEntity, at: Long) {
        val recurrence = Recurrence.fromId(task.recurrence)
        if (recurrence == Recurrence.NONE) return
        val fromDate = Dates.parseOrNull(task.dueDate ?: "") ?: Dates.today()
        val nextDate = RecurrenceRules.next(fromDate, recurrence, task.recurrenceParameter) ?: return
        val newId = Ids.new()
        taskDao.upsert(
            task.copy(
                id = newId,
                status = TaskStatus.OPEN.code,
                completedAt = null,
                dueDate = Dates.iso(nextDate),
                createdAt = at,
                updatedAt = at,
            ),
        )
    }

    suspend fun moveToTrashTask(id: String) {
        taskDao.softDelete(id, Dates.nowMillis())
        db.lifeEventDao().deleteForSource("tasks", id)
    }

    suspend fun restoreTask(id: String) = taskDao.restore(id, Dates.nowMillis())
    suspend fun deleteTaskForever(id: String) {
        db.lifeEventDao().deleteForSource("tasks", id)
        taskDao.deleteForever(id)
    }

    // --------------------------------------------------------------- Habits

    private val habitDao get() = db.habitDao()

    fun observeHabits(): Flow<List<HabitEntity>> = habitDao.observeActive()
    fun observeHabitTrash(): Flow<List<HabitEntity>> = habitDao.observeTrash()

    /** Live "today" view: habit + today's completion + streak, recomputed on any change. */
    fun observeToday(): Flow<List<HabitTodayItem>> =
        combine(habitDao.observeActive(), habitDao.observeCompletionsOn(Dates.iso(Dates.today()))) { habits, todayCompletions ->
            habits to todayCompletions
        }.map { (habits, todayCompletions) ->
            val byHabit = todayCompletions.groupBy { it.habitId }
            habits.map { habit ->
                val completedToday = byHabit[habit.id]?.sumOf { it.amount } ?: 0
                val target = habitTarget(habit)
                HabitTodayItem(
                    habit = habit,
                    completedToday = completedToday,
                    targetToday = target,
                    isDoneToday = completedToday >= target,
                )
            }
        }

    fun habitTarget(habit: HabitEntity): Int =
        if (habit.mode == "count") habit.targetPerDay.coerceAtLeast(1) else 1

    suspend fun createHabit(
        name: String,
        emoji: String = "",
        area: String = "personal",
        recurrence: Recurrence = Recurrence.DAILY,
        recurrenceParameter: String? = null,
        mode: String = "binary",
        targetPerDay: Int = 1,
    ): String {
        val now = Dates.nowMillis()
        val id = Ids.new()
        habitDao.upsert(
            HabitEntity(
                id = id,
                name = name.trim(),
                emoji = emoji,
                area = area,
                recurrence = recurrence.id,
                recurrenceParameter = recurrenceParameter,
                mode = mode,
                targetPerDay = targetPerDay,
                createdAt = now,
                updatedAt = now,
            ),
        )
        logEvent(LifeEventType.HABIT_CREATED, name.trim(), area = area, sourceType = "habits", sourceId = id, at = now)
        return id
    }

    /**
     * Records `amount` progress for [habit] on [date] (defaults to today).
     * For binary habits any positive amount marks the day complete.
     */
    suspend fun completeHabit(habitId: String, date: LocalDate = Dates.today(), amount: Int = 1): Boolean {
        val habit = habitDao.getById(habitId) ?: return false
        if (habit.deletedAt != 0L) return false
        val iso = Dates.iso(date)
        val now = Dates.nowMillis()
        val target = habitTarget(habit)
        val existing = habitDao.completionOn(habitId, iso)?.amount ?: 0
        val newValue = (existing + amount).coerceAtMost(target.coerceAtLeast(1))
        habitDao.upsertCompletion(
            HabitCompletionEntity(
                habitId = habitId,
                date = iso,
                amount = newValue.coerceAtLeast(1),
                recordedAt = now,
            ),
        )
        logEvent(
            LifeEventType.HABIT_COMPLETED,
            "${habit.emoji.ifBlank { "" }} ${habit.name}".trim(),
            area = habit.area,
            sourceType = "habits",
            sourceId = habitId,
            at = now,
            on = iso,
        )
        return newValue >= target
    }

    /** Removes a completion and its matching timeline row for that date. */
    suspend fun uncompleteHabit(habitId: String, date: LocalDate = Dates.today()) {
        val iso = Dates.iso(date)
        habitDao.removeCompletion(habitId, iso)
        // Drop the timeline row this completion created (same habit + same day).
        val events = db.lifeEventDao()
        matchingEventIds(habitId, iso).forEach { events.softDelete(it, Dates.nowMillis()) }
    }

    private suspend fun matchingEventIds(habitId: String, iso: String): List<String> {
        // Timeline rows for a habit carry sourceId = habitId; filter by date in Kotlin
        // to keep the DAO surface small.
        val todayEvents = db.lifeEventDao().observeRecent(limit = 1000).first()
        return todayEvents
            .filter { it.sourceType == "habits" && it.sourceId == habitId && it.occurredOn == iso }
            .map { it.id }
    }

    suspend fun habitStreak(habitId: String, today: LocalDate = Dates.today()): HabitProgress.StreakResult {
        val habit = habitDao.getById(habitId) ?: return HabitProgress.StreakResult(0, 0)
        val completions = habitDao.completionsFor(habitId).mapNotNull { Dates.parseOrNull(it.date) }.toSet()
        val recurrence = Recurrence.fromId(habit.recurrence)
        val startDate = Dates.parseOrNull(Dates.iso(today)) ?: today
        return HabitProgress.streaks(
            completedDates = completions,
            scheduledPredicate = { date ->
                RecurrenceRules.occursOn(date, earliest(habit, completions, startDate), recurrence, habit.recurrenceParameter)
            },
            today = today,
        )
    }

    private fun earliest(habit: HabitEntity, completions: Set<LocalDate>, fallback: LocalDate): LocalDate =
        minOf(completions.minOrNull() ?: fallback, Dates.toLocalDate(habit.createdAt))

    suspend fun setHabitArchived(id: String, archived: Boolean) =
        habitDao.setArchived(id, if (archived) Dates.nowMillis() else null, Dates.nowMillis())

    suspend fun moveToTrashHabit(id: String) {
        habitDao.softDelete(id, Dates.nowMillis())
        db.lifeEventDao().deleteForSource("habits", id)
    }

    suspend fun restoreHabit(id: String) = habitDao.restore(id, Dates.nowMillis())
    suspend fun deleteHabitForever(id: String) {
        db.lifeEventDao().deleteForSource("habits", id)
        habitDao.deleteForever(id)
    }

    suspend fun completions(habitId: String): List<HabitCompletionEntity> =
        habitDao.completionsFor(habitId)

    // ------------------------------------------------------------- Expenses

    private val expenseDao get() = db.expenseDao()

    fun observeExpenses(): Flow<List<ExpenseEntity>> = expenseDao.observeAll()
    fun observeExpenseTrash(): Flow<List<ExpenseEntity>> = expenseDao.observeTrash()

    fun observeExpenseTotal(from: String, to: String, currency: String): Flow<Long> =
        expenseDao.observeTotalBetween(from, to, currency)

    fun observeCategoryTotals(from: String, to: String, currency: String) =
        expenseDao.observeCategoryTotals(from, to, currency)

    /**
     * @param money parsed with [Money.parse] — minor units only cross this API.
     */
    suspend fun addExpense(
        money: Money,
        category: String,
        note: String = "",
        paymentMethod: String = "",
        date: LocalDate = Dates.today(),
    ): String {
        val now = Dates.nowMillis()
        val id = Ids.new()
        expenseDao.upsert(
            ExpenseEntity(
                id = id,
                amountMinor = money.minorUnits,
                currencyCode = money.currencyCode,
                category = category.trim().ifEmpty { "Other" },
                note = note.trim(),
                paymentMethod = paymentMethod,
                expenseDate = Dates.iso(date),
                createdAt = now,
                updatedAt = now,
            ),
        )
        logEvent(
            LifeEventType.EXPENSE_ADDED,
            "${money.currencyCode} ${money.minorUnits}",
            detail = category,
            sourceType = "expenses",
            sourceId = id,
            at = now,
            on = Dates.iso(date),
        )
        return id
    }

    suspend fun moveToTrashExpense(id: String) {
        expenseDao.softDelete(id, Dates.nowMillis())
        db.lifeEventDao().deleteForSource("expenses", id)
    }

    suspend fun restoreExpense(id: String) = expenseDao.restore(id, Dates.nowMillis())
    suspend fun deleteExpenseForever(id: String) {
        db.lifeEventDao().deleteForSource("expenses", id)
        expenseDao.deleteForever(id)
    }

    // --------------------------------------------------------------- Diary

    private val diaryDao get() = db.diaryDao()

    fun observeDiary(): Flow<List<DiaryEntryEntity>> = diaryDao.observeAll()
    fun observeDiaryOn(date: String): Flow<DiaryEntryEntity?> = diaryDao.observeOn(date)
    fun observeDiaryTrash(): Flow<List<DiaryEntryEntity>> = diaryDao.observeTrash()

    suspend fun writeDiary(
        date: LocalDate = Dates.today(),
        title: String = "",
        body: String,
        mood: String? = null,
    ): String {
        val now = Dates.nowMillis()
        val iso = Dates.iso(date)
        val existing = diaryDao.getOn(iso)
        val id = existing?.id ?: Ids.new()
        diaryDao.upsert(
            DiaryEntryEntity(
                id = id,
                diaryDate = iso,
                title = title.trim(),
                body = body.trim(),
                mood = mood,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
        logEvent(
            LifeEventType.DIARY_WRITTEN,
            title.trim().ifEmpty { "Diary entry" },
            detail = mood?.let { "Mood: $it" } ?: "",
            sourceType = "diary_entries",
            sourceId = id,
            at = now,
            on = iso,
        )
        return id
    }

    suspend fun moveToTrashDiary(id: String) {
        diaryDao.softDelete(id, Dates.nowMillis())
        db.lifeEventDao().deleteForSource("diary_entries", id)
    }

    suspend fun restoreDiary(id: String) = diaryDao.restore(id, Dates.nowMillis())
    suspend fun deleteDiaryForever(id: String) {
        db.lifeEventDao().deleteForSource("diary_entries", id)
        diaryDao.deleteForever(id)
    }

    suspend fun moodSeries(from: String, to: String) = diaryDao.moodSeries(from, to)

    // ------------------------------------------------------------- helpers

    private suspend fun logEvent(
        type: LifeEventType,
        title: String,
        detail: String = "",
        area: String = "personal",
        sourceType: String? = null,
        sourceId: String? = null,
        at: Long = Dates.nowMillis(),
        on: String = Dates.iso(Dates.today()),
    ) {
        db.lifeEventDao().upsert(
            LifeEventEntity(
                id = Ids.new(),
                type = type.code,
                title = title,
                detail = detail,
                area = area,
                occurredOn = on,
                occurredAt = at,
                sourceType = sourceType,
                sourceId = sourceId,
            ),
        )
    }
}

/** One row of the Habits "today" screen. */
data class HabitTodayItem(
    val habit: HabitEntity,
    val completedToday: Int,
    val targetToday: Int,
    val isDoneToday: Boolean,
)
