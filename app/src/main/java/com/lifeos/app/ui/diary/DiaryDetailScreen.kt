package com.lifeos.app.ui.diary

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.media.DiaryAudioPlayer
import com.lifeos.app.core.media.DiaryAudioRecorder
import com.lifeos.app.core.media.DevicePhotoImporter
import com.lifeos.app.core.media.RecordingState
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.domain.model.DiaryTextStats
import com.lifeos.app.domain.model.DiaryWeather
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.DiaryTagInk
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.data.repository.WeatherRepository
import com.lifeos.app.core.location.LocationProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lifeos.app.core.media.AudioRecorder
import com.lifeos.app.core.media.AudioPlayback
import com.lifeos.app.core.media.PhotoImporter
import java.io.File

class DiaryDetailViewModel(
    private val entryId: String,
    private val diaryRepository: DiaryRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {
    val entry: StateFlow<DiaryEntity?> = diaryRepository.observeAll().map { all -> all.firstOrNull { it.id == entryId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _showEditor = MutableStateFlow(false)
    val showEditor: StateFlow<Boolean> = _showEditor
    fun startEdit() { _showEditor.value = true }
    fun dismissEditor() { _showEditor.value = false }
    fun delete(id: String) = viewModelScope.launch { withContext(NonCancellable) { diaryRepository.delete(id) } }
    fun toggleFavorite() = viewModelScope.launch { diaryRepository.toggleFavorite(entryId) }
    fun removeAttachment(filePath: String) = viewModelScope.launch { diaryRepository.removeAttachment(entryId, filePath) }
    private val _weather = MutableStateFlow<DiaryWeather>(DiaryWeather.Unavailable(DiaryWeather.UnavailableReason.NO_LOCATION))
    val weather: StateFlow<DiaryWeather> = _weather
    fun refreshWeather() {
        viewModelScope.launch {
            val stored = entry.value ?: return@launch
            val place = DiaryAttachments.place(DiaryAttachments.decode(stored.attachmentsJson))
            _weather.value = weatherRepository.weatherFor(place)
        }
    }
}

@Composable
fun DiaryDetailScreen(entryId: String, onBack: () -> Unit) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryDetailViewModel = viewModel(key = "diary-detail-$entryId", factory = LambdaViewModelFactory { DiaryDetailViewModel(entryId, locator.diaryRepository, locator.weatherRepository) })
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val showEditor by viewModel.showEditor.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val editorViewModel: DiaryEditorViewModel = viewModel(key = "diary-detail-editor-$entryId", factory = LambdaViewModelFactory {
        DiaryEditorViewModel(locator.diaryRepository, locator.weatherRepository, locator.deviceLocationProvider, DiaryAudioRecorder(locator.appContext), DiaryAudioPlayer(), DevicePhotoImporter(locator.appContext))
    })
    val editorState by editorViewModel.state.collectAsStateWithLifecycle()
    val player = remember { DiaryAudioPlayer() }
    val playback by player.state.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { player.release() } }
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showMore by remember { mutableStateOf(false) }
    BackHandler(enabled = showEditor, onBack = viewModel::dismissEditor)
    BackHandler(enabled = !showEditor, onBack = onBack)
    LaunchedEffect(entry) { if (entry == null) onBack() }
    LaunchedEffect(entry?.attachmentsJson) { viewModel.refreshWeather() }
    LaunchedEffect(showEditor, entryId) { if (showEditor) editorViewModel.start(entryId) }
    LaunchedEffect(editorState.saveCount) { if (editorState.saveCount > 0) { editorViewModel.cancelRecording(); viewModel.dismissEditor() } }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        entry?.let { current ->
            val date = DateTimeUtils.epochDayToLocalDate(current.dateEpochDay)
            val attachments = remember(current.id, current.attachmentsJson) { DiaryAttachments.decode(current.attachmentsJson) }
            val photos = remember(attachments) { DiaryAttachments.photos(attachments) }
            val voiceNote = remember(attachments) { DiaryAttachments.voiceNote(attachments) }
            val place = remember(attachments) { DiaryAttachments.place(attachments) }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = LifeOSSpacing.fabContentClearance)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DiaryInkViolet) }
                    Box(Modifier.weight(1f))
                    IconButton(onClick = viewModel::startEdit, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.Edit, contentDescription = "Edit memory", tint = DiaryActionViolet) }
                    Box {
                        IconButton(onClick = { showMore = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.MoreVert, contentDescription = "Memory options", tint = DiaryInkViolet) }
                        DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                            DropdownMenuItem(text = { Text(if (current.isFavorite) "Remove favorite" else "Favorite") }, onClick = { showMore = false; viewModel.toggleFavorite() })
                            DropdownMenuItem(text = { Text("Share memory") }, onClick = { showMore = false; shareEntry(context, current, photos) })
                            DropdownMenuItem(text = { Text("Delete memory") }, onClick = { showMore = false; confirmDelete = true })
                        }
                    }
                }

                Column(Modifier.padding(horizontal = LifeOSSpacing.screenPadding)) {
                    Text(DateTimeUtils.formatDayOfWeek(date), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateTimeUtils.formatFullDate(date), style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif), fontWeight = FontWeight.Bold, color = DiaryInkViolet)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(DateTimeUtils.formatMinutes(current.timeMinutes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.size(8.dp))
                        current.mood?.takeIf { it.isNotBlank() }?.let { mood ->
                            Box(Modifier.clip(CircleShape).background(DiaryMoods.backgroundOf(mood)).padding(horizontal = 10.dp, vertical = 6.dp)) { Text(DiaryMoods.fromStored(mood)?.let { it.emoji + " " + it.label } ?: mood, style = MaterialTheme.typography.labelMedium, color = DiaryTagInk, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                DiaryPanel(Modifier.padding(horizontal = LifeOSSpacing.screenPadding), cornerRadius = 28) {
                    Text(current.content, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp), color = MaterialTheme.colorScheme.onSurface)
                    if (photos.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        DetailPhotoRow(photos = photos, onAddMore = viewModel::startEdit)
                    } else {
                        Spacer(Modifier.height(16.dp))
                        DetailAddPhotoTile(onClick = viewModel::startEdit)
                    }
                }

                if (place != null || weather !is DiaryWeather.Unavailable) {
                    Spacer(Modifier.height(14.dp))
                    DiaryPanel(Modifier.padding(horizontal = LifeOSSpacing.screenPadding), cornerRadius = 22) {
                        if (place != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.size(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(place.placeName.takeIf { it.isNotBlank() } ?: "Saved location", fontWeight = FontWeight.SemiBold, color = DiaryInkViolet)
                                    Text("Current location", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (weather !is DiaryWeather.Unavailable) Spacer(Modifier.height(10.dp))
                        }
                        if (weather !is DiaryWeather.Unavailable) DiaryWeatherRow(weather)
                    }
                }

                val tags = remember(current.id, current.tagsCsv) { current.tagsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() } }
                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    DiaryPanel(Modifier.padding(horizontal = LifeOSSpacing.screenPadding), cornerRadius = 22) {
                        DiarySectionLabel("Tags")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tags.forEach { tag -> Box(Modifier.clip(CircleShape).background(DiaryLavender.copy(alpha = 0.66f)).padding(horizontal = 11.dp, vertical = 7.dp)) { Text(tag, style = MaterialTheme.typography.labelMedium, color = DiaryTagInk) } }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                DiaryPanel(Modifier.padding(horizontal = LifeOSSpacing.screenPadding), cornerRadius = 22) {
                    DiaryMetaRow({ Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(18.dp)) }, "Date", DateTimeUtils.formatFullDate(date))
                    Spacer(Modifier.height(14.dp))
                    DiaryMetaRow({ Icon(Icons.Filled.Schedule, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(18.dp)) }, "Time", DateTimeUtils.formatMinutes(current.timeMinutes))
                    Spacer(Modifier.height(14.dp))
                    DiaryMetaRow({ Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = DiaryActionViolet, modifier = Modifier.size(18.dp)) }, "Word count", "${DiaryTextStats.wordCount(current.content)} words")
                }

                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailAction("Share", Icons.Filled.Share, DiaryActionViolet) { shareEntry(context, current, photos) }
                    DetailAction("Copy", Icons.Filled.ContentCopy, DiaryActionViolet) { copyEntry(context, current); kotlinx.coroutines.MainScope().launch { snackbarHostState.showSnackbar("Memory copied") } }
                    DetailAction("Delete", Icons.Filled.Delete, MaterialTheme.colorScheme.error) { confirmDelete = true }
                    DetailAction("Favorite", if (current.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder, DiaryActionViolet) { viewModel.toggleFavorite() }
                }
                Spacer(Modifier.height(18.dp))
            }
        }

        DiaryEditorOverlay(visible = showEditor) {
            entry?.let { current -> DiaryEditor(
                dayEpochDay = current.dateEpochDay, editing = current, timeMinutes = editorState.timeMinutes, content = editorState.content, onContentChange = editorViewModel::onContentChange, mood = editorState.mood, onMoodChange = editorViewModel::onMoodChange, onDateChange = editorViewModel::onDateChange, onTimeChange = editorViewModel::onTimeChange, canSave = editorState.canSave, onDismiss = { editorViewModel.cancelRecording(); viewModel.dismissEditor() }, onSave = editorViewModel::save, onDelete = { editorViewModel.cancelRecording(); viewModel.dismissEditor(); confirmDelete = true }, attachments = { DiaryEditorAttachments(editorViewModel) }) }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = LifeOSSpacing.screenPadding).padding(bottom = 12.dp))
    }

    entry?.let { current -> if (confirmDelete) MemoryDeleteDialog(dayEpochDay = current.dateEpochDay, timeMinutes = current.timeMinutes, onConfirm = { confirmDelete = false; viewModel.delete(entryId); onBack() }, onDismiss = { confirmDelete = false }) }
}

@Composable
private fun DetailPhotoRow(photos: List<DiaryAttachment.Photo>, onAddMore: () -> Unit) {
    val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        photos.take(3).forEach { photo ->
            val exists = remember(photo.filePath) { File(photo.filePath).exists() }
            Box(Modifier.size(82.dp).clip(RoundedCornerShape(14.dp)).background(DiaryLavender.copy(alpha = 0.26f))) {
                if (exists) AsyncImage(model = ImageRequest.Builder(context).data(File(photo.filePath)).build(), contentDescription = "Diary photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Text("Photo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
            }
        }
        Box(Modifier.size(82.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, DiaryActionViolet.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).clickable(onClick = onAddMore), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Add, contentDescription = null, tint = DiaryActionViolet); Text("Add more photos", style = MaterialTheme.typography.labelSmall, color = DiaryActionViolet, textAlign = TextAlign.Center) }
        }
    }
}

@Composable
private fun DetailAddPhotoTile(onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, DiaryActionViolet.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text("+ Add photos", color = DiaryActionViolet, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(DiaryPaperCard).border(1.dp, DiaryHairline, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(7.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

private fun shareEntry(context: Context, entry: DiaryEntity, photos: List<DiaryAttachment.Photo>) {
    val text = buildString { entry.title?.takeIf { it.isNotBlank() }?.let { appendLine(it) }; appendLine(entry.content); appendLine(); append("${DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(entry.dateEpochDay))} at ${DateTimeUtils.formatMinutes(entry.timeMinutes)}") }
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    photos.firstOrNull()?.let { photo ->
        val uri = runCatching { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(photo.filePath)) }.getOrNull()
        if (uri != null) { intent.type = "image/*"; intent.putExtra(Intent.EXTRA_STREAM, uri); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    context.startActivity(Intent.createChooser(intent, "Share memory"))
}

private fun copyEntry(context: Context, entry: DiaryEntity) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Diary memory", entry.content))
}