package com.lifeos.app.ui.diary

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * LifeOS Diary — a memory timeline, not a notes list.
 *
 * Composition is day → mood → memory: an editorial masthead naming the day, a
 * hairline spine with each memory's mood resting on it, and the journal text
 * as the loudest element on the page. There is no card stack, no date-pill
 * strip and no floating button.
 *
 * Every value shown is Room-backed through [DiaryViewModel]; navigation,
 * repositories and the database are unchanged.
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
    val memories by viewModel.memoriesForSelectedDay.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val daysWithMemories by viewModel.daysWithMemories.collectAsState()
    val showEditor by viewModel.showEditor.collectAsState()
    val editingEntry by viewModel.editingEntry.collectAsState()
    val entryToDelete by viewModel.entryToDelete.collectAsState()
    val editorTimeMinutes by viewModel.editorTimeMinutes.collectAsState()
    val saving by viewModel.saving.collectAsState()

    // With the editor open, Back closes the editor. Otherwise it leaves Diary.
    BackHandler(enabled = showEditor, onBack = viewModel::dismissEditor)
    BackHandler(enabled = !showEditor, onBack = onBack)

    Box(Modifier.fillMaxSize()) {
        Scaffold { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                DiaryDayHeader(
                    selectedDay = selectedDay,
                    daysWithMemories = daysWithMemories,
                    onBack = onBack,
                    onCreate = viewModel::startNewEntry,
                    onSelectDay = viewModel::selectDay
                )

                if (memories.isEmpty()) {
                    DiaryEmptyState(
                        dayLabel = DateTimeUtils.formatFullDate(
                            DateTimeUtils.epochDayToLocalDate(selectedDay)
                        ),
                        onCreate = viewModel::startNewEntry
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = LifeOSSpacing.screenPadding,
                            end = LifeOSSpacing.screenPadding,
                            top = LifeOSSpacing.sectionSpacing,
                            bottom = LifeOSSpacing.fabContentClearance
                        )
                    ) {
                        itemsIndexed(memories, key = { _, entry -> entry.id }) { index, entry ->
                            MemoryMoment(
                                entry = entry,
                                isFirst = index == 0,
                                isLast = index == memories.lastIndex,
                                onOpen = { onOpenEntry(entry.id) },
                                onEdit = { viewModel.startEdit(entry) },
                                onDelete = { viewModel.requestDelete(entry) },
                                modifier = Modifier.revealAsMemory(index)
                            )
                        }

                        item {
                            MemoryStreamFooter(
                                count = memories.size,
                                onCreate = viewModel::startNewEntry
                            )
                        }
                    }
                }
            }
        }

        DiaryEditorOverlay(visible = showEditor) {
            DiaryEditor(
                dayEpochDay = editingEntry?.dateEpochDay ?: selectedDay,
                editing = editingEntry,
                timeMinutes = editorTimeMinutes,
                saving = saving,
                onDismiss = viewModel::dismissEditor,
                onSave = viewModel::saveEntry,
                onDelete = {
                    editingEntry?.let(viewModel::requestDelete)
                    viewModel.dismissEditor()
                }
            )
        }
    }


    entryToDelete?.let { entry ->
        MemoryDeleteDialog(
            dayEpochDay = entry.dateEpochDay,
            timeMinutes = entry.timeMinutes,
            onConfirm = { viewModel.deleteEntry(entry.id) },
            onDismiss = viewModel::dismissDelete
        )
    }
}

/** The "+ Memory" action that closes the day — inline, never floating. */
@Composable
private fun MemoryStreamFooter(count: Int, onCreate: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (count == 1) "1 memory on this day" else "$count memories on this day",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onCreate)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                "+ Memory",
                style = MaterialTheme.typography.labelLarge,
                color = DiaryInkViolet,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
private fun SavedMemoryToast() {
    Box(
        modifier = Modifier
            .padding(top = 18.dp)
            .clip(CircleShape)
            .background(DiaryInkViolet)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "✓",
                color = MaterialTheme.colorScheme.background,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "Memory saved",
                color = MaterialTheme.colorScheme.background,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
