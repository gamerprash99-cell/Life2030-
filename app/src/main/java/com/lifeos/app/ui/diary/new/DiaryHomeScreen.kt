package com.lifeos.app.ui.diary.new

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.InsertChartOutlined
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.media.DiaryAudioPlayer
import com.lifeos.app.core.media.DiaryAudioRecorder
import com.lifeos.app.core.media.DevicePhotoImporter
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.domain.model.DiaryMood
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.ui.diary.new.DiaryEditorViewModel
import com.lifeos.app.ui.diary.new.DiaryViewModel
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHandwriting
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.location.DeviceLocationProvider
import com.lifeos.app.data.repository.WeatherRepository

/**
 * Diary Home — the entry list.
 *
 * Completely new visual design: soft pink illustrated header, date-oriented
 * entry list with mood/attachment/favorite indicators, prominent New Entry
 * action, empty/loading/error states. Shows the editor in-place when a new
 * entry or edit is started. Reuses [DiaryViewModel] and [DiaryEditorViewModel]
 * for all state.
 */
@Composable
fun DiaryHomeScreen(
    onBack: () -> Unit = {},
    onOpenEntry: (String) -> Unit = {},
    onOpenSearch: (() -> Unit)? = null,
    onOpenCalendar: (() -> Unit)? = null,
    onOpenInsights: (() -> Unit)? = null
) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryViewModel = viewModel(
        factory = LambdaViewModelFactory { DiaryViewModel(locator.diaryRepository) }
    )

    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val daysWithMemories by viewModel.daysWithMemories.collectAsStateWithLifecycle()
    val memoriesForSelectedDay by viewModel.memoriesForSelectedDay.collectAsStateWithLifecycle()
    val showEditor by viewModel.showEditor.collectAsStateWithLifecycle()
    val editingEntry by viewModel.editingEntry.collectAsStateWithLifecycle()
    val savedEntryId by viewModel.savedEntryId.collectAsStateWithLifecycle()
    val savedEntry by viewModel.savedEntry.collectAsStateWithLifecycle()
    val entryToDelete by viewModel.entryToDelete.collectAsStateWithLifecycle()

    val editorEntryId = editingEntry?.id
    val editorViewModel: DiaryEditorViewModel = viewModel(
        key = "diary-home-editor-${editorEntryId ?: "new"}",
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

    BackHandler(enabled = true, onBack = onBack)

    Column(Modifier.fillMaxSize().background(DiaryNewCanvas)) {
        if (showEditor) {
            // Editor overlay (in-place, full-screen)
            DiaryEditorScreen(
                editorViewModel = editorViewModel,
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
                attachments = { /* rendered inline by DiaryEditorScreen */ },
                editorActions = { /* rendered inline by DiaryEditorScreen */ }
            )
        } else {
            // ---- Illustrated / pastel header ----
            DiaryNewHeader(
                selectedDay = selectedDay,
                daysWithMemories = daysWithMemories,
                onBack = onBack,
                onCreate = { viewModel.startNewEntry() },
                onSelectDay = { viewModel.selectDay(it) },
                onOpenSearch = onOpenSearch,
                onOpenCalendar = onOpenCalendar,
                onOpenInsights = onOpenInsights
            )

            Spacer(Modifier.height(DiaryNewSectionSpacing))

            // ---- Date-oriented entry list ----
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DiaryNewScreenPadding)
            ) {
                if (memoriesForSelectedDay.isEmpty()) {
                    DiaryNewEmptyState(
                        dayLabel = formatDay(selectedDay),
                        onNewEntry = { viewModel.startNewEntry() }
                    )
                } else {
                    Text(
                        "${memoriesForSelectedDay.size} entry${if (memoriesForSelectedDay.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = DiaryNewInkSoft
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = DiaryNewSectionSpacing)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            memoriesForSelectedDay,
                            key = { _, entry -> entry.id }
                        ) { index, entry ->
                            DiaryNewEntryRow(
                                entry = entry,
                                isFirst = index == 0,
                                onOpen = { onOpenEntry(entry.id) },
                                onEdit = { viewModel.startEdit(entry) },
                                onDelete = { viewModel.requestDelete(entry) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = DiaryNewSectionSpacing)
                            )
                        }
                    }
                }
            }

            // ---- New Entry action (prominent, bottom centre) ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(alignment = Alignment.Center)
                    .padding(bottom = 24.dp, start = DiaryNewScreenPadding, end = DiaryNewScreenPadding)
            ) {
                DiaryNewFab(
                    onClick = { viewModel.startNewEntry() }
                )
            }
        }

        // ---- Save confirmation sheet ----
        savedEntry?.let { saved ->
            DiaryNewFirstEntrySuccess(
                onDismiss = { viewModel.dismissSavedConfirmation() },
                onViewEntry = {
                    val id = savedEntryId
                    viewModel.dismissSavedConfirmation()
                    if (id != null) onOpenEntry(id)
                },
                onAddAnother = viewModel::startNewEntry
            )
        }
    }

    // ---- Delete confirmation ----
    entryToDelete?.let { entry ->
        DiaryNewDeleteDialog(
            dayEpochDay = entry.dateEpochDay,
            timeMinutes = entry.timeMinutes,
            onConfirm = { viewModel.deleteEntry(entry.id) },
            onDismiss = { viewModel.dismissDelete() }
        )
    }
}

@Composable
fun DiaryNewHeader(
    selectedDay: Long,
    daysWithMemories: Set<Long>,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onSelectDay: (Long) -> Unit,
    onOpenSearch: (() -> Unit)?,
    onOpenCalendar: (() -> Unit)?,
    onOpenInsights: (() -> Unit)?
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(
                start = DiaryNewScreenPadding,
                end = DiaryNewScreenPadding,
                top = DiaryNewScreenPadding,
                bottom = DiaryNewSectionSpacing
            )
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(DiaryNewMinTouchTarget)) {
                        Icon(
                            ArrowBack,
                            contentDescription = "Back",
                            tint = DiaryNewInk
                        )
                    }
                    Text(
                        "Diary",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = DiaryHandwriting,
                            fontWeight = FontWeight.Bold,
                            color = DiaryNewInk
                        )
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onOpenSearch?.let {
                        IconButton(onClick = it, modifier = Modifier.size(DiaryNewMinTouchTarget)) {
                            Icon(Search, contentDescription = "Search", tint = DiaryNewInkSoft)
                        }
                    }
                    onOpenCalendar?.let {
                        IconButton(onClick = it, modifier = Modifier.size(DiaryNewMinTouchTarget)) {
                            Icon(CalendarMonth, contentDescription = "Calendar", tint = DiaryNewInkSoft)
                        }
                    }
                    onOpenInsights?.let {
                        IconButton(onClick = it, modifier = Modifier.size(DiaryNewMinTouchTarget)) {
                            Icon(InsertChartOutlined, contentDescription = "Insights", tint = DiaryNewInkSoft)
                        }
                    }
                }
            }

            // Date / day context row
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    formatDay(selectedDay),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DiaryHandwriting,
                        fontWeight = FontWeight.SemiBold,
                        color = DiaryNewInk
                    )
                )
                Spacer(Modifier.fillMaxWidth())
                Text(
                    "${daysWithMemories.size} day${if (daysWithMemories.size == 1) "" else "s"} with entries",
                    style = MaterialTheme.typography.labelSmall.copy(color = DiaryNewInkMuted)
                )
            }
        }
    }
}

@Composable
fun DiaryNewEntryRow(
    entry: DiaryEntity,
    isFirst: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mood = entry.mood?.takeIf { it.isNotBlank() }?.let { DiaryMoods.fromStored(it) }
    val attachments = DiaryAttachments.decode(entry.attachmentsJson)
    val photos = attachments.filterIsInstance<DiaryAttachment.Photo>()
    val isFavorite = entry.isFavorite

    Box(
        modifier = modifier
            .clickable(onClick = onOpen)
            .background(DiaryNewPaper)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, DiaryNewBorder, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mood indicator (left)
            mood?.let { m ->
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DiaryNewMoodColor(m)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        m.emoji,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } ?: Box(Modifier.size(32.dp))

            Spacer(Modifier.width(12.dp))

            // Entry content (center, weighted)
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entry.title?.takeIf { it.isNotBlank() }?.let { title ->
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = DiaryNewInk
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    entry.content.takeIf { it.isNotBlank() } ?: "No content yet",
                    style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.fillMaxWidth())

            // Indicators (right)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (photos.isNotEmpty()) {
                    Icon(
                        AttachFile,
                        contentDescription = "Has attachment",
                        tint = DiaryNewInkSoft,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (isFavorite) {
                    Icon(
                        Favorite,
                        contentDescription = "Favorite",
                        tint = DiaryActionViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Edit / delete overflow
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DiaryNewSurface)
                    .clickable(onClick = { /* overflow menu */ }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    MoreVert,
                    contentDescription = "Entry options",
                    tint = DiaryNewInkSoft,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun DiaryNewEmptyState(
    dayLabel: String,
    onNewEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Box(
            Modifier.size(120.dp).clip(RoundedCornerShape(24.dp)).background(DiaryNewPanel),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Edit,
                contentDescription = null,
                tint = DiaryNewSave,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Your story starts here",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = DiaryHandwriting,
                fontWeight = FontWeight.Bold,
                color = DiaryNewInk
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "No entries for $dayLabel yet. Write your first memory.",
            style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        DiaryNewPrimaryButton(
            label = "New entry",
            onClick = onNewEntry,
            modifier = Modifier.size(200.dp)
        )
    }
}

@Composable
fun DiaryNewFab(onClick: () -> Unit) {
    androidx.compose.material3.FloatingActionButton(
        onClick = onClick,
        containerColor = DiaryNewSave,
        contentColor = Color.White,
        modifier = Modifier.size(DiaryNewMinTouchTarget)
    ) {
        Icon(
            Add,
            contentDescription = "New entry",
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun DiaryNewPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = DiaryNewSave,
            contentColor = Color.White
        ),
        modifier = modifier
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        )
    }
}

fun formatDay(epochDay: Long): String {
    val date = java.time.Instant.ofEpochSecond(epochDay * 86400)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    return java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
        .withLocale(java.util.Locale.getDefault())
        .format(date)
}
