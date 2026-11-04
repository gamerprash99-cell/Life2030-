package com.lifeos.app.ui.diary.new

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.media.DiaryAudioPlayer
import com.lifeos.app.core.media.RecordingState
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.domain.model.DiaryMood
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.domain.model.DiaryTextStats
import com.lifeos.app.domain.model.DiaryWeather
import com.lifeos.app.ui.diary.new.DiaryDetailViewModel
import com.lifeos.app.ui.diary.new.DiaryStickers
import com.lifeos.app.ui.diary.new.DiaryThemes
import com.lifeos.app.ui.diary.new.DiaryStickers
import com.lifeos.app.ui.diary.new.DiaryEditorOverlay
import com.lifeos.app.ui.diary.new.DiaryEditorViewModel
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHandwriting
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryTagInk
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.data.repository.WeatherRepository
import com.lifeos.app.core.location.LocationProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import com.lifeos.app.core.media.AudioRecorder
import com.lifeos.app.core.media.AudioPlayback
import com.lifeos.app.core.media.PhotoImporter
import java.io.File

@Composable
fun DiaryNewDetailScreen(
    entryId: String,
    onBack: () -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryDetailViewModel = viewModel(
        key = "diary-new-detail-$entryId",
        factory = LambdaViewModelFactory {
            DiaryDetailViewModel(entryId, locator.diaryRepository, locator.weatherRepository)
        }
    )
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val showEditor by viewModel.showEditor.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val editorViewModel: DiaryEditorViewModel = viewModel(
        key = "diary-new-detail-editor-$entryId",
        factory = LambdaViewModelFactory {
            DiaryEditorViewModel(
                locator.diaryRepository,
                locator.weatherRepository,
                locator.deviceLocationProvider,
                com.lifeos.app.core.media.DiaryAudioRecorder(locator.appContext),
                com.lifeos.app.core.media.DiaryAudioPlayer(),
                com.lifeos.app.core.media.DevicePhotoImporter(locator.appContext)
            )
        }
    )
    val editorState by editorViewModel.state.collectAsStateWithLifecycle()
    val player = remember { DiaryAudioPlayer() }
    val playback by player.state.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { player.release() } }
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMore by remember { mutableStateOf(false) }

    BackHandler(enabled = showEditor, onBack = viewModel::dismissEditor)
    BackHandler(enabled = !showEditor, onBack = onBack)

    LaunchedEffect(entry) { if (entry == null) onBack() }
    LaunchedEffect(entry?.attachmentsJson) { viewModel.refreshWeather() }
    LaunchedEffect(showEditor, entryId) { if (showEditor) editorViewModel.start(entryId) }
    LaunchedEffect(editorState.saveCount) {
        if (editorState.saveCount > 0) {
            editorViewModel.cancelRecording()
            viewModel.dismissEditor()
        }
    }

    Box(Modifier.fillMaxSize().background(DiaryNewCanvas)) {
        entry?.let { current ->
            val date = DateTimeUtils.epochDayToLocalDate(current.dateEpochDay)
            val attachments = remember(current.id, current.attachmentsJson) {
                DiaryAttachments.decode(current.attachmentsJson)
            }
            val photos = remember(attachments) {
                attachments.filterIsInstance<DiaryAttachment.Photo>()
            }
            val voiceNote = remember(attachments) {
                attachments.filterIsInstance<DiaryAttachment.VoiceNote>().lastOrNull()
            }
            val place = remember(attachments) {
                attachments.filterIsInstance<DiaryAttachment.Place>().lastOrNull()
            }
            val themeStyle = remember(attachments) {
                attachments.filterIsInstance<DiaryAttachment.ThemeStyle>().lastOrNull()
            }
            val stickers = remember(attachments) {
                attachments.filterIsInstance<DiaryAttachment.Sticker>()
            }
            val drawing = remember(attachments) {
                attachments.filterIsInstance<DiaryAttachment.Drawing>().lastOrNull()
            }
            val importedAudio = remember(attachments) {
                attachments.filterIsInstance<DiaryAttachment.ImportedAudio>()
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = LifeOSSpacing.scrollBottomBreathingRoom)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(DiaryNewMinTouchTarget)) {
                        Icon(
                            ArrowBack,
                            contentDescription = "Back",
                            tint = DiaryNewInk
                        )
                    }
                    Box(Modifier.fillMaxWidth())
                    IconButton(onClick = { /* edit */ }, modifier = Modifier.size(DiaryNewMinTouchTarget)) {
                        Icon(
                            Edit,
                            contentDescription = "Edit entry",
                            tint = DiaryNewSave
                        )
                    }
                    Box {
                        IconButton(onClick = { showMore = true }, modifier = Modifier.size(DiaryNewMinTouchTarget)) {
                            Icon(
                                MoreVert,
                                contentDescription = "Entry options",
                                tint = DiaryNewInk
                            )
                        }
                        DropdownMenu(
                            expanded = showMore,
                            onDismissRequest = { showMore = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (current.isFavorite) "Remove favorite" else "Favorite",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInk)
                                    )
                                },
                                onClick = { showMore = false; viewModel.toggleFavorite() }
                            )
                            DropdownMenuItem(
                                text = { Text("Share entry", style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInk)) },
                                onClick = { showMore = false; shareNewEntry(context, current, photos) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete entry", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error)) },
                                onClick = { showMore = false; confirmDelete = true }
                            )
                        }
                    }
                }

                Column(Modifier.padding(horizontal = DiaryNewScreenPadding)) {
                    Text(
                        DateTimeUtils.formatDayOfWeek(date),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = DiaryHandwriting,
                            color = DiaryNewInkSoft
                        )
                    )
                    Text(
                        DateTimeUtils.formatFullDate(date),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = DiaryHandwriting,
                            fontWeight = FontWeight.Bold,
                            color = DiaryNewInk
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            DateTimeUtils.formatMinutes(current.timeMinutes),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = DiaryHandwriting,
                                color = DiaryNewInkSoft
                            )
                        )
                        Spacer(Modifier.size(8.dp))
                        current.mood?.takeIf { it.isNotBlank() }?.let { moodKey ->
                            val mood = DiaryMoods.fromStored(moodKey)
                            mood?.let { m ->
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(DiaryNewMoodColor(m))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        m.emoji + " " + m.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DiaryTagInk,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                themeStyle?.let { theme ->
                    DiaryNewPanel(
                        modifier = Modifier.padding(horizontal = DiaryNewScreenPadding),
                        cornerRadius = 16
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                ColorLens,
                                contentDescription = null,
                                tint = DiaryNewInkSoft,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Theme: ${com.lifeos.app.ui.diary.DiaryThemes.byKey(theme.key).name}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                DiaryNewPanel(
                    Modifier.padding(horizontal = DiaryNewScreenPadding),
                    cornerRadius = 16
                ) {
                    Text(
                        current.content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 30.sp,
                            fontFamily = DiaryHandwriting
                        ),
                        color = DiaryNewInk
                    )
                }
                Spacer(Modifier.height(14.dp))

                if (photos.isNotEmpty()) {
                    DiaryNewPhotoRow(photos = photos)
                    Spacer(Modifier.height(14.dp))
                }

                if (stickers.isNotEmpty()) {
                    DiaryNewStickerRow(stickers = stickers)
                    Spacer(Modifier.height(14.dp))
                }

                drawing?.let { draw ->
                    DiaryNewPanel(
                        Modifier.padding(horizontal = DiaryNewScreenPadding),
                        cornerRadius = 16
                    ) {
                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Drawing",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = DiaryNewInkSoft
                                )
                            )
                            Box(
                                Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DiaryNewLavender.copy(alpha = 0.3f))
                                    .border(1.dp, DiaryNewBorder, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(File(draw.filePath))
                                        .build(),
                                    contentDescription = "Diary drawing",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(140.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                if (voiceNote != null || importedAudio.isNotEmpty()) {
                    DiaryNewAudioRow(
                        voiceNote = voiceNote,
                        importedAudio = importedAudio,
                        playback = playback,
                        onTogglePlayback = { filePath ->
                            editorViewModel.toggleAudioPlayback(filePath)
                        },
                        onRemoveVoiceNote = { viewModel.removeAttachment(voiceNote!!.filePath) },
                        onRemoveImportedAudio = { filePath ->
                            viewModel.removeAttachment(filePath)
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                }

                if (place != null || weather !is DiaryWeather.Unavailable) {
                    Spacer(Modifier.height(14.dp))
                    DiaryNewPanel(
                        Modifier.padding(horizontal = DiaryNewScreenPadding),
                        cornerRadius = 16
                    ) {
                        if (place != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    LocationOn,
                                    contentDescription = null,
                                    tint = DiaryNewSave,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.size(10.dp))
                                Column(Modifier.fillMaxWidth()) {
                                    Text(
                                        place.placeName.takeIf { it.isNotBlank() } ?: "Saved location",
                                        fontWeight = FontWeight.SemiBold,
                                        color = DiaryNewInk
                                    )
                                    Text(
                                        "Current location",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DiaryNewInkSoft
                                    )
                                }
                            }
                            if (weather !is DiaryWeather.Unavailable) Spacer(Modifier.height(10.dp))
                        }
                        if (weather !is DiaryWeather.Unavailable) {
                            DiaryNewWeatherRow(weather)
                        }
                    }
                }

                val tags = remember(current.id, current.tagsCsv) {
                    current.tagsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }
                }
                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    DiaryNewPanel(
                        Modifier.padding(horizontal = DiaryNewScreenPadding),
                        cornerRadius = 16
                    ) {
                        DiaryNewSectionLabel("Tags")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tags.forEach { tag ->
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(DiaryNewLavender.copy(alpha = 0.66f))
                                        .padding(horizontal = 11.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        tag,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DiaryTagInk
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                DiaryNewPanel(
                    Modifier.padding(horizontal = DiaryNewScreenPadding),
                    cornerRadius = 16
                ) {
                    Icon(
                        icon = DateRange,
                        label = "Date",
                        value = DateTimeUtils.formatFullDate(date)
                    )
                    Spacer(Modifier.height(14.dp))
                    DiaryNewMetaRow(
                        icon = AccessTime,
                        label = "Time",
                        value = DateTimeUtils.formatMinutes(current.timeMinutes)
                    )
                    Spacer(Modifier.height(14.dp))
                    DiaryNewMetaRow(
                        icon = ContentCopy,
                        label = "Word count",
                        value = "${DiaryTextStats.wordCount(current.content)} words"
                    )
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = DiaryNewScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DiaryNewDetailAction(
                        Modifier.fillMaxWidth(),
                        "Share",
                        Share,
                        DiaryNewSave
                    ) { shareNewEntry(context, current, photos) }
                    DiaryNewDetailAction(
                        Modifier.fillMaxWidth(),
                        "Copy",
                        ContentCopy,
                        DiaryNewSave
                    ) {
                        copyNewEntry(context, current)
                        scope.launch { snackbarHostState.showSnackbar("Entry copied") }
                    }
                    DiaryNewDetailAction(
                        Modifier.fillMaxWidth(),
                        "Delete",
                        Delete,
                        MaterialTheme.colorScheme.error
                    ) { confirmDelete = true }
                    DiaryNewDetailAction(
                        Modifier.fillMaxWidth(),
                        if (current.isFavorite) "Unfavorite" else "Favorite",
                        if (current.isFavorite) Star else StarBorder,
                        DiaryNewSave
                    ) { viewModel.toggleFavorite() }
                }
                Spacer(Modifier.height(18.dp))
            }
        }

        DiaryEditorOverlay(visible = showEditor) {
            entry?.let { current ->
                DiaryEditorScreen(
                    editorViewModel = editorViewModel,
                    dayEpochDay = current.dateEpochDay,
                    editing = current,
                    timeMinutes = editorState.timeMinutes,
                    title = editorState.title,
                    onTitleChange = editorViewModel::onTitleChange,
                    content = editorState.content,
                    onContentChange = editorViewModel::onContentChange,
                    mood = editorState.mood,
                    onMoodChange = editorViewModel::onMoodChange,
                    onDateChange = editorViewModel::onDateChange,
                    onTimeChange = editorViewModel::onTimeChange,
                    canSave = editorState.canSave,
                    onDismiss = { editorViewModel.cancelRecording(); viewModel.dismissEditor() },
                    onSave = editorViewModel::save,
                    onDelete = { editorViewModel.cancelRecording(); viewModel.dismissEditor(); confirmDelete = true },
                    attachments = { /* rendered inline by DiaryEditorScreen */ },
                    editorActions = { /* rendered inline by DiaryEditorScreen */ }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = DiaryNewScreenPadding)
                .padding(bottom = 12.dp)
        )
    }

    entry?.let { current ->
        if (confirmDelete) {
            DiaryNewDeleteDialog(
                dayEpochDay = current.dateEpochDay,
                timeMinutes = current.timeMinutes,
                onConfirm = {
                    confirmDelete = false
                    viewModel.delete(entryId)
                    onBack()
                },
                onDismiss = { confirmDelete = false }
            )
        }
    }
}

@Composable
private fun DiaryNewPhotoRow(photos: List<DiaryAttachment.Photo>) {
    val context = LocalContext.current
    DiaryNewSectionLabel("Photos")
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        photos.take(3).forEach { photo ->
            val exists = remember(photo.filePath) { File(photo.filePath).exists() }
            Box(
                Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DiaryNewLavender.copy(alpha = 0.26f))
            ) {
                if (exists) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(photo.filePath))
                            .build(),
                        contentDescription = "Diary photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        "Photo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryNewStickerRow(stickers: List<DiaryAttachment.Sticker>) {
    DiaryNewSectionLabel("Stickers")
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stickers.forEach { sticker ->
            val stickerDef = com.lifeos.app.ui.diary.new.DiaryStickers.byKey(sticker.assetKey)
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(DiaryNewLavender.copy(alpha = 0.7f))
                    .border(1.dp, DiaryNewBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stickerDef.glyph,
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun DiaryNewAudioRow(
    voiceNote: com.lifeos.app.domain.model.DiaryAttachment.VoiceNote?,
    importedAudio: List<DiaryAttachment.ImportedAudio>,
    playback: com.lifeos.app.core.media.PlaybackState,
    onTogglePlayback: (String) -> Unit,
    onRemoveVoiceNote: () -> Unit,
    onRemoveImportedAudio: (String) -> Unit
) {
    DiaryNewSectionLabel("Audio")
    Spacer(Modifier.height(8.dp))
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        voiceNote?.let { note ->
            DiaryNewAudioItem(
                label = "Voice note",
                duration = note.durationMillis,
                isPlaying = playback is com.lifeos.app.core.media.PlaybackState.Playing &&
                    playback.filePath == note.filePath,
                onTogglePlayback = { onTogglePlayback(note.filePath) },
                onRemove = onRemoveVoiceNote
            )
        }
        importedAudio.forEach { audio ->
            DiaryNewAudioItem(
                label = audio.displayName,
                duration = audio.durationMillis,
                isPlaying = playback is com.lifeos.app.core.media.PlaybackState.Playing &&
                    playback.filePath == audio.filePath,
                onTogglePlayback = { onTogglePlayback(audio.filePath) },
                onRemove = { onRemoveImportedAudio(audio.filePath) }
            )
        }
    }
}

@Composable
private fun DiaryNewAudioItem(
    label: String,
    duration: Long,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = DiaryNewSave,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryNewInk
                )
            )
            Text(
                formatDuration(duration),
                style = MaterialTheme.typography.labelSmall.copy(color = DiaryNewInkSoft)
            )
        }
        Spacer(Modifier.fillMaxWidth())
        IconButton(onClick = onTogglePlayback) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = DiaryNewSave
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Delete,
                contentDescription = "Remove audio",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DiaryNewWeatherRow(weather: DiaryWeather) {
    when (weather) {
        is DiaryWeather.Available -> {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    WbSunny,
                    contentDescription = null,
                    tint = DiaryNewSave,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${weather.temperatureCelsius.toInt()}°C · ${weather.condition}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft)
                )
            }
        }
        else -> Unit
    }
}

@Composable
private fun DiaryNewMetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = DiaryNewSave, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = DiaryNewInk
            )
        )
        Spacer(Modifier.fillMaxWidth())
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft)
        )
    }
}

private fun shareNewEntry(context: Context, entry: DiaryEntity, photos: List<DiaryAttachment.Photo>) {
    val text = buildString {
        entry.title?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
        appendLine(entry.content)
        appendLine()
        append(
            "${DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(entry.dateEpochDay))} at " +
                "${DateTimeUtils.formatMinutes(entry.timeMinutes)}"
        )
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    photos.firstOrNull()?.let { photo ->
        val uri = runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(photo.filePath)
            )
        }.getOrNull()
        if (uri != null) {
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(intent, "Share entry"))
}

private fun copyNewEntry(context: Context, entry: DiaryEntity) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Diary entry", entry.content))
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
private fun DiaryNewDetailAction(
    modifier: Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(DiaryNewPaper)
            .border(1.dp, DiaryNewBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(7.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}
