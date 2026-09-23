package com.lifeos.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifeos.app.core.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms every still-active reminder after the events that can invalidate armed
 * alarms — a device reboot (alarms don't survive it), an app update/reinstall
 * (`MY_PACKAGE_REPLACED`), or the user changing the clock/time zone.
 *
 * Re-arming is idempotent: [ReminderRepository.rebuildAllActive] re-schedules
 * alarms whose trigger is still in the future and skips everything else, so a
 * missed alarm is never re-fired retroactively.
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
                locator.reminderRepository.rebuildAllActive()
            } catch (_: Exception) {
                // Never crash a system broadcast; the reminder simply stays
                // unaltered until the next re-arm opportunity.
            } finally {
                pending.finish()
            }
        }
    }
}