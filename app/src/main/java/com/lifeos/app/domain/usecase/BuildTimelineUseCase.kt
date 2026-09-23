package com.lifeos.app.domain.usecase

import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.domain.model.ExpenseCategories
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class BuildTimelineUseCase(
    private val taskRepo: TaskRepository,
    private val habitRepo: HabitRepository,
    private val expenseRepo: ExpenseRepository,
    private val diaryRepo: DiaryRepository
) {
    suspend operator fun invoke(epochDay: Long): List<TimelineItem> {
        val startMillis = com.lifeos.app.core.util.DateTimeUtils.startOfLocalDayMillis(epochDay)
        val endMillis = com.lifeos.app.core.util.DateTimeUtils.endOfLocalDayMillis(epochDay)

        return coroutineScope {
            // Fire every source query concurrently instead of waiting on one
            // stream at a time (keeps the Home summary assembly fast).
            val tasks = async { taskRepo.getCompletedBetween(startMillis, endMillis) }
            val habits = async { habitRepo.observeAll().first() }
            val todayCompletions = async { habitRepo.observeAllForDay(epochDay).first() }
            val expenses = async { expenseRepo.observeForDay(epochDay).first() }
            val diaries = async { diaryRepo.observeForDay(epochDay).first() }

            val items = mutableListOf<TimelineItem>()
            val habitsById = habits.await().associateBy { it.id }

            tasks.await()
                .forEach { task ->
                    items += TimelineItem(
                        id = "task-${task.id}", type = TimelineItemType.TASK_COMPLETED,
                        title = task.title, subtitle = "Task completed", dateEpochDay = epochDay,
                        timeMinutes = task.completedAtEpochMillis?.let { DateTimeUtils.minutesOfDay(it) } ?: DateTimeUtils.minutesOfDay(task.updatedAt),
                        icon = "✅", sourceId = task.id, moodOrCategory = task.category
                    )
                }

            todayCompletions.await()
                .filter { completion ->
                    val habit = habitsById[completion.habitId]
                    habit != null && completion.progressCount >= habit.goalCount
                }
                .forEach { completion ->
                    val habit = habitsById[completion.habitId] ?: return@forEach
                    items += TimelineItem(
                        id = "habit-${habit.id}-$epochDay", type = TimelineItemType.HABIT_COMPLETED,
                        title = habit.name, subtitle = "Habit completed", dateEpochDay = epochDay,
                        timeMinutes = DateTimeUtils.minutesOfDay(completion.completedAtEpochMillis),
                        icon = habit.icon, sourceId = habit.id
                    )
                }

            expenses.await().forEach { expense ->
                items += TimelineItem(
                    id = "expense-${expense.id}", type = TimelineItemType.EXPENSE,
                    title = expense.merchant ?: expense.category, subtitle = "₹${expense.amount}",
                    dateEpochDay = epochDay, timeMinutes = expense.timeMinutes,
                    icon = ExpenseCategories.emojiFor(expense.category), sourceId = expense.id,
                    moodOrCategory = expense.category
                )
            }

            diaries.await().forEach { diary ->
                items += TimelineItem(
                    id = "diary-${diary.id}", type = TimelineItemType.DIARY,
                    title = diary.title ?: "Diary entry", subtitle = diary.mood,
                    dateEpochDay = epochDay, timeMinutes = diary.timeMinutes, icon = "📔",
                    sourceId = diary.id, moodOrCategory = diary.mood
                )
            }

            items.sortedBy { it.timeMinutes }
        }
    }
}
