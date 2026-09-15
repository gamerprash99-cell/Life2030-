package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskPriority { HIGH, MEDIUM, LOW }

enum class RepeatRule { NONE, DAILY, WEEKLY, MONTHLY, CUSTOM_DAYS }

/**
 * Task Management — Section 9/10.
 * `sourceType`/`sourceId` links back to the note/diary entry/voice capture the
 * task was extracted from, so the app can show "created from this note" —
 * one of the core "connected experience" requirements (Section 60).
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val dueDateEpochDay: Long? = null,   // LocalDate.toEpochDay()
    val dueTimeMinutes: Int? = null,      // minutes since midnight, null = no specific time
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: String? = null,
    val reminderEpochMillis: Long? = null,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val repeatDaysCsv: String? = null,    // for CUSTOM_DAYS, e.g. "MON,WED,FRI"
    val isCompleted: Boolean = false,
    val completedAtEpochMillis: Long? = null,
    val isDeleted: Boolean = false,
    val notes: String? = null,
    val attachmentsJson: String = "[]",
    val sourceType: String? = null,       // "note" | "diary" | "voice" | "ai" | null (manually created)
    val sourceId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
