package com.lifeos.app.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.EmptyState
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import com.lifeos.app.ui.components.Pill
import com.lifeos.app.ui.components.SectionHeader

/** Notes list: search, tag filter, pinned-first ordering (from the repository). */
@Composable
fun NotesScreen(
    onOpenNote: (String) -> Unit,
    onCreateNote: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()

    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LifeOsTopBar(
                title = "Notes",
                onBack = null,
                actions = {
                    IconButton(onClick = onCreateNote) {
                        Icon(Icons.Default.Add, contentDescription = "New note")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNote,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "New note")
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                placeholder = { Text("Search notes") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                colors = fieldColors(),
            )

            if (state.allTags.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    item {
                        Pill(
                            text = "All",
                            selected = state.filterTag == null,
                            onClick = { viewModel.setFilterTag(null) },
                        )
                    }
                    items(state.allTags) { tag ->
                        Pill(
                            text = "#$tag",
                            selected = state.filterTag == tag,
                            onClick = {
                                viewModel.setFilterTag(if (state.filterTag == tag) null else tag)
                            },
                        )
                    }
                }
            }

            when {
                state.isLoading -> Spacer(Modifier.height(Spacing.lg))
                state.visible.isEmpty() -> EmptyState(
                    title = if (state.isEmpty) "No notes yet" else "Nothing matches",
                    subtitle = if (state.isEmpty) {
                        "Everything you write stays on this device. Tap + to write your first note."
                    } else {
                        "Try a different word or clear the tag filter."
                    },
                    modifier = Modifier.fillMaxSize(),
                    actionLabel = if (state.isEmpty) "New note" else null,
                    onAction = if (state.isEmpty) onCreateNote else null,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(state.visible, key = { it.note.id }) { item ->
                        NoteRow(item = item, onClick = { onOpenNote(item.note.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(item: com.lifeos.app.core.data.NoteWithTags, onClick: () -> Unit) {
    LifeOsCard(onClick = onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.note.pinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = Spacing.xs),
                    )
                }
                Text(
                    text = item.note.title.ifBlank { "Untitled note" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (item.note.body.isNotBlank()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = item.note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.tags.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    item.tags.take(4).forEach { tag -> Pill(text = "#$tag") }
                }
            }
        }
    }
}

/** Note editor: title / body / tags, explicit Save, delete-confirmation. */
@Composable
fun NoteEditorScreen(
    noteId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val editor by viewModel.editorState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(noteId) {
        if (noteId == null || noteId == "new") viewModel.startNew() else viewModel.load(noteId)
    }

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(
            title = if (noteId == null || noteId == "new") "New note" else "Edit note",
            onBack = onBack,
            actions = {
                if (editor.canSave) {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.save { onBack() } },
                        enabled = !editor.saving,
                    ) {
                        Text(if (editor.saving) "Saving…" else "Save")
                    }
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedTextField(
                value = editor.title,
                onValueChange = viewModel::setTitle,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Title") },
                textStyle = MaterialTheme.typography.headlineSmall,
                singleLine = true,
                colors = fieldColors(),
            )
            OutlinedTextField(
                value = editor.body,
                onValueChange = viewModel::setBody,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.huge * 6),
                placeholder = { Text("Write…") },
                colors = fieldColors(),
            )
            OutlinedTextField(
                value = editor.tags,
                onValueChange = viewModel::setTags,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tags, e.g. #health #work") },
                supportingText = { Text("Tags are stored lowercased; #Health equals #health.") },
                singleLine = true,
                colors = fieldColors(),
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = LifeOs.tokens.border,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
)
