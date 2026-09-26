package com.lifeos.app.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/** Full-page view of one diary entry. */
class DiaryDetailViewModel(
    private val entryId: String,
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    val entry: StateFlow<DiaryEntity?> = diaryRepository.observeAll()
        .map { all -> all.firstOrNull { it.id == entryId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showEditor = MutableStateFlow(false)
    val showEditor: StateFlow<Boolean> = _showEditor
    private val _editorTimeMinutes = MutableStateFlow<Int?>(null)
    val editorTimeMinutes: StateFlow<Int?> = _editorTimeMinutes
    private val _editorPhotoUris = MutableStateFlow<List<String>>(emptyList())
    val editorPhotoUris: StateFlow<List<String>> = _editorPhotoUris
    private val _editorTags = MutableStateFlow<List<String>>(emptyList())
    val editorTags: StateFlow<List<String>> = _editorTags
    private val _editorAudioUri = MutableStateFlow<String?>(null)
    val editorAudioUri: StateFlow<String?> = _editorAudioUri
    private val _editorLocation = MutableStateFlow<String?>(null)
    val editorLocation: StateFlow<String?> = _editorLocation
    private val _editorWeather = MutableStateFlow<String?>(null)
    val editorWeather: StateFlow<String?> = _editorWeather

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    fun startEdit() {
        val current = entry.value ?: return
        val attachments = decode(current.attachmentsJson)
        _editorTimeMinutes.value = current.timeMinutes
        _editorPhotoUris.value = attachments.filterNot { it.startsWith("audio:") || it.startsWith("meta:") }
        _editorAudioUri.value = attachments.firstOrNull { it.startsWith("audio:") }?.removePrefix("audio:")
        _editorLocation.value = attachments.firstOrNull { it.startsWith("meta:location=") }?.removePrefix("meta:location=")
        _editorWeather.value = attachments.firstOrNull { it.startsWith("meta:weather=") }?.removePrefix("meta:weather=")
        _editorTags.value = splitTags(current.tagsCsv)
        _showEditor.value = true
    }

    fun dismissEditor() { _showEditor.value = false }

    fun updateEditorTime(minutes: Int) { _editorTimeMinutes.value = minutes.coerceIn(0, 1439) }
    fun setEditorPhotos(value: List<String>) { _editorPhotoUris.value = value.distinct().take(12) }
    fun removeEditorPhoto(value: String) { _editorPhotoUris.value = _editorPhotoUris.value.filterNot { it == value } }
    fun setEditorTags(value: List<String>) { _editorTags.value = value.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(8) }
    fun setEditorAudio(value: String?) { _editorAudioUri.value = value }
    fun setEditorLocation(value: String?) { _editorLocation.value = value }
    fun setEditorWeather(value: String?) { _editorWeather.value = value }

    fun save(content: String, mood: String?) {
        if (content.isBlank() || _saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val current = entry.value ?: return@launch
                // updateEntry copies the stored row, so the id, day and time the
                // memory was written at are preserved.
                diaryRepository.updateEntry(current.id, current.title, content, mood, _editorTags.value, Json.encodeToString(ListSerializer(String.serializer()), editorAttachments()), _editorTimeMinutes.value)
                _showEditor.value = false
            } finally {
                _saving.value = false
            }
        }
    }

    fun delete(id: String) = viewModelScope.launch { diaryRepository.delete(id) }

    private fun splitTags(csv: String): List<String> = csv.split(',').map { it.trim() }.filter { it.isNotBlank() }
    private fun decode(json: String): List<String> = runCatching { Json.decodeFromString<List<String>>(json) }.getOrDefault(emptyList())
    private fun editorAttachments(): List<String> = buildList {
        addAll(_editorPhotoUris.value)
        _editorAudioUri.value?.takeIf { it.isNotBlank() }?.let { add("audio:" + it) }
        _editorLocation.value?.takeIf { it.isNotBlank() }?.let { add("meta:location=" + it) }
        _editorWeather.value?.takeIf { it.isNotBlank() }?.let { add("meta:weather=" + it) }
    }
}

/**
 * One memory, opened full page. Shares the timeline's editorial language — same
 * spine, same mood marker, same leading — so arriving here from the Diary list
 * or from the Timeline feels like the same surface seen closer.
 */
@Composable
fun DiaryDetailScreen(entryId: String, onBack: () -> Unit) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryDetailViewModel = viewModel(
        key = "diary-detail-$entryId",
        factory = LambdaViewModelFactory {
            DiaryDetailViewModel(entryId, locator.diaryRepository)
        }
    )
    val entry by viewModel.entry.collectAsState()
    val showEditor by viewModel.showEditor.collectAsState()
    val editorTimeMinutes by viewModel.editorTimeMinutes.collectAsState()
    val editorPhotoUris by viewModel.editorPhotoUris.collectAsState()
    val editorTags by viewModel.editorTags.collectAsState()
    val editorAudioUri by viewModel.editorAudioUri.collectAsState()
    val editorLocation by viewModel.editorLocation.collectAsState()
    val editorWeather by viewModel.editorWeather.collectAsState()
    val saving by viewModel.saving.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(enabled = showEditor, onBack = viewModel::dismissEditor)
    BackHandler(enabled = !showEditor, onBack = onBack)

    val wasVisible = remember { mutableStateOf(false) }
    LaunchedEffect(entry) {
        if (entry == null) {
            if (wasVisible.value) onBack()
        } else wasVisible.value = true
    }

    Box(Modifier.fillMaxSize()) {
        entry?.let { current ->
            val date = DateTimeUtils.epochDayToLocalDate(current.dateEpochDay)
            val hasMood = !current.mood.isNullOrBlank()

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = LifeOSSpacing.fabContentClearance)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DiaryInkViolet
                        )
                    }
                    Box(Modifier.weight(1f))
                    TextButton(onClick = viewModel::startEdit) {
                        Text("Edit", color = DiaryInkViolet)
                    }
                }

                Column(Modifier.padding(horizontal = LifeOSSpacing.screenPadding)) {
                    EditorEyebrow(dayEpochDay = current.dateEpochDay, isEditing = false)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        DateTimeUtils.formatFullDate(date),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        DateTimeUtils.formatMinutes(current.timeMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Same gutter + spine as the timeline, so the memory keeps its
                // place in the day even when read on its own.
                Row(Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding)) {
                    Box(Modifier.width(14.dp), contentAlignment = Alignment.TopCenter) {
                        if (hasMood) MoodDot(moodKey = current.mood) else {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(DiaryHairline)
                            )
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        if (hasMood) {
                            Text(
                                DiaryMoods.displayLabel(current.mood).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = DiaryMoods.colorOf(current.mood),
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            current.content,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val tags = remember(current.id, current.tagsCsv) {
                            current.tagsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }.take(6)
                        }
                        if (tags.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                tags.joinToString("  ·  "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LifeOSSpacing.screenPadding)
                        .height(1.dp)
                        .background(DiaryHairline)
                )
                Spacer(Modifier.height(4.dp))

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.compactPadding),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Delete memory", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        DiaryEditorOverlay(visible = showEditor) {
            entry?.let { current ->
                DiaryEditor(
                    dayEpochDay = current.dateEpochDay,
                    editing = current,
                    // This screen only ever edits an existing memory, so the time
                    // shown is the stored one and the edit keeps it.
                    timeMinutes = editorTimeMinutes,
                    photoUris = editorPhotoUris,
                    tags = editorTags,
                    audioUri = editorAudioUri,
                    location = editorLocation,
                    weather = editorWeather,
                    saving = saving,
                    onDismiss = viewModel::dismissEditor,
                    onSave = viewModel::save,
                    onTimeChange = viewModel::updateEditorTime,
                    onPhotosChange = viewModel::setEditorPhotos,
                    onRemovePhoto = viewModel::removeEditorPhoto,
                    onTagsChange = viewModel::setEditorTags,
                    onAudioChange = viewModel::setEditorAudio,
                    onLocationChange = viewModel::setEditorLocation,
                    onWeatherChange = viewModel::setEditorWeather,
                    onDelete = { viewModel.dismissEditor(); confirmDelete = true }
                )
            }
        }
    }

    entry?.let { current ->
        if (confirmDelete) {
            MemoryDeleteDialog(
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
