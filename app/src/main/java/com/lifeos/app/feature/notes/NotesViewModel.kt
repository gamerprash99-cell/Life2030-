package com.lifeos.app.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.data.AppContainer
import com.lifeos.app.core.data.NoteRepository
import com.lifeos.app.core.data.NoteWithTags
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.NoteKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Notes feature state.
 *
 * The list is a live projection of the database; the editor keeps draft text in
 * memory and only writes on an explicit save (no accidental half-saves, no
 * background autosave races — documented in TESTING.md as intended behavior).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(private val container: AppContainer) : ViewModel() {

    private val repo: NoteRepository get() = container.notes

    data class ListState(
        val notes: List<NoteWithTags> = emptyList(),
        val query: String = "",
        val filterTag: String? = null,
        val isLoading: Boolean = true,
    ) {
        val visible: List<NoteWithTags>
            get() = notes
                .filter { note ->
                    query.isBlank() ||
                        note.note.title.contains(query, ignoreCase = true) ||
                        note.note.body.contains(query, ignoreCase = true) ||
                        note.tags.any { it.contains(query.trim().removePrefix("#").lowercase()) }
                }
                .filter { note -> filterTag == null || note.tags.contains(filterTag) }
        val allTags: List<String>
            get() = notes.flatMap { it.tags }.distinct().sorted()
        val isEmpty: Boolean get() = notes.isEmpty()
    }

    private val query = MutableStateFlow("")
    private val filterTag = MutableStateFlow<String?>(null)

    val listState: StateFlow<ListState> =
        combine(repo.observeNotes(), query, filterTag) { notes, q, tag ->
            ListState(notes = notes, query = q, filterTag = tag, isLoading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListState())

    data class EditorState(
        val id: String? = null,
        val title: String = "",
        val body: String = "",
        val tags: String = "",
        val kind: NoteKind = NoteKind.PLAIN,
        val pinned: Boolean = false,
        val loaded: Boolean = false,
        val saving: Boolean = false,
    ) {
        val canSave: Boolean get() = title.isNotBlank() || body.isNotBlank()
    }

    private val editor = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = editor

    /** Null = new note; non-null = existing note (values stay in this flow). */
    val editing: StateFlow<NoteWithTags?> = editor
        .flatMapLatest { state -> state.id?.let { repo.observeNote(it) } ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilterTag(tag: String?) {
        filterTag.value = tag
    }

    fun startNew() {
        editor.value = EditorState()
    }

    fun load(id: String) {
        viewModelScope.launch {
            val note = repo.get(id)
            editor.value = if (note == null) {
                EditorState()
            } else {
                EditorState(
                    id = note.id,
                    title = note.title,
                    body = note.body,
                    tags = repo.tagTextFor(id),
                    kind = NoteKind.fromCode(note.kind),
                    pinned = note.pinned,
                    loaded = true,
                )
            }
        }
    }

    fun setTitle(value: String) {
        editor.value = editor.value.copy(title = value)
    }

    fun setBody(value: String) {
        editor.value = editor.value.copy(body = value)
    }

    fun setTags(value: String) {
        editor.value = editor.value.copy(tags = value)
    }

    /** Explicit save; returns the note id when the write succeeded. */
    fun save(onSaved: (String) -> Unit) {
        val state = editor.value
        if (!state.canSave || state.saving) return
        editor.value = state.copy(saving = true)
        viewModelScope.launch {
            val id = state.id ?: run {
                repo.create(
                    title = state.title,
                    body = state.body,
                    kind = state.kind,
                    tagText = state.tags,
                )
            }
            if (state.id != null) {
                repo.update(state.id, state.title, state.body, state.tags)
            }
            editor.value = editor.value.copy(saving = false, id = id, loaded = true)
            onSaved(id)
        }
    }

    fun togglePinned(id: String, pinned: Boolean) {
        viewModelScope.launch { repo.setPinned(id, pinned) }
    }

    fun delete(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.moveToTrash(id)
            onDone()
        }
    }

    fun clearEditor() {
        editor.value = EditorState()
    }

    fun humanUpdated(epochMillis: Long): String = Dates.human(Dates.toLocalDate(epochMillis))
}
