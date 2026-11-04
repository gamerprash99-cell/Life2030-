package com.lifeos.app.ui.diary.new

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Diary screen state. Everything flows from [DiaryRepository.observeAll()] —
 * the list, the day filter and the new/edit editor are all Room-backed, no
 * simulated data.
 *
 * The screen is day-scoped: [selectedDay] always holds a concrete day (today
 * until the user moves).
 */
class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val todayEpochDay: () -> Long = { DateTimeUtils.today().toEpochDay() }
) : ViewModel() {

    private val HISTORY_DAYS = 365L

    val entries: StateFlow<List<DiaryEntity>> = diaryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDay = MutableStateFlow(todayEpochDay())
    val selectedDay: StateFlow<Long> = _selectedDay

    val daysWithMemories: StateFlow<Set<Long>> = entries
        .map { all -> all.mapTo(mutableSetOf()) { it.dateEpochDay } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val memoriesForSelectedDay: StateFlow<List<DiaryEntity>> =
        combine(entries, _selectedDay) { all, day ->
            all.filter { it.dateEpochDay == day }
                .sortedByDescending { it.timeMinutes }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showEditor = MutableStateFlow(false)
    val showEditor: StateFlow<Boolean> = _showEditor
    private val _editingEntry = MutableStateFlow<DiaryEntity?>(null)
    val editingEntry: StateFlow<DiaryEntity?> = _editingEntry

    private val _entryToDelete = MutableStateFlow<DiaryEntity?>(null)
    val entryToDelete: StateFlow<DiaryEntity?> = _entryToDelete

    private val _savedEntryId = MutableStateFlow<String?>(null)
    val savedEntryId: StateFlow<String?> = _savedEntryId

    val savedEntry: StateFlow<DiaryEntity?> =
        combine(entries, _savedEntryId) { all, id ->
            id?.let { wanted -> all.firstOrNull { it.id == wanted } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectDay(day: Long) {
        dismissSavedConfirmation()
        _selectedDay.value = day.coerceIn(todayEpochDay() - HISTORY_DAYS, todayEpochDay())
    }

    fun startNewEntry() {
        dismissSavedConfirmation()
        _editingEntry.value = null
        _showEditor.value = true
    }

    fun startEdit(entry: DiaryEntity) {
        dismissSavedConfirmation()
        _editingEntry.value = entry
        _showEditor.value = true
    }

    fun dismissEditor() {
        _showEditor.value = false
        _editingEntry.value = null
    }

    fun onEditorSaved(savedId: String?) {
        dismissEditor()
        _savedEntryId.value = savedId
    }

    fun dismissSavedConfirmation() {
        _savedEntryId.value = null
    }

    fun requestDelete(entry: DiaryEntity) {
        _entryToDelete.value = entry
    }

    fun dismissDelete() {
        _entryToDelete.value = null
    }

    fun deleteEntry(id: String) {
        _entryToDelete.value = null
        viewModelScope.launch { diaryRepository.delete(id) }
    }
}
