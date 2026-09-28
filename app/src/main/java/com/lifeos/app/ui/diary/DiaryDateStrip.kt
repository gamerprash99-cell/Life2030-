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

internal const val VISIBLE_DATES = 5
internal const val HISTORY_DAYS = 365L

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

    LaunchedEffect(selectedIndex) {
        if (listState.firstVisibleItemIndex != selectedIndex) listState.animateScrollToItem(selectedIndex)
    }

    val flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center)
    val settledIndex by remember {
        derivedStateOf { if (listState.isScrollInProgress) null else listState.firstVisibleItemIndex }
    }
    LaunchedEffect(settledIndex) {
        val index = settledIndex ?: return@LaunchedEffect
        if (index != selectedIndex) onSelectDay(days[index])
    }

    var hapticDay by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(selectedDay) {
        val previous = hapticDay
        if (previous != null && previous != selectedDay) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        hapticDay = selectedDay
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cellWidth: Dp = maxWidth / VISIBLE_DATES
        val edgePadding = (maxWidth - cellWidth) / 2f
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = edgePadding),
            flingBehavior = flingBehavior
        ) {
            items(count = days.size, key = { index -> days[index] }) { index ->
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

internal fun dayStripRange(today: Long, historyDays: Long = HISTORY_DAYS): List<Long> =
    ((today - historyDays)..today).toList()

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
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "diaryDateCellScale"
    )
    val fill by animateColorAsState(
        targetValue = if (isSelected) com.lifeos.app.ui.theme.DiaryLavender else Color.Transparent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "diaryDateCellFill"
    )
    val numeralColor by animateColorAsState(
        targetValue = when {
            isSelected -> com.lifeos.app.ui.theme.DiaryActionViolet
            isToday -> com.lifeos.app.ui.theme.DiaryInkViolet.copy(alpha = 0.66f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        },
        label = "diaryDateCellNumeral"
    )

    Box(
        modifier = modifier
            .width(cellWidth)
            .heightIn(min = 72.dp)
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
        Column(
            modifier = Modifier
                .padding(horizontal = 5.dp, vertical = 4.dp)
                .clip(CircleShape)
                .background(fill)
                .padding(horizontal = 11.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                DateTimeUtils.shortDayName(date).uppercase().take(3),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) com.lifeos.app.ui.theme.DiaryInkViolet.copy(alpha = 0.72f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
            Box(
                Modifier
                    .padding(top = 3.dp)
                    .size(3.dp)
                    .alpha(if (hasMemories) 1f else 0f)
                    .clip(CircleShape)
                    .background(if (isSelected) com.lifeos.app.ui.theme.DiaryActionViolet else com.lifeos.app.ui.theme.DiaryInkViolet.copy(alpha = 0.38f))
            )
        }
    }
}