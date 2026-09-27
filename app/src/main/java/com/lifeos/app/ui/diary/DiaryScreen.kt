package com.lifeos.app.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.media.DiaryAudioPlayer
import com.lifeos.app.core.media.DevicePhotoImporter
import com.lifeos.app.core.media.DiaryAudioRecorder
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.LifeOSSpacing


@Composable
fun DiaryScreen(
    onBack: () -> Unit = {},
    onOpenEntry: (String) -> Unit = {}
) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryViewModel = viewModel(
        factory = LambdaViewModelFactory { DiaryViewModel(locator.diaryRepository) }
    )
    val memories by viewModel.memoriesForSelectedDay.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val daysWithMemories by viewModel.daysWithMemories.collectAsState()
    val showEditor by viewModel.showEditor.collectAsState()
    val editingEntry by viewModel.editingEntry.collectAsState()
    val entryToDelete by viewModel.entryToDelete.collectAsState()
    val savedEntryId by viewModel.savedEntryId.collectAsStateWithLifecycle()
    val savedEntry by viewModel.savedEntry.collectAsStateWithLifecycle()

    // The editor's real draft — words, mood, photos, voice note, place, weather
    // and the save itself — lives in its own view model, keyed on the entry being
    // edited. The day view above keeps only "is the editor open, and for what".
    val editorEntryId = editingEntry?.id
    val editorViewModel: DiaryEditorViewModel = viewModel(
        key = "diary-editor-${editorEntryId ?: "new"}",
        factory = LambdaViewModelFactory {
            // The recorder and player are per-editor on purpose: each draft owns
            // its own capture file and decoder, and the view model releases both
            // in onCleared.
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

    // Seed the draft when the editor opens; clear a recording if it closes
    // without saving. Keyed on the entry so switching entries re-seeds.
    LaunchedEffect(showEditor, editorEntryId) {
        if (showEditor) editorViewModel.start(editorEntryId, defaultDay = selectedDay)
    }

    // The composer closes the moment the write is committed — it is already
    // durable, so there is nothing to protect it from — and the confirmation
    // takes its place. Keyed on the monotonic count so a second save of the same
    // memory is a second event and the sheet returns.
    LaunchedEffect(editorState.saveCount) {
        if (editorState.saveCount == 0) return@LaunchedEffect
        viewModel.onEditorSaved(editorState.savedEntryId)
    }

    // Registered last so it wins the back press while the confirmation is up.
    BackHandler(enabled = showEditor, onBack = viewModel::dismissEditor)
    BackHandler(enabled = !showEditor, onBack = onBack)
    BackHandler(
        enabled = !showEditor && savedEntryId != null,
        onBack = viewModel::dismissSavedConfirmation
    )

    Scaffold(
        // The single create affordance, anchored by the Scaffold itself to the
        // lower-right of the content area. It used to be a `Box` child with
        // `align(BottomEnd)` plus a 96dp bottom pad on top of the Scaffold's own
        // inner padding, which stacked two insets and left the button floating a
        // third of the way up the page.
        //
        // `contentWindowInsets` is zeroed on purpose: this screen is composed
        // inside the app's bottom-bar `Scaffold`, which has already lifted the
        // day view above the bar and the bar has consumed the system navigation
        // inset itself. Adding it again would lift the button off the bottom of
        // the content a second time — exactly the gap this removes.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            // The app's single most pressable thing, so it wears the single
            // action colour (Stitch `#6C47EB` with a white glyph) rather than
            // the heading ink.
            FloatingActionButton(
                onClick = viewModel::startNewEntry,
                containerColor = DiaryActionViolet,
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Write a new memory",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // `MainActivity` calls `enableEdgeToEdge()`, and the only Scaffold
            // in this hierarchy is the app's bottom-bar one, so `padding` carries
            // a bottom inset and *no* top inset — the inner Scaffold above
            // contributes none. Without the status-bar padding below the header's
            // back button and the date line render underneath the clock.
            // `DiaryEditorOverlay` already consumes the insets for the composer;
            // this is the same treatment for the day view.
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
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
                        onCreate = viewModel::startNewEntry,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = LifeOSSpacing.screenPadding,
                            end = LifeOSSpacing.screenPadding,
                            // The header already closes with a small gap of its
                            // own; the old 20dp on top of it opened a visible
                            // band between the date strip and the first entry.
                            top = LifeOSSpacing.cardSpacing,
                            // Kept: the list has to be able to scroll its last
                            // card clear of the button, which is the one thing
                            // that legitimately sits over the content.
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

            DiaryEditorOverlay(visible = showEditor) {
                DiaryEditor(
                    dayEpochDay = editorState.dateEpochDay.takeIf { editorState.entryId != null }
                        ?: (editingEntry?.dateEpochDay ?: selectedDay),
                    editing = editingEntry,
                    timeMinutes = editorState.timeMinutes,
                    content = editorState.content,
                    onContentChange = editorViewModel::onContentChange,
                    mood = editorState.mood,
                    canSave = editorState.canSave,
                    onDismiss = {
                        editorViewModel.cancelRecording()
                        viewModel.dismissEditor()
                    },
                    onSave = editorViewModel::save,
                    onDelete = {
                        editingEntry?.let(viewModel::requestDelete)
                        editorViewModel.cancelRecording()
                        viewModel.dismissEditor()
                    },
                    attachments = { DiaryEditorAttachments(editorViewModel) }
                )
            }

            // The confirmation sits over the day list, not over the composer: the
            // write is already committed, so the composer is gone by the time it
            // appears. Rendered only once the entry is resolvable, so it can never
            // appear with nothing in it.
            if (!showEditor && savedEntry != null) {
                MemorySavedSheet(
                    entry = savedEntry!!,
                    onViewMemory = {
                        val id = savedEntryId
                        viewModel.dismissSavedConfirmation()
                        id?.let(onOpenEntry)
                    },
                    onAddAnother = {
                        // Reopens the composer as a *new* draft on the day being
                        // read — startNewEntry() re-seeds from the selected day, so
                        // a second memory written from a past day stays in that day.
                        viewModel.startNewEntry()
                    },
                    onDismiss = viewModel::dismissSavedConfirmation
                )
            }
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
                // Stitch's primary action: a full pill in the action violet with
                // a white label. This was a bare violet word with no container,
                // so it read as a label rather than something to press.
                .clip(CircleShape)
                .background(DiaryActionViolet)
                .clickable(onClick = onCreate)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                "+ Memory",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}


/**
 * The post-save confirmation: a scrim over the day, and one paper card naming
 * the memory that was just committed.
 *
 * Both the day and the minute on the card are read from the stored row, so the
 * confirmation reports what was actually filed — including when the user wrote
 * into a past day or saved an edit that kept the original minute.
 *
 * The two actions are the Stitch design's: read it back, or start the next one
 * without leaving the day. A transient toast could not carry either, which is
 * why this replaced one.
 */
@Composable
private fun MemorySavedSheet(
    entry: DiaryEntity,
    onViewMemory: () -> Unit,
    onAddAnother: () -> Unit,
    onDismiss: () -> Unit
) {
    val cardSwallowTap = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.34f))
            // Tapping the scrim retires the sheet; the two actions do the work.
            .clickable(onClick = onDismiss)
            .padding(LifeOSSpacing.screenPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(DiaryPaperCard)
                // The card must swallow taps meant for the scrim behind it. A
                // *disabled* clickable does not consume the pointer event, so the
                // tap would fall through and retire the sheet from a touch on the
                // card itself; an enabled no-op consumes it. The indication is
                // suppressed so the card does not read as tappable.
                .clickable(
                    interactionSource = cardSwallowTap,
                    indication = null,
                    onClick = {}
                )
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(DiaryLavender),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = DiaryActionViolet,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Memory saved!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DiaryInkViolet,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Your thoughts have been added to " +
                    "${DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(entry.dateEpochDay))}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // The minute the memory was actually filed under, so a back-dated
            // entry confirms its real position on the day's timeline.
            Spacer(Modifier.height(10.dp))
            Text(
                text = DateTimeUtils.formatMinutes(entry.timeMinutes),
                style = MaterialTheme.typography.labelSmall,
                color = DiaryInkViolet.copy(alpha = 0.55f)
            )

            Spacer(Modifier.height(22.dp))

            MemorySheetAction(label = "View memory", primary = true, onClick = onViewMemory)
            Spacer(Modifier.height(12.dp))
            MemorySheetAction(label = "Add another memory", primary = false, onClick = onAddAnother)
        }
    }
}

/** One full-width pill in the sheet. 58dp tall, comfortably above the 48dp minimum. */
@Composable
private fun MemorySheetAction(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(CircleShape)
            .background(if (primary) DiaryActionViolet else DiaryLavender.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (primary) Color.White else DiaryInkViolet,
            textAlign = TextAlign.Center
        )
    }
}
