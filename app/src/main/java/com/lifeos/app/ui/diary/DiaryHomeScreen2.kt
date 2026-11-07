package com.lifeos.app.ui.diary

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.ui.theme.Diary2ActionViolet
import com.lifeos.app.ui.theme.Diary2Canvas
import com.lifeos.app.ui.theme.Diary2FabBottomOffset
import com.lifeos.app.ui.theme.Diary2FabSize
import com.lifeos.app.ui.theme.Diary2Hairline
import com.lifeos.app.ui.theme.Diary2InkSoft
import com.lifeos.app.ui.theme.Diary2InkViolet
import com.lifeos.app.ui.theme.Diary2Lavender
import com.lifeos.app.ui.theme.Diary2MinTouchTarget
import com.lifeos.app.ui.theme.Diary2PaperCard
import com.lifeos.app.ui.theme.Diary2RadiusLg
import com.lifeos.app.ui.theme.Diary2ScreenPadding
import com.lifeos.app.ui.theme.Diary2SectionSpacing
import com.lifeos.app.ui.theme.Diary2SurfaceLavender
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.ui.theme.Diary2RadiusMd

/**
 * LifeOS Diary Home — Entries Dashboard (v2).
 *
 * Premium minimal journal home with pastel pink canvas, date masthead, entry
 * cards, prominent New Entry FAB, empty state.
 *
 * Uses existing [DiaryViewModel] — no data-layer change.
 */
@Composable
fun DiaryHomeScreen2(
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
    val memories by viewModel.memoriesForSelectedDay.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val daysWithMemories by viewModel.daysWithMemories.collectAsStateWithLifecycle()
    val showEditor by viewModel.showEditor.collectAsStateWithLifecycle()
    val editingEntry by viewModel.editingEntry.collectAsStateWithLifecycle()
    val entryToDelete by viewModel.entryToDelete.collectAsStateWithLifecycle()
    val savedEntryId by viewModel.savedEntryId.collectAsStateWithLifecycle()
    val savedEntry by viewModel.savedEntry.collectAsStateWithLifecycle()

    BackHandler(enabled = showEditor, onBack = { viewModel.dismissEditor() })
    BackHandler(enabled = !showEditor && savedEntryId != null, onBack = viewModel::dismissSavedConfirmation)
    BackHandler(enabled = !showEditor && savedEntryId == null, onBack = onBack)

    if (showEditor) {
        Diary2EditorPlaceholder(
            onDismiss = { viewModel.dismissEditor() },
            editingEntry = editingEntry,
            viewModel = viewModel,
            locator = locator
        )
    } else {
        Column(
            Modifier.fillMaxSize().background(Diary2Canvas)
        ) {
            Diary2HomeHeader(
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
                    Diary2EmptyState(
                        dayLabel = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy")
                            .withLocale(java.util.Locale.getDefault())
                            .format(java.time.LocalDate.ofEpochDay(selectedDay)),
                        modifier = Modifier.fillMaxSize()
                    )
                    Diary2Fab(
                        onClick = viewModel::startNewEntry,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = Diary2ScreenPadding,
                                bottom = Diary2FabBottomOffset
                            )
                    )
                }
            } else {
                Column(Modifier.weight(1f)) {
                    Diary2DaySummary(memories)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Diary2ScreenPadding, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your moments",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = com.lifeos.app.ui.theme.DiaryHandwriting
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Diary2InkViolet
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${memories.size} ${if (memories.size == 1) "memory" else "memories"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Diary2InkSoft
                        )
                    }

                    Box(Modifier.weight(1f)) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = Diary2ScreenPadding,
                                end = Diary2ScreenPadding,
                                top = 8.dp,
                                bottom = com.lifeos.app.ui.theme.LifeOSSpacing.diaryFabOccupiedBottom
                            )
                        ) {
                            itemsIndexed(memories, key = { _, entry -> entry.id }) { index, entry ->
                                Diary2EntryCard(
                                    entry = entry,
                                    isFirst = index == 0,
                                    isLast = index == memories.lastIndex,
                                    onOpen = { onOpenEntry(entry.id) },
                                    onEdit = { viewModel.startEdit(entry) },
                                    onDelete = { viewModel.requestDelete(entry) }
                                )
                            }
                        }
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                            Diary2Fab(
                                onClick = viewModel::startNewEntry,
                                modifier = Modifier
                                    .padding(
                                        end = Diary2ScreenPadding,
                                        bottom = Diary2FabBottomOffset
                                    )
                            )
                        }
                    }
                }
            }
        }

        savedEntry?.let { saved ->
            Diary2SaveConfirmationSheet(
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
        Diary2DeleteDialog(
            dayEpochDay = entry.dateEpochDay,
            timeMinutes = entry.timeMinutes,
            onConfirm = { viewModel.deleteEntry(entry.id) },
            onDismiss = viewModel::dismissDelete
        )
    }
}

/* ============================================================================
   Editor placeholder (wired to full editor in next step)
   ============================================================================ */

@Composable
private fun Diary2EditorPlaceholder(
    onDismiss: () -> Unit,
    editingEntry: DiaryEntity?,
    viewModel: DiaryViewModel,
    locator: com.lifeos.app.core.di.ServiceLocator
) {
    Column(
        Modifier.fillMaxSize().background(Diary2Canvas)
    ) {
        Text(
            text = "Editor (v2)",
            style = MaterialTheme.typography.headlineMedium,
            color = Diary2InkViolet
        )
        androidx.compose.material3.TextButton(onClick = onDismiss) {
            Text("Close")
        }
    }
}

/* ============================================================================
   Home sub-components
   ============================================================================ */

@Composable
private fun Diary2HomeHeader(
    selectedDay: Long,
    daysWithMemories: Set<Long>,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onSelectDay: (Long) -> Unit,
    onOpenSearch: (() -> Unit)?,
    onOpenCalendar: (() -> Unit)?,
    onOpenInsights: (() -> Unit)?
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Diary2ScreenPadding, vertical = Diary2SectionSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Diary2BackButton(onClick = onBack)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = "Diary",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = com.lifeos.app.ui.theme.DiaryHandwriting
                ),
                fontWeight = FontWeight.Bold,
                color = Diary2InkViolet
            )
            Text(
                text = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
                    .withLocale(java.util.Locale.getDefault())
                    .format(java.time.LocalDate.ofEpochDay(selectedDay)),
                style = MaterialTheme.typography.bodyMedium,
                color = Diary2InkSoft
            )
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            onOpenSearch?.let {
                Diary2IconButton48(
                    onClick = it,
                    description = "Search",
                    content = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Diary2InkViolet,
                            modifier = androidx.compose.ui.Modifier.size(20.dp)
                        )
                    }
                )
            }
            onOpenCalendar?.let {
                Diary2IconButton48(
                    onClick = it,
                    description = "Calendar",
                    content = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = Diary2InkViolet,
                            modifier = androidx.compose.ui.Modifier.size(20.dp)
                        )
                    }
                )
            }
            onOpenInsights?.let {
                Diary2IconButton48(
                    onClick = it,
                    description = "Insights",
                    content = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = Diary2InkViolet,
                            modifier = androidx.compose.ui.Modifier.size(20.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun Diary2DaySummary(memories: List<DiaryEntity>) {
    val wordCount = memories.sumOf {
        it.content.trim().let { text -> if (text.isBlank()) 0 else text.split(Regex("\\s+")).size }
    }
    val moodKeys = memories.mapNotNull { it.mood?.takeIf(String::isNotBlank) }.distinct()
    val mood = moodKeys.singleOrNull()?.let { DiaryMoods.fromStored(it) }
    val moodLabel = when {
        mood != null -> "${mood.emoji} ${mood.label}"
        moodKeys.isNotEmpty() -> "Mixed moods"
        else -> "No mood"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Diary2ScreenPadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Diary2SummaryChip(
            value = memories.size.toString(),
            label = if (memories.size == 1) "memory" else "memories",
            modifier = Modifier.weight(1f)
        )
        Diary2SummaryChip(
            value = wordCount.toString(),
            label = "words",
            modifier = Modifier.weight(1f)
        )
        Diary2SummaryChip(
            value = moodLabel,
            label = "mood",
            compact = true,
            modifier = Modifier.weight(1.35f)
        )
    }
}

@Composable
private fun Diary2SummaryChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Diary2PaperCard)
            .border(1.dp, Diary2Hairline, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = (if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge)
                .copy(fontFamily = com.lifeos.app.ui.theme.DiaryHandwriting),
            color = Diary2InkViolet,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Diary2InkSoft,
            maxLines = 1
        )
    }
}

@Composable
private fun Diary2EntryCard(
    entry: DiaryEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val mood = entry.mood?.takeIf(String::isNotBlank)?.let { DiaryMoods.fromStored(it) }
    val hasAttachments = entry.attachmentsJson.isNotBlank() && entry.attachmentsJson != "[]"
    val isFavorite = entry.isFavorite

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (isFirst) 0.dp else Diary2SectionSpacing,
                bottom = if (isLast) Diary2SectionSpacing else 0.dp
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Diary2PaperCard)
                .border(1.dp, Diary2Hairline, RoundedCornerShape(20.dp))
                .clickable(role = Role.Button, onClick = onOpen),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (mood != null) {
                        Text(
                            text = mood.emoji,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Diary2Lavender.copy(alpha = 0.4f))
                        )
                    }
                    Column {
                        entry.title?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = com.lifeos.app.ui.theme.DiaryHandwriting
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = Diary2InkViolet,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = entry.content.takeIf { it.isNotBlank() }?.let { it.take(120) } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Diary2InkSoft,
                            maxLines = 2
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (isFavorite) {
                        Text(
                            text = "★",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Diary2ActionViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (hasAttachments) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.AddAPhoto,
                            contentDescription = null,
                            tint = Diary2InkSoft,
                            modifier = androidx.compose.ui.Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = com.lifeos.app.core.util.DateTimeUtils.formatFullDate(
                            com.lifeos.app.core.util.DateTimeUtils.epochDayToLocalDate(entry.dateEpochDay)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Diary2InkSoft
                    )
                    Text(
                        text = com.lifeos.app.core.util.DateTimeUtils.formatMinutes(entry.timeMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = Diary2InkSoft
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Diary2IconButton48(
                onClick = onEdit,
                description = "Edit entry",
                content = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = Diary2ActionViolet,
                        modifier = androidx.compose.ui.Modifier.size(18.dp)
                    )
                }
            )
            Diary2IconButton48(
                onClick = onDelete,
                description = "Delete entry",
                content = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = androidx.compose.ui.Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun Diary2EmptyState(
    dayLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Diary2ActionViolet)
                .align(Alignment.CenterHorizontally)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = androidx.compose.ui.Modifier.size(72.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Your story starts here",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = com.lifeos.app.ui.theme.DiaryHandwriting
            ),
            fontWeight = FontWeight.Bold,
            color = Diary2InkViolet,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Begin your first journal entry for $dayLabel",
            style = MaterialTheme.typography.bodyMedium,
            color = Diary2InkSoft,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Diary2Fab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.FloatingActionButton(
        onClick = onClick,
        containerColor = Diary2ActionViolet,
        contentColor = Color.White,
        modifier = modifier.size(Diary2FabSize)
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Write a new entry",
            modifier = androidx.compose.ui.Modifier.size(28.dp)
        )
    }
}

/* ============================================================================
   Save confirmation sheet (v2)
   ============================================================================ */

@Composable
fun Diary2SaveConfirmationSheet(
    entry: DiaryEntity,
    onViewMemory: () -> Unit,
    onAddAnother: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.2f))
            .clickable(onClick = onDismiss)
            .padding(Diary2ScreenPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Diary2PaperCard)
                .clickable(onClick = {})
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(160.dp).clip(RoundedCornerShape(24.dp)).background(Diary2ActionViolet),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = androidx.compose.ui.Modifier.size(64.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Entry saved!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = com.lifeos.app.ui.theme.DiaryHandwriting
                ),
                fontWeight = FontWeight.Bold,
                color = Diary2InkViolet
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Your thoughts have been added to ${com.lifeos.app.core.util.DateTimeUtils.formatFullDate(
                    com.lifeos.app.core.util.DateTimeUtils.epochDayToLocalDate(entry.dateEpochDay)
                )}.",
                style = MaterialTheme.typography.bodyMedium,
                color = Diary2InkSoft,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(Diary2ActionViolet)
                    .clickable(onClick = onViewMemory),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "View entry  →",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(Diary2Lavender.copy(alpha = 0.55f))
                    .clickable(onClick = onAddAnother),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add another entry",
                    color = Diary2InkViolet,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/* ============================================================================
   Delete dialog (v2)
   ============================================================================ */

@Composable
fun Diary2DeleteDialog(
    dayEpochDay: Long,
    timeMinutes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val date = com.lifeos.app.core.util.DateTimeUtils.epochDayToLocalDate(dayEpochDay)
    val time = com.lifeos.app.core.util.DateTimeUtils.formatMinutes(timeMinutes)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this entry?", style = MaterialTheme.typography.titleMedium) },
        text = {
            Text(
                text = "The entry from $date at $time will be removed from your journal.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onConfirm
            ) { Text("Delete", style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/* ============================================================================
   Reusable components
   ============================================================================ */

@Composable
fun Diary2CanvasHome(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxSize(),
        color = Diary2Canvas
    ) {
        content()
    }
}

@Composable
fun Diary2BackButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(Diary2MinTouchTarget)
            .clip(CircleShape)
            .background(Diary2Lavender.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = null,
            tint = Diary2InkViolet,
            modifier = androidx.compose.ui.Modifier.size(20.dp)
        )
    }
}

@Composable
fun Diary2IconButton48(
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .size(Diary2MinTouchTarget)
            .clip(CircleShape)
            .background(Diary2Lavender.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Canvas background modifier. */
private fun Modifier.diary2PaperBackground(): Modifier = this.then(
    drawBehind {
        drawRect(color = Diary2Canvas)
    }
)
