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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.core.media.PlaybackState
import com.lifeos.app.core.media.RecordingFailure
import com.lifeos.app.core.media.RecordingState
import com.lifeos.app.core.media.formatAudioDuration
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryWeather
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.io.File

/**
 * The reusable pieces of the Diary visual language, shared by the day list,
 * the composer and the entry detail so all three stay identical (and so no
 * screen grows a private one-off style).
 *
 * Every value these render comes from the database or from a real device API.
 * Nothing is hardcoded for appearance: an absent photo, place or reading
 * renders an explicit unavailable state instead of a stand-in.
 */

/** A hairline-bordered paper card — the surface every Diary panel sits on. */
@Composable
fun DiaryPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(DiaryPaperCard)
            .border(1.dp, DiaryHairline, RoundedCornerShape(cornerRadius.dp))
            .padding(LifeOSSpacing.compactPadding),
        content = { content() }
    )
}

/** Section label such as "Photos", "Tags" or "Location". */
@Composable
fun DiarySectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/**
 * Photo strip. Each tile uses `ContentScale.Crop` inside a fixed
 * [PHOTO_ASPECT_RATIO] box, so an arbitrary source aspect ratio is cropped
 * rather than stretched, and the tile never depends on the device width.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryPhotoStrip(
    photos: List<DiaryAttachment.Photo>,
    onAdd: () -> Unit,
    onRemove: (DiaryAttachment.Photo) -> Unit,
    modifier: Modifier = Modifier,
    addLabel: String = "Add photo"
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DiarySectionLabel(
            text = if (photos.isEmpty()) "Photos" else "Photos (${photos.size})"
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            photos.forEach { photo ->
                DiaryPhotoTile(filePath = photo.filePath, onRemove = { onRemove(photo) })
            }
            AddTile(label = addLabel, onClick = onAdd)
        }
    }
}

@Composable
private fun DiaryPhotoTile(filePath: String, onRemove: () -> Unit) {
    val context = LocalContext.current
    val exists = remember(filePath) { File(filePath).exists() }
    Box(
        modifier = Modifier
            .size(PHOTO_TILE_SIZE)
            .clip(RoundedCornerShape(16.dp))
            .background(DiaryLavender.copy(alpha = 0.35f))
    ) {
        if (exists) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(filePath)).build(),
                contentDescription = "Diary photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(PHOTO_ASPECT_RATIO)
            )
        } else {
            // The file was removed outside the app. Say so instead of showing a
            // broken or placeholder image.
            MissingMediaLabel(Modifier.fillMaxWidth().aspectRatio(PHOTO_ASPECT_RATIO), "Photo unavailable")
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(DiaryInkViolet.copy(alpha = 0.55f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove photo",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun AddTile(label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(PHOTO_TILE_SIZE)
            .clip(RoundedCornerShape(16.dp))
            .background(DiaryLavender.copy(alpha = 0.28f))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = DiaryInkViolet, modifier = Modifier.size(22.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = DiaryInkViolet,
            modifier = Modifier.padding(top = 4.dp)
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
    modifier: Modifier = Modifier
) {
    val isRecording = recording is RecordingState.Recording
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

            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(DiaryLavender.copy(alpha = 0.22f))
                        .clickable(onClick = onStartRecording)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = DiaryInkViolet, modifier = Modifier.size(20.dp))
                    Text("Add voice note", style = MaterialTheme.typography.labelLarge, color = DiaryInkViolet)
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

/**
 * Location row. Shows the resolved place name when the geocoder produced one,
 * the raw coordinates when it did not, and an explicit action (or honest
 * reason) otherwise. Never a hardcoded city.
 */
@Composable
fun DiaryLocationRow(
    place: DiaryAttachment.Place?,
    status: LocationStatus,
    onAdd: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DiarySectionLabel(text = "Location")
        when (status) {
            LocationStatus.REQUESTING -> DiaryPanel(cornerRadius = 18) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Finding your location…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            else -> if (place != null) {
                DiaryPanel(cornerRadius = 18) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = DiaryInkViolet, modifier = Modifier.size(20.dp))
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
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(DiaryLavender.copy(alpha = 0.22f))
                            .clickable(onClick = onAdd)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = DiaryInkViolet, modifier = Modifier.size(20.dp))
                        Text(
                            when (status) {
                                LocationStatus.PERMISSION_DENIED -> "Allow location to attach a place"
                                LocationStatus.PERMISSION_PERMANENTLY_DENIED -> "Enable location in Settings"
                                else -> "Add current location"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = DiaryInkViolet
                        )
                    }
                    if (status == LocationStatus.PERMISSION_PERMANENTLY_DENIED) {
                        Text(
                            "Location permission was permanently denied. Open Settings to grant it.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
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

/**
 * The mood pill, tinted from the *stored* mood string only. Returns without
 * emitting anything when the entry has no mood, so an un-mooded entry simply
 * shows no pill rather than a placeholder one.
 */
@Composable
fun EntryMoodPill(entry: DiaryEntity) {
    val mood = DiaryMoods.fromStored(entry.mood)
    val label = mood?.let { "${it.emoji} ${it.label}" } ?: DiaryMoods.displayLabel(entry.mood)
    if (label.isEmpty()) return
    val color = mood?.let { DiaryMoods.colorOf(it.key) } ?: DiaryInkViolet
    val pastel = mood?.let { DiaryMoods.backgroundOf(it.key) } ?: color.copy(alpha = 0.14f)
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(pastel)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** A selectable mood pill for the composer, using the persisted mood keys. */
@Composable
fun DiaryMoodChip(
    option: DiaryMood,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = DiaryMoods.colorOf(option.key)
    val container = if (selected) DiaryMoods.backgroundOf(option.key) else Color.Transparent
    val borderColor = if (selected) color.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    val labelColor = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(option.emoji, style = MaterialTheme.typography.bodyMedium)
        Text(
            option.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor
        )
    }
}

/** A tag chip, e.g. "Gratitude". */
@Composable
fun ThemeKeywordChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = DiaryInkViolet,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DiaryLavender.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

/** A labelled metadata line, e.g. "Date 26 September 2026". */@Composable
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
private val PHOTO_TILE_SIZE = 84.dp
private const val PHOTO_ASPECT_RATIO = 1f
private val RECORD_DOT_SIZE = 40.dp
private val META_LABEL_WIDTH = 92.dp
