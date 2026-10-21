package com.lifeos.app.feature.life

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.database.entity.LifeEventEntity
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.MinTouchTarget
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.LifeEventType
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.EmptyState
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendar: a month grid whose cells carry the real number of events recorded on
 * that day. Intensity is bucketed from actual counts, so an empty month looks
 * empty rather than decorated.
 */
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LifeViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val state by viewModel.calendarState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(
            title = "Calendar",
            onBack = onBack,
            actions = {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous month",
                    )
                }
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next month",
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Text(
                    text = state.month.title(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${state.dayCounts.values.sum()} events this month",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item { MonthGrid(state = state, onSelect = viewModel::selectDay) }

            item {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(0, 1, 2, 3, 4).forEach { level ->
                        Box(
                            modifier = Modifier
                                .height(14.dp)
                                .weight(1f)
                                .background(intensityColor(level), MaterialTheme.shapes.extraSmall),
                        )
                    }
                }
            }

            item {
                HorizontalDivider(color = LifeOs.tokens.border)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = state.selectedDate?.let { Dates.humanLong(Dates.parse(it)) }
                        ?: "Tap a day to see what happened",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            if (state.selectedDate != null) {
                if (state.selectedEvents.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Nothing on this day",
                            subtitle = "No events were recorded on this date.",
                        )
                    }
                } else {
                    items(state.selectedEvents, key = { it.id }) { event ->
                        LifeOsCard {
                            Column {
                                Text(
                                    text = event.title.ifBlank {
                                        LifeEventType.fromCode(event.type).label
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = LifeEventType.fromCode(event.type).label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(state: LifeViewModel.CalendarState, onSelect: (String) -> Unit) {
    val firstDay = state.month.atDay(1)
    // Monday-first grid, padded with leading blanks.
    val leading = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = state.month.lengthOfMonth()
    val cells: List<LocalDate?> = buildList {
        repeat(leading) { add(null) }
        for (day in 1..daysInMonth) add(state.month.atDay(day))
    }
    val rows = cells.chunked(7)
    val todayIso = Dates.iso(Dates.today())

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { dow ->
                Text(
                    text = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        rows.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                week.forEach { date ->
                    if (date == null) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        DayCell(
                            date = date,
                            count = state.dayCounts[Dates.iso(date)] ?: 0,
                            isToday = Dates.iso(date) == todayIso,
                            isSelected = state.selectedDate == Dates.iso(date),
                            onClick = { onSelect(Dates.iso(date)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                repeat(7 - week.size) { Box(Modifier.weight(1f).aspectRatio(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    count: Int,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val level = intensityLevel(count)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(intensityColor(level), MaterialTheme.shapes.small)
            .border(
                width = if (isSelected || isToday) 2.dp else 1.dp,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> LifeOs.tokens.beigeAccent
                    else -> LifeOs.tokens.border
                },
                shape = MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick)
            .padding(Spacing.xxs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (level >= 3) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

/** Buckets a real event count into 0..4 intensity levels. */
private fun intensityLevel(count: Int): Int = when {
    count <= 0 -> 0
    count == 1 -> 1
    count <= 3 -> 2
    count <= 6 -> 3
    else -> 4
}

@Composable
private fun intensityColor(level: Int) = when (level) {
    0 -> MaterialTheme.colorScheme.surface
    1 -> LifeOs.tokens.softPinkSurface
    2 -> LifeOs.tokens.beigeAccent.copy(alpha = 0.55f)
    3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    else -> MaterialTheme.colorScheme.primary
}

private fun YearMonth.title(): String =
    "${month.getDisplayName(TextStyle.FULL, Locale.getDefault())} $year"
