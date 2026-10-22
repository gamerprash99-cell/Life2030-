package com.lifeos.app.core.reminders

import com.lifeos.app.data.db.entities.ReminderEntity

/**
 * Pure, JVM-testable projection of the reminders table into the coalesced set
 * of [AlarmEvent]s to arm.
 *
 * Rules:
 *  - only enabled reminders with an effective trigger strictly in the future;
 *  - a snooze's return time ([ReminderEntity.snoozeReturnAtEpochMillis])
 *    overrides the next real occurrence;
 *  - every reminder that shares a trigger time collapses into ONE event;
 *  - events sort by trigger time; items inside an event sort by reminder id.
 *
 * Deliberately no cap exists on event size or event count: the number of
 * separate AlarmManager alarms equals the number of *distinct trigger times*,
 * never the number of reminders, so a flood of reminders can never hit the
 * Doze "once per 9 minutes per app" per-alarm limit.
 */
object AlarmEventProjector {

    fun buildEvents(reminders: List<ReminderEntity>, nowMillis: Long): List<AlarmEvent> =
        reminders.asSequence()
            .filter { it.enabled }
            .mapNotNull { reminder ->
                val triggerAt = reminder.snoozeReturnAtEpochMillis
                    ?: reminder.nextTriggerAtEpochMillis
                if (triggerAt <= nowMillis) {
                    null
                } else {
                    reminder to triggerAt
                }
            }
            .groupBy({ it.second }, { it.first })
            .map { (triggerAt, group) ->
                AlarmEvent(
                    triggerAtEpochMillis = triggerAt,
                    items = group
                        .sortedBy { it.id }
                        .map { reminder ->
                            AlarmEventItem(
                                reminderId = reminder.id,
                                entityType = reminder.entityType,
                                title = reminder.title,
                                soundEnabled = reminder.soundEnabled,
                                vibrationEnabled = reminder.vibrationEnabled
                            )
                        }
                )
            }
            .sortedBy { it.triggerAtEpochMillis }
}