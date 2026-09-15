package com.lifeos.app.core.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Centralized runtime permission handling (Section 6 of the UI/UX pass:
 * "fix repeated requests"). Requested only by the specific screen that
 * needs them (Capture -> Camera/Audio), never at app launch.
 *
 * ROOT CAUSE of the "keeps asking" feeling: Android silently no-ops the
 * system dialog once a permission has been denied twice ("permanently
 * denied") — tapping "Grant" again produces no visible dialog at all,
 * which reads as the app being broken or stuck in a loop. The fix is to
 * detect that state explicitly and route the user to system Settings
 * instead of calling the (now silently ignored) request again.
 */
object PermissionManager {
    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

/** The three states the UI actually needs to render distinctly. */
enum class PermissionStatus { GRANTED, NOT_YET_REQUESTED_OR_DENIABLE, PERMANENTLY_DENIED }

data class PermissionState(
    val status: PermissionStatus,
    val isGranted: Boolean,
    val request: () -> Unit,
    val openSettings: () -> Unit
)

/**
 * Remembers a single permission's live status. Re-checks against the real
 * OS permission state (not cached app state) every time this enters
 * composition, so a permission granted via system Settings while the app
 * was backgrounded is picked up correctly too.
 */
@Composable
fun rememberPermissionState(permission: String): PermissionState {
    val context = LocalContext.current
    val activity = context as? Activity

    var granted by remember { mutableStateOf(PermissionManager.hasPermission(context, permission)) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        granted = isGranted
        if (!isGranted && activity != null) {
            // If the rationale can no longer be shown after a denial, the OS has moved
            // this permission into "permanently denied" — further launch() calls are
            // silent no-ops, so we must offer "Open Settings" instead of asking again.
            val canShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            permanentlyDenied = !canShowRationale
        }
    }

    val status = when {
        granted -> PermissionStatus.GRANTED
        permanentlyDenied -> PermissionStatus.PERMANENTLY_DENIED
        else -> PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE
    }

    return remember(status) {
        PermissionState(
            status = status,
            isGranted = granted,
            request = { if (status != PermissionStatus.PERMANENTLY_DENIED) launcher.launch(permission) },
            openSettings = { PermissionManager.openAppSettings(context) }
        )
    }
}

object LifeOSPermissions {
    const val CAMERA = Manifest.permission.CAMERA
    const val RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
}
