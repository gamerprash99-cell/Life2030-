package com.lifeos.app.ui.components

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.lifeos.app.core.util.PermissionManager

/**
 * Guards "arm a timed reminder" actions behind Android's
 * `POST_NOTIFICATIONS` runtime permission.
 *
 * The host is composed exactly where a timed reminder can be created or set
 * (never at app startup) and its [runProtected] is invoked at the moment the
 * reminder would actually be scheduled:
 *  - API < 33 or already granted → the action runs immediately;
 *  - not granted → the host shows an explanation dialog and only runs the
 *    action after the user actually grants the permission (or never, if they
 *    decline — a reminder is simply not scheduled);
 *  - permanently denied → the dialog offers "Open Settings" instead of a
 *    second request the OS would silently ignore.
 *
 * The ownership of the launcher lives in this composable (it mirrors
 * [com.lifeos.app.core.util.rememberPermissionState]) so the pending action can
 * be released synchronously from the OS result callback.
 */
class ReminderPermissionHost(
    /** Runs [action] only when notifications can be delivered. Returns true if it ran now. */
    val runProtected: (() -> Unit) -> Boolean
)

@Suppress("InlinedApi") // Only dereferenced behind the Build.VERSION >= TIRAMISU gate below; pre-33 never reaches it.
@Composable
fun rememberReminderPermissionHost(): ReminderPermissionHost {
    val context = LocalContext.current
    val activity = context as? Activity
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    var granted by remember {
        mutableStateOf(needsPermission && PermissionManager.hasPermission(context, Manifest.permission.POST_NOTIFICATIONS))
    }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted && activity != null) {
            permanentlyDenied = !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        }
        asking = false
        val action = pendingAction
        pendingAction = null
        if (isGranted) action?.invoke()
    }

    if (asking) {
        AlertDialog(
            onDismissRequest = {
                asking = false
                pendingAction = null
            },
            title = { Text("Allow notifications?") },
            text = {
                Text(
                    "LifeOS plays your task and habit reminders as alerts at the set time. " +
                        "Notifications are only requested when you create a timed reminder, " +
                        "and everything stays on your device."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    asking = false
                    if (permanentlyDenied) {
                        PermissionManager.openAppSettings(context)
                    } else {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) {
                    Text(if (permanentlyDenied) "Open Settings" else "Allow notifications")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    asking = false
                    pendingAction = null
                }) { Text("Not now") }
            }
        )
    }

    return ReminderPermissionHost { action ->
        if (!needsPermission || granted) {
            action()
            true
        } else {
            pendingAction = action
            asking = true
            false
        }
    }
}