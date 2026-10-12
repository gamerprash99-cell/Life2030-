package com.lifeos.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.data.repository.NoteRepository
import com.lifeos.app.ui.components.LifeOSIconButton
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A single note's editing state. [noteId] is null until a first save, and
 * [saved] is a one-shot signal the screen uses to close.
 */
data class NoteEditorUiState(
    val loading: Boolean = true,
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val folderId: String? = null,
    val noteId: String? = null,
    val saved: Boolean = false
)

/** Backs the note editor. A new note is created lazily on first save. */
class NoteEditorViewModel(
    private val repository: NoteRepository,
    requestedId: String
) : ViewModel() {

    private val _state = MutableStateFlow(NoteEditorUiState())
    val state: StateFlow<NoteEditorUiState> = _state.asStateFlow()

    private var existingId: String? = requestedId.takeUnless { it == "new" || it.isBlank() }

    init {
        val id = existingId
        if (id == null) {
            _state.update { it.copy(loading = false) }
        } else {
            viewModelScope.launch {
                val note = repository.getById(id)
                _state.update {
                    if (note == null) it.copy(loading = false)
                    else it.copy(
                        loading = false,
                        title = note.title,
                        content = note.content,
                        tags = note.tagsCsv.split(",").map(String::trim).filter(String::isNotEmpty),
                        folderId = note.folderId,
                        noteId = note.id
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value) }
    fun onContentChange(value: String) = _state.update { it.copy(content = value) }

    fun addTag(raw: String) {
        val tag = raw.trim().removePrefix("#")
        if (tag.isEmpty()) return
        _state.update { s -> if (s.tags.any { it.equals(tag, true) }) s else s.copy(tags = s.tags + tag) }
    }

    fun removeTag(tag: String) = _state.update { s -> s.copy(tags = s.tags - tag) }

    /** Persists the note. An empty note is not written — there is nothing to keep. */
    fun save() {
        val current = _state.value
        if (current.title.isBlank() && current.content.isBlank() && current.tags.isEmpty()) {
            _state.update { it.copy(saved = true) }
            return
        }
        viewModelScope.launch {
            val id = existingId
            if (id == null) {
                val newId = repository.create(current.title, current.content, current.tags, current.folderId)
                existingId = newId
                _state.update { it.copy(saved = true, noteId = newId) }
            } else {
                repository.update(id, current.title, current.content, current.tags, current.folderId)
                _state.update { it.copy(saved = true) }
            }
        }
    }
}

/**
 * The note editor. A quiet, full-window writing surface: a large title, a
 * borderless body, and a lightweight tag rail. Save is explicit and the screen
 * closes on the one-shot [NoteEditorUiState.saved] signal.
 */
@Composable
fun NoteEditorScreen(
    noteId: String,
    onBack: () -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: NoteEditorViewModel = viewModel(
        key = noteId,
        factory = LambdaViewModelFactory { NoteEditorViewModel(locator.noteRepository, noteId) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = LifeOSSpacing.screenPadding)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = LifeOSSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
        ) {
            LifeOSIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text(
                if (state.noteId == null) "New note" else "Editing",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            LifeOSPrimaryButton(
                text = "Save",
                onClick = viewModel::save,
                icon = Icons.Filled.Check
            )
        }

        BasicTextField(
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            textStyle = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(LifeOSPrimary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (state.title.isEmpty()) {
                        Text("Title", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    inner()
                }
            }
        )
        Spacer(Modifier.height(LifeOSSpacing.md))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
            if (state.tags.isNotEmpty()) {
                LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
                    items(state.tags, key = { it }) { tag ->
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(LifeOSRadius.pill))
                                .background(LifeOSPrimary.copy(alpha = 0.12f))
                                .clickable { viewModel.removeTag(tag) }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("#$tag", style = MaterialTheme.typography.labelSmall, color = LifeOSPrimary)
                            Text("×", style = MaterialTheme.typography.labelSmall, color = LifeOSPrimary)
                        }
                    }
                }
            } else Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(LifeOSSpacing.sm))

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            BasicTextField(
                value = state.content,
                onValueChange = viewModel::onContentChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(LifeOSPrimary),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { inner ->
                    Box {
                        if (state.content.isEmpty()) {
                            Text(
                                "Start writing…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}
