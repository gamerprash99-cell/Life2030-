package com.lifeos.app.data.repository

import android.content.Context
import com.lifeos.app.core.reminders.AlarmEventProjector
import com.lifeos.app.core.reminders.ReminderScheduler
import com.lifeos.app.core.reminders.ReminderScheduleCalculator
import com.lifeos.app.data.db.dao.ReminderDao
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.RepeatRule
import com.lifeos.app.data.db.entities.ReminderEntity
import com.lifeos.app.data.db.entities.ReminderRepeatType
import com.lifeos.app.data.db.entities.TaskEntity

/**
 * Orchestrates the reminders table + AlarmManager projection.
 *
 * The table is the source of truth for *what* to remind; [AlarmEventProjector]
 * collapses it into one merged event per distinct trigger time and
 * [ReminderScheduler] projects those events (never individual reminders) onto
 * the platform. The whole lifecycle funnels through one entry point —
 * [reprojectAll] — so every mutation (create/edit/fire/snooze/delete/toggle/
 * boot/time-change) re-derives the armed set atomically and irrelevant alarms
 * are cancelled instead of leaking.
 *
 *  - a real fire advances DAILY/WEEKDAYS to its next occurrence or disables ONCE;
 *  - a snooze echoes the current presentation after N minutes without moving the
 *    next real occurrence;
 *  - past-due rows are normalized on every projection pass so a recurring
 *    reminder never sits armed in the past and a dead one-shot is disabled.
 */
class ReminderRepository(
    private val dao: ReminderDao,
    private val appContext: Context
) {

    suspend fun getById(id: String): ReminderEntity? = dao.getById(id)

    /**
     * Reconciles the row for a task/habit whose `reminderEpochMillis` mirror
     * changed (create/edit) — inserts, updates, or cleans up the row, then
     * re-projects. [preferredRepeat] carries the user's UI choice for a *new*
     * reminder; an existing row keeps its configured repeat type.
     */
    suspend fun syncReminderForEntity(
        startId: String,
        entityType: String,
        entityId: String,
        title: String,
        reminderEpochMillis: Long?,
        defaultRepeat: ReminderRepeatType,
        preferredRepeat: ReminderRepeatType?,
        active: Boolean
    ) {
        syncReminderRow(
            startId, entityType, entityId, title, reminderEpochMillis,
            defaultRepeat, preferredRepeat, active
        )
        reprojectAll()
    }

    suspend fun syncReminderForTask(task: TaskEntity, preferredRepeat: ReminderRepeatType? = null) {
        syncReminderForEntity(
            startId = ReminderEntity.idFor(TYPE_TASK, task.id),
            entityType = TYPE_TASK,
            entityId = task.id,
            title = task.title,
            reminderEpochMillis = task.reminderEpochMillis,
            defaultRepeat = if (task.repeatRule != RepeatRule.NONE) ReminderRepeatType.DAILY else ReminderRepeatType.ONCE,
            preferredRepeat = preferredRepeat,
            active = !task.isCompleted && !task.isDeleted
        )
    }

    suspend fun syncReminderForHabit(habit: HabitEntity, preferredRepeat: ReminderRepeatType? = null) {
        syncReminderForEntity(
            startId = ReminderEntity.idFor(TYPE_HABIT, habit.id),
            entityType = TYPE_HABIT,
            entityId = habit.id,
            title = habit.name,
            reminderEpochMillis = habit.reminderEpochMillis,
            defaultRepeat = ReminderRepeatType.DAILY,
            preferredRepeat = preferredRepeat,
            active = !habit.isArchived
        )
    }

    suspend fun deleteForTask(taskId: String) = deleteForEntity(TYPE_TASK, taskId)

    suspend fun deleteForHabit(habitId: String) = deleteForEntity(TYPE_HABIT, habitId)

    private suspend fun deleteForEntity(entityType: String, entityId: String) {
        val id = ReminderEntity.idFor(entityType, entityId)
        if (dao.getById(id) != null) dao.deleteById(id)
        reprojectAll()
    }

    /**
     * Called from [com.lifeos.app.core.reminders.AlarmReceiver] for every
     * reminder wrapped in the fired event. Runs the state machine per reminder
     * (snooze echo vs real occurrence) and then re-projects once.
     */
    suspend fun handleEventFired(reminderIds: Set<String>) {
        val now = System.currentTimeMillis()
        reminderIds.forEach { id ->
            val reminder = dao.getById(id) ?: return@forEach
            if (!reminder.enabled) return@forEach

            val snoozeEcho = reminder.snoozeReturnAtEpochMillis != null &&
                reminder.snoozeReturnAtEpochMillis <= now
            if (snoozeEcho) {
                dao.clearSnooze(id, now)
                return@forEach
            }
            if (reminder.nextTriggerAtEpochMillis > now) return@forEach // Clock moved forward; not due yet.

            when (reminder.repeatType) {
                ReminderRepeatType.ONCE -> dao.disable(id, now)
                ReminderRepeatType.DAILY, ReminderRepeatType.WEEKDAYS -> {
                    val next = ReminderScheduleCalculator.nextFutureOccurrenceMillis(
                        reminder.repeatType, reminder.repeatDaysCsv, reminder.nextTriggerAtEpochMillis, now
                    )
                    if (next == null) {
                        dao.disable(id, now)
                    } else {
                        dao.advanceTrigger(id, next, now)
                    }
                }
            }
        }
        reprojectAll()
    }

    /** User tapped "Snooze": echo the reminders after [minutes] without moving their next real occurrence. */
    suspend fun snoozeMany(reminderIds: List<String>, minutes: Int) {
        val now = System.currentTimeMillis()
        val returnAt = now + minutes.coerceAtLeast(1) * 60_000L
        var changed = false
        reminderIds.forEach { id ->
            val reminder = dao.getById(id) ?: return@forEach
            if (!reminder.enabled) return@forEach
            dao.setSnoozeReturn(id, returnAt, now)
            changed = true
        }
        if (changed) reprojectAll()
    }

    /** Re-arms every still-active reminder (toggle on, boot, app start). */
    suspend fun rebuildAllActive() = reprojectAll()

    /**
     * Rebuilds the reminders table from the task/habit `reminderEpochMillis`
     * mirrors (backup restore, first-run upgrade to v4, and
     * TIME_SET/TIMEZONE_CHANGED recovery). Preserves repeat/sound/vibration/
     * snooze choices where a row already exists; past-due occurrences are
     * normalized by [reprojectAll].
     */
    suspend fun rebuildFromMirrors(tasks: List<TaskEntity>, habits: List<HabitEntity>) {
        tasks.forEach {
            syncReminderRow(
                startId = ReminderEntity.idFor(TYPE_TASK, it.id),
                entityType = TYPE_TASK,
                entityId = it.id,
                title = it.title,
                reminderEpochMillis = it.reminderEpochMillis,
                defaultRepeat = if (it.repeatRule != RepeatRule.NONE) ReminderRepeatType.DAILY else ReminderRepeatType.ONCE,
                preferredRepeat = null,
                active = !it.isCompleted && !it.isDeleted
            )
        }
        habits.forEach {
            syncReminderRow(
                startId = ReminderEntity.idFor(TYPE_HABIT, it.id),
                entityType = TYPE_HABIT,
                entityId = it.id,
                title = it.name,
                reminderEpochMillis = it.reminderEpochMillis,
                defaultRepeat = ReminderRepeatType.DAILY,
                preferredRepeat = null,
                active = !it.isArchived
            )
        }
        reprojectAll()
    }

    /**
     * The single projection entry point: normalize past-due rows, build the
     * coalesced event set, and reconcile the armed alarms with it.
     */
    suspend fun reprojectAll() {
        if (!ReminderScheduler.areRemindersEnabled(appContext)) {
            ReminderScheduler.cancelAll(appContext)
            return
        }
        normalizePastDue()
        val events = AlarmEventProjector.buildEvents(dao.getEnabled(), System.currentTimeMillis())
        ReminderScheduler.reproject(appContext, events)
    }

    private suspend fun syncReminderRow(
        startId: String,
        entityType: String,
        entityId: String,
        title: String,
        reminderEpochMillis: Long?,
        defaultRepeat: ReminderRepeatType,
        preferredRepeat: ReminderRepeatType?,
        active: Boolean
    ) {
        val existing = dao.getById(startId)
        val now = System.currentTimeMillis()

        if (reminderEpochMillis == null || !active) {
            if (existing != null) dao.deleteById(startId)
            return
        }

        val repeat = existing?.repeatType ?: preferredRepeat ?: defaultRepeat
        val reminder = existing?.copy(
            entityType = entityType,
            entityId = entityId,
            title = title,
            nextTriggerAtEpochMillis = reminderEpochMillis,
            snoozeReturnAtEpochMillis = null,
            updatedAt = now
        ) ?: ReminderEntity(
            id = startId,
            entityType = entityType,
            entityId = entityId,
            title = title,
            nextTriggerAtEpochMillis = reminderEpochMillis,
            repeatType = repeat,
            updatedAt = now
        )
        dao.upsert(reminder)
    }

    /**
     * Resolves every enabled reminder whose effective trigger is already past
     * (missed alarm, clock rolled back, stale mirror, expired snooze echo) so
     * the projection only ever sees future triggers:
     *  - an elapsed snooze echo collapses back to the next real occurrence;
     *  - a past-due recurring (DAILY/WEEKDAYS) trigger fast-forwards to the
     *    next future occurrence (persisted);
     *  - a past-due [ReminderRepeatType.ONCE] is disabled (a one-off that is
     *    already past can never become due again).
     */
    private suspend fun normalizePastDue() {
        val now = System.currentTimeMillis()
        dao.getEnabled().forEach { reminder ->
            val effectiveTrigger = reminder.snoozeReturnAtEpochMillis
                ?: reminder.nextTriggerAtEpochMillis
            if (effectiveTrigger > now) return@forEach

            if (reminder.snoozeReturnAtEpochMillis != null) {
                dao.clearSnooze(reminder.id, now)
            }
            val fresh = dao.getById(reminder.id) ?: return@forEach
            if (!fresh.enabled) return@forEach
            val freshTrigger = fresh.snoozeReturnAtEpochMillis ?: fresh.nextTriggerAtEpochMillis
            if (freshTrigger > now) return@forEach

            when (fresh.repeatType) {
                ReminderRepeatType.ONCE -> dao.disable(reminder.id, now)
                ReminderRepeatType.DAILY, ReminderRepeatType.WEEKDAYS -> {
                    val next = ReminderScheduleCalculator.nextFutureOccurrenceMillis(
                        fresh.repeatType, fresh.repeatDaysCsv, fresh.nextTriggerAtEpochMillis, now
                    )
                    if (next == null) {
                        dao.disable(reminder.id, now)
                    } else {
                        dao.advanceTrigger(reminder.id, next, now)
                    }
                }
            }
        }
    }

    companion object {
        const val TYPE_TASK = "task"
        const val TYPE_HABIT = "habit"
    }
}