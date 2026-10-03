package com.lifeos.app.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A month calendar over the user's own entries.
 *
 * The dot encodes presence, not volume. Day 14 with three entries and day 15
 * with one both get one 6dp dot; a size difference that small is not readable
 * at this density, so a varying dot would look like data while being noise. The
 * count is not hidden, though — it is in the selected-day list underneath.
 *
 * Future days are rendered but not selectable. The composer clamps a memory's
 * date to the past, so a future day cannot have an entry and letting it open an
 * empty panel would imply otherwise.
 *
 * Padded cells are blank, not greyed dates from the neighbouring months, for the
 * reason in [DiaryCalendarState.gridCells]: a tappable-looking date from another
 * month is a tap that goes somewhere the user did not ask for.
 */
@Composable
fun DiaryCalendarScreen(
    state: DiaryCalendarState,
    onSelectDay: (Long) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = LifeOSSpacing.screenPadding)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to journal")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    state.monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryInkViolet
                )
                Text(
                    "${state.year}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Disabled rather than hidden at the current month: a control that
            // appears and disappears moves everything else on the row.
            IconButton(onClick = onPreviousMonth, enabled = !state.isCurrentMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            IconButton(onClick = onNextMonth, enabled = !state.isCurrentMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }

        WeekdayHeader()

        val today = LocalDate.ofEpochDay(state.todayEpochDay)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.gridCells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { epochDay ->
                        if (epochDay == null) {
                            // Weight 1 rather than a fixed size, so the blank
                            // cells still hold the row's seven columns and the
                            // days below stay aligned with the weekday header.
                            Box(Modifier.weight(1f))
                        } else {
                            val date = LocalDate.ofEpochDay(epochDay)
                            DayCell(
                                epochDay = epochDay,
                                dayOfMonth = date.dayOfMonth,
                                hasEntries = state.daysWithEntries.contains(epochDay),
                                moodKey = state.moodByDay[epochDay],
                                isSelected = epochDay == state.selectedDay,
                                isToday = epochDay == state.todayEpochDay,
                                // A future day cannot have an entry, because
                                // the composer clamps to the past.
                                isSelectable = !date.isAfter(today),
                                onClick = { onSelectDay(epochDay) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        SelectedDayPanel(
            dateEpochDay = state.selectedDay,
            entries = state.entriesForSelectedDay,
            isLoading = state.isLoading,
            onOpenEntry = onOpenEntry,
            modifier = Modifier
                .weight(1f)
                .padding(top = 20.dp)
        )
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Monday-first, to match gridCells' leading-offset calculation. Getting
        // this wrong by one day is the classic calendar bug: the 1st lands under
        // the wrong weekday and every entry looks like it is a day early.
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * One day.
 *
 * `aspectRatio(1f)` keeps the grid square whatever the font scale, and the
 * selectable region is the whole cell — a 28dp dot would be well under the 48dp
 * minimum target, so the tap area is the cell and the dot is only the marker.
 */
@Composable
private fun DayCell(
    epochDay: Long,
    dayOfMonth: Int,
    hasEntries: Boolean,
    moodKey: String?,
    isSelected: Boolean,
    isToday: Boolean,
    isSelectable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mood = moodKey?.let { DiaryMoods.fromStored(it) }
    val cellBackground = when {
        isSelected -> DiaryActionViolet
        mood != null -> DiaryMoodVisuals.haloOf(mood)
        else -> Color.Transparent
    }

    val described = buildString {
        append(LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("d MMMM yyyy")))
        if (hasEntries) append(", has entries") else append(", no entries")
        if (isToday) append(", today")
    }

    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(cellBackground)
            .then(
                // Today gets a ring even when unselected, because "today" is a
                // position in time and must not be carried by colour alone.
                when {
                    isToday -> Modifier.border(1.5.dp, DiaryActionViolet, RoundedCornerShape(12.dp))
                    isSelectable -> Modifier.border(1.dp, DiaryHairline, RoundedCornerShape(12.dp))
                    else -> Modifier
                }
            )
            .then(if (isSelectable) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics {
                contentDescription = described
                this.selected = isSelected
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    isSelectable -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                }
            )
            if (hasEntries) {
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            // White on a selected cell keeps the dot visible;
                            // the selected cell is already saturated, so the
                            // mood's own colour would vanish into it.
                            if (isSelected) Color.White else mood?.let { DiaryMoodVisuals.accentOf(it) } ?: DiaryActionViolet
                        )
                )
            }
        }
    }
}

/** The entries for whichever day is selected. */
@Composable
private fun SelectedDayPanel(
    dateEpochDay: Long,
    entries: List<DiaryEntity>,
    isLoading: Boolean,
    onOpenEntry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val label = LocalDate.ofEpochDay(dateEpochDay).format(DateTimeFormatter.ofPattern("EEEE d MMMM"))

    Column(modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = DiaryInkViolet
        )
        Text(
            when {
                isLoading -> "…"
                entries.isEmpty() -> "No entries"
                entries.size == 1 -> "1 entry"
                else -> "${entries.size} entries"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (!isLoading && entries.isEmpty()) {
            MessageState(
                title = "A quiet day",
                body = "Nothing was written on this date.",
                accent = DiaryActionViolet
            )
        } else {
            LazyColumn(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    CalendarEntryRow(entry = entry, onClick = { onOpenEntry(entry.id) })
                }
            }
        }
    }
}

@Composable
private fun CalendarEntryRow(entry: DiaryEntity, onClick: () -> Unit) {
    val time = LocalDate.ofEpochDay(entry.dateEpochDay)
        .atStartOfDay()
        .plusMinutes(entry.timeMinutes.toLong())
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, DiaryHairline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            time,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(DiaryLavender)
        )
        Text(
            entry.title?.takeIf { it.isNotBlank() } ?: entry.content.trim().take(48),
            style = MaterialTheme.typography.bodyMedium,
            color = DiaryInkViolet,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        entry.mood?.let { DiaryMoods.fromStored(it) }?.let { mood ->
            Text(mood.emoji, style = MaterialTheme.typography.bodyMedium)
        }
    }
}