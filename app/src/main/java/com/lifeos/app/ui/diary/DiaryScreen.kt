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
import com.lifeos.app.core.media.RecordingState
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
fun DiaryScreen(
    onBack: () -> Unit = {},
    onOpenEntry: (String) -> Unit = {},
    // The three sub-screens. Nullable and defaulted so the existing previews and
    // any other caller keep compiling; the menu rows simply do not appear.
    onOpenSearch: (() -> Unit)? = null,
    onOpenCalendar: (() -> Unit)? = null,
    onOpenInsights: (() -> Unit)? = null
) {
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
    // One permission flow per capability, shared by the in-card icon row and
    // the attachment rows below it.
    val editorTriggers = rememberDiaryEditorActionTriggers(editorViewModel)

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
            title = editorState.title,
            onTitleChange = editorViewModel::onTitleChange,
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
            attachments = { DiaryEditorAttachments(editorViewModel, editorTriggers) },
            editorActions = {
                DiaryEditorActionBar(
                    isRecording = editorState.recording is RecordingState.Recording,
                    onAddPhoto = editorTriggers.addPhoto,
                    onToggleVoiceNote = editorTriggers.toggleVoiceNote,
                    onAddLocation = editorTriggers.addLocation,
                    locationStatus = editorState.locationStatus
                )
            }
        )
    } else {
        Column(Modifier.fillMaxSize()) {
            DiaryDayHeader(
                selectedDay = selectedDay,
                daysWithMemories = daysWithMemories,
                onBack = onBack,
                onCreate = viewModel::startNewEntry,
                onSelectDay = viewModel::selectDay,
                onOpenSearch = onOpenSearch,
                onOpenCalendar = onOpenCalendar,
                onOpenInsights = onOpenInsights
            )
            if (memories.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    DiaryEmptyState(
                        dayLabel = DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(selectedDay)),
                        // The FAB is drawn over this box, so the empty state has
                        // to reserve the band it occupies. It used to fill the box
                        // and centre itself, which put "YOUR STORY STARTS HERE"
                        // and the line under it straight into the button's band
                        // on any phone whose box was not much taller than the
                        // artwork.
                        modifier = Modifier.fillMaxSize()
                    )
                    DiaryFab(
                        onClick = viewModel::startNewEntry,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = LifeOSSpacing.screenPadding,
                                bottom = LifeOSSpacing.diaryFabBottomOffset
                            )
                    )
                }
            } else {
                Box(Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // The FAB's whole band, not just its bottom offset: with
                        // only the offset the last item could be scrolled up to
                        // the button's bottom edge and no further, so the closing
                        // lines of a long memory stayed underneath it.
                        contentPadding = PaddingValues(
                            start = LifeOSSpacing.screenPadding,
                            end = LifeOSSpacing.screenPadding,
                            top = 8.dp,
                            bottom = LifeOSSpacing.diaryFabOccupiedBottom
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
                    DiaryFab(
                        onClick = viewModel::startNewEntry,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = LifeOSSpacing.screenPadding,
                                bottom = LifeOSSpacing.diaryFabBottomOffset
                            )
                    )
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

/**
 * The Diary list's floating action button, in one place.
 *
 * It used to be written out twice — once for the empty day, once for a day with
 * memories — with the same modifier chain copied in both. That is why the two
 * copies could disagree about how much room they took away from the content:
 * the button's *offset* was also being used as the scrollable's trailing
 * clearance, so the button's own height was never added and the last line of a
 * long memory stayed underneath it.
 *
 * The size is pinned to [LifeOSSpacing.diaryFabSize] and the offset to
 * [LifeOSSpacing.diaryFabBottomOffset] so that the band content reserves
 * ([LifeOSSpacing.diaryFabOccupiedBottom]) is derived from the button's real
 * geometry rather than restated next to it.
 */
@Composable
private fun DiaryFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = DiaryActionViolet,
        contentColor = Color.White,
        modifier = modifier.size(LifeOSSpacing.diaryFabSize)
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = "Write a new memory",
            modifier = Modifier.size(28.dp)
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
            Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(DiaryPaperCard).clickable(onClick = {}).padding(horizontal = 24.dp, vertical = 26.dp),
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