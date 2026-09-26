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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryAttachments
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.io.File
import java.time.LocalDate

/**
 * Diary — a day-scoped journal.
 *
 * The header names the real selected day, the week strip moves between days,
 * and the day's entries render as a time-ordered timeline. Every value comes
 * from the Room-backed diary repository; there is no simulated content.
 */
@Composable
fun DiaryScreen(
    onBack: () -> Unit = {},
    onOpenEntry: (String) -> Unit = {},
    onComposeEntry: (String?) -> Unit = {}
) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryViewModel = viewModel(
        factory = LambdaViewModelFactory { DiaryViewModel(locator.diaryRepository) }
    )

    val selectedDay by viewModel.selectedDay.collectAsState()
    val dayEntries by viewModel.dayEntries.collectAsState()
    val weekDays by viewModel.weekDays.collectAsState()
    val daysWithEntries by viewModel.daysWithEntries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val entryToDelete by viewModel.entryToDelete.collectAsState()

    BackHandler(onBack = onBack)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onComposeEntry(null) },
                shape = CircleShape,
                containerColor = DiaryLavender,
                contentColor = DiaryInkViolet
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New diary entry")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = LifeOSSpacing.screenPadding,
                end = LifeOSSpacing.screenPadding,
                top = 8.dp,
                bottom = LifeOSSpacing.fabContentClearance
            ),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.cardSpacing)
        ) {
            item(key = "header") {
                DiaryDayHeader(
                    date = DateTimeUtils.epochDayToLocalDate(selectedDay),
                    onBack = onBack
                )
            }

            item(key = "week") {
                DiaryWeekStrip(
                    weekDays = weekDays,
                    selectedDay = selectedDay,
                    daysWithEntries = daysWithEntries,
                    onSelect = viewModel::selectDay
                )
            }

            if (isLoading) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DiaryInkViolet)
                    }
                }
            } else if (dayEntries.isEmpty()) {
                item(key = "empty") {
                    DiaryEmptyDay(
                        date = DateTimeUtils.epochDayToLocalDate(selectedDay),
                        onCompose = { onComposeEntry(null) }
                    )
                }
            } else {
                items(dayEntries, key = { it.id }) { entry ->
                    DiaryTimelineCard(
                        entry = entry,
                        onOpen = { onOpenEntry(entry.id) },
                        onEdit = { onComposeEntry(entry.id) }
                    )
                }
            }
        }
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Delete this entry?") },
            text = { Text("This permanently removes the entry and its photos and audio from this device.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteEntry(entry.id) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissDelete) { Text("Cancel") } }
        )
    }
}

/** "TODAY" pill + the real date + weekday, with a back affordance. */
@Composable
private fun DiaryDayHeader(
    date: LocalDate,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = date == DateTimeUtils.today()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isToday) {
                Text(
                    "TODAY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DiaryInkViolet,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DiaryLavender)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            SpacerGap()
            IconButton48(onClick = onBack, description = "Back") {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    DateTimeUtils.formatFullDate(date),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    DateTimeUtils.formatDayOfWeek(date).lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Weekday strip: THU FRI SAT … with the real day numbers of the current week. */
@Composable
private fun DiaryWeekStrip(
    weekDays: List<LocalDate>,
    selectedDay: Long,
    daysWithEntries: Set<Long>,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (weekDays.isEmpty()) return
    val today = DateTimeUtils.today().toEpochDay()
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(weekDays, key = { it.toEpochDay() }) { day ->
            val epochDay = day.toEpochDay()
            val isSelected = epochDay == selectedDay
            val isToday = epochDay == today
            val hasEntries = epochDay in daysWithEntries
            WeekDayChip(
                weekday = DateTimeUtils.shortDayName(day).uppercase(),
                dayNumber = day.dayOfMonth,
                isSelected = isSelected,
                isToday = isToday,
                hasEntries = hasEntries,
                onClick = { onSelect(epochDay) }
            )
        }
    }
}

@Composable
private fun WeekDayChip(
    weekday: String,
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasEntries: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val container = if (isSelected) DiaryLavender else MaterialTheme.colorScheme.surface
    val border = when {
        isSelected -> DiaryLavender
        isToday -> DiaryInkViolet
        else -> DiaryHairline
    }
    val contentColor = if (isSelected) DiaryInkViolet else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .width(DAY_CHIP_WIDTH)
            .clip(shape)
            .background(container)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            weekday.take(3),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
        Text(
            dayNumber.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) DiaryInkViolet else MaterialTheme.colorScheme.onSurface
        )
        // A real marker: this day has at least one persisted entry.
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (hasEntries) DiaryInkViolet else Color.Transparent)
        )
    }
}

/** One entry on the day timeline: time, mood, title, body, media and favourite. */
@Composable
private fun DiaryTimelineCard(
    entry: DiaryEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    val attachments = remember(entry.id, entry.attachmentsJson) { DiaryAttachments.decode(entry.attachmentsJson) }
    val photos = remember(attachments) { DiaryAttachments.photos(attachments) }
    val voiceNote = remember(attachments) { DiaryAttachments.voiceNote(attachments) }
    val tags = remember(entry.id, entry.tagsCsv) { entryTags(entry) }

    Row(modifier = Modifier.fillMaxWidth()) {
        // The timeline spine: a time label and a dot, per the reference.
        Column(
            modifier = Modifier.width(TIMELINE_GUTTER),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                DateTimeUtils.formatMinutes(entry.timeMinutes),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = DiaryInkViolet
            )
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(DiaryInkViolet)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(DiaryPaperCard)
                .border(1.dp, DiaryHairline, RoundedCornerShape(20.dp))
                .clickable(onClick = onOpen)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EntryMoodPill(entry = entry)
                SpacerGap()
                IconButton48(onClick = onEdit, description = "Edit entry") {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            entry.title?.takeIf { it.isNotBlank() }?.let { title ->
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                entry.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (photos.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    photos.take(MAX_INLINE_THUMBS).forEach { photo ->
                        DiaryThumb(filePath = photo.filePath, contentDescription = "Diary photo")
                    }
                    if (photos.size > MAX_INLINE_THUMBS) {
                        Text(
                            "+${photos.size - MAX_INLINE_THUMBS}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (voiceNote != null) {
                    Text(
                        "Voice note",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (tags.isNotEmpty()) {
                    Text(
                        tags.take(MAX_INLINE_TAGS).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (entry.isFavorite) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Favourite",
                        tint = DiaryInkViolet,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/** Square, cropped thumbnail; a missing file degrades to an honest label. */
@Composable
private fun DiaryThumb(filePath: String, contentDescription: String) {
    val context = LocalContext.current
    val exists = remember(filePath) { File(filePath).exists() }
    Box(
        modifier = Modifier
            .size(THUMB_SIZE)
            .clip(RoundedCornerShape(10.dp))
            .background(DiaryLavender.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        if (exists) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(filePath)).build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(THUMB_SIZE)
            )
        } else {
            Icon(
                Icons.Filled.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** The reference empty state: "Your story starts here" + what is missing. */
@Composable
private fun DiaryEmptyDay(
    date: LocalDate,
    onCompose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 32.dp, bottom = 24.dp),
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
            "Your story starts here",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp)
        )
        // Names the real day, so two different empty days never read the same.
        Text(
            "Nothing recorded on ${DateTimeUtils.formatFullDate(date)}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, start = 32.dp, end = 32.dp),
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onCompose, modifier = Modifier.padding(top = 8.dp)) {
            Text("Tap + to capture your day", color = DiaryInkViolet)
        }
    }
}

private fun entryTags(entry: DiaryEntity): List<String> = entry.tagsCsv
    .split(',')
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .take(2)

@Composable
private fun SpacerGap() {
    Box(Modifier.size(WEEKDAY_GAP))
}

private val DAY_CHIP_WIDTH = 52.dp
private val TIMELINE_GUTTER = 56.dp
private val THUMB_SIZE = 44.dp
private val WEEKDAY_GAP = 6.dp
private const val MAX_INLINE_THUMBS = 3
private const val MAX_INLINE_TAGS = 2
