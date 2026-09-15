package com.lifeos.app.domain.usecase

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.NoteRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.domain.model.ExpenseCategories
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType
import kotlinx.coroutines.flow.first
import java.time.ZoneOffset

/**
 * Builds the unified Timeline (Section 3/60) for a given day by pulling from
 * every feature's repository and merging by time — the concrete implementation
 * of the spec's "connect everything through Date/Time/Tags/Timeline" mandate.
 * Deliberately a suspend function (not a Flow) — the Timeline screen re-invokes
 * it when the selected date changes, keeping this simple and predictable.
 */
class BuildTimelineUseCase(
    private val noteRepo: NoteRepository,
    private val taskRepo: TaskRepository,
    private val habitRepo: HabitRepository,
    private val expenseRepo: ExpenseRepository,
    private val diaryRepo: DiaryRepository,
    private val captureRepo: CaptureRepository
) {
    suspend operator fun invoke(epochDay: Long): List<TimelineItem> {
        val items = mutableListOf<TimelineItem>()

        val startMillis = epochDay * 86_400_000L
        val endMillis = startMillis + 86_400_000L

        // Notes created/updated that day
        val notes = noteRepo.getCreatedBetween(startMillis, endMillis)
        items += notes.map {
            val minutes = epochMillisToMinutesOfDay(it.createdAt)
            TimelineItem(
                id = "note-${it.id}", type = TimelineItemType.NOTE, title = it.title.ifBlank { "Untitled note" },
                subtitle = it.folder, dateEpochDay = epochDay, timeMinutes = minutes, icon = "📝", sourceId = it.id
            )
        }

        // Tasks completed that day
        val completedTasks = taskRepo.getCreatedBetween(startMillis, endMillis).filter { it.isCompleted }
        items += completedTasks.map {
            val minutes = it.completedAtEpochMillis?.let(::epochMillisToMinutesOfDay) ?: 0
            TimelineItem(
                id = "task-${it.id}", type = TimelineItemType.TASK_COMPLETED, title = it.title,
                subtitle = "Task completed", dateEpochDay = epochDay, timeMinutes = minutes, icon = "✅", sourceId = it.id,
                moodOrCategory = it.category
            )
        }

        // Habits completed that day (repositories expose Flow; take the first/current emission)
        val habitsToday = habitRepo.observeAllForDay(epochDay).first()
        val habitsById = habitRepo.observeAll().first().associateBy { it.id }
        items += habitsToday.filter { completion -> 
            val habit = habitsById[completion.habitId]
            habit != null && completion.progressCount >= habit.goalCount
        }.map { completion ->
            val habit = habitsById[completion.habitId]!!
            val minutes = epochMillisToMinutesOfDay(completion.completedAtEpochMillis)
            TimelineItem(
                id = "habit-${habit.id}-$epochDay", type = TimelineItemType.HABIT_COMPLETED, title = habit.name,
                subtitle = "Habit completed", dateEpochDay = epochDay, timeMinutes = minutes, icon = habit.icon,
                sourceId = habit.id
            )
        }

        // Expenses
        val expenses = expenseRepo.observeForDay(epochDay).first()
        items += expenses.map {
            TimelineItem(
                id = "expense-${it.id}", type = TimelineItemType.EXPENSE,
                title = it.merchant ?: it.category, subtitle = "₹${it.amount}",
                dateEpochDay = epochDay, timeMinutes = it.timeMinutes,
                icon = ExpenseCategories.emojiFor(it.category), sourceId = it.id, moodOrCategory = it.category
            )
        }

        // Diary
        val diaryEntries = diaryRepo.observeForDay(epochDay).first()
        items += diaryEntries.map {
            TimelineItem(
                id = "diary-${it.id}", type = TimelineItemType.DIARY,
                title = it.title ?: "Diary entry", subtitle = it.mood,
                dateEpochDay = epochDay, timeMinutes = it.timeMinutes, icon = "📔", sourceId = it.id,
                moodOrCategory = it.mood
            )
        }

        // Captures
        val captures = captureRepo.observeForDay(epochDay).first()
        items += captures.map {
            val icon = when (it.type.name) {
                "PHOTO" -> "📷"; "VIDEO" -> "🎥"; "AUDIO" -> "🎙"; else -> "💭"
            }
            TimelineItem(
                id = "capture-${it.id}", type = TimelineItemType.CAPTURE,
                title = it.caption ?: it.type.name.lowercase().replaceFirstChar { c -> c.uppercase() },
                dateEpochDay = epochDay, timeMinutes = it.timeMinutes, icon = icon, sourceId = it.id
            )
        }

        return items.sortedBy { it.timeMinutes }
    }

    private fun epochMillisToMinutesOfDay(millis: Long): Int {
        val instant = java.time.Instant.ofEpochMilli(millis)
        val local = instant.atZone(ZoneOffset.systemDefault()).toLocalTime()
        return local.hour * 60 + local.minute
    }
}
