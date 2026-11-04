package com.lifeos.app.ui.diary.new

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.media.AudioRecorder
import com.lifeos.app.core.media.AudioPlayback
import com.lifeos.app.core.media.PhotoImporter
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.WeatherRepository
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.domain.model.DiaryWeather
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * ViewModel for the new Diary detail screen.
 *
 * Extracted from the old `DiaryDetailScreen.kt` (ui/diary/) so the old UI layer
 * can be removed. Loads a single entry by id and exposes editor open/close,
 * favorite toggle, attachment removal, and weather refresh.
 */
class DiaryDetailViewModel(
    private val entryId: String,
    private val diaryRepository: DiaryRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    val entry: StateFlow<DiaryEntity?> = diaryRepository.observeById(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showEditor = MutableStateFlow(false)
    val showEditor: StateFlow<Boolean> = _showEditor

    fun startEdit() { _showEditor.value = true }
    fun dismissEditor() { _showEditor.value = false }

    fun delete(id: String) = viewModelScope.launch {
        withContext(NonCancellable) { diaryRepository.delete(id) }
    }

    fun toggleFavorite() = viewModelScope.launch {
        diaryRepository.toggleFavorite(entryId)
    }

    fun removeAttachment(filePath: String) = viewModelScope.launch {
        diaryRepository.removeAttachment(entryId, filePath)
    }

    private val _weather = MutableStateFlow<DiaryWeather>(
        DiaryWeather.Unavailable(DiaryWeather.UnavailableReason.NO_LOCATION)
    )
    val weather: StateFlow<DiaryWeather> = _weather

    fun refreshWeather() {
        viewModelScope.launch {
            val stored = entry.value ?: return@launch
            val place = DiaryAttachments.place(DiaryAttachments.decode(stored.attachmentsJson))
            _weather.value = weatherRepository.weatherFor(place)
        }
    }
}
