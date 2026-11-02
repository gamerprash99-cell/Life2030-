package com.lifeos.app.domain.usecase

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.completionTarget
import com.lifeos.app.core.util.isCompletionDone
import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitTrackingMode
import com.lifeos.app.data.db.entities.HabitType
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType

/**
 * Entity → timeline-row mapping, in one place.
 *
 * Both timeline builders (the single-day [BuildTimelineUseCase] that feeds
 * Home, and the ranged [BuildLifeTimelineUseCase] that feeds the Life tab) use
 * these functions, so a row can never read differently depending on which
 * screen asked for it. They are pure functions over already-loaded rows: no
 * repository, no clock, no Android type — which is also what makes the
 * timeline sortable and testable on a plain JVM.
 */

/** A completed task. [epochDay] is the day the row is being listed under. */
internal fun taskTimelineItem(task: TaskEntity, epochDay: Long): TimelineItem = TimelineItem(
    id = "task-${task.id}",
    type = TimelineItemType.TASK_COMPLETED,
    title = task.title,
    subtitle = task.category,
    dateEpochDay = epochDay,
    // The real completion instant is the honest timestamp; rows completed
    // before the timestamp was tracked fall back to the last write.
    timeMinutes = task.completedAtEpochMillis
        ?.let { DateTimeUtils.minutesOfDay(it) }
        ?: DateTimeUtils.minutesOfDay(task.updatedAt),
    icon = "✅",
    sourceId = task.id,
    moodOrCategory = task.category,
    statusLabel = "Completed"
)

/** A habit day that actually met its goal (slipped days are not entries). */
internal fun habitTimelineItem(
    habit: HabitEntity,
    completion: HabitCompletionEntity,
    epochDay: Long
): TimelineItem {
    val target = habit.completionTarget(completion)
    val status = when {
        habit.trackingMode == HabitTrackingMode.COUNT ->
            "${completion.progressCount.coerceAtLeast(0)}/$target done"
        else -> "Completed"
    }
    return TimelineItem(
        id = "habit-${habit.id}-$epochDay",
        type = TimelineItemType.HABIT_COMPLETED,
        title = habit.name,
        subtitle = when {
            habit.habitType == HabitType.QUIT -> "Stayed on track"
            habit.trackingMode == HabitTrackingMode.CHECKLIST -> "Checklist complete"
            else -> "Habit completed"
        },
        dateEpochDay = epochDay,
        timeMinutes = DateTimeUtils.minutesOfDay(completion.completedAtEpochMillis),
        icon = habit.icon,
        sourceId = habit.id,
        statusLabel = status
    )
}

/** True when a completion row belongs on the timeline at all. */
internal fun HabitEntity.showsOnTimeline(completion: HabitCompletionEntity): Boolean =
    showOnTimeline && isCompletionDone(completion)

/** An expense. The amount stays a number so the UI formats it. */
internal fun expenseTimelineItem(expense: ExpenseEntity): TimelineItem = TimelineItem(
    id = "expense-${expense.id}",
    type = TimelineItemType.EXPENSE,
    title = expense.merchant?.takeIf { it.isNotBlank() } ?: expense.category,
    subtitle = expense.category.takeIf { expense.merchant?.isNotBlank() == true },
    dateEpochDay = expense.dateEpochDay,
    timeMinutes = expense.timeMinutes,
    icon = com.lifeos.app.domain.model.ExpenseCategories.emojiFor(expense.category),
    sourceId = expense.id,
    moodOrCategory = expense.category,
    amount = expense.amount
)

/** A diary entry; its own text is the readable preview. */
internal fun diaryTimelineItem(entry: DiaryEntity): TimelineItem = TimelineItem(
    id = "diary-${entry.id}",
    type = TimelineItemType.DIARY,
    title = entry.title?.takeIf { it.isNotBlank() } ?: entry.content.lineSequence().firstOrNull()?.take(48)?.trim().orEmpty().ifBlank { "Diary entry" },
    subtitle = entry.content.previewText(),
    dateEpochDay = entry.dateEpochDay,
    timeMinutes = entry.timeMinutes,
    icon = "📔",
    sourceId = entry.id,
    moodOrCategory = entry.mood
)

/** A saved moment: photo, video, voice, or one of the text captures. */
internal fun captureTimelineItem(capture: CaptureEntity): TimelineItem = TimelineItem(
    id = "capture-${capture.id}",
    type = capture.timelineType(),
    title = capture.text?.takeIf { it.isNotBlank() } ?: capture.defaultCaptureTitle(),
    subtitle = capture.subtitleText(),
    dateEpochDay = DateTimeUtils.epochDayOf(capture.createdAt),
    timeMinutes = DateTimeUtils.minutesOfDay(capture.createdAt),
    icon = captureIcon(capture.type),
    sourceId = capture.id,
    moodOrCategory = capture.moodKey,
    imagePath = capture.filePath?.takeIf { capture.type == CaptureType.PHOTO },
    mediaPath = capture.filePath?.takeIf {
        capture.type == CaptureType.AUDIO || capture.type == CaptureType.VIDEO
    },
    durationMillis = capture.durationMillis
)

/** The typed kind the router/viewer uses for this capture. */
internal fun CaptureEntity.timelineType(): TimelineItemType = when (type) {
    CaptureType.PHOTO -> TimelineItemType.PHOTO
    CaptureType.VIDEO -> TimelineItemType.VIDEO
    CaptureType.AUDIO -> TimelineItemType.AUDIO
    else -> TimelineItemType.SNAP
}

private fun CaptureEntity.subtitleText(): String {
    val kind = when (type) {
        CaptureType.PHOTO -> "Photo"
        CaptureType.VIDEO -> "Video"
        CaptureType.AUDIO -> "Voice note"
        CaptureType.MOOD -> "Mood"
        CaptureType.LOCATION -> "Place"
        else -> "Moment"
    }
    val extra = placeLabel?.takeIf { it.isNotBlank() }
        ?: moodKey?.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
    val length = durationMillis?.takeIf { it > 0 }
        ?.let { com.lifeos.app.core.media.formatAudioDuration(it) }
    return listOfNotNull(kind, length, extra).joinToString(" · ")
}

private fun CaptureEntity.defaultCaptureTitle(): String = when (type) {
    CaptureType.AUDIO -> "Voice note"
    CaptureType.PHOTO -> "A photo"
    CaptureType.VIDEO -> "A video"
    else -> "A moment captured"
}

private fun captureIcon(type: String): String = when (type) {
    CaptureType.PHOTO -> "📸"
    CaptureType.VIDEO -> "🎬"
    CaptureType.AUDIO -> "🎙️"
    CaptureType.MOOD -> "😊"
    CaptureType.LOCATION -> "📍"
    else -> "✨"
}

/** First non-blank line, trimmed to a readable preview length. */
private fun String.previewText(maxLength: Int = 140): String {
    val flat = lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
    return if (flat.length <= maxLength) flat else flat.take(maxLength - 1).trimEnd() + "…"
}
