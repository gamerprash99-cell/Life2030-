package com.lifeos.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lifeos.app.R
import com.lifeos.app.core.reminders.ReminderScheduler
import com.lifeos.app.core.util.PermissionManager

/**
 * Guards arming a timed reminder (tasks and habits) behind Android's
 * `SCHEDULE_EXACT_ALARM` special access.
 *
 * Exact alarms are not a runtime permission with a result callback: the access
 * is toggled by the user inside the system "Alarms & reminders" page
 * (`Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`), so this host
 *  - checks the live grant with [ReminderScheduler.canScheduleExactAlarms];
 *  - when not allowed, holds a pending action behind an explanation dialog and
 *    deep-links the user to the system page via
 *    [PermissionManager.openExactAlarmSettings];
 *  - re-checks the grant every time the host's lifecycle resumes (i.e. when the
 *    user returns from system Settings) and only then releases the pending
 *    action — if the grant is still missing, nothing is run, nothing is
 *    scheduled, and the UI never claims a "scheduled" state.
 *
 * The host is composed exactly where a timed reminder can be set (never at
 * startup), mirroring [ReminderPermissionHost]. On API < 31 exact scheduling
 * needs no permission, so [runProtected] always runs the action immediately.
 * When the user declines, the scheduler still degrades gracefully to a
 * permission-free, Doze-aware inexact API — reminders keep working, just not
 * pinned to the exact minute.
 */
class ExactAlarmPermissionHost(
    /** Runs [action] only when exact-alarm access is available. Returns true if it ran now. */
    val runProtected: (() -> Unit) -> Boolean
)

@Composable
fun rememberExactAlarmPermissionHost(): ExactAlarmPermissionHost {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var canScheduleExact by remember { mutableStateOf(ReminderScheduler.canScheduleExactAlarms(context)) }
    var asking by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canScheduleExact = ReminderScheduler.canScheduleExactAlarms(context)
                if (canScheduleExact) {
                    // User returned from the system Alarms & reminders page (or
                    // granted the access elsewhere): release the held action now
                    // that exact scheduling is available. If still not allowed,
                    // the dialog stays open so the user can retry or back out —
                    // nothing is persisted or armed in the meantime.
                    val action = pendingAction
                    pendingAction = null
                    asking = false
                    action?.invoke()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (asking) {
        AlertDialog(
            onDismissRequest = {
                asking = false
                pendingAction = null
            },
            title = { Text(context.getString(R.string.exact_alarm_dialog_title)) },
            text = { Text(context.getString(R.string.exact_alarm_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Keep the dialog up; when the user returns, ON_RESUME
                        // re-checks the grant and completes (or keeps holding)
                        // the pending action.
                        PermissionManager.openExactAlarmSettings(context)
                    }
                ) {
                    Text(context.getString(R.string.exact_alarm_dialog_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    asking = false
                    pendingAction = null
                }) { Text(context.getString(R.string.exact_alarm_dialog_not_now)) }
            }
        )
    }

    return ExactAlarmPermissionHost { action ->
        if (ReminderScheduler.canScheduleExactAlarms(context)) {
            action()
            true
        } else {
            pendingAction = action
            asking = true
            false
        }
    }
}