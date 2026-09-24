package com.lifeos.app.core.reminders

import android.content.Context
import com.lifeos.app.R

/** Shared copy for the coalesced event, avoiding a `yet another alarm app` wall of text. */
object AlarmStrings {

    fun titleOf(context: Context, event: AlarmEvent): String = when {
        event.hasTasks && event.hasHabits -> context.getString(R.string.alarm_generic_reminder)
        event.hasTasks -> context.getString(R.string.alarm_task_reminder)
        else -> context.getString(R.string.alarm_habit_reminder)
    }

    fun bodyOf(context: Context, event: AlarmEvent): String =
        if (event.items.size == 1) {
            event.items.first().title
        } else {
            context.getString(R.string.alarm_multiple_reminders, event.items.size)
        }
}