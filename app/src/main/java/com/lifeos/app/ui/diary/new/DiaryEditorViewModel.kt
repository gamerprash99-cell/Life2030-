package com.lifeos.app.ui.diary.new

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.location.LocationProvider
import com.lifeos.app.core.location.LocationFailure
import com.lifeos.app.core.location.LocationOutcome
import com.lifeos.app.core.media.AudioPlayback
import com.lifeos.app.core.media.AudioRecorder
import com.lifeos.app.core.media.PhotoImporter
import com.lifeos.app.core.media.PlaybackState
import com.lifeos.app.core.media.RecordingState
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.WeatherRepository
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.domain.model.DiaryWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_MEMORY_CHARACTERS = 1000
private const val MAX_TITLE_CHARACTERS = 120
private const val MAX_TAG_CHARACTERS = 32

data class DiaryEditorState(
    val entryId: String? = null,
    val isEditing: Boolean = false,
    val title: String = "",
    val content: String = "",
    val mood: String? = null,
    val tags: List<String> = emptyList(),
    val dateEpochDay: Long = 0L,
    val timeMinutes: Int = 0,
    val photos: List<DiaryAttachment.Photo> = emptyList(),
    val voiceNote: DiaryAttachment.VoiceNote? = null,
    val place: DiaryAttachment.Place? = null,
    val playingFilePath: String? = null,
    val theme: DiaryAttachment.ThemeStyle? = null,
    val stickers: List<DiaryAttachment.Sticker> = emptyList(),
    val drawing: DiaryAttachment.Drawing? = null,
    val importedAudio: List<DiaryAttachment.ImportedAudio> = emptyList(),
    val locationStatus: LocationStatus = LocationStatus.IDLE,
    val weather: DiaryWeather = DiaryWeather.Unavailable(DiaryWeather.UnavailableReason.NO_LOCATION),
    val recording: RecordingState = RecordingState.Idle,
    val playback: PlaybackState = PlaybackState.Idle,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val savedEntryId: String? = null,
    val saveCount: Int = 0,
    val errorMessage: String? = null
) {
    val canSave: Boolean get() =
        content.isNotBlank() && !isSaving && !isLoading && recording !is RecordingState.Recording
}

enum class LocationStatus {
    IDLE,
    REQUESTING,
    PERMISSION_DENIED,
    PERMISSION_PERMANENTLY_DENIED,
    SERVICE_DISABLED,
    NO_PROVIDER,
    NO_FIX,
    FAILED
}

class DiaryEditorViewModel(
    private val diaryRepository: DiaryRepository,
    private val weatherRepository: WeatherRepository,
    private val locationProvider: LocationProvider,
    private val recorder: AudioRecorder,
    private val player: AudioPlayback,
    private val photoImporter: PhotoImporter,
    private val nowMinutes: () -> Int = DateTimeUtils::nowMinutesOfDay,
    private val todayEpochDay: () -> Long = { DateTimeUtils.today().toEpochDay() }
) : ViewModel() {

    private var loadToken = 0L
    private val _state = MutableStateFlow(DiaryEditorState())
    val state: StateFlow<DiaryEditorState> = _state.asStateFlow()

    fun start(entryId: String?, defaultDay: Long = todayEpochDay()) {
        val token = ++loadToken
        val committedWrites = _state.value.saveCount
        if (entryId == null) {
            _state.value = DiaryEditorState(
                isEditing = false,
                dateEpochDay = defaultDay,
                timeMinutes = nowMinutes(),
                saveCount = committedWrites
            )
            return
        }
        _state.value = DiaryEditorState(isLoading = true, saveCount = committedWrites)
        viewModelScope.launch {
            val existing = diaryRepository.getById(entryId)
            if (token != loadToken) return@launch
            if (existing == null) {
                _state.value = DiaryEditorState(
                    errorMessage = "That diary entry is no longer available on this device.",
                    saveCount = committedWrites
                )
                return@launch
            }
            val attachments = DiaryAttachments.decode(existing.attachmentsJson)
            _state.value = DiaryEditorState(
                entryId = existing.id,
                isEditing = true,
                title = existing.title.orEmpty(),
                content = existing.content,
                mood = existing.mood,
                tags = splitTags(existing.tagsCsv),
                dateEpochDay = existing.dateEpochDay,
                timeMinutes = existing.timeMinutes,
                photos = DiaryAttachments.photos(attachments),
                voiceNote = DiaryAttachments.voiceNote(attachments),
                place = DiaryAttachments.place(attachments),
                theme = DiaryAttachments.themeStyle(attachments),
                stickers = DiaryAttachments.stickers(attachments),
                drawing = DiaryAttachments.drawing(attachments),
                importedAudio = DiaryAttachments.importedAudio(attachments),
                saveCount = committedWrites
            )
            refreshWeather()
        }
    }

    fun onTitleChange(value: String) {
        _state.value = _state.value.copy(title = value.take(MAX_TITLE_CHARACTERS), errorMessage = null)
    }
    fun onContentChange(value: String) {
        _state.value = _state.value.copy(content = value.take(MAX_MEMORY_CHARACTERS), errorMessage = null)
    }
    fun onMoodChange(value: String?) { _state.value = _state.value.copy(mood = value) }
    fun onDateChange(epochDay: Long) { _state.value = _state.value.copy(dateEpochDay = epochDay) }
    fun onTimeChange(minutes: Int) { _state.value = _state.value.copy(timeMinutes = minutes.coerceIn(0, 1439)) }

    fun addTag(raw: String) {
        val tag = raw.trim().take(MAX_TAG_CHARACTERS)
        if (tag.isEmpty()) return
        val current = _state.value.tags
        if (current.any { it.equals(tag, ignoreCase = true) }) return
        _state.value = _state.value.copy(tags = current + tag)
    }
    fun removeTag(tag: String) {
        _state.value = _state.value.copy(tags = _state.value.tags.filterNot { it == tag })
    }

    fun attachPhoto(sourceUri: android.net.Uri) {
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) { photoImporter.import(sourceUri) }
            if (stored == null) {
                _state.value = _state.value.copy(errorMessage = "That photo could not be added.")
                return@launch
            }
            _state.value = _state.value.copy(photos = _state.value.photos + stored)
        }
    }
    fun removePhoto(photo: DiaryAttachment.Photo) {
        _state.value = _state.value.copy(photos = _state.value.photos.filterNot { it.filePath == photo.filePath })
        viewModelScope.launch(Dispatchers.IO) { MediaStorage.deleteIfExists(photo.filePath) }
    }

    fun attachLocation(isPermanentlyDenied: Boolean = false) {
        if (!locationProvider.hasLocationPermission()) {
            _state.value = _state.value.copy(
                locationStatus = if (isPermanentlyDenied) LocationStatus.PERMISSION_PERMANENTLY_DENIED
                else LocationStatus.PERMISSION_DENIED
            )
            return
        }
        _state.value = _state.value.copy(locationStatus = LocationStatus.REQUESTING, errorMessage = null)
        viewModelScope.launch {
            when (val outcome = locationProvider.currentPlace()) {
                is LocationOutcome.Success -> {
                    _state.value = _state.value.copy(
                        place = outcome.place,
                        locationStatus = LocationStatus.IDLE
                    )
                    refreshWeather()
                }
                is LocationOutcome.Failure -> {
                    _state.value = _state.value.copy(
                        locationStatus = locationStatusFor(outcome.reason)
                    )
                }
            }
        }
    }
    fun clearLocation() {
        _state.value = _state.value.copy(
            place = null,
            locationStatus = LocationStatus.IDLE,
            weather = DiaryWeather.Unavailable(DiaryWeather.UnavailableReason.NO_LOCATION)
        )
    }

    private suspend fun refreshWeather() {
        val weather = weatherRepository.weatherFor(_state.value.place)
        _state.value = _state.value.copy(weather = weather)
    }

    private fun locationStatusFor(reason: LocationFailure): LocationStatus = when (reason) {
        LocationFailure.PERMISSION_DENIED -> LocationStatus.PERMISSION_DENIED
        LocationFailure.PROVIDERS_DISABLED -> LocationStatus.SERVICE_DISABLED
        LocationFailure.NO_PROVIDER -> LocationStatus.NO_PROVIDER
        LocationFailure.NO_FIX -> LocationStatus.NO_FIX
    }

    fun startRecording() {
        val started = recorder.start()
        _state.value = _state.value.copy(recording = started, errorMessage = null)
    }
    fun stopRecording() {
        val result = recorder.stop()
        when (result) {
            is RecordingState.Finished -> {
                val previousVoicePath = _state.value.voiceNote?.filePath
                if (previousVoicePath != null) viewModelScope.launch(Dispatchers.IO) { MediaStorage.deleteIfExists(previousVoicePath) }
                _state.value = _state.value.copy(
                    voiceNote = DiaryAttachment.VoiceNote(result.filePath, result.durationMillis),
                    recording = result,
                    errorMessage = null
                )
            }
            is RecordingState.Failure -> _state.value = _state.value.copy(
                recording = result,
                errorMessage = when (result.reason) {
                    com.lifeos.app.core.media.RecordingFailure.TOO_SHORT -> "Hold to record for a little longer."
                    else -> "That recording could not be saved."
                }
            )
            else -> _state.value = _state.value.copy(recording = result)
        }
    }
    fun cancelRecording() {
        recorder.cancel()
        _state.value = _state.value.copy(recording = RecordingState.Idle)
    }

    fun toggleAudioPlayback(filePath: String) {
        val result = player.toggle(filePath)
        if (result is PlaybackState.MissingFile) {
            _state.value = _state.value.copy(
                playback = result,
                playingFilePath = null,
                errorMessage = "That audio is no longer on this device."
            )
        } else {
            _state.value = _state.value.copy(
                playback = result,
                playingFilePath = if (result is PlaybackState.Playing) filePath else null
            )
        }
    }
    fun togglePlayback() {
        val note = _state.value.voiceNote ?: return
        toggleAudioPlayback(note.filePath)
    }
    fun removeVoiceNote() {
        player.stop()
        val path = _state.value.voiceNote?.filePath
        _state.value = _state.value.copy(
            voiceNote = null,
            playback = PlaybackState.Idle,
            playingFilePath = if (_state.value.playingFilePath == path) null else _state.value.playingFilePath
        )
        if (path != null) viewModelScope.launch(Dispatchers.IO) { MediaStorage.deleteIfExists(path) }
    }

    fun setTheme(key: String?) {
        _state.value = _state.value.copy(theme = key?.let { DiaryAttachment.ThemeStyle(it) })
    }

    fun addSticker(assetKey: String, x: Float, y: Float, scale: Float = 1f, rotation: Float = 0f) {
        _state.value = _state.value.copy(
            stickers = _state.value.stickers + DiaryAttachment.Sticker(assetKey, x, y, scale, rotation)
        )
    }

    fun setDrawing(filePath: String, width: Float, height: Float) {
        val previous = _state.value.drawing?.filePath
        if (previous != null && previous != filePath) {
            viewModelScope.launch(Dispatchers.IO) { MediaStorage.deleteIfExists(previous) }
        }
        _state.value = _state.value.copy(
            drawing = DiaryAttachment.Drawing(filePath, width, height)
        )
    }
    fun removeDrawing() {
        val path = _state.value.drawing?.filePath
        _state.value = _state.value.copy(drawing = null)
        if (path != null) viewModelScope.launch(Dispatchers.IO) { MediaStorage.deleteIfExists(path) }
    }

    fun addImportedAudioFile(file: File, mimeType: String, displayName: String) {
        _state.value = _state.value.copy(
            importedAudio = _state.value.importedAudio + DiaryAttachment.ImportedAudio(
                filePath = file.absolutePath,
                mimeType = mimeType,
                displayName = displayName,
                durationMillis = 0L
            )
        )
    }
    fun removeImportedAudio(filePath: String) {
        _state.value = _state.value.copy(
            importedAudio = _state.value.importedAudio.filter { it.filePath != filePath },
            playingFilePath = if (_state.value.playingFilePath == filePath) null else _state.value.playingFilePath
        )
        viewModelScope.launch(Dispatchers.IO) { MediaStorage.deleteIfExists(filePath) }
    }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        _state.value = current.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            val attachments = buildList {
                addAll(current.photos)
                current.voiceNote?.let { add(it) }
                current.place?.let { add(it) }
                current.theme?.let { add(it) }
                addAll(current.stickers)
                current.drawing?.let { add(it) }
                addAll(current.importedAudio)
            }
            runCatching {
                val existing = current.entryId
                if (existing != null) {
                    diaryRepository.updateEntry(
                        id = existing,
                        title = current.title.trim().takeIf { it.isNotEmpty() },
                        content = current.content.trim(),
                        mood = current.mood,
                        tags = current.tags,
                        dateEpochDay = current.dateEpochDay,
                        timeMinutes = current.timeMinutes,
                        attachments = attachments
                    )
                    existing
                } else {
                    diaryRepository.createEntry(
                        title = current.title.trim().takeIf { it.isNotEmpty() },
                        content = current.content.trim(),
                        mood = current.mood,
                        tags = current.tags,
                        dateEpochDay = current.dateEpochDay,
                        timeMinutes = current.timeMinutes,
                        attachments = attachments
                    )
                }
            }.onSuccess { id ->
                _state.value = _state.value.copy(
                    isSaving = false,
                    savedEntryId = id,
                    saveCount = _state.value.saveCount + 1
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isSaving = false,
                    errorMessage = error.message ?: "That entry could not be saved."
                )
            }
        }
    }

    fun dismissError() { _state.value = _state.value.copy(errorMessage = null) }

    override fun onCleared() {
        recorder.cancel()
        player.release()
        super.onCleared()
    }

    private fun splitTags(csv: String): List<String> = csv.split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
}
