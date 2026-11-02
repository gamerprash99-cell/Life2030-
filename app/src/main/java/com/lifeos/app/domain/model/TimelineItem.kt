package com.lifeos.app.domain.model

/**
 * One row of the Life timeline (Section 3/60).
 *
 * The timeline is deliberately NOT its own database table. It is a real-time
 * aggregation over Tasks, Habits, Expenses, Diary entries and Captures — this
 * is what the spec means by "connect through Date, Time, Tags, Timeline,
 * Relationships" rather than duplicating data into a redundant table.
 * See domain/usecase/BuildTimelineUseCase.kt (one day) and
 * domain/usecase/BuildLifeTimelineUseCase.kt (the Life tab's history) for how
 * it is assembled, and ui/life/LifeTimelineScreen.kt for how it is drawn.
 *
 * The row carries everything the journal needs to render itself without
 * another query: a title, optional body text, the real timestamp, the module
 * label, and the media/amount/status that belongs to the record it points at.
 *
 * @param imagePath set for a photo so the row can draw the picture itself.
 * @param mediaPath set for a video or voice capture so the row can hand the
 *   file to the viewer/player without a second lookup.
 * @param durationMillis measured length of a voice/video capture, or null.
 * @param amount the real amount for an expense row, or null for every other
 *   type. Formatting happens in the UI so one currency rule is used app-wide.
 * @param statusLabel the completion state for task/habit rows ("Completed",
 *   "Stayed on track", "3/5 today"), or null where a state does not apply.
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
    val moodOrCategory: String? = null,
    val imagePath: String? = null,
    val mediaPath: String? = null,
    val durationMillis: Long? = null,
    val amount: Double? = null,
    val statusLabel: String? = null
)

/**
 * What produced a timeline row. [PHOTO]/[VIDEO]/[AUDIO] are the typed capture
 * kinds the media viewer and the audio player route on; [SNAP] stays the kind
 * for the capture types that are text at heart (thought, mood, place).
 */
enum class TimelineItemType {
    TASK_COMPLETED,
    HABIT_COMPLETED,
    EXPENSE,
    DIARY,
    PHOTO,
    VIDEO,
    AUDIO,
    SNAP
}
