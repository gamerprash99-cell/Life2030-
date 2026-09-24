package com.lifeos.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifeos.app.core.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms the alarm projection after the events that can invalidate armed
 * alarms:
 *  - a device reboot (alarms don't survive it) and an app update/reinstall
 *    (`MY_PACKAGE_REPLACED`) → re-project every still-active reminder;
 *  - the user changing the clock or time zone (`TIME_SET`, `TIMEZONE_CHANGED`)
 *    → rebuild the reminders table from the task/habit `reminderEpochMillis`
 *    wall-clock mirrors before re-projecting, so recurring reminders stay
 *    pinned to local time instead of moving relative to the new clock.
 *
 * Both paths are idempotent and fast-forward past-due occurrences, so a missed
 * alarm is never re-fired retroactively.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val locator = ServiceLocator.get(context.applicationContext)
                if (action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
                    val tasks = locator.taskRepository.getAllForBackup()
                    val habits = locator.habitRepository.getAllForBackup()
                    locator.reminderRepository.rebuildFromMirrors(tasks, habits)
                } else {
                    locator.reminderRepository.rebuildAllActive()
                }
            } catch (_: Exception) {
                // Never crash a system broadcast; the reminder simply stays
                // unaltered until the next re-arm opportunity.
            } finally {
                pending.finish()
            }
        }
    }
}