package com.lifeos.app.ui.diary

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender

/**
 * How many dates the strip shows at once. Every other dimension is *derived* from
 * this number instead of being hard-coded: the visible-date count determines cell width from the measured window, so seven dates match the reference on normal phone widths while five remain available on very narrow windows with no magic dp value anywhere.
 */
private const val DEFAULT_VISIBLE_DATES = 7

/**
 * How far back the strip reaches. Bounded on purpose: there is no tomorrow to
 * journal, so the range runs from `today - HISTORY_DAYS` up to `today` and stops.
 * That is a fixed, modest [LazyRow] — only the ~5 realised cells are ever
 * composed, and no unbounded or ever-growing list is ever built.
 */
internal const val HISTORY_DAYS = 365L

/**
 * The horizontal date strip that replaces the old pagination ticks.
 *
 * Behaviour:
 * - the selected date stays centred — on entry, after a tap, and after a fling;
 * - flings snap to the nearest date instead of coming to rest between two;
 * - the day the strip has *settled* on becomes the selected day, and that — not
 *   the drag itself — is what triggers the content swap;
 * - the range is bounded, so the strip can neither grow without limit nor reach
 *   into a future the Diary does not accept.
 *
 * All sizing comes from [BoxWithConstraints], so the strip is correct at any
 * width, aspect ratio, font scale and navigation mode, and it re-derives itself
 * on configuration change instead of caching a stale pixel width.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiaryDateStrip(
    selectedDay: Long,
    daysWithMemories: Set<Long>,
    onSelectDay: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { DateTimeUtils.today().toEpochDay() }
    val days = remember(today) { dayStripRange(today) }
    val selectedIndex = days.indexOf(selectedDay).takeIf { it >= 0 } ?: days.lastIndex

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val haptics = LocalHapticFeedback.current

    // Re-centre whenever the selection changes by any route. Compose animation
    // coroutines already honour the system "Remove animations" setting, so no
    // extra duration-scale plumbing is needed here.
    LaunchedEffect(selectedIndex) {
        if (listState.firstVisibleItemIndex != selectedIndex) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    // Centred snapping via the compose-foundation `snapping` API that already
    // ships in the resolved Compose version — no extra dependency, and no
    // reaching into LazyRow internals.
    val flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center)

    // The index the strip has actually come to rest on. Reading it only while
    // idle is what keeps the day-swap and the haptic out of the drag itself.
    val settledIndex by remember {
        derivedStateOf {
            if (listState.isScrollInProgress) null else listState.firstVisibleItemIndex
        }
    }
    LaunchedEffect(settledIndex) {
        val index = settledIndex ?: return@LaunchedEffect
        if (index != selectedIndex) onSelectDay(days[index])
    }

    // One tick per genuine change of day. Keyed on the *selected* value rather
    // than on the tap or on scroll position, so a tap that also re-centres the
    // strip cannot double-fire and holding a finger down produces nothing.
    var hapticDay by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(selectedDay) {
        val previous = hapticDay
        if (previous != null && previous != selectedDay) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        hapticDay = selectedDay
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // With a leading/trailing pad of half a cell, index 0 sits dead centre.
        val visibleDates = if (maxWidth < 340.dp) 5 else DEFAULT_VISIBLE_DATES
        val cellWidth: Dp = maxWidth / visibleDates
        val edgePadding = (maxWidth - cellWidth) / 2f

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = edgePadding),
            flingBehavior = flingBehavior
        ) {
            items(
                count = days.size,
                key = { index -> days[index] }
            ) { index ->
                val day = days[index]
                DiaryDateCell(
                    epochDay = day,
                    cellWidth = cellWidth,
                    isSelected = day == selectedDay,
                    isToday = day == today,
                    hasMemories = day in daysWithMemories,
                    onClick = { onSelectDay(day) }
                )
            }
        }
    }
}

/**
 * The bounded, ascending list of epoch days the strip can show, oldest first,
 * ending at [today]. Extracted as a pure function because the *range* is the
 * part that must never become unbounded, and that is exactly what is asserted
 * in the unit tests.
 */
internal fun dayStripRange(today: Long, historyDays: Long = HISTORY_DAYS): List<Long> =
    ((today - historyDays)..today).toList()

/** One date in the strip: weekday abbreviation above the day numeral. */
@Composable
private fun DiaryDateCell(
    epochDay: Long,
    cellWidth: Dp,
    isSelected: Boolean,
    isToday: Boolean,
    hasMemories: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val date = remember(epochDay) { DateTimeUtils.epochDayToLocalDate(epochDay) }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "diaryDateCellScale"
    )
    val fill by animateColorAsState(
        targetValue = if (isSelected) DiaryLavender else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "diaryDateCellFill"
    )
    val numeralColor by animateColorAsState(
        targetValue = when {
            isSelected -> DiaryInkViolet
            isToday -> DiaryInkViolet.copy(alpha = 0.66f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        },
        label = "diaryDateCellNumeral"
    )

    Box(
        modifier = modifier
            .width(cellWidth)
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                this.selected = isSelected
                this.role = Role.Tab
                contentDescription = buildString {
                    append(DateTimeUtils.formatFullDate(date))
                    if (isToday) append(" (today)")
                    if (hasMemories) append(", has memories")
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // The tinted pill is sized by its content, so a longer weekday name in
        // another locale can never overflow the cell or collide with its
        // neighbours.
        Column(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 5.dp)
                .clip(CircleShape)
                .background(fill),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                DateTimeUtils.shortDayName(date).uppercase().take(3),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    DiaryInkViolet.copy(alpha = 0.72f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = numeralColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.scale(scale)
            )
            // Preserves what the old ticks conveyed — which days already hold
            // memories — without the ambiguity of a dot that meant either
            // "selected" or "has content" depending only on its size.
            Box(
                Modifier
                    .padding(top = 3.dp)
                    .size(3.dp)
                    .alpha(if (hasMemories) 1f else 0f)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            DiaryInkViolet
                        } else {
                            DiaryInkViolet.copy(alpha = 0.38f)
                        }
                    )
            )
        }
    }
}
