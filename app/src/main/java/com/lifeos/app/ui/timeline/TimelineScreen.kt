package com.lifeos.app.ui.timeline

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.usecase.BuildTimelineUseCase
import com.lifeos.app.ui.components.GlassCard
import com.lifeos.app.ui.components.HeaderActionCircle
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.diary.DiaryTheme
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryHandwriting
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.FigmaAccent
import com.lifeos.app.ui.theme.FigmaAccentDark
import com.lifeos.app.ui.theme.FigmaMuted
import com.lifeos.app.ui.theme.FigmaStroke
import com.lifeos.app.ui.theme.FigmaSurface
import com.lifeos.app.ui.theme.FigmaText
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimelineViewModel(private val buildTimeline: BuildTimelineUseCase) : ViewModel() {
    private val _items = MutableStateFlow<List<TimelineItem>>(emptyList())
    val items: StateFlow<List<TimelineItem>> = _items
    fun loadFor(epochDay: Long) { viewModelScope.launch { _items.value = buildTimeline(epochDay) } }
}

/**
 * Life Timeline — a real-time, read-only aggregation of the day's Tasks,
 * Habits, Expenses and Diary entries (see BuildTimelineUseCase), drawn as a
 * dated journal from the Stitch "Timeline" reference: a 2px hairline spine on
 * a fixed left track, anchored by paper node badges, feeding 24dp-radius
 * cards. Navigation and data flow are unchanged.
 */
enum class LifeView { TIMELINE, CALENDAR }

@Composable
fun TimelineScreen(
    onBack: () -> Unit = {},
    onOpenItem: (TimelineItem) -> Unit = {},
    onOpenSearch: () -> Unit = {}
) {
    val locator = LocalServiceLocator.current
    val viewModel: TimelineViewModel = viewModel(factory = LambdaViewModelFactory { TimelineViewModel(locator.buildTimelineUseCase) })
    val today = remember { DateTimeUtils.today() }
    var selectedDate by remember { mutableStateOf(today) }
    val items by viewModel.items.collectAsState()
    LaunchedEffect(selectedDate) { viewModel.loadFor(selectedDate.toEpochDay()) }
    BackHandler(onBack = onBack)
    val canGoForward = selectedDate.isBefore(today)
    var lifeView by remember { mutableStateOf(LifeView.TIMELINE) }

    DiaryTheme {
        Scaffold { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(FigmaCanvasTop, FigmaCanvasMid, FigmaCanvasBottom)))
                    .padding(padding)
            ) {
                LifePageHeader(
                    title = if (lifeView == LifeView.TIMELINE) "Life Timeline" else "Life Calendar",
                    subtitle = if (lifeView == LifeView.TIMELINE) "The story of your days" else "Life, day by day",
                    onBack = onBack,
                    onAction = if (lifeView == LifeView.TIMELINE) {
                        { lifeView = LifeView.CALENDAR }
                    } else {
                        { onOpenSearch() }
                    },
                    actionIcon = if (lifeView == LifeView.TIMELINE) Icons.Filled.Today else Icons.Filled.Search
                )
                LifeViewsToggle(
                    selected = lifeView,
                    onSelect = { lifeView = it }
                )
                if (lifeView == LifeView.TIMELINE) {
                    TimelineHeader(
                        dateLabel = DateTimeUtils.formatFullDate(selectedDate),
                        canGoForward = canGoForward,
                        onBack = null,
                        onPrevious = { selectedDate = selectedDate.minusDays(1) },
                        onNext = { if (canGoForward) selectedDate = selectedDate.plusDays(1) }
                    )
                    if (items.isEmpty()) {
                        TimelineEmptyState(Modifier.fillMaxSize())
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = LifeOSSpacing.screenPadding,
                                end = LifeOSSpacing.screenPadding,
                                top = 6.dp,
                                bottom = LifeOSSpacing.compactPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                                TimelineSpineRow(
                                    item = item,
                                    isFirst = index == 0,
                                    isLast = index == items.lastIndex,
                                    onClick = { onOpenItem(item) }
                                )
                            }
                        }
                    }
                } else {
                    // Calendar view: minimal placeholder matching Figma intent (full grid can be added later)
                    TimelineEmptyState(Modifier.fillMaxSize())
                }
            }
        }
    }
}

/** Life page header matching Figma (mint kicker + title + trailing action circle). */
@Composable
private fun LifePageHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit = {},
    onAction: () -> Unit,
    actionIcon: ImageVector = Icons.Filled.Today
) {
    LifeOSTopBar(
        title = title,
        subtitle = subtitle,
        onBack = onBack,
        actions = {
            HeaderActionCircle(actionIcon, "Action", onAction)
        }
    )
}

/** Life views toggle: Timeline | Calendar (Figma tag style). */
@Composable
private fun LifeViewsToggle(selected: LifeView, onSelect: (LifeView) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = LifeOSSpacing.screenPadding, vertical = LifeOSSpacing.sm),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(FigmaSurface, RoundedCornerShape(100.dp))
                .border(1.dp, FigmaStroke, RoundedCornerShape(100.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tag(
                text = "Timeline",
                selected = selected == LifeView.TIMELINE,
                onClick = { onSelect(LifeView.TIMELINE) }
            )
            Tag(
                text = "Calendar",
                selected = selected == LifeView.CALENDAR,
                onClick = { onSelect(LifeView.CALENDAR) }
            )
        }
    }
}

@Composable
private fun Tag(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clickable(onClick = onClick)
            .background(if (selected) FigmaAccentDark else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(100.dp))
            .border(
                if (selected) BorderStroke(1.dp, FigmaAccent.copy(alpha = 0.3f)) else BorderStroke(0.dp, androidx.compose.ui.graphics.Color.Transparent),
                RoundedCornerShape(100.dp)
            )
            .padding(horizontal = LifeOSSpacing.md, vertical = LifeOSSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) FigmaAccent else FigmaMuted,
            maxLines = 1
        )
    }
}

/** Centered dated header with day-navigation chevrons for timeline view. */
@Composable
private fun TimelineHeader(
    dateLabel: String,
    canGoForward: Boolean,
    onBack: (() -> Unit)?,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        }
        Text(
            dateLabel,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = DiaryHandwriting),
            fontWeight = FontWeight.SemiBold,
            color = FigmaText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f).padding(horizontal = 4.dp)
        )
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day")
            }
            IconButton(enabled = canGoForward, onClick = onNext, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next day")
            }
        }
    }
}

/**
 * One journal row: a 2px hairline spine on a fixed 24dp+ track, anchored by a
 * paper node badge, with the entry card beside it.
 */
@Composable
private fun TimelineSpineRow(item: TimelineItem, isFirst: Boolean, isLast: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .width(32.dp)
        ) {
            val nodeSize = 30.dp
            val nodeTopPadding = 6.dp
            val nodeCenterY = nodeTopPadding + (nodeSize / 2f)
            val railX = 16.dp
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        val x = railX.toPx()
                        val center = nodeCenterY.toPx()
                        drawLine(
                            color = FigmaStroke.copy(alpha = 0.8f),
                            strokeWidth = 2.dp.toPx(),
                            start = Offset(x, if (isFirst) center else 0f),
                            end = Offset(x, if (isLast) center else size.height)
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = nodeTopPadding)
            ) {
                NodeBadge(item.icon)
            }
        }
        Spacer(Modifier.width(10.dp))
        TimelineEntryCard(item, onClick = onClick)
    }
}

/** Paper node badge that anchors a timeline entry on the spine. */
@Composable
private fun NodeBadge(icon: String) {
    Surface(
        color = FigmaElevated,
        shape = CircleShape,
        border = BorderStroke(1.dp, FigmaStroke),
        shadowElevation = 1.dp,
        modifier = Modifier.size(30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(icon, fontSize = 13.sp)
        }
    }
}

@Composable
private fun TimelineEntryCard(item: TimelineItem, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        cornerRadius = 24.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FigmaText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                item.subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = FigmaMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                color = FigmaElevated,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    DateTimeUtils.formatMinutes(item.timeMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = FigmaText,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/** Muted, on-brand empty state that mirrors the Diary's, tuned to the day view. */
@Composable
private fun TimelineEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(DiaryLavender.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Timeline, contentDescription = null, tint = FigmaAccent, modifier = Modifier.size(36.dp))
        }
        Text(
            "No timeline entries",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = FigmaText,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            "Tasks, habits, expenses and diary entries from this day will appear here.",
            style = MaterialTheme.typography.bodySmall,
            color = FigmaMuted,
            modifier = Modifier.padding(top = 6.dp, start = 32.dp, end = 32.dp),
            textAlign = TextAlign.Center
        )
    }
}

