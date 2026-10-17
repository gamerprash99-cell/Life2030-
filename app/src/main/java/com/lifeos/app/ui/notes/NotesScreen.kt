package com.lifeos.app.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.data.db.entities.NoteEntity
import com.lifeos.app.ui.components.HeaderActionCircle
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSEmptyState
import com.lifeos.app.ui.components.LifeOSFilterChips
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.LifeOSSearchBar
import com.lifeos.app.ui.components.LifeOSSectionHeader
import com.lifeos.app.ui.components.LifeOSSkeletonList
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.theme.FigmaAccent
import com.lifeos.app.ui.theme.FigmaAccentStroke
import com.lifeos.app.ui.theme.FigmaCard
import com.lifeos.app.ui.theme.FigmaStroke
import com.lifeos.app.ui.theme.FigmaText
import com.lifeos.app.ui.theme.FigmaTertiary
import com.lifeos.app.ui.theme.FigmaWash
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The order the library is read in. The trailing label states which is live. */
private enum class NoteSort(val label: String) {
    RECENT("Recently edited ↓"),
    TITLE("Title A–Z ↓")
}

/**
 * SMART NOTES · LIBRARY — the Figma frame `#5:36299`.
 *
 * A calm, editorial library: the shared 86dp page header (mint kicker "THINK IT.
 * KEEP IT." + "Smart Notes" + a 42×42 sort action), the search field, the smart
 * views, the folder rail, a pinned shelf, then the notes. Pin/favorite/archive
 * stay behind each card's overflow so the list itself stays quiet.
 *
 * Every control is real. The sort action and the "Your notes" trailing label are
 * the same toggle over the list the ViewModel already returns; the folder rail
 * filters through the existing ViewModel; the smart views are the existing
 * filters. Nothing here is decorative.
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
    var sort by rememberSaveable { mutableStateOf(NoteSort.RECENT) }

    val folderNames = remember(state.folders) { state.folders.associate { it.id to it.name } }
    // Pinned notes get their own shelf only on the unfiltered "All" view: under
    // Pinned everything is pinned, so a second shelf would just repeat the list.
    val pinned = if (state.filter == NotesFilter.ALL) state.notes.filter { it.isPinned } else emptyList()
    val rest = if (pinned.isEmpty()) state.notes else state.notes.filterNot { it.isPinned }
    val orderedPinned = remember(pinned, sort) { pinned.sortedWith(sort.comparator) }
    val orderedRest = remember(rest, sort) { rest.sortedWith(sort.comparator) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = LifeOSSpacing.screenPadding,
            end = LifeOSSpacing.screenPadding,
            top = 0.dp,
            bottom = LifeOSSpacing.screenPadding
        ),
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
    ) {
        item {
            LifeOSTopBar(
                title = "Smart Notes",
                subtitle = "Think it. Keep it.",
                actions = {
                    HeaderActionCircle(Icons.Filled.Tune, "Sort notes", onClick = {
                        sort = if (sort == NoteSort.RECENT) NoteSort.TITLE else NoteSort.RECENT
                    })
                }
            )
        }
        item {
            LifeOSSearchBar(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Search notes, tags or attachments"
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            else -> {
                if (orderedPinned.isNotEmpty()) {
                    item { LifeOSSectionHeader("Pinned for you", "All folders") { viewModel.onFolderChange(null) } }
                    items(orderedPinned, key = { "pin-${it.id}" }) { note ->
                        NoteCard(
                            note = note,
                            folderName = note.folderId?.let { folderNames[it] },
                            onClick = { onOpenNote(note.id) },
                            onTogglePin = { viewModel.togglePinned(note) },
                            onToggleFavorite = { viewModel.toggleFavorite(note) },
                            onArchive = { viewModel.archive(note, !note.isArchived) },
                            onTrash = { viewModel.trash(note) }
                        )
                    }
                }
                item {
                    LifeOSSectionHeader("Your notes", sort.label) {
                        sort = if (sort == NoteSort.RECENT) NoteSort.TITLE else NoteSort.RECENT
                    }
                }
                items(orderedRest, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        folderName = note.folderId?.let { folderNames[it] },
                        onClick = { onOpenNote(note.id) },
                        onTogglePin = { viewModel.togglePinned(note) },
                        onToggleFavorite = { viewModel.toggleFavorite(note) },
                        onArchive = { viewModel.archive(note, !note.isArchived) },
                        onTrash = { viewModel.trash(note) }
                    )
                }
            }
        }

        item {
            LifeOSPrimaryButton(
                text = "New note",
                icon = Icons.Filled.Add,
                onClick = { onOpenNote("new") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** The comparator the sort toggle switches between. Pure ordering, no data change. */
private val NoteSort.comparator: Comparator<NoteEntity>
    get() = when (this) {
        NoteSort.RECENT -> compareByDescending { it.updatedAt }
        NoteSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title.ifBlank { "Untitled note" } }
    }

/**
 * Folder shortcut, restyled to the Figma pill: a folder glyph and the name on the
 * faint wash, mint-tinted when it is the active folder. Selects through the
 * existing ViewModel folder filter.
 */
@Composable
private fun FolderChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(LifeOSRadius.pill)
    Row(
        Modifier
            .clip(shape)
            .background(if (selected) FigmaAccent.copy(alpha = 0.16f) else FigmaWash)
            .border(BorderStroke(1.dp, if (selected) FigmaAccentStroke else FigmaStroke), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = name; role = Role.Button }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            Icons.Filled.Folder,
            contentDescription = null,
            tint = if (selected) FigmaAccent else FigmaTertiary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) FigmaAccent else FigmaTertiary,
            maxLines = 1
        )
    }
}

/**
 * One note: heading (pin marker + title) with its overflow, the preview, then the
 * metadata line — `#tag · Folder` and when it was last touched. The tag comes from
 * the note's own CSV, the folder from the folder table, both real.
 */
@Composable
private fun NoteCard(
    note: NoteEntity,
    folderName: String?,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit
) {
    val firstTag = note.tagsCsv.split(",").map { it.trim() }.firstOrNull { it.isNotEmpty() }
    val meta = when {
        firstTag != null && folderName != null -> "#$firstTag  ·  $folderName"
        firstTag != null -> "#$firstTag"
        folderName != null -> folderName
        else -> "Note"
    }
    LifeOSCard(Modifier.fillMaxWidth(), tint = FigmaCard) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { role = Role.Button },
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (note.isPinned) {
                    Icon(Icons.Filled.PushPin, contentDescription = "Pinned", tint = FigmaAccent, modifier = Modifier.size(15.dp))
                }
                Text(
                    note.title.ifBlank { "Untitled note" },
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    color = FigmaText,
                    maxLines = 2,
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
                    color = FigmaText.copy(alpha = 0.62f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (note.isFavorite) {
                    Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = FigmaAccent, modifier = Modifier.size(14.dp))
                }
                Text(
                    meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = FigmaAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                Text(
                    relativeTime(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = FigmaTertiary,
                    maxLines = 1
                )
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
                .clickable { expanded = true }
                .semantics { contentDescription = "Note actions"; role = Role.Button },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = null, tint = FigmaTertiary, modifier = Modifier.size(18.dp))
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
