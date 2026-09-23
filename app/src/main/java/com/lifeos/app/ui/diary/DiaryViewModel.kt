package com.lifeos.app.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Diary screen state. Everything flows from `DiaryRepository.observeAll()` —
 * the list, the date filter and the new/edit sheet are all Room-backed, no
 * simulated data.
 */
class DiaryViewModel(
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    val entries: StateFlow<List<DiaryEntity>> = diaryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDay = MutableStateFlow<Long?>(null)
    val selectedDay: StateFlow<Long?> = _selectedDay

    val filteredEntries: StateFlow<List<DiaryEntity>> =
        combine(entries, _selectedDay) { all, day ->
            (if (day == null) all else all.filter { it.dateEpochDay == day })
                .sortedWith(compareByDescending<DiaryEntity> { it.dateEpochDay }.thenByDescending { it.timeMinutes })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showEditor = MutableStateFlow(false)
    val showEditor: StateFlow<Boolean> = _showEditor
    private val _editingEntry = MutableStateFlow<DiaryEntity?>(null)
    val editingEntry: StateFlow<DiaryEntity?> = _editingEntry

    private val _entryToDelete = MutableStateFlow<DiaryEntity?>(null)
    val entryToDelete: StateFlow<DiaryEntity?> = _entryToDelete

    fun selectDay(day: Long?) {
        _selectedDay.value = day
    }

    fun startNewEntry() {
        _editingEntry.value = null
        _showEditor.value = true
    }

    fun startEdit(entry: DiaryEntity) {
        _editingEntry.value = entry
        _showEditor.value = true
    }

    fun dismissEditor() {
        _showEditor.value = false
        _editingEntry.value = null
    }

    fun saveEntry(content: String, mood: String?) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val editing = _editingEntry.value
            if (editing != null) {
                diaryRepository.updateEntry(
                    editing.id, editing.title, content, mood, splitTags(editing.tagsCsv)
                )
            } else {
                val now = DateTimeUtils.today()
                val minutes = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
                diaryRepository.createEntry(
                    title = null, content = content, mood = mood, tags = emptyList(),
                    dateEpochDay = now.toEpochDay(), timeMinutes = minutes
                )
            }
            dismissEditor()
        }
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

    private fun splitTags(csv: String): List<String> = csv.split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
}