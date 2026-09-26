package com.lifeos.app.ui.diary

import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.io.File
import java.io.FileOutputStream

private val EditorCardShape = RoundedCornerShape(28.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryEditor(
    dayEpochDay: Long,
    editing: DiaryEntity?,
    timeMinutes: Int?,
    photoUris: List<String> = emptyList(),
    tags: List<String> = emptyList(),
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (content: String, mood: String?) -> Unit,
    onDelete: () -> Unit,
    onTimeChange: (Int) -> Unit = {},
    onPhotosChange: (List<String>) -> Unit = {},
    onRemovePhoto: (String) -> Unit = {},
    onTagsChange: (List<String>) -> Unit = {}
) {
    val isEditing = editing != null
    var content by rememberSaveable(editing?.id) { mutableStateOf(editing?.content.orEmpty()) }
    var mood by rememberSaveable(editing?.id) { mutableStateOf(editing?.mood) }
    var showTags by remember { mutableStateOf(false) }
    var tagDraft by remember(tags) { mutableStateOf(tags.joinToString(", ")) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            onPhotosChange((photoUris + uris.map(Uri::toString)).distinct().take(12))
        }
    }

    val cameraPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap ?: return@rememberLauncherForActivityResult
        runCatching {
            val dir = File(context.filesDir, "diary/photos").apply { mkdirs() }
            val file = File(dir, "memory_" + System.currentTimeMillis() + ".jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            onPhotosChange((photoUris + Uri.fromFile(file).toString()).distinct().take(12))
        }
    }

    LaunchedEffect(editing?.id) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BackHandler(onBack = onDismiss)

    val bottomInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
    val canSave = content.isNotBlank() && !saving
    val date = DateTimeUtils.epochDayToLocalDate(dayEpochDay)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .windowInsetsPadding(bottomInsets)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, "Close", tint = DiaryInkViolet)
                }
                Spacer(Modifier.weight(1f))
                EditorSavePill(enabled = canSave) { onSave(content, mood) }
            }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = LifeOSSpacing.screenPadding)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (date == DateTimeUtils.today()) "TODAY" else date.dayOfWeek.name,
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                letterSpacing = 2.4.sp
                            ),
                            color = DiaryInkViolet.copy(alpha = 0.58f)
                        )
                        Text(
                            DateTimeUtils.formatFullDate(date),
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 29.sp,
                                lineHeight = 34.sp
                            ),
                            color = DiaryInkViolet
                        )
                    }
                    MoonDecoration()
                }

                TimePill(timeMinutes ?: 0) {
                    val current = timeMinutes ?: 0
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> onTimeChange(hour * 60 + minute) },
                        current / 60,
                        current % 60,
                        false
                    ).show()
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    "How did the day feel?",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = DiaryInkViolet
                )
                Spacer(Modifier.height(8.dp))
                MoodSelector(
                    selectedKey = mood,
                    onSelect = { mood = if (mood == it) null else it }
                )
                Spacer(Modifier.height(12.dp))

                Box(
                    Modifier.fillMaxWidth().clip(EditorCardShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    BasicTextField(
                        value = content,
                        onValueChange = { if (it.length <= 1000) content = it },
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                            .verticalScroll(rememberScrollState())
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            lineHeight = 27.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(DiaryInkViolet),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxSize()) {
                                if (content.isBlank()) {
                                    Text(
                                        "Write whatever is on your mind…",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 18.sp,
                                            lineHeight = 27.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f)
                                    )
                                }
                                inner()
                            }
                        }
                    )
                    Text(
                        content.length.toString() + "/1000",
                        Modifier.align(Alignment.BottomEnd),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))
                EditorMediaToolbar(
                    onImages = { imagePicker.launch(arrayOf("image/*")) },
                    onCamera = { cameraPicker.launch(null) },
                    onLocation = { },
                    onWeather = { },
                    onTags = { showTags = true }
                )

                if (photoUris.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    PhotoStrip(photoUris, onRemovePhoto) { imagePicker.launch(arrayOf("image/*")) }
                }

                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    TagStrip(tags) { tag -> onTagsChange(tags.filterNot { it == tag }) }
                }

                Spacer(Modifier.height(16.dp))
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoftAction("◉  Add voice") { }
                Spacer(Modifier.weight(1f))
                FilledSaveAction(enabled = canSave) { onSave(content, mood) }
            }
        }
    }

    if (showTags) {
        AlertDialog(
            onDismissRequest = { showTags = false },
            title = { Text("Add tags") },
            text = {
                OutlinedTextField(
                    value = tagDraft,
                    onValueChange = { tagDraft = it },
                    label = { Text("Tags") },
                    placeholder = { Text("Personal, Gratitude, Family") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onTagsChange(tagDraft.split(",").map { it.trim() }
                        .filter { it.isNotBlank() }.distinct().take(8))
                    showTags = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTags = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EditorSavePill(enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape)
            .background(if (enabled) DiaryLavender else DiaryHairline.copy(alpha = 0.45f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Save, null, Modifier.size(18.dp),
            tint = DiaryInkViolet.copy(alpha = if (enabled) 1f else 0.4f))
        Spacer(Modifier.width(7.dp))
        Text("Save memory", style = MaterialTheme.typography.labelLarge,
            color = DiaryInkViolet.copy(alpha = if (enabled) 1f else 0.4f))
    }
}

@Composable
private fun TimePill(minutes: Int, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(DiaryLavender.copy(alpha = 0.7f))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.AccessTime, null, Modifier.size(19.dp), tint = DiaryInkViolet)
        Spacer(Modifier.width(8.dp))
        Text(DateTimeUtils.formatMinutes(minutes), style = MaterialTheme.typography.titleMedium, color = DiaryInkViolet)
        Spacer(Modifier.width(9.dp))
        Text("⌄", color = DiaryInkViolet, fontSize = 20.sp)
    }
}

@Composable
private fun MoonDecoration() {
    Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Text("☾", fontSize = 68.sp, color = DiaryInkViolet.copy(alpha = 0.62f))
        Text("✦", Modifier.padding(start = 68.dp, bottom = 54.dp), fontSize = 17.sp, color = DiaryInkViolet.copy(alpha = 0.45f))
        Text("✦", Modifier.padding(start = 42.dp, top = 56.dp), fontSize = 12.sp, color = DiaryInkViolet.copy(alpha = 0.35f))
    }
}

@Composable
private fun EditorMediaToolbar(
    onImages: () -> Unit, onCamera: () -> Unit, onLocation: () -> Unit,
    onWeather: () -> Unit, onTags: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorToolIcon(Icons.Filled.Image, "Add photos", onImages)
        EditorToolIcon(Icons.Filled.CameraAlt, "Take photo", onCamera)
        EditorToolIcon(Icons.Filled.LocationOn, "Add location", onLocation)
        EditorToolIcon(Icons.Filled.WbSunny, "Add weather", onWeather)
        EditorToolIcon(Icons.Filled.Label, "Add tags", onTags)
    }
}

@Composable
private fun EditorToolIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PhotoStrip(
    photoUris: List<String>,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(photoUris, key = { it }) { uri ->
            Box(Modifier.size(92.dp).clip(RoundedCornerShape(14.dp))) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Diary photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onRemove(uri) },
                    modifier = Modifier.align(Alignment.TopEnd).size(30.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), CircleShape)
                ) {
                    Icon(Icons.Filled.Close, "Remove photo", Modifier.size(16.dp))
                }
            }
        }
        item {
            Box(
                Modifier.size(92.dp).clip(RoundedCornerShape(14.dp))
                    .background(DiaryLavender.copy(alpha = 0.65f))
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Add, null, tint = DiaryInkViolet)
                    Text("Add photo", style = MaterialTheme.typography.labelSmall, color = DiaryInkViolet)
                }
            }
        }
    }
}

@Composable
private fun TagStrip(tags: List<String>, onRemove: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            Row(
                Modifier.clip(CircleShape).background(DiaryLavender)
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tag, style = MaterialTheme.typography.labelMedium, color = DiaryInkViolet)
                Spacer(Modifier.width(6.dp))
                Text("×", Modifier.clickable { onRemove(tag) }, color = DiaryInkViolet)
            }
        }
    }
}

@Composable
private fun SoftAction(text: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = DiaryInkViolet)
    }
}

@Composable
private fun FilledSaveAction(enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape)
            .background(if (enabled) DiaryInkViolet else DiaryHairline)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Save, null, Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.background)
        Spacer(Modifier.width(7.dp))
        Text("Save memory", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.background)
    }
}

@Composable
fun DiaryEditorOverlay(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) +
            slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 14 },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 20 }
    ) { content() }
}
