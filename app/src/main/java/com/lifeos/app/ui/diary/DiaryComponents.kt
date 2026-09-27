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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        // The visible chip is 24dp, but the *touch* target is the full 48dp
        // minimum: a 24dp target is half the accessible size and, on a photo
        // the user is trying to clean up, easy to miss and easy to hit the
        // wrong tile with. The circle is nested and corner-aligned inside the
        // larger target so the drawn size and position are unchanged.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(DiaryInkViolet.copy(alpha = 0.55f)),
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
        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(22.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = DiaryActionViolet,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}


@Composable
fun DiaryPanel(modifier: Modifier = Modifier, cornerRadius: Int = 24, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(DiaryPaperCard)
            .border(1.dp, DiaryHairline, RoundedCornerShape(cornerRadius.dp))
            .padding(LifeOSSpacing.compactPadding),
        content = { content() }
    )
}

@Composable
fun DiarySectionLabel(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = DiaryInkViolet, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DiaryPhotoStrip(photos: List<DiaryAttachment.Photo>, onAdd: () -> Unit, onRemove: (DiaryAttachment.Photo) -> Unit, modifier: Modifier = Modifier, addLabel: String = "Add more photos") {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DiarySectionLabel("Photos", trailing = {
            if (photos.isNotEmpty()) Text(photos.size.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        })
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 4
        ) {
            photos.forEach { photo -> DiaryPhotoTile(photo, onRemove) }
            AddTile(addLabel, onAdd)
        }
    }
}

@Composable
private fun DiaryPhotoTile(photo: DiaryAttachment.Photo, onRemove: (DiaryAttachment.Photo) -> Unit) {
    val context = LocalContext.current
    val exists = remember(photo.filePath) { File(photo.filePath).exists() }
    Box(Modifier.size(92.dp).clip(RoundedCornerShape(16.dp)).background(DiaryLavender.copy(alpha = 0.3f))) {
        if (exists) AsyncImage(
            model = ImageRequest.Builder(context).data(File(photo.filePath)).build(),
            contentDescription = "Diary photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth()
        ) else MissingMediaLabel(Modifier.fillMaxWidth(), "Photo unavailable")
        Box(Modifier.align(Alignment.TopEnd).size(48.dp).clickable { onRemove(photo) }, contentAlignment = Alignment.TopEnd) {
            Box(Modifier.padding(5.dp).size(24.dp).clip(CircleShape).background(DiaryInkViolet.copy(alpha = 0.62f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun AddTile(label: String, onClick: () -> Unit) {
    Column(
        Modifier.size(92.dp).clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, DiaryActionViolet.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
            .background(DiaryLavender.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = DiaryActionViolet, modifier = Modifier.padding(top = 5.dp), maxLines = 2)
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DiaryPhotoStrip(photos: List<DiaryAttachment.Photo>, onAdd: () -> Unit, onRemove: (DiaryAttachment.Photo) -> Unit, modifier: Modifier = Modifier, addLabel: String = "Add more photos") {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DiarySectionLabel("Photos", trailing = {
            if (photos.isNotEmpty()) Text(photos.size.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        })
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 4
        ) {
            photos.forEach { photo -> DiaryPhotoTile(photo, onRemove) }
            AddTile(addLabel, onAdd)
        }
    }
}

@Composable
private fun DiaryPhotoTile(photo: DiaryAttachment.Photo, onRemove: (DiaryAttachment.Photo) -> Unit) {
    val context = LocalContext.current
    val exists = remember(photo.filePath) { File(photo.filePath).exists() }
    Box(Modifier.size(92.dp).clip(RoundedCornerShape(16.dp)).background(DiaryLavender.copy(alpha = 0.3f))) {
        if (exists) AsyncImage(
            model = ImageRequest.Builder(context).data(File(photo.filePath)).build(),
            contentDescription = "Diary photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth()
        ) else MissingMediaLabel(Modifier.fillMaxWidth(), "Photo unavailable")
        Box(Modifier.align(Alignment.TopEnd).size(48.dp).clickable { onRemove(photo) }, contentAlignment = Alignment.TopEnd) {
            Box(Modifier.padding(5.dp).size(24.dp).clip(CircleShape).background(DiaryInkViolet.copy(alpha = 0.62f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun AddTile(label: String, onClick: () -> Unit) {
    Column(
        Modifier.size(92.dp).clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, DiaryActionViolet.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
            .background(DiaryLavender.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = DiaryActionViolet, modifier = Modifier.padding(top = 5.dp), maxLines = 2)
    }
}

@Composable
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
private val PHOTO_TILE_SIZE = 84.dp
private const val PHOTO_ASPECT_RATIO = 1f
private val RECORD_DOT_SIZE = 40.dp
private val META_LABEL_WIDTH = 92.dp

/** Tags with add/remove, persisted in the existing `tagsCsv` column. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DiaryTagEditor(
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Tags",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (draft.isNotBlank()) {
                TextButton(onClick = { onAdd(draft); draft = "" }) {
                    Text("Add tag", color = DiaryActionViolet)
                }
            }
        }

        if (tags.isNotEmpty()) {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(DiaryLavender.copy(alpha = 0.55f))
                            .padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tag, style = MaterialTheme.typography.labelSmall, color = DiaryTagInk)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onRemove(tag) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", style = MaterialTheme.typography.labelSmall, color = DiaryActionViolet)
                        }
                    }
                }
            }
        }

        TextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Add a tag", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = diaryFieldColors()
        )
    }
}

@Composable
private fun diaryFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = DiaryInkViolet,
    unfocusedIndicatorColor = DiaryHairline
)
