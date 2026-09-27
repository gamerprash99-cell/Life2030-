package com.lifeos.app.ui.diary

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.media.RecordingState
import com.lifeos.app.core.util.PermissionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * All media/location/tag attachments remain driven by the same editor ViewModel.
 * Only the visual order is changed so the high-priority photo + tag sections
 * appear in the same place as the supplied editor reference.
 */
@Composable
fun DiaryEditorAttachments(
    viewModel: DiaryEditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val state by viewModel.state.collectAsStateWithLifecycle()

    var micPermanentlyDenied by remember { mutableStateOf(false) }
    var locationPermanentlyDenied by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startRecording()
        else micPermanentlyDenied = !canShowRationale(activity, Manifest.permission.RECORD_AUDIO)
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) viewModel.attachLocation(isPermanentlyDenied = false)
        else {
            val permanent = results.keys.none { canShowRationale(activity, it) }
            locationPermanentlyDenied = permanent
            viewModel.attachLocation(isPermanentlyDenied = permanent)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::attachPhoto)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.cancelRecording() }
    }

    val isRecording = state.recording is RecordingState.Recording
    LaunchedEffect(isRecording) {
        if (!isRecording) return@LaunchedEffect
        while (isActive) {
            viewModel.tickRecording()
            delay(RECORDING_TICK_MILLIS)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = com.lifeos.app.ui.theme.LifeOSSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DiaryPhotoStrip(
            photos = state.photos,
            onAdd = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemove = viewModel::removePhoto
        )

        DiaryTagEditor(
            tags = state.tags,
            onAdd = viewModel::addTag,
            onRemove = viewModel::removeTag
        )

        DiaryVoiceNoteRow(
            voiceNote = state.voiceNote,
            recording = state.recording,
            playback = state.playback,
            onStartRecording = {
                when {
                    hasPermission(context, Manifest.permission.RECORD_AUDIO) -> viewModel.startRecording()
                    micPermanentlyDenied -> PermissionManager.openAppSettings(context)
                    else -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onStopRecording = viewModel::stopRecording,
            onCancelRecording = viewModel::cancelRecording,
            onTogglePlayback = viewModel::togglePlayback,
            onRemove = viewModel::removeVoiceNote
        )

        DiaryLocationRow(
            place = state.place,
            status = state.locationStatus,
            onAdd = {
                when {
                    hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ->
                        viewModel.attachLocation(isPermanentlyDenied = false)
                    locationPermanentlyDenied -> PermissionManager.openAppSettings(context)
                    else -> locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
            },
            onClear = viewModel::clearLocation
        )

        DiaryWeatherRow(weather = state.weather)

        state.errorMessage?.let { message ->
            InlineErrorText(message)
        }
    }
}

@Composable
private fun InlineErrorText(message: String) {
    Text(message, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
}

private fun hasPermission(context: Context, permission: String): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun canShowRationale(activity: Activity?, permission: String): Boolean =
    activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } ?: true

private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private const val RECORDING_TICK_MILLIS = 100L
