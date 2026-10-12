package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * How often a reminder re-arms itself after it fires:
 *  - [ONCE]: fire a single time, then disable itself.
 *  - [DAILY]: re-arm ~24h later at the same wall-clock time every day.
 *  - [WEEKDAYS]: re-arm on the next Mon–Fri at the same wall-clock time.
 */
@Serializable
enum class ReminderRepeatType { ONCE, DAILY, WEEKDAYS }

/**
 * A single task or habit reminder — Section 9/11.
 *
 * This is the *source of truth* for what LifeOS wants to fire, kept on disk so
 * a reboot/update never forgets a reminder the user configured. [ReminderScheduler]
 * projects each enabled row onto an AlarmManager exact alarm; [AppDatabase] v4
 * adds this table alongside the retained `reminderEpochMillis` mirrors on
 * tasks/habits (the mirrors keep backup/restore and the existing UI simple).
 */
@Serializable
@Entity(tableName = "reminders", indices = [
    Index(value = ["entityType"]),
    Index(value = ["entityId"]),
    Index(value = ["nextTriggerAtEpochMillis"]),
    Index(value = ["enabled"])
])
data class ReminderEntity(
    /** Stable "task:<id>" / "habit:<id>" — also the AlarmManager request identity. */
    @PrimaryKey val id: String,
    /** "task" or "habit". */
    val entityType: String,
    val entityId: String,
    val title: String,
    /** Next *real* occurrence. The armed alarm may instead fire at snoozeReturnAtEpochMillis. */
    val nextTriggerAtEpochMillis: Long,
    val repeatType: ReminderRepeatType = ReminderRepeatType.ONCE,
    /** ISO-8601 day-of-week CSV ("1,2,3,4,5" = Mon–Fri) used only by [ReminderRepeatType.WEEKDAYS]. */
    val repeatDaysCsv: String? = null,
    val enabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    /** While set, the armed alarm is a snooze echo of [nextTriggerAtEpochMillis]. */
    val snoozeReturnAtEpochMillis: Long? = null,
    val updatedAt: Long
) {
    companion object {
        const val WEEKDAYS_DEFAULT_CSV = "1,2,3,4,5"
        fun idFor(entityType: String, entityId: String) = "$entityType:$entityId"
    }
}