package com.lifeos.app.ui.diary

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.media.RecordingState
import com.lifeos.app.core.util.PermissionManager
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * The three editor actions (photo, voice note, location) and the permission
 * plumbing they need. They live in one place so the icon row pinned at the foot
 * of the memory card and the media rows inside it drive the *same* launchers —
 * there is exactly one permission flow per capability, and no permission is ever
 * requested before the user taps the matching action.
 */
class DiaryEditorActionTriggers(
    val addPhoto: () -> Unit,
    val toggleVoiceNote: () -> Unit,
    val addLocation: () -> Unit
)

/**
 * Builds the editor action triggers. Call this once per editor instance, from a
 * stable position in the composition, and pass the result to both
 * [DiaryEditorActionBar] and [DiaryEditorAttachments].
 */
@Composable
fun rememberDiaryEditorActionTriggers(viewModel: DiaryEditorViewModel): DiaryEditorActionTriggers {
    val context = LocalContext.current
    val activity = context.findActivity()

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

    return DiaryEditorActionTriggers(
        addPhoto = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        toggleVoiceNote = {
            if (viewModel.state.value.recording is RecordingState.Recording) {
                viewModel.stopRecording()
            } else when {
                hasPermission(context, Manifest.permission.RECORD_AUDIO) -> viewModel.startRecording()
                micPermanentlyDenied -> PermissionManager.openAppSettings(context)
                else -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        addLocation = {
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
        }
    )
}

/**
 * The compact utility row that lives at the bottom of the white "Your memory"
 * card. It is pinned below the card's scroll viewport, so it stays put while the
 * diary text and its media scroll behind it and the icons can never cover the
 * text, the cursor, or the character counter.
 *
 * [locationStatus] defaults to [LocationStatus.IDLE] so existing callers keep
 * compiling unchanged, and changes nothing about the layout, the icons or what
 * a tap does — it only makes the location icon's *spoken* label tell the truth
 * about the current state, the same way [isRecording] already does for the
 * microphone. Without it a screen reader announces "Add a location to this
 * memory" while the app is in a state where that tap cannot attach anything.
 */
@Composable
fun DiaryEditorActionBar(
    isRecording: Boolean,
    onAddPhoto: () -> Unit,
    onToggleVoiceNote: () -> Unit,
    onAddLocation: () -> Unit,
    modifier: Modifier = Modifier,
    locationStatus: LocationStatus = LocationStatus.IDLE
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorActionIcon(Icons.Outlined.AddAPhoto, "Add a photo to this memory", onAddPhoto)
        EditorActionIcon(
            icon = Icons.Outlined.Mic,
            contentDescription = if (isRecording) "Stop recording" else "Record a voice note",
            onClick = onToggleVoiceNote,
            active = isRecording
        )
        // Same tap in every state — the row below is what changes — but the
        // label says what the tap will actually do next, so the control is
        // never announced as a promise it cannot keep.
        EditorActionIcon(
            icon = Icons.Outlined.LocationOn,
            contentDescription = locationActionDescription(locationStatus),
            onClick = onAddLocation
        )
    }
}

/**
 * What the location icon will actually do, in the given state.
 *
 * [internal] rather than private so the invariant can be unit-tested: the icon
 * must never be announced as a promise the current state cannot keep.
 */
internal fun locationActionDescription(status: LocationStatus): String = when (status) {
    LocationStatus.IDLE -> "Add a location to this memory"
    LocationStatus.REQUESTING -> "Finding your location"
    LocationStatus.PERMISSION_DENIED -> "Allow location access to attach a place"
    LocationStatus.PERMISSION_PERMANENTLY_DENIED -> "Location access is off. Open settings to turn it on"
    LocationStatus.SERVICE_DISABLED -> "Location is turned off. Open location settings to turn it on"
    LocationStatus.NO_PROVIDER -> "This device has no location service"
    LocationStatus.NO_FIX -> "No location fix yet. Try again"
    LocationStatus.FAILED -> "That place could not be attached. Try again"
}

@Composable
private fun EditorActionIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    // The touch target is the existing LifeOS minimum; only the tinted circle
    // is smaller, so the control reads as a small utility button but stays
    // comfortably tappable.
    //
    // The description is applied to the *clickable* node, not to the `Icon`
    // inside it. A `contentDescription` on a child of a clickable is announced
    // as a separate, non-clickable element, so TalkBack would offer the user a
    // label they could not activate and a button they could not name. Putting
    // it on the same node that carries `onClick` is what makes the control a
    // single, correctly-labelled button — the same treatment `IconButton48`
    // already uses elsewhere in this card.
    Box(
        modifier = modifier
            .size(LifeOSSpacing.minTouchTarget)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (active) DiaryLavender.copy(alpha = 0.85f) else DiaryLavender.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) DiaryActionViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * The memory's own media: photos, tags, the voice note, the place and the
 * weather. Rendered by [DiaryEditor] *inside* the white memory card, in the same
 * scroll viewport as the text, so the media belongs to the writing surface
 * instead of sitting in a separate attachment panel outside it.
 *
 * The add affordances are handed to the card's own three action icons
 * (`showAddAction = false` below), which drive the very same
 * [DiaryEditorActionTriggers] — so there is exactly one permission flow and
 * exactly one control per capability, and nothing is requested before a tap.
 */
@Composable
fun DiaryEditorAttachments(
    viewModel: DiaryEditorViewModel,
    triggers: DiaryEditorActionTriggers,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

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

    // Two labelled groups, in the order a memory is actually written: what was
    // written, then what it is *about* (tags), then what came with it. Before
    // this they were one flat column that put photos above the tags and gave no
    // signal that tags are metadata rather than another kind of attachment, so
    // a new photo read as though it were part of the entry's subject. The tags
    // component already labels itself "Tags", so only the second group needed
    // a heading, and each of its children names itself as before.
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DiaryTagEditor(
            tags = state.tags,
            onAdd = viewModel::addTag,
            onRemove = viewModel::removeTag
        )

        // The break between the two groups is a little wider than the column's
        // own 12dp so the heading reads as a new section rather than another
        // row of the tags block.
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = DiaryHairline)
        DiarySectionLabel("Attachments")

        DiaryPhotoStrip(
            photos = state.photos,
            onAdd = { triggers.addPhoto() },
            onRemove = viewModel::removePhoto,
            showAddAction = false
        )

        DiaryVoiceNoteRow(
            voiceNote = state.voiceNote,
            recording = state.recording,
            playback = state.playback,
            onStartRecording = { triggers.toggleVoiceNote() },
            onStopRecording = viewModel::stopRecording,
            onCancelRecording = viewModel::cancelRecording,
            onTogglePlayback = viewModel::togglePlayback,
            onRemove = viewModel::removeVoiceNote,
            showAddAction = false
        )

        DiaryLocationRow(
            place = state.place,
            status = state.locationStatus,
            onAdd = { triggers.addLocation() },
            onClear = viewModel::clearLocation,
            showAddAction = false,
            // Both routes go through the existing centralized PermissionManager,
            // so the row never builds an intent of its own and the composer
            // still has exactly one permission flow per capability.
            onOpenAppSettings = { PermissionManager.openAppSettings(context) },
            onOpenLocationSettings = { PermissionManager.openLocationSettings(context) }
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
