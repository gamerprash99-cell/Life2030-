package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class HabitFrequency { DAILY, WEEKLY, CUSTOM }

/** Habit Tracker — Section 11/12/13. */
@Serializable
@Entity(tableName = "habits", indices = [Index(value = ["isArchived"])])
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val category: String? = null,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val customDaysCsv: String? = null,
    val goalCount: Int = 1,
    val reminderEpochMillis: Long? = null,
    val startDateEpochDay: Long,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
@Entity(tableName = "habit_completions", primaryKeys = ["habitId", "dateEpochDay"], indices = [Index(value = ["dateEpochDay"])])
data class HabitCompletionEntity(
    val habitId: String,
    val dateEpochDay: Long,
    val progressCount: Int,
    val completedAtEpochMillis: Long
)
