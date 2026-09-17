package com.lifeos.app.data.repository

import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitFrequency
import com.lifeos.app.data.db.entities.NoteEntity
import com.lifeos.app.data.db.entities.TaskEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackupSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun sampleBackup() = LifeOSBackup(
        exportedAtEpochMillis = 1_700_000_000_000L,
        appVersion = "0.2.0",
        notes = listOf(
            NoteEntity(
                id = "n1", title = "Draft", contentJson = "[]", plainTextForSearch = "Hello",
                folder = null, tagsCsv = "ideas", createdAt = 1L, updatedAt = 1L
            )
        ),
        tasks = listOf(
            TaskEntity(
                id = "t1", title = "Ship release", description = "Push the build",
                dueDateEpochDay = 20_000L, priority = com.lifeos.app.data.db.entities.TaskPriority.HIGH,
                repeatRule = com.lifeos.app.data.db.entities.RepeatRule.WEEKLY,
                createdAt = 1L, updatedAt = 1L
            )
        ),
        habits = listOf(
            HabitEntity(
                id = "h1", name = "Read", icon = "📚", category = "Growth",
                frequency = HabitFrequency.DAILY, goalCount = 1,
                startDateEpochDay = 19_000L, createdAt = 1L, updatedAt = 1L
            )
        ),
        habitCompletions = listOf(HabitCompletionEntity(habitId = "h1", dateEpochDay = 19_001L, progressCount = 1, completedAtEpochMillis = 2L)),
        expenses = listOf(
            ExpenseEntity(
                id = "e1", amount = 12.5, category = "Food", dateEpochDay = 19_001L,
                timeMinutes = 60, merchant = "Café", createdAt = 1L
            )
        ),
        diaryEntries = listOf(
            DiaryEntity(
                id = "d1", title = "Day one", content = "Notes here", mood = "Positive",
                tagsCsv = "life", dateEpochDay = 19_001L, timeMinutes = 30,
                createdAt = 1L, updatedAt = 2L
            )
        ),
        captures = listOf(
            CaptureEntity(
                id = "c1", type = CaptureType.PHOTO, filePath = "/tmp/a.jpg",
                dateEpochDay = 19_001L, timeMinutes = 5, createdAt = 1L
            )
        )
    )

    @Test
    fun `backup round trips all entity types`() {
        val backup = sampleBackup()
        val encoded = json.encodeToString(backup)
        val decoded = json.decodeFromString<LifeOSBackup>(encoded)
        assertEquals(backup, decoded)
    }

    @Test
    fun `backup json is stable enough to stringify`() {
        val backup = sampleBackup()
        val first = json.encodeToString(backup)
        val second = json.encodeToString(backup)
        assertEquals(first, second)
        assertNotEquals("stability", first)
        assertEquals(true, first.contains("\"appVersion\":\"0.2.0\""))
    }

    @Test
    fun `empty backup round trips`() {
        val empty = LifeOSBackup(1L, "0.2.0", emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val decoded = json.decodeFromString<LifeOSBackup>(json.encodeToString(empty))
        assertEquals(empty, decoded)
    }
}