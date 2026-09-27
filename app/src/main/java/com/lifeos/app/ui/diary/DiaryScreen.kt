package com.lifeos.app.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.media.DiaryAudioPlayer
import com.lifeos.app.core.media.DiaryAudioRecorder
import com.lifeos.app.core.media.DevicePhotoImporter
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.LifeOSSpacing
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DiaryScreen(onBack: () -> Unit = {}, onOpenEntry: (String) -> Unit = {}) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryViewModel = viewModel(factory = LambdaViewModelFactory { DiaryViewModel(locator.diaryRepository) })
    val memories by viewModel.memoriesForSelectedDay.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val daysWithMemories by viewModel.daysWithMemories.collectAsStateWithLifecycle()
    val showEditor by viewModel.showEditor.collectAsStateWithLifecycle()
    val editingEntry by viewModel.editingEntry.collectAsStateWithLifecycle()
    val entryToDelete by viewModel.entryToDelete.collectAsStateWithLifecycle()
    val savedEntryId by viewModel.savedEntryId.collectAsStateWithLifecycle()
    val savedEntry by viewModel.savedEntry.collectAsStateWithLifecycle()

    val editorEntryId = editingEntry?.id
    val editorViewModel: DiaryEditorViewModel = viewModel(
        key = "diary-editor-${editorEntryId ?: "new"}",
        factory = LambdaViewModelFactory {
            DiaryEditorViewModel(
                diaryRepository = locator.diaryRepository,
                weatherRepository = locator.weatherRepository,
                locationProvider = locator.deviceLocationProvider,
                recorder = DiaryAudioRecorder(locator.appContext),
                player = DiaryAudioPlayer(),
                photoImporter = DevicePhotoImporter(locator.appContext)
            )
        }
    )
    val editorState by editorViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(showEditor, editorEntryId) {
        if (showEditor) editorViewModel.start(editorEntryId, defaultDay = selectedDay)
    }
    LaunchedEffect(editorState.saveCount) {
        if (editorState.saveCount > 0) viewModel.onEditorSaved(editorState.savedEntryId)
    }

    BackHandler(enabled = showEditor, onBack = { editorViewModel.cancelRecording(); viewModel.dismissEditor() })
    BackHandler(enabled = !showEditor && savedEntryId != null, onBack = viewModel::dismissSavedConfirmation)
    BackHandler(enabled = !showEditor && savedEntryId == null, onBack = onBack)

    if (showEditor) {
        DiaryEditor(
            dayEpochDay = editorState.dateEpochDay.takeIf { editorState.entryId != null } ?: (editingEntry?.dateEpochDay ?: selectedDay),
            editing = editingEntry,
            timeMinutes = editorState.timeMinutes,
            content = editorState.content,
            onContentChange = editorViewModel::onContentChange,
            mood = editorState.mood,
            onMoodChange = editorViewModel::onMoodChange,
            onDateChange = editorViewModel::onDateChange,
            onTimeChange = editorViewModel::onTimeChange,
            canSave = editorState.canSave,
            onDismiss = { editorViewModel.cancelRecording(); viewModel.dismissEditor() },
            onSave = editorViewModel::save,
            onDelete = { editingEntry?.let(viewModel::requestDelete); editorViewModel.cancelRecording(); viewModel.dismissEditor() },
            attachments = { DiaryEditorAttachments(editorViewModel) }
        )
    } else {
        Scaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
                DiaryDayHeader(
                    selectedDay = selectedDay,
                    daysWithMemories = daysWithMemories,
                    onBack = onBack,
                    onCreate = viewModel::startNewEntry,
                    onSelectDay = viewModel::selectDay
                )
                if (memories.isEmpty()) {
                    DiaryEmptyState(
                        dayLabel = DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(selectedDay)),
                        onCreate = viewModel::startNewEntry,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Box(Modifier.weight(1f)) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = LifeOSSpacing.screenPadding,
                                end = LifeOSSpacing.screenPadding,
                                top = 12.dp,
                                bottom = LifeOSSpacing.fabContentClearance + 12.dp
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
                        }
                        FloatingActionButton(
                            onClick = viewModel::startNewEntry,
                            containerColor = DiaryActionViolet,
                            contentColor = Color.White,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = LifeOSSpacing.screenPadding, bottom = LifeOSSpacing.fabContentClearance)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Write a new memory", modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
        savedEntry?.let { saved ->
            MemorySavedSheet(
                entry = saved,
                onViewMemory = {
                    val id = savedEntryId
                    viewModel.dismissSavedConfirmation()
                    if (id != null) onOpenEntry(id)
                },
                onAddAnother = viewModel::startNewEntry,
                onDismiss = viewModel::dismissSavedConfirmation
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

@Composable
private fun MemorySavedSheet(entry: DiaryEntity, onViewMemory: () -> Unit, onAddAnother: () -> Unit, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.2f)).clickable(onClick = onDismiss).padding(LifeOSSpacing.screenPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(DiaryPaperCard).clickable(indication = null, onClick = {}).padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SavedNotebookIllustration()
            Spacer(Modifier.height(12.dp))
            Text("Memory saved!", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif), fontWeight = FontWeight.Bold, color = DiaryInkViolet)
            Spacer(Modifier.height(6.dp))
            Text("Your thoughts have been added to ${DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(entry.dateEpochDay))}.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().height(58.dp).clip(CircleShape).background(DiaryActionViolet).clickable(onClick = onViewMemory), contentAlignment = Alignment.Center) {
                Text("View memory  →", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(58.dp).clip(CircleShape).background(DiaryLavender.copy(alpha = 0.55f)).clickable(onClick = onAddAnother), contentAlignment = Alignment.Center) {
                Text("Add another memory", color = DiaryInkViolet, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SavedNotebookIllustration() {
    Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(124.dp, 146.dp).clip(RoundedCornerShape(24.dp)).background(DiaryActionViolet).align(Alignment.Center), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(72.dp))
        }
        Box(Modifier.size(54.dp).clip(CircleShape).background(DiaryActionViolet).align(Alignment.TopCenter), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}