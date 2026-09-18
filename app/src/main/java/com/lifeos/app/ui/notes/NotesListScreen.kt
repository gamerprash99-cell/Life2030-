package com.lifeos.app.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.data.db.entities.NoteEntity
import com.lifeos.app.ui.components.GlassCard
import com.lifeos.app.ui.components.LifeOSTopBar

@Composable
fun NotesListScreen(onOpenNote: (String?) -> Unit, onBack: () -> Unit = {}) {
    val locator = LocalServiceLocator.current
    val viewModel: NotesListViewModel = viewModel(factory = LambdaViewModelFactory { NotesListViewModel(locator.noteRepository) })
    val notes by viewModel.notes.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val folder by viewModel.folder.collectAsState()
    val folders by viewModel.folders.collectAsState()

    Scaffold(
        topBar = { LifeOSTopBar("Notes", "Your local ideas and notes", onBack = onBack) },
        floatingActionButton = { FloatingActionButton(onClick = { onOpenNote(null) }) { Icon(Icons.Filled.Add, contentDescription = "New note") } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotesFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option && folder == null,
                        onClick = { viewModel.setFilter(option) },
                        label = { Text(option.label) }
                    )
                }
            }
            if (folders.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = folder == null, onClick = { viewModel.setFolder(null) }, label = { Text("All folders") })
                    folders.forEach { name ->
                        FilterChip(
                            selected = folder == name,
                            onClick = { viewModel.setFolder(name) },
                            leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.padding(0.dp)) },
                            label = { Text(name) }
                        )
                    }
                }
            }

            if (notes.isEmpty()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(emptyMessage(filter), style = MaterialTheme.typography.titleMedium)
                    Text("Notes stay encrypted on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = com.lifeos.app.ui.theme.LifeOSSpacing.fabContentClearance),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteRow(
                            note = note,
                            filter = filter,
                            onOpen = { onOpenNote(note.id) },
                            onTogglePin = { viewModel.togglePin(note.id, !note.isPinned) },
                            onToggleFavorite = { viewModel.toggleFavorite(note.id, !note.isFavorite) },
                            onArchive = { viewModel.setArchived(note.id, !note.isArchived) },
                            onTrash = { viewModel.moveToTrash(note.id) },
                            onRestore = { viewModel.restoreFromTrash(note.id) },
                            onDeleteForever = { viewModel.permanentlyDelete(note.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun emptyMessage(filter: NotesFilter): String = when (filter) {
    NotesFilter.ALL -> "No notes yet"
    NotesFilter.FAVORITES -> "No favorites yet"
    NotesFilter.ARCHIVED -> "Nothing archived"
    NotesFilter.TRASH -> "Trash is empty"
}

@Composable
private fun NoteRow(
    note: NoteEntity,
    filter: NotesFilter,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val inTrash = filter == NotesFilter.TRASH

    GlassCard(modifier = Modifier.fillMaxWidth().clickable(enabled = !inTrash, onClick = onOpen)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title.ifBlank { "Untitled note" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    note.plainTextForSearch.take(80),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (!inTrash) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (note.isFavorite) "Remove favorite" else "Add favorite",
                        tint = if (note.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (note.isPinned) Icon(Icons.Filled.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More actions") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (inTrash) {
                    DropdownMenuItem(
                        text = { Text("Restore") },
                        leadingIcon = { Icon(Icons.Filled.Restore, contentDescription = null) },
                        onClick = { menuOpen = false; onRestore() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete forever") },
                        leadingIcon = { Icon(Icons.Filled.DeleteForever, contentDescription = null) },
                        onClick = { menuOpen = false; onDeleteForever() }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text(if (note.isPinned) "Unpin" else "Pin") },
                        leadingIcon = { Icon(Icons.Filled.PushPin, contentDescription = null) },
                        onClick = { menuOpen = false; onTogglePin() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (note.isArchived) "Unarchive" else "Archive") },
                        leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                        onClick = { menuOpen = false; onArchive() }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to trash") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = { menuOpen = false; onTrash() }
                    )
                }
            }
        }
    }
}
