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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Diary screen state. Everything flows from `DiaryRepository.observeAll()` —
 * the list, the day filter and the new/edit editor are all Room-backed, no
 * simulated data.
 *
 * The screen is day-scoped: [selectedDay] always holds a concrete day (today
 * until the user moves) rather than an "all days" sentinel, because the Diary
 * is read one day at a time.
 */
class DiaryViewModel(
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    val entries: StateFlow<List<DiaryEntity>> = diaryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDay = MutableStateFlow(DateTimeUtils.today().toEpochDay())
    val selectedDay: StateFlow<Long> = _selectedDay

    /** Epoch days that hold at least one memory — drives the masthead's day ticks. */
    val daysWithMemories: StateFlow<Set<Long>> = entries
        .map { all -> all.mapTo(mutableSetOf()) { it.dateEpochDay } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** Memories on [selectedDay], newest first. */
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

    /** True while a write is in flight, so the save action cannot be double-fired. */
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    fun selectDay(day: Long) {
        _selectedDay.value = day
    }

    /** Steps the selected day, refusing to move past today (there is no tomorrow to journal). */
    fun shiftDay(delta: Long) {
        val today = DateTimeUtils.today().toEpochDay()
        val next = _selectedDay.value + delta
        if (next <= today) _selectedDay.value = next
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

    /**
     * Persists a memory. New entries are stamped with the day currently being
     * read — not with `today()` — so writing into a past day lands in that day.
     * Edits keep the entry's existing id, date and time; only the fields the
     * editor owns are replaced.
     */
    fun saveEntry(content: String, mood: String?) {
        if (content.isBlank() || _saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val editing = _editingEntry.value
                if (editing != null) {
                    diaryRepository.updateEntry(
                        editing.id, editing.title, content, mood, splitTags(editing.tagsCsv)
                    )
                } else {
                    diaryRepository.createEntry(
                        title = null,
                        content = content,
                        mood = mood,
                        tags = emptyList(),
                        dateEpochDay = _selectedDay.value,
                        timeMinutes = DateTimeUtils.nowMinutesOfDay()
                    )
                }
                dismissEditor()
            } finally {
                // Never leave the save action stuck disabled if the write throws.
                _saving.value = false
            }
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
