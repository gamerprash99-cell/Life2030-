package com.lifeos.app.ui.diary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.media.DiaryAudioPlayer
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.WeatherRepository
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.domain.model.DiaryTextStats
import com.lifeos.app.domain.model.DiaryWeather
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Detail state for a single entry. The entry itself is observed straight from
 * Room, so a change made in the composer (or a favourite toggled here) is
 * reflected without any manual cache.
 */
class DiaryDetailViewModel(
    private val entryId: String,
    private val diaryRepository: DiaryRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    val entry: StateFlow<DiaryEntity?> = diaryRepository.observeAll()
        .map { all -> all.firstOrNull { it.id == entryId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _weather = MutableStateFlow<DiaryWeather>(
        DiaryWeather.Unavailable(DiaryWeather.UnavailableReason.NO_LOCATION)
    )
    val weather: StateFlow<DiaryWeather> = _weather

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting

    /** Loads the weather for whatever place the entry carries. */
    fun refreshWeather() {
        viewModelScope.launch {
            val place = entry.value?.let { DiaryAttachments.place(DiaryAttachments.decode(it.attachmentsJson)) }
            _weather.value = weatherRepository.weatherFor(place)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch { diaryRepository.toggleFavorite(entryId) }
    }

    /**
     * Removes a photo or voice note from this entry in place. [filePath] is the
     * attachment the user actually tapped, so a stale UI cannot remove a
     * different one; the repository no-ops on an unknown path.
     */
    fun removeAttachment(filePath: String) {
        viewModelScope.launch { diaryRepository.removeAttachment(entryId, filePath) }
    }

    /** Returns true once the row is really gone from the database. */
    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            diaryRepository.delete(entryId)
            _isDeleting.value = false
            onDeleted()
        }
    }
}

/**
 * Full-page view of one diary entry: its photos, place, weather, tags and the
 * real derived metadata (date, time, word count), plus the Share / Copy /
 * Delete / Favourite actions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryDetailScreen(
    entryId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit
) {
    val context = LocalContext.current
    val locator = LocalServiceLocator.current
    val player = remember { DiaryAudioPlayer() }

    val viewModel: DiaryDetailViewModel = viewModel(
        key = "diary-detail-$entryId",
        factory = LambdaViewModelFactory {
            DiaryDetailViewModel(entryId, locator.diaryRepository, locator.weatherRepository)
        }
    )

    val entry by viewModel.entry.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    // Leaving composition must not leave an audio decoder open.
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    val current = entry
    if (current == null) {
        if (!isDeleting) {
            // The row is gone (deleted here, or by a restore) — leave rather
            // than render an empty shell.
            LaunchedEffect(Unit) { onBack() }
        }
        return
    }

    LaunchedEffect(current.id, current.attachmentsJson) { viewModel.refreshWeather() }

    val attachments = remember(current.id, current.attachmentsJson) {
        DiaryAttachments.decode(current.attachmentsJson)
    }
    val photos = remember(attachments) { DiaryAttachments.photos(attachments) }
    val voiceNote = remember(attachments) { DiaryAttachments.voiceNote(attachments) }
    val tags = remember(current.id, current.tagsCsv) { splitTags(current.tagsCsv) }
    val playback by player.state.collectAsState()

    Box(Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = LifeOSSpacing.screenPadding,
                end = LifeOSSpacing.screenPadding,
                top = 8.dp,
                bottom = LifeOSSpacing.sectionSpacing
            ),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            item(key = "head") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(current.dateEpochDay)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            DateTimeUtils.formatDayOfWeek(current.dateEpochDay.let {
                                DateTimeUtils.epochDayToLocalDate(it)
                            }).lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton48(
                            onClick = viewModel::toggleFavorite,
                            description = if (current.isFavorite) "Remove from favourites" else "Mark as favourite"
                        ) {
                            Icon(
                                imageVector = if (current.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = null,
                                tint = if (current.isFavorite) DiaryInkViolet else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    EntryMoodPill(entry = current)
                }
            }

            current.title?.takeIf { it.isNotBlank() }?.let { title ->
                item(key = "title") {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }

            item(key = "body") {
                Text(
                    current.content,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.35f
                )
            }

            if (photos.isNotEmpty()) {
                item(key = "photos") {
                    DiaryPhotoStrip(
                        photos = photos,
                        onAdd = { onEdit(current.id) },
                        onRemove = { photo -> viewModel.removeAttachment(photo.filePath) }
                    )
                }
            }

            if (voiceNote != null) {
                item(key = "voice") {
                    DiaryVoiceNoteRow(
                        voiceNote = voiceNote,
                        recording = com.lifeos.app.core.media.RecordingState.Idle,
                        playback = playback,
                        onStartRecording = {},
                        onStopRecording = {},
                        onCancelRecording = {},
                        onTogglePlayback = {
                            if (playback is com.lifeos.app.core.media.PlaybackState.MissingFile) {
                                scope.launch { snackbarHostState.showSnackbar("That voice note is no longer on this device.") }
                            } else {
                                player.toggle(voiceNote.filePath)
                            }
                        },
                        onRemove = {
                            // Stop playback first, otherwise we would be holding
                            // a decoder open for a file we are about to delete.
                            player.stop()
                            viewModel.removeAttachment(voiceNote.filePath)
                        }
                    )
                }
            }

            if (tags.isNotEmpty()) {
                item(key = "tags") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DiarySectionLabel("Tags")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tags.forEach { tag -> ThemeKeywordChip(tag) }
                        }
                    }
                }
            }

            item(key = "context") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DiarySectionLabel("Context")
                    val place = DiaryAttachments.place(attachments)
                    if (place != null) {
                        DiaryMetaRow(
                            icon = { Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            label = "Location",
                            value = place.placeName.takeIf { it.isNotBlank() } ?: formatPlaceCoordinates(place)
                        )
                    }
                    DiaryWeatherRow(weather = weather)
                }
            }

            item(key = "meta") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DiarySectionLabel("Details")
                    DiaryMetaRow(
                        icon = { Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = "Date",
                        value = DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(current.dateEpochDay))
                    )
                    DiaryMetaRow(
                        icon = { Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = "Time",
                        value = DateTimeUtils.formatMinutes(current.timeMinutes)
                    )
                    DiaryMetaRow(
                        icon = { Icon(Icons.Filled.TextFields, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = "Word count",
                        // Derived from the stored body, so it can never drift.
                        value = "${DiaryTextStats.wordCount(current.content)} words"
                    )
                }
            }

            item(key = "actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DetailAction(Icons.Filled.Share, "Share", onClick = { shareEntry(context, current, photos) })
                    DetailAction(Icons.Filled.ContentCopy, "Copy", onClick = {
                        copyEntry(context, current)
                        scope.launch { snackbarHostState.showSnackbar("Entry copied") }
                    })
                    DetailAction(
                        Icons.Filled.Edit,
                        "Edit",
                        onClick = { onEdit(current.id) }
                    )
                    DetailAction(
                        Icons.Filled.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { confirmDelete = true }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp)
        )

        if (isDeleting) {
            Box(
                modifier = Modifier.fillMaxWidth().background(DiaryLavender.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DiaryInkViolet)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("This permanently removes the entry and its photos and audio from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DetailAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(ACTION_COLUMN_WIDTH)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/**
 * Shares the entry through the system chooser. The text is plain diary content
 * the user explicitly chose to share; photos are handed over as single-purpose
 * FileProvider URIs with a one-shot read grant, so no storage permission is
 * involved and no other app gets a lasting path.
 */
private fun shareEntry(context: Context, entry: DiaryEntity, photos: List<DiaryAttachment.Photo>) {
    val text = buildString {
        entry.title?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
        appendLine(entry.content)
        appendLine()
        append(
            "${DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(entry.dateEpochDay))} " +
                "at ${DateTimeUtils.formatMinutes(entry.timeMinutes)}"
        )
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    photos.firstOrNull()?.let { photo ->
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(photo.filePath))
        }.getOrNull()
        if (uri != null) {
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(
        Intent.createChooser(intent, "Share entry").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

/** Copies the entry body (plus title) to the system clipboard, on-device. */
private fun copyEntry(context: Context, entry: DiaryEntity) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val text = buildString {
        entry.title?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
        append(entry.content)
    }
    clipboard.setPrimaryClip(ClipData.newPlainText("Diary entry", text))
}

private fun formatPlaceCoordinates(place: DiaryAttachment.Place): String =
    String.format(java.util.Locale.getDefault(), "%.4f, %.4f", place.latitude, place.longitude)

private fun splitTags(csv: String): List<String> = csv.split(',')
    .map { it.trim() }
    .filter { it.isNotBlank() }

private val ACTION_COLUMN_WIDTH = 64.dp
