package com.lifeos.app.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.ui.components.GlassCard
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.time.LocalDate

/**
 * Diary — an editorial journal list with paper cards, a date strip for
 * filtering and a mood-first composer. Every value shown comes from the
 * Room-backed diary repository; no simulated data.
 */
@Composable
fun DiaryScreen(
    onBack: () -> Unit = {},
    onOpenEntry: (String) -> Unit = {}
) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryViewModel = viewModel(
        factory = LambdaViewModelFactory {
            DiaryViewModel(locator.diaryRepository)
        }
    )
    val entries by viewModel.entries.collectAsState()
    val filtered by viewModel.filteredEntries.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val showEditor by viewModel.showEditor.collectAsState()
    val editingEntry by viewModel.editingEntry.collectAsState()
    val entryToDelete by viewModel.entryToDelete.collectAsState()

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            LifeOSTopBar("Diary", "Reflect and keep your journal local", onBack = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::startNewEntry,
                shape = CircleShape,
                containerColor = DiaryLavender,
                contentColor = DiaryInkViolet
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New entry")
            }
        }
    ) { padding ->
        if (entries.isEmpty()) {
            DiaryEmptyState(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = LifeOSSpacing.screenPadding, end = LifeOSSpacing.screenPadding,
                    top = 4.dp, bottom = LifeOSSpacing.fabContentClearance
                ),
                verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.cardSpacing)
            ) {
                item { DayStrip(selectedDay = selectedDay, onSelect = viewModel::selectDay) }

                items(filtered, key = { it.id }) { entry ->
                    DiaryEntryCard(
                        entry = entry,
                        onOpen = { onOpenEntry(entry.id) },
                        onEdit = { viewModel.startEdit(entry) },
                        onDelete = { viewModel.requestDelete(entry) }
                    )
                }
            }
        }
    }

    if (showEditor) {
        DiaryEditorSheet(
            editing = editingEntry,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::saveEntry,
            onDelete = { editingEntry?.let { viewModel.requestDelete(it) }; viewModel.dismissEditor() }
        )
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Delete this entry?") },
            text = { Text("This permanently removes the diary entry from this device.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteEntry(entry.id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissDelete) { Text("Cancel") } }
        )
    }
}

/** Horizontal date filter: "All" plus the last 14 days (newest first). */
@Composable
private fun DayStrip(selectedDay: Long?, onSelect: (Long?) -> Unit) {
    val today = DateTimeUtils.today()
    val days = remember(today) { (0L..13L).map { today.minusDays(it) } }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            DayChip(
                label = "All",
                selected = selectedDay == null,
                onClick = { onSelect(null) }
            )
        }
        items(days, key = { it.toEpochDay() }) { day ->
            DayChip(
                label = dayChipLabel(day),
                sublabel = dayChipWeekday(day),
                selected = selectedDay == day.toEpochDay(),
                onClick = { onSelect(day.toEpochDay()) }
            )
        }
    }
}

@Composable
private fun DayChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    sublabel: String? = null
) {
    val shape = RoundedCornerShape(14.dp)
    val container = if (selected) DiaryLavender else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) DiaryLavender else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    Column(
        modifier = Modifier
            .width(if (sublabel != null) 52.dp else 62.dp)
            .clip(shape)
            .background(container)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        if (sublabel != null) {
            Text(sublabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        } else {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

private fun dayChipWeekday(day: LocalDate): String =
    DateTimeUtils.shortDayName(day).take(3)

private fun dayChipLabel(day: LocalDate): String = day.dayOfMonth.toString()

/** The journal card: date + mood pill, body, and any stored tags. */
@Composable
private fun DiaryEntryCard(
    entry: DiaryEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val date = DateTimeUtils.epochDayToLocalDate(entry.dateEpochDay)
    val tags = remember(entry.id, entry.tagsCsv) { entryTags(entry) }
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onOpen), cornerRadius = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        DateTimeUtils.formatFullDate(date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    EntryMoodPill(entry = entry)
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit entry",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete entry",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Text(
                entry.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        Text(
                            tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun entryTags(entry: DiaryEntity): List<String> = entry.tagsCsv
    .split(',')
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .take(2)

/** Muted, on-brand empty state — mirrors the Stitch empty screen (icon + copy + FAB hint). */
@Composable
private fun DiaryEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(DiaryLavender.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Book, contentDescription = null, tint = DiaryInkViolet, modifier = Modifier.size(36.dp))
        }
        Text(
            "No diary entries yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            "Start writing your first entry — it stays on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, start = 32.dp, end = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}