package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class TaskPriority { HIGH, MEDIUM, LOW }

@Serializable
enum class RepeatRule { NONE, DAILY, WEEKLY, MONTHLY, CUSTOM_DAYS }

/** Task Management — Section 9/10. */
@Serializable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val dueDateEpochDay: Long? = null,
    val dueTimeMinutes: Int? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: String? = null,
    val reminderEpochMillis: Long? = null,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val repeatDaysCsv: String? = null,
    val isCompleted: Boolean = false,
    val completedAtEpochMillis: Long? = null,
    val isDeleted: Boolean = false,
    val notes: String? = null,
    val attachmentsJson: String = "[]",
    val sourceType: String? = null,
    val sourceId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
