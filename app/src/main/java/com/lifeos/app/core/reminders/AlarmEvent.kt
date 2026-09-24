package com.lifeos.app.core.reminders

/**
 * A single task or habit reminder due at an alarm's trigger time.
 */
data class AlarmEventItem(
    val reminderId: String,
    val entityType: String,
    val title: String,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

/**
 * One "intelligent alarm": every reminder that fires at the same trigger time
 * coalesced into a single event. The event travels through AlarmManager inside
 * the PendingIntent extras (see [AlarmEventCodec]) so the alarm can present
 * instantly without a database read.
 *
 * [id] is stable for [triggerAtEpochMillis] and is used as the PendingIntent /
 * notification identity, so re-projecting the same trigger replaces the armed
 * alarm (with refreshed snapshot extras) and never duplicates it.
 */
data class AlarmEvent(
    val triggerAtEpochMillis: Long,
    val items: List<AlarmEventItem>
) {
    val id: String get() = "$ID_PREFIX$triggerAtEpochMillis"
    val hasTasks: Boolean get() = items.any { it.entityType == TYPE_TASK }
    val hasHabits: Boolean get() = items.any { it.entityType == TYPE_HABIT }
    val anySoundEnabled: Boolean get() = items.any { it.soundEnabled }
    val anyVibrationEnabled: Boolean get() = items.any { it.vibrationEnabled }

    fun itemsOf(entityType: String): List<AlarmEventItem> = items.filter { it.entityType == entityType }

    companion object {
        const val ID_PREFIX = "event:"
        const val TYPE_TASK = "task"
        const val TYPE_HABIT = "habit"
    }
}