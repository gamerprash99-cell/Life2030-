package com.lifeos.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.ai.AiResult
import com.lifeos.app.core.ai.AiRepository
import com.lifeos.app.core.ai.NoteAiAction
import com.lifeos.app.data.db.entities.NoteEntity
import com.lifeos.app.data.repository.NoteRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.domain.model.NoteBlock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NotesFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    ARCHIVED("Archived"),
    TRASH("Trash")
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotesListViewModel(private val noteRepository: NoteRepository) : ViewModel() {

    private val _filter = MutableStateFlow(NotesFilter.ALL)
    val filter: StateFlow<NotesFilter> = _filter

    private val _folder = MutableStateFlow<String?>(null)
    val folder: StateFlow<String?> = _folder

    val folders: StateFlow<List<String>> = noteRepository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = combine(_filter, _folder) { filter, folder -> filter to folder }
        .flatMapLatest { (filter, folder) ->
            when {
                folder != null -> noteRepository.observeByFolder(folder)
                filter == NotesFilter.FAVORITES -> noteRepository.observeFavorites()
                filter == NotesFilter.ARCHIVED -> noteRepository.observeArchived()
                filter == NotesFilter.TRASH -> noteRepository.observeTrash()
                else -> noteRepository.observeAll()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: NotesFilter) {
        _filter.value = filter
        if (filter != NotesFilter.ALL) _folder.value = null
    }

    fun setFolder(folder: String?) {
        _folder.value = folder
        if (folder != null) _filter.value = NotesFilter.ALL
    }

    fun togglePin(id: String, pinned: Boolean) = viewModelScope.launch { noteRepository.togglePin(id, pinned) }
    fun toggleFavorite(id: String, favorite: Boolean) = viewModelScope.launch { noteRepository.toggleFavorite(id, favorite) }
    fun setArchived(id: String, archived: Boolean) = viewModelScope.launch { noteRepository.archive(id, archived) }
    fun moveToTrash(id: String) = viewModelScope.launch { noteRepository.moveToTrash(id) }
    fun restoreFromTrash(id: String) = viewModelScope.launch { noteRepository.restoreFromTrash(id) }
    fun permanentlyDelete(id: String) = viewModelScope.launch { noteRepository.permanentlyDelete(id) }
}

class NoteEditorViewModel(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val aiRepository: AiRepository,
    private val existingNoteId: String?
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _blocks = MutableStateFlow<List<NoteBlock>>(listOf(NoteBlock.Paragraph(id = "b0", text = "")))
    val blocks: StateFlow<List<NoteBlock>> = _blocks

    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy

    private val _aiResult = MutableStateFlow<String?>(null)
    val aiResult: StateFlow<String?> = _aiResult

    private val _aiResultAction = MutableStateFlow<NoteAiAction?>(null)
    val aiResultAction: StateFlow<NoteAiAction?> = _aiResultAction

    private val _extractedTasks = MutableStateFlow<List<String>>(emptyList())
    val extractedTasks: StateFlow<List<String>> = _extractedTasks

    var noteId: String? = existingNoteId
        private set

    init {
        existingNoteId?.let { id ->
            viewModelScope.launch {
                noteRepository.getById(id)?.let { note ->
                    _title.value = note.title
                    _blocks.value = noteRepository.decodeBlocks(note).ifEmpty { listOf(NoteBlock.Paragraph(id = "b0", text = "")) }
                }
            }
        }
    }

    fun updateTitle(value: String) { _title.value = value }

    fun updateBlockText(blockId: String, text: String) {
        _blocks.value = _blocks.value.map { block ->
            when (block) {
                is NoteBlock.Paragraph -> if (block.id == blockId) block.copy(text = text) else block
                is NoteBlock.Heading -> if (block.id == blockId) block.copy(text = text) else block
                is NoteBlock.BulletItem -> if (block.id == blockId) block.copy(text = text) else block
                is NoteBlock.NumberedItem -> if (block.id == blockId) block.copy(text = text) else block
                is NoteBlock.ChecklistItem -> if (block.id == blockId) block.copy(text = text) else block
            }
        }
    }

    fun toggleChecklistItem(blockId: String) {
        _blocks.value = _blocks.value.map { block ->
            if (block is NoteBlock.ChecklistItem && block.id == blockId) block.copy(checked = !block.checked) else block
        }
    }

    fun addParagraphBlock() {
        _blocks.value = _blocks.value + NoteBlock.Paragraph(id = newBlockId("b"), text = "")
    }

    fun addChecklistBlock() {
        _blocks.value = _blocks.value + NoteBlock.ChecklistItem(id = newBlockId("c"), text = "")
    }

    private fun newBlockId(prefix: String) = "$prefix${_blocks.value.size}-${System.nanoTime()}"

    fun save() {
        viewModelScope.launch {
            val id = noteId
            if (id == null) {
                noteId = noteRepository.createNote(title = _title.value, blocks = _blocks.value)
            } else {
                noteRepository.updateNoteContent(id, _title.value, _blocks.value)
            }
        }
    }

    private fun currentPlainText(): String = _blocks.value.joinToString("\n") { block ->
        when (block) {
            is NoteBlock.Paragraph -> block.text
            is NoteBlock.Heading -> block.text
            is NoteBlock.BulletItem -> "• ${block.text}"
            is NoteBlock.NumberedItem -> "${block.index}. ${block.text}"
            is NoteBlock.ChecklistItem -> "[${if (block.checked) "x" else " "}] ${block.text}"
        }
    }

    /** Runs a note-level AI action (Section 8). Result is shown for review — never auto-applied. */
    fun runAiAction(action: NoteAiAction) {
        viewModelScope.launch {
            _aiBusy.value = true
            _aiResult.value = null
            _aiResultAction.value = action
            when (val result = aiRepository.runNoteAction(action, currentPlainText())) {
                is AiResult.Success -> _aiResult.value = result.text
                is AiResult.Error -> _aiResult.value = "Couldn't complete that: ${result.message}"
            }
            _aiBusy.value = false
        }
    }

    fun extractTasks() {
        viewModelScope.launch {
            _aiBusy.value = true
            when (val result = aiRepository.extractTasks(currentPlainText())) {
                is AiResult.Success -> _extractedTasks.value = aiRepository.parseExtractedTasks(result.text).map { it.title }
                is AiResult.Error -> _aiResult.value = "Couldn't scan for tasks: ${result.message}"
            }
            _aiBusy.value = false
        }
    }

    /**
     * Applies the reviewed AI output to the note (Section 21). This is the
     * explicit user approval step — the only path that inserts AI text — so a
     * result is never silently discarded when the dialog closes. Title actions
     * set the title; list-like actions become checklist/bullet blocks; prose
     * actions append a paragraph under an "AI · <action>" heading.
     */
    fun applyAiResult() {
        val result = _aiResult.value?.trim().orEmpty()
        if (result.isBlank()) return
        val action = _aiResultAction.value
        if (action == NoteAiAction.GENERATE_TITLE) {
            _title.value = result.lineSequence().firstOrNull()?.trim()?.trim('"').orEmpty()
            dismissAiResult()
            return
        }
        val lines = result.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.replace(Regex("^[-•*]\\s+"), "").replace(Regex("^\\d+[.)]\\s+"), "") }
        val additions = mutableListOf<NoteBlock>()
        additions += NoteBlock.Heading(id = newBlockId("h"), text = "AI · ${action?.label ?: "Result"}")
        if (lines.size > 1) {
            val asChecklist = action == NoteAiAction.CREATE_CHECKLIST
            lines.forEach { line ->
                additions += if (asChecklist) NoteBlock.ChecklistItem(id = newBlockId("c"), text = line)
                else NoteBlock.BulletItem(id = newBlockId("b"), text = line)
            }
        } else {
            additions += NoteBlock.Paragraph(id = newBlockId("p"), text = result)
        }
        _blocks.value = _blocks.value + additions
        dismissAiResult()
    }

    /** User taps [CREATE TASKS] to approve — the only path that writes tasks (Rule #9). */
    fun approveExtractedTasks(titles: List<String>) {
        viewModelScope.launch {
            taskRepository.createFromAiExtraction(
                titles = titles,
                dueDateEpochDay = null,
                sourceType = "note",
                sourceId = noteId ?: return@launch
            )
            _extractedTasks.value = emptyList()
        }
    }

    fun dismissAiResult() {
        _aiResult.value = null
        _aiResultAction.value = null
    }
}
