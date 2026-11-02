package com.lifeos.app.ui.life

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.ui.components.LifeOSDateHeader
import com.lifeos.app.ui.components.LifeOSEmptyState
import com.lifeos.app.ui.components.LifeOSIconButton
import com.lifeos.app.ui.components.LifeOSLoadingState
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.LifeOSSearchBar
import com.lifeos.app.ui.components.LifeOSTimelineItem
import com.lifeos.app.ui.components.ProfileAvatar
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * Life — the app's chronological memory timeline.
 *
 * One scroll, newest first: everything the user has actually saved in LifeOS
 * (diary entries, completed tasks, habit days, expenses, photos, videos and
 * voice captures) grouped under the day it happened on, with Today and
 * Yesterday named. Nothing here is decorative or demo data — every row is a
 * persisted record read through [LifeTimelineViewModel].
 *
 * The tab has exactly one job. It is not a dashboard: there are no module
 * tiles, no promotional cards and no statistics. Module entry points live in
 * Home (quick actions) and the bottom bar, so this screen stays a journal.
 */
@Composable
fun LifeTimelineScreen(
    onOpenItem: (TimelineItem) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locator = LocalServiceLocator.current
    val viewModel: LifeTimelineViewModel = viewModel(
        factory = LambdaViewModelFactory { LifeTimelineViewModel(locator.buildLifeTimelineUseCase) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var showDatePicker by remember { mutableStateOf(false) }
    var pendingScrollToDay by remember { mutableStateOf<Long?>(null) }
    val todayEpochDay = state.todayEpochDay

    // Coming back to the tab after saving something elsewhere must show it.
    // Refreshing on resume (not on every recomposition) is what keeps the
    // timeline live without an observer per table.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val rows = remember(state.sections) { state.sections.toRows() }

    // Scroll to a day the user picked, once its page has actually loaded.
    LaunchedEffect(pendingScrollToDay, rows) {
        val target = pendingScrollToDay ?: return@LaunchedEffect
        val index = rows.indexOfFirst { it is LifeTimelineRow.Day && it.section.epochDay == target }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            pendingScrollToDay = null
        }
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LifeOSSpacing.screenPadding,
                end = LifeOSSpacing.screenPadding,
                top = LifeOSSpacing.sm,
                bottom = LifeOSSpacing.xxxl
            )
        ) {
            item(key = "life_header") {
                LifeTimelineHeader(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    onOpenProfile = onOpenProfile,
                    onOpenSearch = onOpenSearch,
                    onPickDate = { showDatePicker = true }
                )
            }

            when {
                state.loading -> item(key = "life_loading") {
                    LifeOSLoadingState(label = "Gathering your memories…")
                }

                state.sections.isEmpty() -> item(key = "life_empty") {
                    LifeTimelineEmptyState(
                        filtered = state.isFilteredEmpty,
                        query = state.query,
                        onClearQuery = viewModel::clearQuery
                    )
                }

                else -> {
                    rows.forEach { row ->
                        when (row) {
                            is LifeTimelineRow.Day -> item(key = "day_${row.section.epochDay}") {
                                LifeOSDateHeader(
                                    label = row.section.label,
                                    epochDay = row.section.epochDay,
                                    entryCount = row.section.items.size
                                )
                            }

                            is LifeTimelineRow.Entry -> item(key = row.item.id) {
                                LifeOSTimelineItem(
                                    item = row.item,
                                    onClick = { onOpenItem(row.item) },
                                    isFirst = row.isFirstInDay,
                                    isLast = row.isLastInDay,
                                    modifier = Modifier.animateContentSize()
                                )
                            }

                            LifeTimelineRow.Footer -> item(key = "life_footer") {
                                TimelineFooter(
                                    hasMoreHistory = state.hasMoreHistory,
                                    loadingOlder = state.loadingOlder,
                                    windowDays = state.windowDays,
                                    onLoadOlder = viewModel::loadOlder
                                )
                            }

                            LifeTimelineRow.Header -> Unit
                        }
                    }
                }
            }
        }

        // A quiet way back to the top once the user has scrolled into history.
        AnimatedVisibility(
            visible = listState.firstVisibleItemIndex > 2,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(LifeOSSpacing.screenPadding)
        ) {
            LifeOSIconButton(
                icon = Icons.Filled.VerticalAlignTop,
                contentDescription = "Back to today",
                onClick = {
                    pendingScrollToDay = todayEpochDay
                }
            )
        }
    }

    if (showDatePicker) {
        JumpToDateDialog(
            todayEpochDay = todayEpochDay,
            onDismiss = { showDatePicker = false },
            onPick = { epochDay ->
                showDatePicker = false
                viewModel.ensureDayLoaded(epochDay)
                pendingScrollToDay = epochDay
            }
        )
    }
}

/**
 * The Life header: the tab's name, today's date, and the three things a memory
 * journal needs — filter what is loaded, jump to a date, and reach the profile.
 */
@Composable
private fun LifeTimelineHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit,
    onPickDate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Life",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    DateTimeUtils.formatFullDate(DateTimeUtils.today()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LifeOSIconButton(
                icon = Icons.Filled.CalendarMonth,
                contentDescription = "Jump to a date",
                onClick = onPickDate
            )
            Spacer(Modifier.size(LifeOSSpacing.xs))
            ProfileAvatar(size = 40.dp, onClick = onOpenProfile)
        }

        LifeOSSearchBar(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Filter your timeline…"
        )

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
        ) {
            Text(
                "MEMORIES, NEWEST FIRST",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenSearch)
                    .padding(horizontal = LifeOSSpacing.xs, vertical = LifeOSSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.xs)
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    "Search everything",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Why the timeline is empty, and what to do about it. */
@Composable
private fun LifeTimelineEmptyState(
    filtered: Boolean,
    query: String,
    onClearQuery: () -> Unit
) {
    if (filtered) {
        LifeOSEmptyState(
            icon = Icons.Filled.Search,
            title = "No memories match “$query”",
            body = "Your timeline only filters the days it has already loaded. Clear the filter, or search everything you have ever saved.",
            actionLabel = "Clear filter",
            onAction = onClearQuery,
            modifier = Modifier.padding(top = LifeOSSpacing.xxl)
        )
    } else {
        LifeOSEmptyState(
            icon = Icons.Filled.Timeline,
            title = "Your timeline starts here",
            body = "Everything you save in LifeOS lands here in order — diary entries, completed tasks, habit days, spending, photos, videos and voice notes.",
            modifier = Modifier.padding(top = LifeOSSpacing.xxl)
        )
    }
}

/**
 * The end of the loaded history. Offers one more span of older days while
 * there may be any, and says so plainly instead of pretending to be infinite.
 */
@Composable
private fun TimelineFooter(
    hasMoreHistory: Boolean,
    loadingOlder: Boolean,
    windowDays: Long,
    onLoadOlder: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = LifeOSSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
    ) {
        when {
            loadingOlder -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Loading older days…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            hasMoreHistory -> {
                Text(
                    "Showing your last $windowDays days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LifeOSPrimaryButton(text = "Load older history", onClick = onLoadOlder)
            }

            else -> Text(
                "That's everything you've saved.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Material 3 date picker, capped at today — a memory cannot be in the future. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JumpToDateDialog(
    todayEpochDay: Long,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit
) {
    val todayMillis = DateTimeUtils.startOfLocalDayMillis(todayEpochDay)
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = todayMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                utcTimeMillis <= todayMillis + DAY_MILLIS

            override fun isSelectableYear(year: Int) =
                year <= DateTimeUtils.today().year
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val picked = pickerState.selectedDateMillis
                    if (picked != null) onPick(DateTimeUtils.epochDayOf(picked))
                }
            ) { Text("Show that day") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(
            state = pickerState,
            title = {
                Text(
                    "Jump to a day",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = LifeOSSpacing.xl, top = LifeOSSpacing.lg, bottom = LifeOSSpacing.xs)
                )
            },
            headline = {
                Text(
                    "Find the memories you saved on a particular day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = LifeOSSpacing.xl)
                )
            }
        )
    }
}

// ---- Row model -------------------------------------------------------------
//
// A single flat list keeps the scroll position stable and lets "jump to date"
// resolve to a real index without the screen knowing how many rows each day
// has.

private sealed interface LifeTimelineRow {
    data object Header : LifeTimelineRow
    data class Day(val section: TimelineDaySection) : LifeTimelineRow
    data class Entry(
        val item: TimelineItem,
        val isFirstInDay: Boolean,
        val isLastInDay: Boolean
    ) : LifeTimelineRow
    data object Footer : LifeTimelineRow
}

private fun List<TimelineDaySection>.toRows(): List<LifeTimelineRow> = buildList {
    this@toRows.forEach { section ->
        add(LifeTimelineRow.Day(section))
        section.items.forEachIndexed { index, item ->
            add(
                LifeTimelineRow.Entry(
                    item = item,
                    isFirstInDay = index == 0,
                    isLastInDay = index == section.items.lastIndex
                )
            )
        }
    }
    add(LifeTimelineRow.Footer)
}

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
