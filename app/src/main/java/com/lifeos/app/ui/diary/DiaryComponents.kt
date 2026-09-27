package com.lifeos.app.ui.diary
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.DiaryTagInk
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.io.File

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
fun DiaryMetaRow(icon: @Composable () -> Unit, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        icon()
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}

@Composable
fun MissingMediaLabel(modifier: Modifier = Modifier, text: String) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp))
    }
}

private fun formatCoordinates(place: DiaryAttachment.Place): String = String.format(java.util.Locale.getDefault(), "%.4f, %.4f", place.latitude, place.longitude)

@Composable
fun DiaryTagEditor(tags: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit, modifier: Modifier = Modifier) {
    var draft by rememberSaveable { mutableStateOf("") }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DiarySectionLabel("Tags", trailing = {
            Box(Modifier.clip(CircleShape).border(1.dp, DiaryActionViolet.copy(alpha = 0.35f), CircleShape).clickable { showDialog = true }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("+ Add tag", style = MaterialTheme.typography.labelMedium, color = DiaryActionViolet)
            }
        })
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    Row(Modifier.clip(CircleShape).background(DiaryLavender.copy(alpha = 0.64f)).padding(start = 11.dp, end = 4.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tag, style = MaterialTheme.typography.labelMedium, color = DiaryTagInk)
                        Box(Modifier.size(36.dp).clickable { onRemove(tag) }, contentAlignment = Alignment.Center) { Text("×", style = MaterialTheme.typography.titleSmall, color = DiaryActionViolet) }
                    }
                }
            }
        } else Text("No tags yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false; draft = "" },
            title = { Text("Add tag") },
            text = {
                TextField(
                    value = draft, onValueChange = { value -> draft = value.take(40) }, singleLine = true, placeholder = { Text("e.g. Gratitude") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = DiaryActionViolet, unfocusedIndicatorColor = DiaryHairline)
                )
            },
            confirmButton = {
                TextButton(enabled = draft.isNotBlank(), onClick = { onAdd(draft); draft = ""; showDialog = false }) { Text("Add", color = DiaryActionViolet) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false; draft = "" }) { Text("Cancel") } }
        )
    }
}