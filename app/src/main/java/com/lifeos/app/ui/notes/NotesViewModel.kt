package com.lifeos.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.data.db.entities.NoteEntity
import com.lifeos.app.data.db.entities.NoteFolderEntity
import com.lifeos.app.data.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The five smart views on the Notes list, matching the spec's filter chips. */
enum class NotesFilter(val label: String) {
    ALL("All"),
    RECENT("Recent"),
    PINNED("Pinned"),
    FAVORITES("Favorites"),
    ARCHIVED("Archived")
}

/** Everything the Notes list renders. [loading] drives the skeleton, never a spinner. */
data class NotesUiState(
    val loading: Boolean = true,
    val filter: NotesFilter = NotesFilter.ALL,
    val folderId: String? = null,
    val query: String = "",
    val notes: List<NoteEntity> = emptyList(),
    val folders: List<NoteFolderEntity> = emptyList()
)

/**
 * Backs the Smart Notes list. Reads are reactive Room flows, so a note pinned,
 * archived or edited anywhere updates the list without a manual refresh. Search
 * and folder filtering run over the already-loaded rows — the list a person
 * scrolls is small enough that an in-memory filter is instant and keeps one
 * source of truth.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val filter = MutableStateFlow(NotesFilter.ALL)
    private val folderId = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")

    private data class Params(
        val filter: NotesFilter,
        val folderId: String?,
        val query: String,
        val folders: List<NoteFolderEntity>
    )

    val state: StateFlow<NotesUiState> = combine(
        filter, folderId, query, repository.observeFolders()
    ) { f, folder, q, folders -> Params(f, folder, q, folders) }
        .flatMapLatest { params ->
            notesFor(params.filter).map { rows ->
                NotesUiState(
                    loading = false,
                    filter = params.filter,
                    folderId = params.folderId,
                    query = params.query,
                    folders = params.folders,
                    notes = rows
                        .filter { params.folderId == null || it.folderId == params.folderId }
                        .filter { matches(it, params.query) }
                        .sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAt })
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    private fun notesFor(filter: NotesFilter): Flow<List<NoteEntity>> = when (filter) {
        NotesFilter.ALL, NotesFilter.RECENT -> repository.observeActive()
        NotesFilter.PINNED -> repository.observePinned()
        NotesFilter.FAVORITES -> repository.observeFavorites()
        NotesFilter.ARCHIVED -> repository.observeArchived()
    }

    private fun matches(note: NoteEntity, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim()
        return note.title.contains(q, ignoreCase = true) ||
            note.content.contains(q, ignoreCase = true) ||
            note.tagsCsv.contains(q, ignoreCase = true)
    }

    fun onFilterChange(next: NotesFilter) {
        filter.value = next
        if (next == NotesFilter.ARCHIVED) folderId.value = null
    }

    fun onFolderChange(id: String?) { folderId.value = id }
    fun onQueryChange(text: String) { query.value = text }

    fun togglePinned(note: NoteEntity) = viewModelScope.launch { repository.setPinned(note.id, !note.isPinned) }
    fun toggleFavorite(note: NoteEntity) = viewModelScope.launch { repository.setFavorite(note.id, !note.isFavorite) }
    fun archive(note: NoteEntity, archived: Boolean = true) = viewModelScope.launch { repository.setArchived(note.id, archived) }
    fun trash(note: NoteEntity) = viewModelScope.launch { repository.trash(note.id) }
    fun restore(note: NoteEntity) = viewModelScope.launch { repository.restore(note.id) }
    fun deleteForever(note: NoteEntity) = viewModelScope.launch { repository.deleteForever(note.id) }

    fun createFolder(name: String) = viewModelScope.launch { repository.createFolder(name) }
}
