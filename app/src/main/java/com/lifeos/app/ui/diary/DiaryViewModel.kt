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
    private val diaryRepository: DiaryRepository,
    private val todayEpochDay: () -> Long = { DateTimeUtils.today().toEpochDay() }
) : ViewModel() {

    val entries: StateFlow<List<DiaryEntity>> = diaryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDay = MutableStateFlow(todayEpochDay())
    val selectedDay: StateFlow<Long> = _selectedDay

    /** Epoch days that hold at least one memory — marks those cells in the strip. */
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

    /**
     * Selects a day, clamped to the span the strip can actually show: never a
     * future (there is no tomorrow to journal) and never older than the strip's
     * history window. Clamping here rather than in the composable means a stale
     * saved state or any other caller cannot park the screen on a day the strip
     * is unable to render.
     */
    fun selectDay(day: Long) {
        _selectedDay.value = day.coerceIn(todayEpochDay() - HISTORY_DAYS, todayEpochDay())
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
     * Called once the editor's own view model reports a committed write. Keeps
     * the day view's "the editor is finished" transition in one place, so the
     * editor only has to say *that* it saved, not how the screen should react.
     */
    fun onEditorSaved() {
        dismissEditor()
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

    /**
     * Flips a memory's favourite flag. The write is a single Room update, so
     * the list re-renders from the same `observeAll()` flow the screen already
     * collects — there is no separate in-memory copy to fall out of sync.
     */
    fun toggleFavorite(id: String) {
        viewModelScope.launch { diaryRepository.toggleFavorite(id) }
    }

    /**
     * Strips one attachment from a memory without opening the editor, so the
     * detail screen's remove affordances do what they look like they do. The
     * repository no-ops on a path the entry does not hold, and deletes the file
     * once nothing references it.
     */
    fun removeAttachment(id: String, filePath: String) {
        viewModelScope.launch { diaryRepository.removeAttachment(id, filePath) }
    }

}
