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
     * The entry id a just-committed write produced, held while the confirmation
     * is on screen. It is only the *id*: the sheet's content is resolved from
     * [entries] below, so what it shows is the stored row rather than a copy that
     * could disagree with the database.
     */
    private val _savedEntryId = MutableStateFlow<String?>(null)
    val savedEntryId: StateFlow<String?> = _savedEntryId

    /** The committed entry itself, or null while there is nothing to confirm. */
    val savedEntry: StateFlow<DiaryEntity?> =
        combine(entries, _savedEntryId) { all, id ->
            id?.let { wanted -> all.firstOrNull { it.id == wanted } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Selects a day, clamped to the span the strip can actually show: never a
     * future (there is no tomorrow to journal) and never older than the strip's
     * history window. Clamping here rather than in the composable means a stale
     * saved state or any other caller cannot park the screen on a day the strip
     * is unable to render.
     */
    fun selectDay(day: Long) {
        // Moving to another day means the user is done looking at what was just
        // saved, so the confirmation retires rather than sitting over a day it
        // has nothing to do with.
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

    /**
     * Called once the editor's own view model reports a committed write. Keeps
     * the day view's "the editor is finished" transition in one place, so the
     * editor only has to say *that* it saved, not how the screen should react.
     *
     * The composer closes immediately — the write is already durable — and the
     * confirmation takes its place. [savedId] is null when the commit produced no
     * id, in which case there is nothing to confirm and the sheet is skipped
     * rather than shown empty.
     */
    fun onEditorSaved(savedId: String?) {
        dismissEditor()
        _savedEntryId.value = savedId
    }

    /** Retires the confirmation — Back, a tap outside, or either of its actions. */
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
