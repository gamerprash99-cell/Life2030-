package com.lifeos.app.core.reminders

import com.lifeos.app.data.db.entities.ReminderEntity
import com.lifeos.app.data.db.entities.ReminderRepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the coalescing projection (no Android dependencies).
 *
 * These anchor a fixed `nowMillis` so grouping, ordering and the future-only
 * filter are deterministic regardless of the host clock/zone.
 */
class AlarmEventProjectorTest {

    private val now = 1_800_000_000_000L

    private fun reminder(
        id: String,
        entityType: String = AlarmEvent.TYPE_TASK,
        title: String = "Reminder $id",
        nextTrigger: Long = now + 60_000L,
        snoozeReturn: Long? = null,
        enabled: Boolean = true,
        sound: Boolean = true,
        vibration: Boolean = true
    ) = ReminderEntity(
        id = id,
        entityType = entityType,
        entityId = id.substringAfter(':'),
        title = title,
        nextTriggerAtEpochMillis = nextTrigger,
        repeatType = ReminderRepeatType.DAILY,
        enabled = enabled,
        soundEnabled = sound,
        vibrationEnabled = vibration,
        snoozeReturnAtEpochMillis = snoozeReturn,
        updatedAt = now
    )

    @Test
    fun emptyList_producesNoEvents() {
        assertEquals(emptyList<AlarmEvent>(), AlarmEventProjector.buildEvents(emptyList(), now))
    }

    @Test
    fun disabledReminders_areExcluded() {
        val disabled = reminder("task:a", nextTrigger = now + 60_000L, enabled = false)
        val events = AlarmEventProjector.buildEvents(listOf(disabled), now)
        assertTrue(events.isEmpty())
    }

    @Test
    fun pastDueTriggers_areExcluded() {
        val past = reminder("task:a", nextTrigger = now - 1_000L)
        val future = reminder("task:b", nextTrigger = now + 5_000L)
        val events = AlarmEventProjector.buildEvents(listOf(past, future), now)
        assertEquals(1, events.size)
        assertEquals(listOf("task:b"), events.first().items.map { it.reminderId })
    }

    @Test
    fun snoozeReturnOverridesTheRealTrigger() {
        val r = reminder(
            id = "task:a",
            nextTrigger = now + 10 * 60_000L,
            snoozeReturn = now + 5 * 60_000L
        )
        val events = AlarmEventProjector.buildEvents(listOf(r), now)
        assertEquals(1, events.size)
        assertEquals(now + 5 * 60_000L, events.first().triggerAtEpochMillis)
    }

    @Test
    fun remindersSharingATrigger_collapseIntoOneEvent() {
        val task = reminder("task:a", nextTrigger = now + 60_000L)
        val habit = reminder("habit:b", entityType = AlarmEvent.TYPE_HABIT, nextTrigger = now + 60_000L)
        val events = AlarmEventProjector.buildEvents(listOf(task, habit), now)

        assertEquals(1, events.size)
        val event = events.first()
        assertEquals(now + 60_000L, event.triggerAtEpochMillis)
        assertEquals("event:${now + 60_000L}", event.id)
        assertEquals(2, event.items.size)
        assertTrue(event.hasTasks)
        assertTrue(event.hasHabits)
    }

    @Test
    fun distinctTriggers_keepSeparateEvents_sortedByTime() {
        val early = reminder("task:early", nextTrigger = now + 60_000L)
        val late = reminder("task:late", nextTrigger = now + 5 * 60_000L)
        val events = AlarmEventProjector.buildEvents(listOf(late, early), now)

        assertEquals(2, events.size)
        assertEquals(now + 60_000L, events[0].triggerAtEpochMillis)
        assertEquals(now + 5 * 60_000L, events[1].triggerAtEpochMillis)
        assertEquals("task:early", events[0].items.single().reminderId)
    }

    @Test
    fun itemsWithinAnEvent_areSortedByReminderId() {
        val a = reminder("task:a", nextTrigger = now + 60_000L)
        val b = reminder("task:b", nextTrigger = now + 60_000L)
        val c = reminder("habit:c", entityType = AlarmEvent.TYPE_HABIT, nextTrigger = now + 60_000L)
        val events = AlarmEventProjector.buildEvents(listOf(c, b, a), now)

        val ids = events.single().items.map { it.reminderId }
        assertEquals(listOf("habit:c", "task:a", "task:b"), ids)
    }

    @Test
    fun eventCarriesTypeAndSoundFlagsPerItem() {
        val silent = reminder("task:a", nextTrigger = now + 60_000L, sound = false, vibration = true)
        val events = AlarmEventProjector.buildEvents(listOf(silent), now)

        val item = events.single().items.single()
        assertEquals(AlarmEvent.TYPE_TASK, item.entityType)
        assertFalse(item.soundEnabled)
        assertTrue(item.vibrationEnabled)
        assertFalse(events.single().anySoundEnabled)
        assertTrue(events.single().anyVibrationEnabled)
    }

    @Test
    fun returnsNoEvents_whenEveryReminderIsPastDue() {
        val past = reminder("task:a", nextTrigger = now - 1L)
        assertTrue(AlarmEventProjector.buildEvents(listOf(past), now).isEmpty())
    }
}