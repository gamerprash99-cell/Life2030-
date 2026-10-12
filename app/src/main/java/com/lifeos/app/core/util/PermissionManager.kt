package com.lifeos.app.core.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
 * Centralized runtime permission handling. Permissions are requested only
 * from the feature that needs them, never at app launch.
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

    fun openLocationSettings(context: Context) {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Settings.ACTION_LOCATION_SOURCE_SETTINGS
        } else {
            Settings.ACTION_SECURITY_SETTINGS
        }
        runCatching {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { openAppSettings(context) }
    }

    @Suppress("InlinedApi")
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure { openAppSettings(context) }
        } else {
            openAppSettings(context)
        }
    }
}

/** The three states the UI renders distinctly. */
enum class PermissionStatus { GRANTED, NOT_YET_REQUESTED_OR_DENIABLE, PERMANENTLY_DENIED }

data class PermissionState(
    val status: PermissionStatus,
    val isGranted: Boolean,
    val request: () -> Unit,
    val openSettings: () -> Unit
)

@Composable
fun rememberPermissionState(permission: String): PermissionState {
    val context = LocalContext.current
    val activity = context as? Activity
    var granted by remember(permission) {
        mutableStateOf(PermissionManager.hasPermission(context, permission))
    }
    var permanentlyDenied by remember(permission) { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        granted = isGranted
        if (!isGranted && activity != null) {
            permanentlyDenied = !androidx.core.app.ActivityCompat
                .shouldShowRequestPermissionRationale(activity, permission)
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
