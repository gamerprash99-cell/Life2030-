package com.lifeos.app.domain.usecase

import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitTrackingMode
import com.lifeos.app.data.db.entities.HabitType
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.data.db.entities.TaskPriority
import com.lifeos.app.data.db.entities.RepeatRule
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineMappingTest {

    @Test
    fun `task row keeps the completion timestamp and routes to Tasks tab`() {
        val completedAt = 9L * 60 * 60 * 1000 + 14L * 60 * 1000 // 09:14
        val task = TaskEntity(
            id = "t1", title = "Buy milk", description = null,
            dueDateEpochDay = null, dueTimeMinutes = null,
            priority = TaskPriority.MEDIUM, category = null,
            reminderEpochMillis = null, repeatRule = RepeatRule.NONE,
            repeatDaysCsv = null, isCompleted = true,
            completedAtEpochMillis = completedAt,
            isDeleted = false, notes = null, attachmentsJson = "[]",
            sourceType = null, sourceId = null, createdAt = 1L, updatedAt = 1L
        )
        val epochDay = DateTimeUtils.epochDayOf(completedAt)
        val item = taskTimelineItem(task, epochDay)
        assertEquals("Buy milk", item.title)
        assertEquals(TimelineItemType.TASK_COMPLETED, item.type)
        assertEquals("Completed", item.statusLabel)
        // The minute is the real completion time-of-day.
        assertEquals(DateTimeUtils.minutesOfDay(completedAt), item.timeMinutes)
        assertEquals("task-t1", item.id)
    }

    @Test
    fun `habit completion row only appears when the goal was met`() {
        val habit = HabitEntity(
            id = "h1", name = "Read", icon = "📚", category = null,
            frequency = com.lifeos.app.data.db.entities.HabitFrequency.DAILY,
            customDaysCsv = null, goalCount = 1, reminderEpochMillis = null,
            startDateEpochDay = 1L, isArchived = false, createdAt = 1L, updatedAt = 1L,
            description = null, colorArgb = 0L, habitType = HabitType.BUILD,
            trackingMode = HabitTrackingMode.TICK, scheduleType = com.lifeos.app.data.db.entities.HabitScheduleType.LEGACY,
            daysOfWeekCsv = null, daysOfMonthCsv = null, timesPerPeriod = null,
            showOnTimeline = true
        )
        val done = HabitCompletionEntity(habitId = "h1", dateEpochDay = 2L, progressCount = 1,
            completedAtEpochMillis = 1000L, outcome = com.lifeos.app.data.db.entities.HabitOutcome.DONE, targetCount = 0)
        val item = habitTimelineItem(habit, done, 2L)
        assertEquals("Read", item.title)
        assertEquals(TimelineItemType.HABIT_COMPLETED, item.type)
        assertTrue(habit.showsOnTimeline(done))

        val slipped = HabitCompletionEntity(habitId = "h1", dateEpochDay = 2L, progressCount = 0,
            completedAtEpochMillis = 1000L, outcome = com.lifeos.app.data.db.entities.HabitOutcome.SLIPPED, targetCount = 0)
        assertFalse(habit.showsOnTimeline(slipped))
    }

    @Test
    fun `expense row carries the real amount`() {
        val expense = ExpenseEntity(
            id = "e1", amount = 249.50, category = "Food", dateEpochDay = 2L,
            timeMinutes = 12 * 60 + 30, merchant = "Cafe", paymentMethod = com.lifeos.app.data.db.entities.PaymentMethod.CASH,
            note = null, tagsCsv = "", createdAt = 1L
        )
        val item = expenseTimelineItem(expense)
        assertEquals("Cafe", item.title)
        assertEquals(TimelineItemType.EXPENSE, item.type)
        assertEquals(249.50, item.amount)
        assertEquals("Food", item.subtitle)
    }

    @Test
    fun `diary row uses the entry text as preview when untitled`() {
        val entry = DiaryEntity(
            id = "d1", title = null, content = "Had a lovely morning walk.", mood = "Positive",
            tagsCsv = "", dateEpochDay = 2L, timeMinutes = 8 * 60 + 5,
            aiGenerated = false, isReviewed = true, attachmentsJson = "[]",
            isFavorite = false, createdAt = 1L, updatedAt = 1L
        )
        val item = diaryTimelineItem(entry)
        assertEquals("Had a lovely morning walk.", item.title)
        assertEquals(TimelineItemType.DIARY, item.type)
        assertEquals("Positive", item.moodOrCategory)
    }

    @Test
    fun `photo capture carries the image path, audio carries media path`() {
        val photo = CaptureEntity(
            id = "c1", type = CaptureType.PHOTO, text = "Sunset", filePath = "/files/photo.jpg",
            mimeType = "image/jpeg", durationMillis = null, latitude = null, longitude = null,
            placeLabel = null, moodKey = null, tagsCsv = "", isFavorite = false, createdAt = 1L
        )
        val photoItem = captureTimelineItem(photo)
        assertEquals(TimelineItemType.PHOTO, photoItem.type)
        assertEquals("/files/photo.jpg", photoItem.imagePath)
        assertNull(photoItem.mediaPath)

        val voice = CaptureEntity(
            id = "c2", type = CaptureType.AUDIO, text = null, filePath = "/files/voice.m4a",
            mimeType = "audio/mp4", durationMillis = 45_000L, latitude = null, longitude = null,
            placeLabel = null, moodKey = null, tagsCsv = "", isFavorite = false, createdAt = 1L
        )
        val voiceItem = captureTimelineItem(voice)
        assertEquals(TimelineItemType.AUDIO, voiceItem.type)
        assertNull(voiceItem.imagePath)
        assertEquals("/files/voice.m4a", voiceItem.mediaPath)
        assertEquals(45_000L, voiceItem.durationMillis)
        assertEquals("Voice note", voiceItem.title)
    }

    @Test
    fun `timeline sorts newest day first, then newest minute first`() {
        val items = listOf(
            TimelineItem("a", TimelineItemType.EXPENSE, "Early", null, 1L, 9 * 60, "•", "a"),
            TimelineItem("b", TimelineItemType.EXPENSE, "Late", null, 1L, 17 * 60, "•", "b"),
            TimelineItem("c", TimelineItemType.EXPENSE, "Older day", null, 0L, 17 * 60, "•", "c")
        )
        val grouped = TimelineDayGrouping.group(items.sortedWith(
            compareByDescending<TimelineItem> { it.dateEpochDay }
                .thenByDescending { it.timeMinutes }
                .thenBy { it.id }
        ))
        assertEquals(2, grouped.size)
        assertEquals(1L, grouped[0].epochDay)
        assertEquals("Late", grouped[0].items[0].title)
        assertEquals("Early", grouped[0].items[1].title)
        assertEquals(0L, grouped[1].epochDay)
    }

    private fun assertNull(value: Any?) {
        org.junit.Assert.assertNull(value)
    }
}
