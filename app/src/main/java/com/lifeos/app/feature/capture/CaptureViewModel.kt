package com.lifeos.app.feature.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.data.AppContainer
import com.lifeos.app.core.database.entity.CaptureEntity
import com.lifeos.app.core.domain.CaptureType
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.Mood
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Capture sheet: photo / video / audio / thought / mood / location.
 *
 * Each entry is persisted the moment the user confirms; the sheet then closes and
 * reports success so the caller can show a confirmation. Nothing is held in a
 * fragile "draft", and a failed media import reports failure instead of silently
 * creating an empty row.
 */
class CaptureViewModel(private val container: AppContainer) : ViewModel() {

    data class CaptureState(
        val captures: List<CaptureEntity> = emptyList(),
        val filter: CaptureType? = null,
        val message: String? = null,
        val busy: Boolean = false,
    ) {
        val visible: List<CaptureEntity>
            get() = filter?.let { type -> captures.filter { it.type == type.code } } ?: captures
    }

    private val filter = MutableStateFlow<CaptureType?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)

    val state: StateFlow<CaptureState> = combine(
        container.captures.observeCaptures(),
        filter,
        message,
        busy,
    ) { captures, type, msg, isBusy ->
        CaptureState(captures = captures, filter = type, message = msg, busy = isBusy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaptureState())

    fun setFilter(type: CaptureType?) {
        filter.value = type
    }

    fun clearMessage() {
        message.value = null
    }

    /** Shows a transient status line (e.g. permission denial) without saving. */
    fun report(message: String) {
        this.message.value = message
    }

    /** Free-text capture (thought). */
    fun captureThought(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            busy.value = true
            container.captures.saveText(CaptureType.THOUGHT, text = text)
            busy.value = false
            message.value = "Thought saved"
        }
    }

    /** Mood capture: stores the score and a readable label. */
    fun captureMood(mood: Mood, note: String = "") {
        viewModelScope.launch {
            busy.value = true
            container.captures.saveText(
                type = CaptureType.MOOD,
                text = note,
                moodScore = mood.score,
            )
            busy.value = false
            message.value = "Mood logged (${mood.label})"
        }
    }

    /** Location capture: only ever called with a location the OS just returned. */
    fun captureLocation(latitude: Double, longitude: Double, label: String?) {
        viewModelScope.launch {
            busy.value = true
            container.captures.saveText(
                type = CaptureType.LOCATION,
                text = label.orEmpty(),
                latitude = latitude,
                longitude = longitude,
                placeLabel = label,
            )
            busy.value = false
            message.value = "Location saved"
        }
    }

    /** Media import from a picker/camera result. */
    fun captureMedia(type: CaptureType, uri: Uri, mimeType: String, displayName: String) {
        viewModelScope.launch {
            busy.value = true
            val id = container.captures.saveMedia(type, uri, mimeType, displayName)
            busy.value = false
            message.value = if (id != null) {
                "${type.label} saved"
            } else {
                "Could not read that file — nothing was saved."
            }
        }
    }

    fun trash(id: String) {
        viewModelScope.launch {
            container.captures.moveToTrash(id)
            message.value = "Moved to Trash"
        }
    }

    companion object {
        /** Shown when a capture lacks a permission the user has not granted. */
        fun permissionMessage(type: CaptureType): String = when (type) {
            CaptureType.PHOTO, CaptureType.VIDEO ->
                "Camera access is only needed while taking this photo or video."
            CaptureType.AUDIO ->
                "Microphone access is only needed while recording this audio note."
            CaptureType.LOCATION ->
                "Location access is only needed to save this one location."
            else -> "Permission is only requested when you use this feature."
        }
    }
}

/** Local day label for the capture sheet header. */
fun todayIso(): String = Dates.iso(Dates.today())
