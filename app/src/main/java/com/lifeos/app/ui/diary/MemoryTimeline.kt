package com.lifeos.app.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.DiaryTagInk

/** A rounded memory card from the supplied reference, preserving the existing timeline callbacks. */
@Composable
fun MemoryMoment(
    entry: DiaryEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasMood = !entry.mood.isNullOrBlank()
    val selectedMood = DiaryMoods.fromStored(entry.mood)
    val attachments = remember(entry.id, entry.attachmentsJson) { DiaryAttachments.decode(entry.attachmentsJson) }
    val photos = remember(attachments) { DiaryAttachments.photos(attachments) }
    var showMenu by remember(entry.id) { mutableStateOf(false) }

    Row(modifier = modifier.fillMaxWidth()) {
        Box(Modifier.width(58.dp), contentAlignment = Alignment.TopEnd) {
            Text(
                DateTimeUtils.formatMinutes(entry.timeMinutes),
                style = MaterialTheme.typography.labelMedium,
                color = DiaryInkViolet.copy(alpha = 0.68f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, end = 8.dp)
            )
        }

        MemorySpine(entry.mood.takeIf { hasMood }, isFirst, isLast)
        Spacer(Modifier.width(6.dp))

        Column(Modifier.weight(1f).padding(bottom = 8.dp)) {
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(DiaryPaperCard)
                    .border(1.dp, DiaryHairline.copy(alpha = 0.72f), RoundedCornerShape(24.dp))
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedMood != null) {
                            Box(Modifier.clip(CircleShape).background(selectedMood.halo).padding(horizontal = 11.dp, vertical = 7.dp)) {
                                Text(
                                    selectedMood.emoji + " " + selectedMood.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DiaryTagInk,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text("Memory", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(Modifier.weight(1f))
                        Box(Modifier.size(40.dp).clickable { showMenu = true }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Memory options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text("Edit memory") }, onClick = { showMenu = false; onEdit() })
                                DropdownMenuItem(text = { Text("Delete memory") }, onClick = { showMenu = false; onDelete() })
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(entry.content, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp), color = MaterialTheme.colorScheme.onSurface)

                    val visiblePhotos = photos.take(3)
                    if (visiblePhotos.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        MemoryPhotoPreviewRow(visiblePhotos, (photos.size - visiblePhotos.size).coerceAtLeast(0))
                    }

                    val tags = remember(entry.id, entry.tagsCsv) { entryTags(entry) }
                    if (tags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            tags.forEach { tag ->
                                Box(Modifier.clip(CircleShape).background(DiaryLavender.copy(alpha = 0.58f)).padding(horizontal = 9.dp, vertical = 5.dp)) {
                                    Text(tag, style = MaterialTheme.typography.labelSmall, color = DiaryTagInk)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryPhotoPreviewRow(photos: List<DiaryAttachment.Photo>, additionalCount: Int) {
    val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        photos.forEach { photo ->
            val exists = remember(photo.filePath) { java.io.File(photo.filePath).exists() }
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(DiaryLavender.copy(alpha = 0.28f))) {
                if (exists) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(java.io.File(photo.filePath)).build(),
                        contentDescription = "Diary photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Photo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
        if (additionalCount > 0) {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(DiaryLavender.copy(alpha = 0.42f)).border(1.dp, DiaryActionViolet.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("+" + additionalCount, style = MaterialTheme.typography.titleMedium, color = DiaryActionViolet, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MemorySpine(moodKey: String?, isFirst: Boolean, isLast: Boolean) {
    Column(Modifier.width(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(1.dp).height(if (isFirst) 4.dp else 8.dp).background(DiaryHairline))
        if (moodKey != null) MoodDot(moodKey, diameter = 16)
        else Box(Modifier.size(7.dp).background(DiaryHairline, CircleShape))
        Box(Modifier.width(1.dp).height(if (isLast) 6.dp else 12.dp).background(DiaryHairline))
    }
}

private fun entryTags(entry: DiaryEntity): List<String> = entry.tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3)
