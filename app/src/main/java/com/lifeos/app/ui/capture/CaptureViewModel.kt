package com.lifeos.app.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.location.LocationFailure
import com.lifeos.app.core.location.LocationOutcome
import com.lifeos.app.core.location.LocationProvider
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.domain.model.DiaryMood
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The state the Capture hub shows. [confirmation] names what was actually
 * stored (never a bare "Saved!"), [error] is the honest what→why→how message
 * when a capture could not be made.
 */
data class CaptureUiState(
    val saving: Boolean = false,
    val confirmation: String? = null,
    val error: String? = null
)

/**
 * Backs the Capture hub. Every path here writes a real row through
 * [CaptureRepository] and reports the real stored value back; nothing is
 * simulated. Photo/Video are handled by the camera flow (Snap) at the call
 * site, so this ViewModel owns the text-backed captures: Thought, Mood and
 * Location.
 */
class CaptureViewModel(
    private val captureRepository: CaptureRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    fun hasLocationPermission(): Boolean = locationProvider.hasLocationPermission()

    /** A quick captured thought. Empty input is a no-op, not an error. */
    fun saveThought(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            captureRepository.create(type = CaptureType.THOUGHT, text = trimmed, tags = listOf("thought"))
            _state.update { it.copy(saving = false, confirmation = "Thought saved to your timeline · ${nowLabel()}") }
        }
    }

    /** A mood check-in, optionally with a short note. */
    fun saveMood(mood: DiaryMood, note: String?) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            captureRepository.create(
                type = CaptureType.MOOD,
                text = note?.trim()?.takeIf { it.isNotEmpty() } ?: mood.label,
                moodKey = mood.key,
                tags = listOf("mood")
            )
            _state.update { it.copy(saving = false, confirmation = "Feeling ${mood.label} saved · ${nowLabel()}") }
        }
    }

    /**
     * A location capture. The caller requests the permission first (see the
     * screen); if it is still missing here the failure is reported honestly
     * rather than fabricating a place.
     */
    fun saveLocation() {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            when (val outcome = locationProvider.currentPlace()) {
                is LocationOutcome.Success -> {
                    val label = outcome.placeName.ifBlank {
                        "%.4f, %.4f".format(Locale.US, outcome.place.latitude, outcome.place.longitude)
                    }
                    captureRepository.create(
                        type = CaptureType.LOCATION,
                        text = label,
                        latitude = outcome.place.latitude,
                        longitude = outcome.place.longitude,
                        placeLabel = outcome.placeName.ifBlank { null },
                        tags = listOf("place")
                    )
                    _state.update { it.copy(saving = false, confirmation = "$label saved · ${nowLabel()}") }
                }

                is LocationOutcome.Failure -> _state.update {
                    it.copy(saving = false, error = locationFailureCopy(outcome.reason))
                }
            }
        }
    }

    /**
     * A recorded voice capture. The file has already been written to app-private
     * storage by the recorder; this persists the row that points at it, together
     * with the recorder's own measured duration and MIME type — never an estimate.
     * A storage failure is surfaced honestly (what → why → how) and the clip is
     * left on disk rather than silently discarded.
     */
    fun saveAudio(filePath: String, durationMillis: Long) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            runCatching {
                captureRepository.create(
                    type = CaptureType.AUDIO,
                    filePath = filePath,
                    mimeType = "audio/mp4",
                    durationMillis = durationMillis,
                    tags = listOf("audio")
                )
            }.onSuccess {
                _state.update { it.copy(saving = false, confirmation = "Voice note saved to your timeline · ${nowLabel()}") }
            }.onFailure { failure ->
                _state.update {
                    it.copy(
                        saving = false,
                        error = "Couldn't save the recording: ${failure.message ?: "storage error"}. " +
                            "The audio is still on this device."
                    )
                }
            }
        }
    }

    fun dismissConfirmation() = _state.update { it.copy(confirmation = null) }

    fun dismissError() = _state.update { it.copy(error = null) }

    private fun nowLabel(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    private fun locationFailureCopy(reason: LocationFailure): String = when (reason) {
        LocationFailure.PERMISSION_DENIED ->
            "Location permission is off. Grant it in settings, or capture a thought instead."
        LocationFailure.NO_PROVIDER ->
            "This device has no location provider."
        LocationFailure.PROVIDERS_DISABLED ->
            "Location is switched off. Turn it on to capture where you are."
        LocationFailure.NO_FIX ->
            "No location fix yet. Step outside or try again in a moment."
    }
}
