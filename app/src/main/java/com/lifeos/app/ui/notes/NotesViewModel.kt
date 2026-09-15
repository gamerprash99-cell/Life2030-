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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesListViewModel(private val noteRepository: NoteRepository) : ViewModel() {
    val notes: StateFlow<List<NoteEntity>> = noteRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun togglePin(id: String, pinned: Boolean) = viewModelScope.launch { noteRepository.togglePin(id, pinned) }
    fun toggleFavorite(id: String, favorite: Boolean) = viewModelScope.launch { noteRepository.toggleFavorite(id, favorite) }
    fun moveToTrash(id: String) = viewModelScope.launch { noteRepository.moveToTrash(id) }
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
        _blocks.value = _blocks.value + NoteBlock.Paragraph(id = "b${_blocks.value.size}-${System.nanoTime()}", text = "")
    }

    fun addChecklistBlock() {
        _blocks.value = _blocks.value + NoteBlock.ChecklistItem(id = "c${_blocks.value.size}-${System.nanoTime()}", text = "")
    }

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

    fun dismissAiResult() { _aiResult.value = null }
}
