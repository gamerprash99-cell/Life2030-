package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Legacy frequency retained for every habit written before v7. New habits also
 * keep a compatible value so an older LifeOS build can still read the row, but
 * [HabitScheduleType] is the authoritative schedule for the new experience.
 */
@Serializable
enum class HabitFrequency { DAILY, WEEKLY, CUSTOM }

/** Whether the habit is something to do (Build) or something to avoid (Quit). */
@Serializable
enum class HabitType { BUILD, QUIT }

/** How a scheduled day is recorded. */
@Serializable
enum class HabitTrackingMode { TICK, COUNT, CHECKLIST }

/**
 * Authoritative schedule shape. [LEGACY] preserves the exact pre-v7 meaning of
 * DAILY / WEEKLY / CUSTOM, so existing streaks are never reinterpreted.
 */
@Serializable
enum class HabitScheduleType {
    LEGACY,
    EVERY_DAY,
    WEEKDAYS,
    MONTH_DAYS,
    TIMES_PER_WEEK,
    TIMES_PER_MONTH
}

/** Explicit result of a logged day. An unrecorded day is neither of these. */
@Serializable
enum class HabitOutcome { DONE, SLIPPED }

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
    val updatedAt: Long,
    // ---- v7 permanent Habits experience -----------------------------------
    val description: String? = null,
    /** ARGB colour chosen by the user; 0 means "use the LifeOS habit default". */
    val colorArgb: Long = 0L,
    val habitType: HabitType = HabitType.BUILD,
    val trackingMode: HabitTrackingMode = HabitTrackingMode.TICK,
    val scheduleType: HabitScheduleType = HabitScheduleType.LEGACY,
    /** ISO weekdays (1=Mon..7=Sun) for [HabitScheduleType.WEEKDAYS]. */
    val daysOfWeekCsv: String? = null,
    /** Days of month (1..31) for [HabitScheduleType.MONTH_DAYS]. */
    val daysOfMonthCsv: String? = null,
    /** Target occurrences for TIMES_PER_WEEK / TIMES_PER_MONTH. */
    val timesPerPeriod: Int? = null,
    val showOnTimeline: Boolean = true
)

@Serializable
@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "dateEpochDay"],
    indices = [Index(value = ["dateEpochDay"])]
)
data class HabitCompletionEntity(
    val habitId: String,
    val dateEpochDay: Long,
    val progressCount: Int,
    val completedAtEpochMillis: Long,
    /** DONE is a successful day; SLIPPED is an explicitly logged failure for a Quit habit. */
    val outcome: HabitOutcome = HabitOutcome.DONE,
    /**
     * Snapshot of the target on the day this row was written. 0 means a legacy
     * row and the habit's current target is used. Snapshots keep history honest
     * when the user later edits a count goal or checklist template.
     */
    val targetCount: Int = 0
)

/**
 * A reusable checklist template item. Items are archived, never deleted while
 * check rows exist, so editing a template can never rewrite history.
 */
@Serializable
@Entity(
    tableName = "habit_checklist_items",
    indices = [Index(value = ["habitId"]), Index(value = ["habitId", "position"])]
)
data class HabitChecklistItemEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val label: String,
    val position: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long
)

/** Per-date checked state for one checklist template item. */
@Serializable
@Entity(
    tableName = "habit_checklist_checks",
    primaryKeys = ["itemId", "dateEpochDay"],
    indices = [Index(value = ["habitId"]), Index(value = ["habitId", "dateEpochDay"])]
)
data class HabitChecklistCheckEntity(
    val itemId: String,
    val habitId: String,
    val dateEpochDay: Long,
    val isChecked: Boolean,
    val checkedAtEpochMillis: Long,
    /** The label at the moment it was checked, so history survives a rename. */
    val labelSnapshot: String = ""
)
