package com.lifeos.app.ui.diary.new

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.ui.diary.new.DiaryCalendarViewModel
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHandwriting
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.time.LocalDate
import java.time.YearMonth

/**
 * Diary Calendar — new visual design.
 *
 * Monthly calendar with mood indicators, entry indicators, selected-day entries.
 * Reuses [DiaryCalendarViewModel] for state.
 */
@Composable
fun DiaryNewCalendarScreen(
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryCalendarViewModel = viewModel(
        factory = LambdaViewModelFactory {
            DiaryCalendarViewModel(locator.getDiaryCalendarUseCase)
        }
    )

    val state by viewModel.state.collectAsStateWithLifecycle()
    val yearMonth = YearMonth.of(state.year, state.month)
    val selectedDayDate = if (state.selectedDay > 0) LocalDate.ofEpochDay(state.selectedDay) else null

    Column(Modifier.fillMaxSize().background(DiaryNewCanvas)) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DiaryNewScreenPadding, vertical = DiaryNewSectionSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(DiaryNewMinTouchTarget)) {
                Icon(
                    ArrowBack,
                    contentDescription = "Back",
                    tint = DiaryNewInk
                )
            }
            Text(
                "Calendar",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = DiaryHandwriting,
                    fontWeight = FontWeight.Bold,
                    color = DiaryNewInk
                ),
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
            )
        }

        // Month navigation
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DiaryNewScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::previousMonth) {
                Icon(
                    ArrowBack,
                    contentDescription = "Previous month",
                    tint = DiaryNewSave
                )
            }
            Text(
                state.monthLabel,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = DiaryHandwriting,
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryNewInk
                ),
                modifier = Modifier.fillMaxWidth()
            )
            IconButton(onClick = viewModel::nextMonth) {
                Icon(
                    ArrowBack,
                    contentDescription = "Next month",
                    tint = DiaryNewSave
                )
            }
        }

        Spacer(Modifier.height(DiaryNewSectionSpacing))

        // Calendar grid
        DiaryNewCalendarGrid(
            yearMonth = yearMonth,
            entryDays = state.daysWithEntries,
            selectedDay = selectedDayDate,
            onDayClick = { day ->
                viewModel.selectDay(day.toEpochDay())
                onOpenEntry(day.toEpochDay().toString())
            },
            modifier = Modifier.padding(horizontal = DiaryNewScreenPadding)
        )

        Spacer(Modifier.height(DiaryNewSectionSpacing))

        // Selected day entries
        if (state.entriesForSelectedDay.isNotEmpty()) {
            Text(
                "Entries on ${state.monthLabel}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryNewInkSoft
                ),
                modifier = Modifier.padding(start = DiaryNewScreenPadding, end = DiaryNewScreenPadding, bottom = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DiaryNewScreenPadding)
            ) {
                itemsIndexed(
                    state.entriesForSelectedDay,
                    key = { _, entry -> entry.id }
                ) { index, entry ->
                    DiaryNewEntryRowCompact(
                        entry = entry,
                        onClick = { onOpenEntry(entry.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = DiaryNewSectionSpacing)
                    )
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DiaryNewScreenPadding)
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No entries on this day",
                    style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft)
                )
            }
        }
    }
}

@Composable
private fun DiaryNewCalendarGrid(
    yearMonth: YearMonth,
    entryDays: Set<Long>,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstOfMonth = yearMonth.atDay(1)
    val lastOfMonth = yearMonth.atEndOfMonth()
    val startDayOfWeek = firstOfMonth.dayOfWeek.value // 1=Monday
    val daysInMonth = lastOfMonth.dayOfMonth

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Weekday headers
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Box(
                        Modifier
                            .fillMaxWidth()
                            .size(32.dp)
                            .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        day,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DiaryNewInkSoft,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        // Calendar days
        var day = 1
        while (day <= daysInMonth) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) { col ->
                    val dayOfWeek = ((firstOfMonth.dayOfWeek.value + col - 2 + 7) % 7) + 1
                    if (col < startDayOfWeek - 1 || day > daysInMonth) {
                        Box(Modifier.fillMaxWidth().size(32.dp))
                    } else {
                        val dayDate = LocalDate.of(yearMonth.year, yearMonth.month, day)
                        val hasEntry = entryDays.contains(dayDate.toEpochDay())
                        val isSelected = selectedDay != null && dayDate == selectedDay
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) DiaryNewSave
                                    else if (hasEntry) DiaryNewLavender.copy(alpha = 0.4f)
                                    else Color.Transparent
                                )
                                .clickable(onClick = { onDayClick(dayDate) }),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    day.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isSelected) Color.White else DiaryNewInk,
                                        fontWeight = if (isSelected || hasEntry) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                                if (hasEntry && !isSelected) {
                                    Box(
                                        Modifier.size(6.dp).clip(CircleShape).background(DiaryNewSave)
                                    )
                                }
                            }
                        }
                        day++
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryNewEntryRowCompact(
    entry: DiaryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mood = entry.mood?.takeIf { it.isNotBlank() }?.let { DiaryMoods.fromStored(it) }

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(DiaryNewPaper)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .border(1.dp, DiaryNewBorder, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Text(
                entry.content.takeIf { it.isNotBlank() } ?: "No content",
                style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInk),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.fillMaxWidth())

            Text(
                com.lifeos.app.core.util.DateTimeUtils.formatMinutes(entry.timeMinutes),
                style = MaterialTheme.typography.labelSmall.copy(color = DiaryNewInkSoft)
            )
        }
    }
}

/** Uses the shared DiaryNewMoodColor from DiaryEditorScreen. */
