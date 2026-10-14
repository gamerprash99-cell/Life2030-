package com.lifeos.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.data.db.entities.NoteEntity
import com.lifeos.app.ui.components.LifeOSEmptyState
import com.lifeos.app.ui.components.LifeOSFilterChips
import com.lifeos.app.ui.components.LifeOSSearchBar
import com.lifeos.app.ui.components.LifeOSSkeletonList
import com.lifeos.app.ui.components.LifeOSSurface
import com.lifeos.app.ui.components.LiquidGlassFab
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Smart Notes — the long-form thinking surface. A calm, editorial list: a live
 * search field, the five smart views, a folder rail, then the notes themselves.
 * Pin/favorite/archive live behind each card's overflow so the list stays quiet.
 */
@Composable
fun NotesScreen(
    onOpenNote: (String) -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: NotesViewModel = viewModel(
        factory = LambdaViewModelFactory { NotesViewModel(locator.noteRepository) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LifeOSSpacing.screenPadding,
                end = LifeOSSpacing.screenPadding,
                top = LifeOSSpacing.sm,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.xs)) {
                    Text("Notes", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        if (state.notes.isEmpty()) "Your ideas live here" else "${state.notes.size} note${if (state.notes.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                LifeOSSearchBar(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Search notes"
                )
            }
            item {
                LifeOSFilterChips(
                    options = NotesFilter.entries.toList(),
                    selected = state.filter,
                    onSelect = viewModel::onFilterChange,
                    label = { it.label }
                )
            }
            if (state.folders.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
                        item {
                            FolderChip(name = "All notes", selected = state.folderId == null) { viewModel.onFolderChange(null) }
                        }
                        items(state.folders, key = { it.id }) { folder ->
                            FolderChip(name = folder.name, selected = state.folderId == folder.id) { viewModel.onFolderChange(folder.id) }
                        }
                    }
                }
            }

            when {
                state.loading -> item { LifeOSSkeletonList(rows = 5) }
                state.notes.isEmpty() -> item {
                    LifeOSEmptyState(
                        icon = Icons.Filled.Description,
                        title = if (state.query.isNotBlank()) "No matches" else "No notes yet",
                        body = if (state.query.isNotBlank())
                            "Nothing here matches “${state.query}”. Try another word."
                        else
                            "Notes are for thinking out loud — plans, ideas, study, anything without a date.",
                        actionLabel = if (state.query.isNotBlank()) null else "Write a note",
                        onAction = if (state.query.isNotBlank()) null else ({ onOpenNote("new") })
                    )
                }
                else -> items(state.notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onOpenNote(note.id) },
                        onTogglePin = { viewModel.togglePinned(note) },
                        onToggleFavorite = { viewModel.toggleFavorite(note) },
                        onArchive = { viewModel.archive(note, !note.isArchived) },
                        onTrash = { viewModel.trash(note) }
                    )
                }
            }
        }

        LiquidGlassFab(
            icon = Icons.Filled.Add,
            contentDescription = "New note",
            onClick = { onOpenNote("new") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = LifeOSSpacing.screenPadding, bottom = 96.dp)
        )
    }
}

@Composable
private fun FolderChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val fg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        name,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(LifeOSRadius.pill))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit
) {
    LifeOSSurface(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
                if (note.isPinned) Icon(Icons.Filled.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                Text(
                    note.title.ifBlank { "Untitled note" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                NoteMenu(
                    note = note,
                    onTogglePin = onTogglePin,
                    onToggleFavorite = onToggleFavorite,
                    onArchive = onArchive,
                    onTrash = onTrash
                )
            }
            if (note.content.isNotBlank()) {
                Text(
                    note.content.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
                if (note.isFavorite) Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = LocalLifeOSSemanticColors.current.gold, modifier = Modifier.size(14.dp))
                val tags = note.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                tags.take(3).forEach { tag ->
                    Text(
                        "#$tag",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(LifeOSRadius.pill))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(relativeTime(note.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NoteMenu(
    note: NoteEntity,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Note actions", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(if (note.isPinned) "Unpin" else "Pin") },
                leadingIcon = { Icon(if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null) },
                onClick = { expanded = false; onTogglePin() }
            )
            DropdownMenuItem(
                text = { Text(if (note.isFavorite) "Remove favorite" else "Favorite") },
                leadingIcon = { Icon(if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder, null) },
                onClick = { expanded = false; onToggleFavorite() }
            )
            DropdownMenuItem(
                text = { Text(if (note.isArchived) "Unarchive" else "Archive") },
                leadingIcon = { Icon(if (note.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive, null) },
                onClick = { expanded = false; onArchive() }
            )
            DropdownMenuItem(
                text = { Text("Move to trash") },
                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                onClick = { expanded = false; onTrash() }
            )
        }
    }
}

private fun relativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        diff < minute -> "now"
        diff < hour -> "${diff / minute}m"
        diff < day -> "${diff / hour}h"
        diff < 7 * day -> "${diff / day}d"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))
    }
}
