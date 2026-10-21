package com.lifeos.app.core.domain

import java.time.LocalDate

/**
 * Shared domain enums and small value types used across features and the database
 * layer. Stored in Room as the [code] strings, which are stable across versions.
 */

enum class LifeArea(val code: String, val label: String) {
    PERSONAL("personal", "Personal"),
    HEALTH("health", "Health"),
    WORK("work", "Work"),
    FINANCE("finance", "Finance"),
    RELATIONSHIPS("relationships", "Relationships"),
    LEARNING("learning", "Learning"),
    HOME("home", "Home"),
    OTHER("other", "Other");

    companion object {
        fun fromCode(code: String?): LifeArea =
            entries.firstOrNull { it.code == code } ?: OTHER
    }
}

enum class NoteKind(val code: String, val label: String) {
    PLAIN("plain", "Note"),
    CHECKLIST("checklist", "Checklist");

    companion object {
        fun fromCode(code: String?): NoteKind = entries.firstOrNull { it.code == code } ?: PLAIN
    }
}

enum class Priority(val code: String, val label: String) {
    NONE("none", "No priority"),
    LOW("low", "Low"),
    MEDIUM("medium", "Medium"),
    HIGH("high", "High");

    val sortOrder: Int get() = when (this) {
        NONE -> 0
        LOW -> 1
        MEDIUM -> 2
        HIGH -> 3
    }

    companion object {
        fun fromCode(code: String?): Priority = entries.firstOrNull { it.code == code } ?: NONE
    }
}

enum class TaskStatus(val code: String, val label: String) {
    OPEN("open", "Open"),
    DONE("done", "Done");

    companion object {
        fun fromCode(code: String?): TaskStatus = entries.firstOrNull { it.code == code } ?: OPEN
    }
}

enum class Mood(val code: String, val label: String, val emoji: String) {
    AWFUL("awful", "Awful", "😞"),
    LOW("low", "Low", "🙁"),
    OKAY("okay", "Okay", "😐"),
    GOOD("good", "Good", "🙂"),
    GREAT("great", "Great", "😄");

    /** Numeric anchor used for Insights charts (1..5). */
    val score: Int get() = ordinal + 1

    companion object {
        fun fromCode(code: String?): Mood = entries.firstOrNull { it.code == code } ?: OKAY

        fun fromScore(score: Int): Mood =
            entries.firstOrNull { it.score == score } ?: OKAY
    }
}

enum class CaptureType(val code: String, val label: String) {
    PHOTO("photo", "Photo"),
    VIDEO("video", "Video"),
    AUDIO("audio", "Audio"),
    THOUGHT("thought", "Thought"),
    MOOD("mood", "Mood"),
    LOCATION("location", "Location");

    companion object {
        fun fromCode(code: String?): CaptureType =
            entries.firstOrNull { it.code == code } ?: THOUGHT
    }
}

enum class LifeEventType(val code: String, val label: String) {
    NOTE_CREATED("note_created", "Note created"),
    TASK_CREATED("task_created", "Task created"),
    TASK_COMPLETED("task_completed", "Task completed"),
    HABIT_CREATED("habit_created", "Habit created"),
    HABIT_COMPLETED("habit_completed", "Habit completed"),
    HABIT_SKIPPED("habit_skipped", "Habit skipped"),
    EXPENSE_ADDED("expense_added", "Expense added"),
    DIARY_WRITTEN("diary_written", "Diary written"),
    CAPTURE_ADDED("capture_added", "Capture added"),
    PHOTO_TAKEN("photo_taken", "Photo taken"),
    VIDEO_RECORDED("video_recorded", "Video recorded"),
    AUDIO_RECORDED("audio_recorded", "Audio recorded"),
    LOCATION_SAVED("location_saved", "Location saved"),
    MOOD_LOGGED("mood_logged", "Mood logged"),
    DOCUMENT_ADDED("document_added", "Document added"),
    MILESTONE("milestone", "Milestone"),
    MANUAL("manual", "Manual entry");

    companion object {
        fun fromCode(code: String?): LifeEventType =
            entries.firstOrNull { it.code == code } ?: MANUAL
    }
}

/** Which tab a search result belongs to (spec: results grouped by type). */
enum class SearchGroup(val code: String, val label: String) {
    NOTES("notes", "Notes"),
    TASKS("tasks", "Tasks"),
    HABITS("habits", "Habits"),
    EXPENSES("expenses", "Expenses"),
    DIARY("diary", "Diary"),
    CAPTURES("captures", "Captures"),
    TIMELINE("timeline", "Timeline");

    companion object {
        fun fromCode(code: String?): SearchGroup =
            entries.firstOrNull { it.code == code } ?: NOTES
    }
}

/** A single day's habit progress, passed straight to the progress ring. */
data class DayProgress(
    val date: LocalDate,
    val completed: Int,
    val target: Int,
) {
    val ratio: Float get() = HabitProgress.progressRatio(completed, target)
    val percent: Int get() = HabitProgress.progressPercent(completed, target)
    val isComplete: Boolean get() = target > 0 && completed >= target
}
