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
 * Everything that hangs off a diary entry, rendered inside the editor: the
 * photo strip, the voice note, the place, the weather and the tags.
 *
 * This is the seam that lets `DiaryEditor` stay a pure writing surface. All the
 * real behaviour lives in [DiaryEditorViewModel] — the photo picker, the
 * recorder, the location fix and the weather lookup — and this composable only
 * renders its state and forwards taps.
 *
 * The two runtime permissions are requested from the handlers rather than on
 * composition, and the live grant is re-read at the point of each tap, because
 * a grant made in system Settings has to be picked up when the user comes back.
 */
@Composable
fun DiaryEditorAttachments(
    viewModel: DiaryEditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Mirrors of the "the OS will no longer prompt" state. The live grant itself
    // is re-read at the point of each tap, so returning from Settings just works.
    var micPermanentlyDenied by remember { mutableStateOf(false) }
    var locationPermanentlyDenied by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.startRecording()
        } else {
            micPermanentlyDenied = !canShowRationale(activity, Manifest.permission.RECORD_AUDIO)
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            viewModel.attachLocation(isPermanentlyDenied = false)
        } else {
            // Rationale gone for every requested permission means the OS will no
            // longer show a dialog, so offer Settings instead of a dead button.
            val permanent = results.keys.none { canShowRationale(activity, it) }
            locationPermanentlyDenied = permanent
            viewModel.attachLocation(isPermanentlyDenied = permanent)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::attachPhoto)
    }

    // Leaving the editor must not leave a recorder or a decoder running.
    DisposableEffect(Unit) {
        onDispose { viewModel.cancelRecording() }
    }

    // The recorder measures its own elapsed time and level from the platform
    // clock, so the row's timer and level dot only advance if something polls it.
    // Keyed on whether a take is actually running, so the loop starts with the
    // take and is cancelled the moment it stops, and `while (isActive)` means it
    // is torn down with the composition even if a stop is missed.
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
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

        DiaryVoiceNoteRow(
            voiceNote = state.voiceNote,
            recording = state.recording,
            playback = state.playback,
            onStartRecording = {
                when {
                    hasPermission(context, Manifest.permission.RECORD_AUDIO) ->
                        viewModel.startRecording()
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

        DiaryTagEditor(
            tags = state.tags,
            onAdd = viewModel::addTag,
            onRemove = viewModel::removeTag
        )

        state.errorMessage?.let { message ->
            InlineErrorText(message)
        }
    }
}

/** A plain inline message, styled like the rest of the editor's supporting text. */
@Composable
private fun InlineErrorText(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.error
    )
}

private fun hasPermission(context: Context, permission: String): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Whether the OS would still show a rationale. `null` when we cannot reach an
 * Activity, in which case we must not claim the permission is permanently
 * denied — a dead "open settings" button is worse than one redundant prompt.
 */
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

/**
 * How often the live recording clock is read while a take runs. 100ms keeps the
 * `m:ss` display moving without a visible jump, and the level dot smooth rather
 * than strobing; `MediaRecorder.maxAmplitude` is a cheap read, so the cost is
 * not worth dropping lower.
 */
private const val RECORDING_TICK_MILLIS = 100L
