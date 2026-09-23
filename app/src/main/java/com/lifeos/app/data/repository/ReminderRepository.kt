package com.lifeos.app.data.repository

import android.content.Context
import com.lifeos.app.core.reminders.ReminderScheduler
import com.lifeos.app.core.reminders.ReminderScheduleCalculator
import com.lifeos.app.data.db.dao.ReminderDao
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.RepeatRule
import com.lifeos.app.data.db.entities.ReminderEntity
import com.lifeos.app.data.db.entities.ReminderRepeatType
import com.lifeos.app.data.db.entities.TaskEntity

/**
 * Orchestrates the reminders table + AlarmManager state machine.
 *
 * The table is the source of truth for *what* to remind; [ReminderScheduler]
 * owns the *when/that* projection onto the platform. All life-cycle transitions
 * live here so the receiver/UI paths share one implementation:
 *  - a real fire advances DAILY/WEEKDAYS to its next occurrence or disables ONCE;
 *  - a snooze echoes the current notification after N minutes without moving the
 *    next real occurrence;
 *  - the global toggle re-arms every still-active reminder.
 */
class ReminderRepository(
    private val dao: ReminderDao,
    private val appContext: Context
) {

    suspend fun getById(id: String): ReminderEntity? = dao.getById(id)

    /**
     * Reconciles the row for a task/habit whose `reminderEpochMillis` mirror
     * changed (create/edit) — inserts, updates, schedules, or cleans up as needed.
     * [preferredRepeat] carries the user's UI choice for a *new* reminder; an
     * existing row keeps its configured repeat type.
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
        val existing = dao.getById(startId)
        val now = System.currentTimeMillis()

        if (reminderEpochMillis == null || !active) {
            if (existing != null) {
                dao.deleteById(startId)
                ReminderScheduler.cancel(appContext, startId)
            }
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

        armNextOccurrence(reminder)
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
        if (dao.getById(id) != null) {
            dao.deleteById(id)
            ReminderScheduler.cancel(appContext, id)
        }
    }

    /**
     * State machine on fire (called from [com.lifeos.app.core.reminders.AlarmReceiver]):
     * distinguishes a snooze echo from a real occurrence and either advances the
     * next occurrence+re-arm, or disables a one-shot reminder.
     */
    suspend fun handleFired(id: String) {
        val reminder = dao.getById(id) ?: run { ReminderScheduler.cancel(appContext, id); return }
        if (!reminder.enabled) return
        val now = System.currentTimeMillis()

        val snoozeEcho = reminder.snoozeReturnAtEpochMillis != null && reminder.snoozeReturnAtEpochMillis <= now
        if (snoozeEcho) {
            dao.clearSnooze(id, now)
            rearmIfFuture(id)
            return
        }

        if (reminder.nextTriggerAtEpochMillis > now) return // Clock moved forward; nothing to fire yet.

        when (reminder.repeatType) {
            ReminderRepeatType.ONCE -> {
                dao.disable(id, now)
                ReminderScheduler.cancel(appContext, id)
            }
            // A delayed recurring fire still re-arms for the next *future*
            // occurrence (never a bare +1 day that could land in the past).
            ReminderRepeatType.DAILY, ReminderRepeatType.WEEKDAYS -> {
                dao.getById(id)?.let { armNextOccurrence(it) }
            }
        }
    }

    /** User tapped "Snooze": echo the reminder after [minutes] without moving the next real occurrence. */
    suspend fun snooze(id: String, minutes: Int) {
        val reminder = dao.getById(id) ?: return
        if (!reminder.enabled) return
        val now = System.currentTimeMillis()
        val returnAt = now + minutes.coerceAtLeast(1) * 60_000L
        dao.setSnoozeReturn(id, returnAt, now)
        ReminderScheduler.schedule(appContext, dao.getById(id) ?: return)
    }

    /** Re-arms every enabled reminder to its next future trigger (toggle on, boot, time change). */
    suspend fun rebuildAllActive() {
        if (!ReminderScheduler.areRemindersEnabled(appContext)) return
        dao.getEnabled().forEach { armNextOccurrence(it) }
    }

    /**
     * Rebuilds the reminders table from the task/habit `reminderEpochMillis`
     * mirrors (backup restore, first-run upgrade to v4). Preserves repeat/sound/
     * vibration/snooze choices where a row already exists; past-due occurrences
     * are normalized by [armNextOccurrence] (recurring reminders fast-forward to
     * the next future occurrence, forgotten one-shots are disabled instead of
     * left as silent zombies).
     */
    suspend fun rebuildFromMirrors(tasks: List<TaskEntity>, habits: List<HabitEntity>) {
        tasks.forEach {
            syncReminderForEntity(
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
            syncReminderForEntity(
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
        // Normalize + arm everything still enabled (past one-shots are disabled).
        dao.getEnabled().forEach { armNextOccurrence(it) }
    }

    /**
     * The single arming path for every reminder lifecycle (create/edit, rebuild,
     * real fire). Schedules [reminder] for its next *future* trigger and
     * normalizes past-due rows so an enabled reminder is never stranded with a
     * trigger that can never fire:
     *  - a future snooze-return or real trigger schedules as-is;
     *  - a past-due recurring (DAILY/WEEKDAYS) trigger fast-forwards to the next
     *    future occurrence (persisted, then armed);
     *  - a past-due [ReminderRepeatType.ONCE] is disabled and its alarm cancelled
     *    (a one-off that is already past can never become due again).
     */
    private suspend fun armNextOccurrence(reminder: ReminderEntity) {
        if (!reminder.enabled) return
        val now = System.currentTimeMillis()
        val triggerAt = reminder.snoozeReturnAtEpochMillis ?: reminder.nextTriggerAtEpochMillis
        if (triggerAt > now) {
            ReminderScheduler.schedule(appContext, reminder)
            return
        }
        val next = ReminderScheduleCalculator.nextFutureOccurrenceMillis(
            reminder.repeatType, reminder.repeatDaysCsv, reminder.nextTriggerAtEpochMillis, now
        )
        if (next == null) {
            dao.disable(reminder.id, now)
            ReminderScheduler.cancel(appContext, reminder.id)
        } else {
            dao.advanceTrigger(reminder.id, next, now)
            ReminderScheduler.schedule(appContext, reminder.copy(
                nextTriggerAtEpochMillis = next,
                snoozeReturnAtEpochMillis = null
            ))
        }
    }

    private suspend fun rearmIfFuture(id: String) {
        val reminder = dao.getById(id) ?: return
        val triggerAt = reminder.snoozeReturnAtEpochMillis ?: reminder.nextTriggerAtEpochMillis
        if (reminder.enabled && triggerAt > System.currentTimeMillis()) {
            ReminderScheduler.schedule(appContext, reminder)
        }
    }

    companion object {
        const val TYPE_TASK = "task"
        const val TYPE_HABIT = "habit"
    }
}