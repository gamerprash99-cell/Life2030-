package com.lifeos.app.core.reminders

import com.lifeos.app.data.db.entities.ReminderEntity
import com.lifeos.app.data.db.entities.ReminderRepeatType
import java.time.Instant
import java.time.ZoneId

/**
 * Pure wall-clock math for recurring reminders (no Android or storage
 * dependencies, so it is directly unit-testable). Every computation runs in the
 * device's current default zone, which keeps reminders pinned to the *local*
 * time the user chose even across DST transitions.
 *
 * Used by [ReminderScheduler]'s state machine and by `BootReceiver` when it
 * re-arms reminders after a reboot/time change.
 */
object ReminderScheduleCalculator {

    /**
     * Returns the epoch millis of the next real occurrence after
     * [currentTriggerMillis], or `null` when the reminder should stop
     * ([ReminderRepeatType.ONCE], or a WEEKDAYS reminder with no valid days).
     */
    fun nextOccurrenceMillis(
        repeatType: ReminderRepeatType,
        repeatDaysCsv: String?,
        currentTriggerMillis: Long
    ): Long? {
        val current = Instant.ofEpochMilli(currentTriggerMillis).atZone(ZoneId.systemDefault())
        return when (repeatType) {
            ReminderRepeatType.ONCE -> null
            ReminderRepeatType.DAILY -> current.plusDays(1).toInstant().toEpochMilli()
            ReminderRepeatType.WEEKDAYS -> {
                val days = parseWeekdaySet(repeatDaysCsv)
                if (days.isEmpty()) {
                    null
                } else {
                    var candidate = current.plusDays(1)
                    while (candidate.dayOfWeek.value !in days) {
                        candidate = candidate.plusDays(1)
                    }
                    candidate.toInstant().toEpochMilli()
                }
            }
        }
    }

    /** weekday (1=Mon..7=Sun) set; WEEKDAYS defaults to Mon–Fri when the CSV is absent, null-ish or fully invalid. */
    fun parseWeekdaySet(repeatDaysCsv: String?): Set<Int> {
        val defaults = ReminderEntity.WEEKDAYS_DEFAULT_CSV.split(',').mapNotNull { it.toIntOrNull() }.toSet()
        val parsed = repeatDaysCsv
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it in 1..7 }
            ?.toSet()
        return if (parsed.isNullOrEmpty()) defaults else parsed
    }
}