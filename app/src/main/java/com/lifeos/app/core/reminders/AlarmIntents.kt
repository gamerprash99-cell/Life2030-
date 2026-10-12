package com.lifeos.app.core.reminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Extras schema for carrying a coalesced [AlarmEvent] inside an Intent / its
 * PendingIntent. Reading the event back is a pure parse of extras, so the
 * alarm UI can present instantly with no database access.
 */
object AlarmEventCodec {

    private const val EXTRA_TRIGGER_AT = "lifeos_event_trigger_at"
    private const val EXTRA_ITEM_IDS = "lifeos_event_item_ids"
    private const val EXTRA_ITEM_TITLES = "lifeos_event_item_titles"

    fun into(intent: Intent, event: AlarmEvent) {
        intent.putExtra(EXTRA_TRIGGER_AT, event.triggerAtEpochMillis)
        intent.putStringArrayListExtra(
            EXTRA_ITEM_IDS,
            ArrayList(event.items.map { it.reminderId })
        )
        intent.putStringArrayListExtra(
            EXTRA_ITEM_TITLES,
            ArrayList(event.items.map { it.title })
        )
    }

    fun from(intent: Intent?): AlarmEvent? {
        val triggerAt = intent?.getLongExtra(EXTRA_TRIGGER_AT, 0L) ?: 0L
        if (triggerAt <= 0L) return null
        val ids = intent?.getStringArrayListExtra(EXTRA_ITEM_IDS).orEmpty()
        if (ids.isEmpty()) return null
        val titles = intent?.getStringArrayListExtra(EXTRA_ITEM_TITLES).orEmpty()
        val items = ids.mapIndexed { index, id ->
            AlarmEventItem(
                reminderId = id,
                entityType = if (id.startsWith(AlarmEvent.TYPE_HABIT + ":")) {
                    AlarmEvent.TYPE_HABIT
                } else {
                    AlarmEvent.TYPE_TASK
                },
                title = titles.getOrNull(index) ?: "LifeOS reminder"
            )
        }
        return AlarmEvent(triggerAtEpochMillis = triggerAt, items = items)
    }
}

/**
 * PendingIntents and Intent actions shared by the scheduler, receiver, playback
 * service and full-screen activity. All identities derive from the event's
 * stable [AlarmEvent.id], so arming/refreshing/cancelling always target the
 * same system alarm.
 */
object AlarmIntents {

    const val ACTION_ALARM_EVENT = "com.lifeos.app.action.ALARM_EVENT"
    const val ACTION_SHOW_ALARM = "com.lifeos.app.action.SHOW_ALARM"
    const val ACTION_FULLSCREEN_ALARM = "com.lifeos.app.action.FULLSCREEN_ALARM"
    const val ACTION_STOP_ALARM = "com.lifeos.app.action.STOP_ALARM"
    const val ACTION_SNOOZE = "com.lifeos.app.action.SNOOZE"

    const val EXTRA_SNOOZE_MINUTES = "lifeos_snooze_minutes"

    private fun eventRequestCode(triggerAtEpochMillis: Long): Int =
        triggerAtEpochMillis.hashCode()

    fun broadcastIntent(context: Context, event: AlarmEvent): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_EVENT
            data = Uri.parse("lifeos://alarm-event/${event.triggerAtEpochMillis}")
            AlarmEventCodec.into(this, event)
        }

    /** The broadcast alarm that actually fires. Also used to cancel. */
    fun eventPendingIntent(context: Context, event: AlarmEvent): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            eventRequestCode(event.triggerAtEpochMillis),
            broadcastIntent(context, event),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Shown pixel in the system status bar's alarm-clock indicator. */
    fun alarmClockShowPendingIntent(context: Context, event: AlarmEvent): PendingIntent =
        PendingIntent.getActivity(
            context,
            eventRequestCode(event.triggerAtEpochMillis),
            Intent(context, AlarmFullScreenActivity::class.java).apply {
                action = ACTION_SHOW_ALARM
                data = Uri.parse("lifeos://alarm/${event.triggerAtEpochMillis}")
                AlarmEventCodec.into(this, event)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Full-screen intent used to elevate the alarm screen above the lock screen. */
    fun fullScreenPendingIntent(context: Context, event: AlarmEvent): PendingIntent =
        PendingIntent.getActivity(
            context,
            eventRequestCode(event.triggerAtEpochMillis),
            fullScreenIntent(context, event),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun fullScreenIntent(context: Context, event: AlarmEvent): Intent =
        Intent(context, AlarmFullScreenActivity::class.java).apply {
            action = ACTION_FULLSCREEN_ALARM
            data = Uri.parse("lifeos://alarm/${event.triggerAtEpochMillis}")
            AlarmEventCodec.into(this, event)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

    fun stopServiceIntent(context: Context): Intent =
        Intent(context, AlarmPlaybackService::class.java).setAction(ACTION_STOP_ALARM)

    fun snoozeServiceIntent(context: Context, event: AlarmEvent, minutes: Int): Intent =
        Intent(context, AlarmPlaybackService::class.java).apply {
            action = ACTION_SNOOZE
            AlarmEventCodec.into(this, event)
            putExtra(EXTRA_SNOOZE_MINUTES, minutes)
        }

    }