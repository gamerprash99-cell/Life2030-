package com.lifeos.app.domain.model

/**
 * The Life Timeline (Section 3/60) is deliberately NOT its own database table.
 * It is a real-time aggregation over Notes, Tasks, Habits, Expenses, Diary and
 * Captures — this is what the spec means by "connect through Date, Time, Tags,
 * Timeline, Relationships" rather than duplicating data into a redundant table.
 * See domain/usecase/BuildTimelineUseCase.kt for how this is assembled.
 */
data class TimelineItem(
    val id: String,
    val type: TimelineItemType,
    val title: String,
    val subtitle: String? = null,
    val dateEpochDay: Long,
    val timeMinutes: Int,
    val icon: String,
    val sourceId: String,
    val moodOrCategory: String? = null
)

enum class TimelineItemType { NOTE, TASK_COMPLETED, HABIT_COMPLETED, EXPENSE, DIARY, CAPTURE }
