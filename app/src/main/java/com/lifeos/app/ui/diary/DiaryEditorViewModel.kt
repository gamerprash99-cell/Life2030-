package com.lifeos.app.ui.diary

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.location.DeviceLocationProvider
import com.lifeos.app.core.location.LocationFailure
import com.lifeos.app.core.location.LocationOutcome
import com.lifeos.app.core.media.DiaryAudioPlayer
import com.lifeos.app.core.media.DiaryAudioRecorder
import com.lifeos.app.core.media.PlaybackState
import com.lifeos.app.core.media.RecordingState
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.WeatherRepository
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.domain.model.DiaryTextStats
import com.lifeos.app.domain.model.DiaryWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Everything the composer renders. All fields are real user input or real device data. */
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
    val locationStatus: LocationStatus = LocationStatus.IDLE,
    val weather: DiaryWeather = DiaryWeather.Unavailable(DiaryWeather.UnavailableReason.NO_LOCATION),
    val recording: RecordingState = RecordingState.Idle,
    val playback: PlaybackState = PlaybackState.Idle,
    val isSaving: Boolean = false,
    val savedEntryId: String? = null,
    val errorMessage: String? = null
) {
    val wordCount: Int get() = DiaryTextStats.wordCount(content)
    val characterCount: Int get() = DiaryTextStats.characterCount(content)
    val canSave: Boolean get() = content.isNotBlank() && !isSaving && recording !is RecordingState.Recording
    val hasAttachments: Boolean get() = photos.isNotEmpty() || voiceNote != null || place != null
}

/** Where the "Add location" flow currently is. */
enum class LocationStatus { IDLE, REQUESTING, PERMISSION_DENIED, PERMISSION_PERMANENTLY_DENIED, FAILED }

/**
 * Drives the full-screen diary composer.
 *
 * Responsibilities are deliberately narrow: hold the draft, own the real
 * recorder/player, ask the real location provider, and persist through
 * [DiaryRepository]. All heavy I/O (file copies, geocoding) runs on
 * [Dispatchers.IO] so the frame never blocks.
 */
class DiaryEditorViewModel(
    private val diaryRepository: DiaryRepository,
    private val weatherRepository: WeatherRepository,
    private val locationProvider: DeviceLocationProvider,
    private val recorder: DiaryAudioRecorder,
    private val player: DiaryAudioPlayer,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(DiaryEditorState())
    val state: StateFlow<DiaryEditorState> = _state.asStateFlow()

    val recordingState: StateFlow<RecordingState> = recorder.state
    val playbackState: StateFlow<PlaybackState> = player.state

    /** Loads an existing entry for editing, or seeds a blank draft for a new one. */
    fun start(entryId: String?) {
        if (_state.value.isEditing || _state.value.entryId != null) return
        if (entryId == null) {
            _state.value = DiaryEditorState(
                isEditing = false,
                // A new entry defaults to the real current date and time, not a
                // constant — the user can change both before saving.
                dateEpochDay = DateTimeUtils.today().toEpochDay(),
                timeMinutes = DateTimeUtils.nowMinutesOfDay()
            )
            return
        }
        viewModelScope.launch {
            val existing = diaryRepository.getById(entryId)
            if (existing == null) {
                _state.value = DiaryEditorState(
                    errorMessage = "That diary entry is no longer available on this device."
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
                place = DiaryAttachments.place(attachments)
            )
            refreshWeather()
        }
    }

    fun onTitleChange(value: String) { _state.value = _state.value.copy(title = value, errorMessage = null) }
    fun onContentChange(value: String) { _state.value = _state.value.copy(content = value, errorMessage = null) }
    fun onMoodChange(value: String?) { _state.value = _state.value.copy(mood = value) }
    fun onDateChange(epochDay: Long) { _state.value = _state.value.copy(dateEpochDay = epochDay) }
    fun onTimeChange(minutes: Int) { _state.value = _state.value.copy(timeMinutes = minutes.coerceIn(0, 1439)) }

    fun addTag(raw: String) {
        val tag = raw.trim()
        if (tag.isEmpty()) return
        val current = _state.value.tags
        if (current.any { it.equals(tag, ignoreCase = true) }) return
        _state.value = _state.value.copy(tags = current + tag)
    }

    fun removeTag(tag: String) {
        _state.value = _state.value.copy(tags = _state.value.tags.filterNot { it == tag })
    }

    /**
     * Copies a picked photo into app-private storage so the entry keeps working
     * after the source image is deleted or a provider read grant expires.
     * A failed copy surfaces an error instead of persisting a dead reference.
     */
    fun attachPhoto(sourceUri: Uri) {
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) { copyIntoPrivateStorage(sourceUri) }
            if (stored == null) {
                _state.value = _state.value.copy(errorMessage = "That photo could not be added.")
                return@launch
            }
            _state.value = _state.value.copy(photos = _state.value.photos + stored)
        }
    }

    private fun copyIntoPrivateStorage(sourceUri: Uri): DiaryAttachment.Photo? = runCatching {
        val target: File = MediaStorage.newDiaryPhotoFile(appContext)
        appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        if (target.length() == 0L) {
            target.delete()
            return null
        }
        DiaryAttachment.Photo(target.absolutePath)
    }.getOrNull()

    fun removePhoto(photo: DiaryAttachment.Photo) {
        MediaStorage.deleteIfExists(photo.filePath)
        _state.value = _state.value.copy(photos = _state.value.photos.filterNot { it.filePath == photo.filePath })
    }

    // ---- location -------------------------------------------------------

    /**
     * Requests the real current place. Callers must have already obtained the
     * user's consent (the permission launcher lives in the composable); this
     * only reads the permission state to decide which honest outcome to report.
     */
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
                    val place = outcome.place.copy(placeName = outcome.placeName)
                    _state.value = _state.value.copy(
                        place = place,
                        locationStatus = LocationStatus.IDLE
                    )
                    refreshWeather()
                }
                is LocationOutcome.Failure -> {
                    val status = when (outcome.reason) {
                        LocationFailure.PERMISSION_DENIED -> LocationStatus.PERMISSION_DENIED
                        else -> LocationStatus.FAILED
                    }
                    _state.value = _state.value.copy(
                        locationStatus = status,
                        errorMessage = locationErrorFor(outcome.reason)
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

    private fun locationErrorFor(reason: LocationFailure): String = when (reason) {
        LocationFailure.PERMISSION_DENIED -> "Location permission is needed to attach a place."
        LocationFailure.NO_PROVIDER -> "This device has no location service available."
        LocationFailure.PROVIDERS_DISABLED -> "Location is turned off. Enable it in system settings to attach a place."
        LocationFailure.NO_FIX -> "No location fix was available yet. Try again in a moment."
    }

    // ---- audio ----------------------------------------------------------

    fun startRecording() {
        val started = recorder.start()
        _state.value = _state.value.copy(recording = started, errorMessage = null)
    }

    /** Advances the visible duration/level from the real recorder clock. */
    fun tickRecording() {
        val ticked = recorder.tick()
        if (ticked != _state.value.recording) _state.value = _state.value.copy(recording = ticked)
    }

    fun stopRecording() {
        val result = recorder.stop()
        when (result) {
            is RecordingState.Finished -> {
                // A finished take replaces any previous voice note on this entry,
                // and the old file is reclaimed rather than orphaned.
                _state.value.voiceNote?.let { MediaStorage.deleteIfExists(it.filePath) }
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

    fun togglePlayback() {
        val note = _state.value.voiceNote ?: return
        val result = player.toggle(note.filePath)
        if (result is PlaybackState.MissingFile) {
            _state.value = _state.value.copy(
                playback = result,
                errorMessage = "That voice note is no longer on this device."
            )
        } else {
            _state.value = _state.value.copy(playback = result)
        }
    }

    fun removeVoiceNote() {
        player.stop()
        _state.value.voiceNote?.let { MediaStorage.deleteIfExists(it.filePath) }
        _state.value = _state.value.copy(voiceNote = null, playback = PlaybackState.Idle)
    }

    // ---- persistence ----------------------------------------------------

    /**
     * Persists the draft. New entries keep the exact date/time the user chose
     * (so a back-dated entry stays back-dated) and record `createdAt` from the
     * real clock, not from the entry's own date.
     */
    fun save() {
        val current = _state.value
        if (!current.canSave) return
        _state.value = current.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            val attachments = buildList {
                addAll(current.photos)
                current.voiceNote?.let { add(it) }
                current.place?.let { add(it) }
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
                _state.value = _state.value.copy(isSaving = false, savedEntryId = id)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isSaving = false,
                    errorMessage = error.message ?: "That entry could not be saved."
                )
            }
        }
    }

    /** Clears the draft for a second entry without leaving the composer. */
    fun startAnother() {
        _state.value = DiaryEditorState(
            dateEpochDay = DateTimeUtils.today().toEpochDay(),
            timeMinutes = DateTimeUtils.nowMinutesOfDay()
        )
    }

    fun dismissError() { _state.value = _state.value.copy(errorMessage = null) }

    override fun onCleared() {
        // Never leave the mic or the audio decoder open behind a dead screen.
        recorder.cancel()
        player.release()
        super.onCleared()
    }

    private fun splitTags(csv: String): List<String> = csv.split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
}
