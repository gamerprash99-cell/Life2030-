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
 * One day of the timeline, as Home's summary and any single-day surface need
 * it. The Life tab's multi-day history is [BuildLifeTimelineUseCase]; both
 * assemble their rows through the same mapping functions
 * (domain/usecase/TimelineMapping.kt), so a task completed at 09:14 reads
 * identically wherever it appears.
 *
 * Every source query fires concurrently instead of waiting on one stream at a
 * time, which keeps the day's assembly fast enough to run while the screen is
 * still painting.
 */
class BuildTimelineUseCase(
    private val taskRepo: TaskRepository,
    private val habitRepo: HabitRepository,
    private val expenseRepo: ExpenseRepository,
    private val diaryRepo: DiaryRepository,
    private val captureRepo: CaptureRepository? = null
) {
    suspend operator fun invoke(epochDay: Long): List<TimelineItem> {
        val startMillis = DateTimeUtils.startOfLocalDayMillis(epochDay)
        val endMillis = DateTimeUtils.endOfLocalDayMillis(epochDay)

        return coroutineScope {
            val tasks = async { taskRepo.getCompletedBetween(startMillis, endMillis) }
            val habits = async { habitRepo.observeAll().first() }
            val todayCompletions = async { habitRepo.observeAllForDay(epochDay).first() }
            val expenses = async { expenseRepo.observeForDay(epochDay).first() }
            val diaries = async { diaryRepo.observeForDay(epochDay).first() }
            val captures = async { captureRepo?.getAllInRange(startMillis, endMillis).orEmpty() }

            val habitsById = habits.await().associateBy { it.id }
            val items = mutableListOf<TimelineItem>()

            tasks.await().forEach { items += taskTimelineItem(it, epochDay) }

            todayCompletions.await().forEach { completion ->
                val habit = habitsById[completion.habitId] ?: return@forEach
                if (!habit.showsOnTimeline(completion)) return@forEach
                items += habitTimelineItem(habit, completion, epochDay)
            }

            expenses.await().forEach { items += expenseTimelineItem(it) }
            diaries.await().forEach { items += diaryTimelineItem(it) }
            captures.await().forEach { items += captureTimelineItem(it) }

            items.sortedBy { it.timeMinutes }
        }
    }
}
