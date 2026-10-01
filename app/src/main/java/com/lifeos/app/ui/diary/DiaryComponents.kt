package com.lifeos.app.ui.diary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lifeos.app.core.media.PlaybackState
import com.lifeos.app.core.media.RecordingFailure
import com.lifeos.app.core.media.RecordingState
import com.lifeos.app.core.media.formatAudioDuration
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryWeather
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.DiaryTagInk
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LifeOSWarning
import java.io.File


@Composable
fun DiaryPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(DiaryPaperCard)
            .border(1.dp, DiaryHairline, RoundedCornerShape(cornerRadius.dp))
            .padding(LifeOSSpacing.compactPadding),
        content = content
    )
}

@Composable
fun DiarySectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = DiaryInkViolet,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryPhotoStrip(
    photos: List<DiaryAttachment.Photo>,
    onAdd: () -> Unit,
    onRemove: (DiaryAttachment.Photo) -> Unit,
    modifier: Modifier = Modifier,
    addLabel: String = "Add more photos",
    /**
     * `false` where another control on the same surface already adds a photo
     * (the composer pins its camera icon at the foot of the memory card): the
     * add tile is dropped and an empty strip collapses to nothing, so the
     * capability is never offered twice. Defaults to `true` everywhere else.
     */
    showAddAction: Boolean = true
) {
    if (photos.isEmpty() && !showAddAction) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DiarySectionLabel(
            "Photos",
            trailing = {
                if (photos.isNotEmpty()) Text(
                    photos.size.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 4
        ) {
            photos.forEach { photo -> DiaryReferencePhotoTile(photo, onRemove) }
            if (showAddAction) DiaryReferenceAddTile(addLabel, onAdd)
        }
    }
}

@Composable
private fun DiaryReferencePhotoTile(photo: DiaryAttachment.Photo, onRemove: (DiaryAttachment.Photo) -> Unit) {
    val context = LocalContext.current
    val exists = remember(photo.filePath) { File(photo.filePath).exists() }
    Box(
        Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DiaryLavender.copy(alpha = 0.3f))
    ) {
        if (exists) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(photo.filePath)).build(),
                contentDescription = "Diary photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            MissingMediaLabel(Modifier.fillMaxSize(), "Photo unavailable")
        }
        // The description is on the clickable node, not the icon inside it, so
        // TalkBack announces one removable-photo button rather than a button
        // with an unnameable action and a stray label beside it.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(48.dp)
                .clickable(role = Role.Button) { onRemove(photo) }
                .semantics { this.contentDescription = "Remove photo" },
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                Modifier.padding(5.dp).size(24.dp).clip(CircleShape).background(DiaryInkViolet.copy(alpha = 0.62f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun DiaryReferenceAddTile(label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DiaryActionViolet.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .background(DiaryLavender.copy(alpha = 0.16f))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = DiaryActionViolet, modifier = Modifier.padding(top = 5.dp), maxLines = 2)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryTagEditor(
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by rememberSaveable { mutableStateOf("") }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DiarySectionLabel("Tags", trailing = {
            // The add control is the only way a tag can be created, and it was
            // a 32dp-tall bordered box of 12sp text — below the app's own 48dp
            // `minTouchTarget`, and quiet enough to read as decoration rather
            // than something to press. It now meets the touch target, is set
            // at the same 14sp as the tag chips it produces, and announces as
            // a button. The violet outline is kept deliberately: it is the
            // established accent in this card, and filling it would make a
            // metadata affordance compete with the pinned actions below.
            Box(
                Modifier.clip(CircleShape)
                    .border(1.dp, DiaryActionViolet.copy(alpha = 0.35f), CircleShape)
                    .clickable(role = Role.Button) { showDialog = true }
                    .defaultMinSize(minHeight = LifeOSSpacing.minTouchTarget)
                    .padding(horizontal = 16.dp)
            ) {
                Text("+ Add tag", style = MaterialTheme.typography.labelLarge, color = DiaryActionViolet)
            }
        })
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    Row(
                        Modifier.clip(CircleShape)
                            .background(DiaryLavender.copy(alpha = 0.64f))
                            .padding(start = 11.dp, end = 1.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // The × is the only way a tag is removed, and it was a
                        // 36dp target — below the app's own 48dp
                        // `minTouchTarget` that every other control in this card
                        // already meets. It now uses the same token, so the
                        // remove action is as easy to hit as the add action that
                        // created the tag.
                        //
                        // Vertical padding drops to 1dp so the *height* is
                        // unchanged: 1 + 48 + 1 is the same 50dp the old 36dp box
                        // produced (7 + 36 + 7), and the × stays centred in the
                        // same place. The chip is 12dp wider, because the target
                        // is 12dp wider; that is the unavoidable cost of the
                        // fix and it is the whole visible change.
                        Text(tag, style = MaterialTheme.typography.labelMedium, color = DiaryTagInk)
                        Box(Modifier.size(LifeOSSpacing.minTouchTarget).clickable { onRemove(tag) }, contentAlignment = Alignment.Center) {
                            Text("×", style = MaterialTheme.typography.titleSmall, color = DiaryActionViolet)
                        }
                    }
                }
            }
        } else {
            // A hint, not a prompt: it stays in the muted on-surface colour so
            // the Tags group keeps its place in the hierarchy, but at 12sp it
            // was the smallest type in the card and the least legible line on
            // the screen. 14sp matches the tag chips and the surrounding
            // metadata, which is all the legibility this line needs.
            Text("No tags yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false; draft = "" },
            title = { Text("Add tag") },
            text = {
                TextField(
                    value = draft,
                    onValueChange = { draft = it.take(40) },
                    singleLine = true,
                    placeholder = { Text("e.g. Gratitude") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = DiaryActionViolet,
                        unfocusedIndicatorColor = DiaryHairline
                    )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = draft.isNotBlank(),
                    onClick = { onAdd(draft); draft = ""; showDialog = false }
                ) { Text("Add", color = DiaryActionViolet) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; draft = "" }) { Text("Cancel") }
            }
        )
    }
}

/** Voice-note row: record / stop / play with the real measured duration. */
@Composable
fun DiaryVoiceNoteRow(
    voiceNote: DiaryAttachment.VoiceNote?,
    recording: RecordingState,
    playback: PlaybackState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onTogglePlayback: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * `false` where another control on the same surface already starts a
     * recording (the composer's pinned microphone icon): the idle "Add voice
     * note" button is dropped and a row with no take and nothing in progress
     * collapses to nothing. Defaults to `true` everywhere else.
     */
    showAddAction: Boolean = true
) {
    val isRecording = recording is RecordingState.Recording
    // Nothing worth printing while idle with no take — the composer's mic icon
    // is the record control there. A real failure is still explained.
    if (!showAddAction && !isRecording && voiceNote == null && recording !is RecordingState.Failure) return
    // The live level is a real amplitude reading, animated only for smoothness.
    val level = (recording as? RecordingState.Recording)?.amplitude ?: 0
    val levelAlpha by animateFloatAsState(
        targetValue = if (isRecording) (level.coerceIn(0, 20_000) / 20_000f).coerceAtLeast(0.15f) else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "micLevel"
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DiarySectionLabel(text = "Voice note")

        when {
            isRecording -> {
                val active = recording as RecordingState.Recording
                DiaryPanel(cornerRadius = 18) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(RECORD_DOT_SIZE)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = levelAlpha.coerceAtLeast(0.35f))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                formatAudioDuration(active.elapsedMillis),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Recording…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton48(onClick = onCancelRecording, description = "Cancel recording") {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton48(onClick = onStopRecording, description = "Stop recording") {
                            Icon(Icons.Filled.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            voiceNote != null -> {
                val exists = remember(voiceNote.filePath) { File(voiceNote.filePath).exists() }
                DiaryPanel(cornerRadius = 18) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton48(
                            onClick = { if (exists) onTogglePlayback() },
                            enabled = exists,
                            description = if (playback is PlaybackState.Playing) "Pause voice note" else "Play voice note"
                        ) {
                            Icon(
                                imageVector = if (playback is PlaybackState.Playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = if (exists) DiaryInkViolet else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                formatAudioDuration(voiceNote.durationMillis),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                when {
                                    !exists -> "Audio file unavailable"
                                    playback is PlaybackState.MissingFile -> "Audio file unavailable"
                                    playback is PlaybackState.Failed -> "Playback failed"
                                    playback is PlaybackState.Playing -> "Playing"
                                    else -> "Tap play to listen"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton48(onClick = onRemove, description = "Remove voice note") {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            else -> if (showAddAction) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(DiaryLavender.copy(alpha = 0.22f))
                        .clickable(role = Role.Button, onClick = onStartRecording)
                        // 12dp of vertical padding around a 20dp icon and a
                        // `labelLarge` line measured ~44dp, so the one control
                        // that starts a recording was under the 48dp minimum
                        // every other control in this card meets. `defaultMinSize`
                        // states that floor without changing the row at any font
                        // scale where the content already exceeds it.
                        .defaultMinSize(minHeight = LifeOSSpacing.minTouchTarget)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(20.dp))
                    Text("Add voice note", style = MaterialTheme.typography.labelLarge, color = DiaryActionViolet)
                }
            }
        }

        // A real failure (mic busy, take too short) is explained, never hidden.
        AnimatedVisibility(
            visible = recording is RecordingState.Failure,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val reason = (recording as RecordingState.Failure).reason
            Text(
                when (reason) {
                    RecordingFailure.TOO_SHORT -> "That take was too short to save."
                    else -> "Recording failed. Please try again."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** What the Location row can offer the user for a given state. */
enum class LocationRowAction {
    /** Ask for permission and fetch a fix — the normal path. */
    REQUEST,

    /** Everything is permitted and available; just try again. */
    RETRY,

    /** Permission was permanently refused: the OS prompt is now a silent no-op. */
    OPEN_APP_PERMISSIONS,

    /** The device's location service is off: it has to be switched on in Settings. */
    OPEN_LOCATION_SETTINGS,

    /** Nothing the user can do from here. Deliberately offers no button. */
    NONE
}

/** How loudly a state should be presented. */
enum class LocationRowTone {
    /** Not a problem — the ordinary idle affordance, or a plain fact. */
    NEUTRAL,

    /** The user has to do something, but nothing has gone wrong. */
    WARNING,

    /** Something unexpected went wrong. The only tone that reaches red. */
    ERROR
}

/**
 * The Location row's content for a state where no place is attached: what it
 * says, and the single action that can actually move the state on.
 */
data class LocationRowOffer(
    val title: String,
    val detail: String?,
    val action: LocationRowAction,
    val tone: LocationRowTone
)

/**
 * The single place that decides what the Location row shows, as a pure function
 * of [LocationStatus] so it can be unit-tested without a device.
 *
 * The rule this exists to enforce: **a state may never offer an action that
 * cannot succeed.** Every status that needs something the user has to do first
 * resolves to a settings action, and the two states where there is genuinely
 * nothing to press resolve to [LocationRowAction.NONE]. Before this, every
 * status except the two permission ones fell through to "Add current
 * location" — so switching location off in the OS still produced a live-looking
 * button that could not work.
 *
 * Only meaningful when no place is attached and no request is in flight; the
 * row checks those two cases before calling it.
 */
fun locationRowOffer(status: LocationStatus): LocationRowOffer = when (status) {
    LocationStatus.IDLE -> LocationRowOffer(
        title = "Add current location",
        detail = null,
        action = LocationRowAction.REQUEST,
        tone = LocationRowTone.NEUTRAL
    )

    LocationStatus.PERMISSION_DENIED -> LocationRowOffer(
        title = "Location access needed",
        // Naming the permission, not just the outcome, is what makes the next
        // tap predictable: the OS prompt is still available in this state.
        detail = "Allow location to attach this place to your memory.",
        action = LocationRowAction.REQUEST,
        tone = LocationRowTone.WARNING
    )

    LocationStatus.PERMISSION_PERMANENTLY_DENIED -> LocationRowOffer(
        title = "Location access is off",
        detail = "It was denied, so Android will not ask again. Turn it on in Settings.",
        action = LocationRowAction.OPEN_APP_PERMISSIONS,
        tone = LocationRowTone.WARNING
    )

    LocationStatus.SERVICE_DISABLED -> LocationRowOffer(
        title = "Location is turned off",
        // Says the cause and the fix, and offers the one destination that can
        // actually change the state. App-permission settings would be a dead
        // end here: the permission is already granted, it is the device switch
        // that is off.
        detail = "Turn location on for this device in Settings, then try again.",
        action = LocationRowAction.OPEN_LOCATION_SETTINGS,
        tone = LocationRowTone.WARNING
    )

    LocationStatus.NO_PROVIDER -> LocationRowOffer(
        title = "No location service on this device",
        detail = "Nothing to turn on, so a place cannot be attached here.",
        action = LocationRowAction.NONE,
        tone = LocationRowTone.NEUTRAL
    )

    LocationStatus.NO_FIX -> LocationRowOffer(
        title = "No fix yet",
        detail = "Your memory is saved as is. Try again in a moment for a place.",
        action = LocationRowAction.RETRY,
        tone = LocationRowTone.NEUTRAL
    )

    // REQUESTING is handled by the row's own progress state and never reaches
    // this function; it is mapped anyway so the mapping is total and a future
    // caller cannot accidentally get a button shown mid-request.
    LocationStatus.REQUESTING -> LocationRowOffer(
        title = "Finding your location…",
        detail = null,
        action = LocationRowAction.NONE,
        tone = LocationRowTone.NEUTRAL
    )

    LocationStatus.FAILED -> LocationRowOffer(
        title = "That place could not be attached",
        detail = "Your memory is safe. You can try once more.",
        action = LocationRowAction.RETRY,
        tone = LocationRowTone.ERROR
    )
}

/**
 * Location row. Shows the resolved place name when the geocoder produced one,
 * the raw coordinates when it did not, and — when there is no place — an offer
 * that matches the real state: the cause, what to do about it, and a single
 * action that can actually work. Never a hardcoded city, and never a button
 * that promises something the current state cannot deliver.
 */
@Composable
fun DiaryLocationRow(
    place: DiaryAttachment.Place?,
    status: LocationStatus,
    onAdd: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * `false` where another control on the same surface already attaches a place
     * (the composer's pinned location icon): the idle "Add current location"
     * button is dropped and an idle row with no place collapses to nothing. A
     * blocked state is still explained, because that is guidance rather than a
     * second way to add. Defaults to `true` everywhere else.
     */
    showAddAction: Boolean = true,
    /** Routes to the app's own permission page. Defaults to inert. */
    onOpenAppSettings: () -> Unit = {},
    /** Routes to Android's location settings. Defaults to inert. */
    onOpenLocationSettings: () -> Unit = {}
) {
    if (!showAddAction && place == null && status == LocationStatus.IDLE) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DiarySectionLabel(text = "Location")
        when {
            status == LocationStatus.REQUESTING -> DiaryPanel(cornerRadius = 18) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(DiaryActionViolet.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DiaryActionViolet)
                        )
                    }
                    Text("Finding your location…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Attached wins over every status: a real place is the most useful
            // thing this row can show, and the existing remove control is kept
            // exactly as it was.
            place != null -> DiaryPanel(cornerRadius = 18) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            place.placeName.takeIf { it.isNotBlank() } ?: formatCoordinates(place),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        // Accuracy is the real reported accuracy, when present.
                        place.accuracyMeters?.let {
                            Text(
                                "±${it.toInt()} m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton48(onClick = onClear, description = "Remove location") {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // No place, nothing in flight: the honest offer for this state.
            // The idle affordance is dropped where another control on this
            // surface already owns it (the composer's pinned icon), but only
            // the idle one — a blocked state still needs its recovery control,
            // which is guidance rather than a second way to add.
            else -> {
                val offer = locationRowOffer(status)
                val showOfferAction = if (status == LocationStatus.IDLE) showAddAction else true
                DiaryPanel(cornerRadius = 18) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                // A blocked state is tinted with the warning
                                // token rather than the action violet: nothing
                                // in this row is an action yet.
                                tint = if (offer.tone == LocationRowTone.WARNING) {
                                    LifeOSWarning
                                } else {
                                    DiaryActionViolet
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    offer.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                offer.detail?.let { detail ->
                                    Text(
                                        detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (showOfferAction) {
                            LocationOfferButton(
                                action = offer.action,
                                onAdd = onAdd,
                                onOpenAppSettings = onOpenAppSettings,
                                onOpenLocationSettings = onOpenLocationSettings
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The single recovery control for a blocked or unfulfilled location state.
 *
 * It is a filled lavender pill with a 48dp target and `Role.Button` rather than
 * the old quiet text row, because this control is the only way out of a state
 * the user cannot otherwise leave. Each label dispatches on [LocationRowAction]
 * so the control always matches the state offering it, and `NONE` renders
 * nothing at all — which is what keeps "no location service on this device"
 * free of a dead button.
 */
@Composable
private fun LocationOfferButton(
    action: LocationRowAction,
    onAdd: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit
) {
    val (label, onClick) = when (action) {
        LocationRowAction.REQUEST -> "Allow & attach" to onAdd
        LocationRowAction.RETRY -> "Try again" to onAdd
        LocationRowAction.OPEN_APP_PERMISSIONS -> "Open settings" to onOpenAppSettings
        LocationRowAction.OPEN_LOCATION_SETTINGS -> "Turn location on" to onOpenLocationSettings
        LocationRowAction.NONE -> return
    }
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = LifeOSSpacing.minTouchTarget)
            .clip(RoundedCornerShape(50))
            .background(DiaryLavender.copy(alpha = 0.55f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = DiaryActionViolet)
    }
}

/**
 * Weather row.
 *
 * Renders a real reading only when one exists. In a stock offline-first build
 * there is no permitted weather source, so this shows an honest unavailable
 * state — it never invents a temperature, an icon or a condition.
 */
@Composable
fun DiaryWeatherRow(
    weather: DiaryWeather,
    modifier: Modifier = Modifier
) {
    val (label, tint) = when (weather) {
        is DiaryWeather.Available -> {
            val rounded = Math.round(weather.temperatureCelsius)
            "${rounded}°C · ${weather.condition}" to MaterialTheme.colorScheme.tertiary
        }
        is DiaryWeather.Unavailable -> when (weather.reason) {
            DiaryWeather.UnavailableReason.NO_LOCATION ->
                "Add a location to show weather" to MaterialTheme.colorScheme.onSurfaceVariant
            DiaryWeather.UnavailableReason.PERMISSION_DENIED ->
                "Location permission needed for weather" to MaterialTheme.colorScheme.onSurfaceVariant
            DiaryWeather.UnavailableReason.PERMISSION_PERMANENTLY_DENIED ->
                "Enable location in Settings for weather" to MaterialTheme.colorScheme.onSurfaceVariant
            DiaryWeather.UnavailableReason.LOCATION_UNAVAILABLE ->
                "Weather unavailable — no location fix" to MaterialTheme.colorScheme.onSurfaceVariant
            DiaryWeather.UnavailableReason.NO_SOURCE_OFFLINE_ONLY ->
                "Weather unavailable — LifeOS works offline" to MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    val iconTint by animateColorAsState(tint, label = "weatherTint")

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Filled.WbSunny,
            contentDescription = null,
            tint = iconTint.copy(alpha = if (weather is DiaryWeather.Available) 1f else 0.45f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = iconTint
        )
    }
}

/** A labelled metadata line, e.g. "Date 26 September 2026". */
@Composable
fun DiaryMetaRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        icon()
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(META_LABEL_WIDTH)
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Shown when a referenced media file is gone. */
@Composable
fun MissingMediaLabel(modifier: Modifier = Modifier, text: String) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

/**
 * Renders real coordinates to 4 decimal places (~11 m), which is the useful
 * precision for a journal entry and avoids implying survey-grade accuracy the
 * device did not provide. Used only when no geocoder backend resolved a place
 * name — it is the honest fallback, not a substitute for a real lookup.
 */
private fun formatCoordinates(place: DiaryAttachment.Place): String =
    String.format(
        java.util.Locale.getDefault(),
        "%.4f, %.4f",
        place.latitude,
        place.longitude
    )

/**
 * A square, comfortably tappable icon button used across the Diary rows.
 *
 * [description] is applied as a real accessibility content description so
 * TalkBack announces the action rather than an unlabelled icon.
 */
@Composable
fun IconButton48(
    onClick: () -> Unit,
    description: String,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(LifeOSSpacing.minTouchTarget)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Fixed layout constants so tiles stay square at any screen size or font scale. */
private val RECORD_DOT_SIZE = 40.dp
private val META_LABEL_WIDTH = 92.dp
