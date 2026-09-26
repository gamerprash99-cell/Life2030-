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
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

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
    private val nowMinutes: () -> Int = DateTimeUtils::nowMinutesOfDay,
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

    /** True while a write is in flight, so the save action cannot be double-fired. */
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    /** Monotonic UI event: increments only after a local Room write succeeds. */
    private val _saveConfirmation = MutableStateFlow(0)
    val saveConfirmation: StateFlow<Int> = _saveConfirmation

    /**
     * The wall-clock minute this entry is stamped with, decided the moment the
     * editor opens rather than the moment it is saved.
     *
     * This is the whole point of the field: someone who opens the editor at 8:04
     * and writes for twenty minutes means "8:04", not "8:24". Reading the clock
     * at save time silently rewrote the timestamp of every long entry, and it
     * also made the time shown in the editor a value that would change under the
     * user's hands. Editing an existing entry keeps that entry's original
     * minute, so re-saving can never move a memory in the day's timeline.
     */
    private var capturedTimeMinutes: Int? = null

    private val _editorTimeMinutes = MutableStateFlow<Int?>(null)
    private val _editorPhotoUris = MutableStateFlow<List<String>>(emptyList())
    val editorPhotoUris: StateFlow<List<String>> = _editorPhotoUris
    private val _editorTags = MutableStateFlow<List<String>>(emptyList())
    private val _editorAudioUri = MutableStateFlow<String?>(null)
    val editorAudioUri: StateFlow<String?> = _editorAudioUri
    private val _editorLocation = MutableStateFlow<String?>(null)
    val editorLocation: StateFlow<String?> = _editorLocation
    private val _editorWeather = MutableStateFlow<String?>(null)
    val editorWeather: StateFlow<String?> = _editorWeather
    val editorTags: StateFlow<List<String>> = _editorTags
    val editorTimeMinutes: StateFlow<Int?> = _editorTimeMinutes

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
        capturedTimeMinutes = nowMinutes()
        _editorTimeMinutes.value = capturedTimeMinutes
        _editorPhotoUris.value = emptyList()
        _editorTags.value = emptyList()
        _editorAudioUri.value = null
        _editorLocation.value = null
        _editorWeather.value = null
        _showEditor.value = true
    }

    fun startEdit(entry: DiaryEntity) {
        _editingEntry.value = entry
        // An edit keeps the minute the memory was originally written at.
        capturedTimeMinutes = entry.timeMinutes
        _editorTimeMinutes.value = entry.timeMinutes
        _editorPhotoUris.value = decodeStringList(entry.attachmentsJson)
        val attachments = decodeStringList(entry.attachmentsJson)
        _editorPhotoUris.value = attachments.filterNot { it.startsWith("audio:") || it.startsWith("meta:") }
        _editorAudioUri.value = attachments.firstOrNull { it.startsWith("audio:") }?.removePrefix("audio:")
        _editorLocation.value = attachments.firstOrNull { it.startsWith("meta:location=") }?.removePrefix("meta:location=")
        _editorWeather.value = attachments.firstOrNull { it.startsWith("meta:weather=") }?.removePrefix("meta:weather=")
        _editorTags.value = splitTags(entry.tagsCsv)
        _showEditor.value = true
    }

    fun dismissEditor() {
        _showEditor.value = false
        _editingEntry.value = null
        capturedTimeMinutes = null
        _editorTimeMinutes.value = null
        _editorPhotoUris.value = emptyList()
        _editorTags.value = emptyList()
        _editorAudioUri.value = null
        _editorLocation.value = null
        _editorWeather.value = null
    }

    fun setEditorLocation(value: String?) {
        _editorLocation.value = value
    }

    fun setEditorWeather(value: String?) {
        _editorWeather.value = value
    }

    /**
     * Persists a memory. New entries are stamped with the day currently being
     * read — not with `today()` — so writing into a past day lands in that day,
     * and with the minute captured when the editor opened, so the timestamp
     * reflects when the memory happened rather than when the user finished
     * typing it. Edits keep the entry's existing id, date and time; only the
     * fields the editor owns are replaced.
     */
    fun updateEditorTime(minutes: Int) {
        val safe = minutes.coerceIn(0, 1439)
        capturedTimeMinutes = safe
        _editorTimeMinutes.value = safe
    }

    fun setEditorPhotos(uris: List<String>) {
        _editorPhotoUris.value = uris.distinct().take(12)
    }

    fun removeEditorPhoto(uri: String) {
        _editorPhotoUris.value = _editorPhotoUris.value.filterNot { it == uri }
    }

    fun setEditorAudio(uri: String?) {
        _editorAudioUri.value = uri
    }

    fun setEditorTags(tags: List<String>) {
        _editorTags.value = tags.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(8)
    }

    fun saveEntry(content: String, mood: String?) {
        if (content.isBlank() || _saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val editing = _editingEntry.value
                if (editing != null) {
                    diaryRepository.updateEntry(
                        editing.id, editing.title, content, mood, _editorTags.value,
                        Json.encodeToString(ListSerializer(String.serializer()), editorAttachments()),
                        timeMinutes = capturedTimeMinutes
                    )
                } else {
                    diaryRepository.createEntry(
                        title = null,
                        content = content,
                        mood = mood,
                        tags = emptyList(),
                        dateEpochDay = _selectedDay.value,
                        timeMinutes = capturedTimeMinutes ?: nowMinutes(),
                        attachmentsJson = Json.encodeToString(editorAttachments())
                    )
                }
                _saveConfirmation.value += 1
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

    /**
     * Clear the one-shot confirmation counter after the UI has consumed the
     * current event. Kept separate from save so a recomposition cannot invent a
     * second confirmation.
     */
    fun consumeSaveConfirmation() {
        _saveConfirmation.value = 0
    }

    private fun splitTags(csv: String): List<String> = csv.split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }

    private fun editorAttachments(): List<String> = buildList {
        addAll(_editorPhotoUris.value)
        _editorAudioUri.value?.takeIf { it.isNotBlank() }?.let { add("audio:" + it) }
        _editorLocation.value?.takeIf { it.isNotBlank() }?.let { add("meta:location=" + it) }
        _editorWeather.value?.takeIf { it.isNotBlank() }?.let { add("meta:weather=" + it) }
    }

    private fun decodeStringList(json: String): List<String> = runCatching {
        Json.decodeFromString<List<String>>(json)
    }.getOrDefault(emptyList())
}
