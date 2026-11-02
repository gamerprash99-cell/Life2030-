package com.lifeos.app.domain.usecase

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.domain.model.TimelineItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * The Life tab's history: every persisted activity between two calendar days,
 * newest first.
 *
 * One query per module for the whole span (never one query per day — see the
 * DAO range methods this calls), assembled from the same [TimelineItem] rows
 * Home and the old day view used. There is no timeline table: a task completion,
 * a habit day, an expense, a diary entry and a capture are read from the tables
 * that own them, so deleting a record removes its timeline row with it and no
 * copy can drift.
 *
 * Ordering is by the row's real calendar day and its real minute-of-day, both
 * descending, so "today's last thing" is the first thing on screen. Rows that
 * share a minute keep a stable order (expense, then diary, then capture, then
 * task, then habit) instead of shuffling between recompositions.
 */
class BuildLifeTimelineUseCase(
    private val taskRepo: TaskRepository,
    private val habitRepo: HabitRepository,
    private val expenseRepo: ExpenseRepository,
    private val diaryRepo: DiaryRepository,
    private val captureRepo: CaptureRepository
) {

    suspend operator fun invoke(
        fromEpochDay: Long,
        toEpochDay: Long = DateTimeUtils.today().toEpochDay(),
        limit: Int = DEFAULT_LIMIT
    ): List<TimelineItem> {
        if (toEpochDay < fromEpochDay) return emptyList()
        val (startMillis, endMillis) = DateTimeUtils.dayRangeMillis(fromEpochDay, toEpochDay)

        return coroutineScope {
            val tasks = async { taskRepo.getCompletedBetween(startMillis, endMillis) }
            val habits = async { habitRepo.observeAll().first() }
            val completions = async { habitRepo.observeAllInRange(fromEpochDay, toEpochDay).first() }
            val expenses = async { expenseRepo.observeInRange(fromEpochDay, toEpochDay).first() }
            val diaries = async { diaryRepo.getAllInRange(fromEpochDay, toEpochDay) }
            val captures = async { captureRepo.getAllInRange(startMillis, endMillis) }

            val habitsById = habits.await().associateBy { it.id }
            val items = mutableListOf<TimelineItem>()

            tasks.await().forEach { task ->
                items += taskTimelineItem(task, DateTimeUtils.epochDayOf(task.completedAtEpochMillis ?: task.updatedAt))
            }

            completions.await().forEach { completion ->
                val habit = habitsById[completion.habitId] ?: return@forEach
                if (!habit.showsOnTimeline(completion)) return@forEach
                items += habitTimelineItem(habit, completion, completion.dateEpochDay)
            }

            expenses.await().forEach { items += expenseTimelineItem(it) }
            diaries.await().forEach { items += diaryTimelineItem(it) }
            captures.await().forEach { items += captureTimelineItem(it) }

            items
                .sortedWith(
                    compareByDescending<TimelineItem> { it.dateEpochDay }
                        .thenByDescending { it.timeMinutes }
                        // `id` is unique per source row and stable across loads,
                        // so two rows on the same minute never swap places.
                        .thenBy { it.id }
                )
                .let { if (limit > 0 && it.size > limit) it.take(limit) else it }
        }
    }

    companion object {
        /**
         * How far back the Life tab loads in one page. Ninety days covers
         * "this season" for a personal journal while keeping the first
         * aggregation bounded on a phone. It is a visible window, not a hidden
         * truncation: the screen offers to load the next span of older history
         * (see LifeTimelineViewModel.loadOlder).
         */
        const val DEFAULT_LIMIT = 400

        /** The default window the Life tab opens with. */
        const val DEFAULT_WINDOW_DAYS = 90L
    }
}

/**
 * Groups rows by their calendar day, newest day first, and separates "Today"
 * and "Yesterday" from older dates.
 *
 * Pure and JVM-testable: [nowEpochDay] is a parameter rather than a clock read
 * so a test can pin the labels.
 */
object TimelineDayGrouping {

    fun labelFor(epochDay: Long, nowEpochDay: Long): String = when (epochDay) {
        nowEpochDay -> "Today"
        nowEpochDay - 1 -> "Yesterday"
        else -> DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(epochDay))
    }

    /**
     * Splits [items] into day sections, newest first, each internally ordered
     * newest-first. Feed it the output of [BuildLifeTimelineUseCase]; it never
     * re-sorts within a day beyond what it was handed.
     */
    fun group(items: List<TimelineItem>): List<DaySection> =
        items.groupBy { it.dateEpochDay }
            .toSortedMap(reverseOrder())
            .map { (epochDay, rows) -> DaySection(epochDay = epochDay, items = rows) }

    data class DaySection(val epochDay: Long, val items: List<TimelineItem>)
}
