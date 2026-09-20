package com.lifeos.app.domain.usecase

import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.NoteRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.domain.model.ExpenseCategories
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.time.ZoneOffset

class BuildTimelineUseCase(
    private val noteRepo: NoteRepository,
    private val taskRepo: TaskRepository,
    private val habitRepo: HabitRepository,
    private val expenseRepo: ExpenseRepository,
    private val diaryRepo: DiaryRepository,
    private val captureRepo: CaptureRepository
) {
    suspend operator fun invoke(epochDay: Long): List<TimelineItem> {
        val startMillis = com.lifeos.app.core.util.DateTimeUtils.startOfLocalDayMillis(epochDay)
        val endMillis = com.lifeos.app.core.util.DateTimeUtils.endOfLocalDayMillis(epochDay)

        return coroutineScope {
            // Fire every source query concurrently instead of waiting on one
            // stream at a time (keeps the Home summary assembly fast).
            val notes = async { noteRepo.observeAll().first() }
            val tasks = async { taskRepo.observeAll().first() }
            val habits = async { habitRepo.observeAll().first() }
            val todayCompletions = async { habitRepo.observeAllForDay(epochDay).first() }
            val expenses = async { expenseRepo.observeForDay(epochDay).first() }
            val diaries = async { diaryRepo.observeForDay(epochDay).first() }
            val captures = async { captureRepo.observeForDay(epochDay).first() }

            val items = mutableListOf<TimelineItem>()
            val habitsById = habits.await().associateBy { it.id }

            notes.await()
                .filter { it.createdAt in startMillis until endMillis }
                .forEach { note ->
                    items += TimelineItem(
                        id = "note-${note.id}", type = TimelineItemType.NOTE,
                        title = note.title.ifBlank { "Untitled note" }, subtitle = note.folder,
                        dateEpochDay = epochDay, timeMinutes = epochMillisToMinutesOfDay(note.createdAt),
                        icon = "📝", sourceId = note.id
                    )
                }

            tasks.await()
                .filter { it.isCompleted && (it.completedAtEpochMillis ?: it.updatedAt) in startMillis until endMillis }
                .forEach { task ->
                    items += TimelineItem(
                        id = "task-${task.id}", type = TimelineItemType.TASK_COMPLETED,
                        title = task.title, subtitle = "Task completed", dateEpochDay = epochDay,
                        timeMinutes = task.completedAtEpochMillis?.let(::epochMillisToMinutesOfDay) ?: epochMillisToMinutesOfDay(task.updatedAt),
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
                        timeMinutes = epochMillisToMinutesOfDay(completion.completedAtEpochMillis),
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

            captures.await().forEach { capture ->
                val icon = when (capture.type.name) {
                    "PHOTO" -> "📷"; "VIDEO" -> "🎥"; "AUDIO" -> "🎙"; else -> "💭"
                }
                items += TimelineItem(
                    id = "capture-${capture.id}", type = TimelineItemType.CAPTURE,
                    title = capture.caption ?: capture.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    dateEpochDay = epochDay, timeMinutes = capture.timeMinutes, icon = icon, sourceId = capture.id
                )
            }

            items.sortedBy { it.timeMinutes }
        }
    }

    private fun epochMillisToMinutesOfDay(millis: Long): Int {
        val local = java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.systemDefault()).toLocalTime()
        return local.hour * 60 + local.minute
    }
}
