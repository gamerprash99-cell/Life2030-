package com.lifeos.app.ui.timeline

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Timeline
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
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryPaperCard
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
@Composable
fun TimelineScreen(
    onBack: () -> Unit = {},
    onOpenItem: (TimelineItem) -> Unit = {}
) {
    val locator = LocalServiceLocator.current
    val viewModel: TimelineViewModel = viewModel(factory = LambdaViewModelFactory { TimelineViewModel(locator.buildTimelineUseCase) })
    val today = remember { DateTimeUtils.today() }
    var selectedDate by remember { mutableStateOf(today) }
    val items by viewModel.items.collectAsState()
    LaunchedEffect(selectedDate) { viewModel.loadFor(selectedDate.toEpochDay()) }
    BackHandler(onBack = onBack)
    val canGoForward = selectedDate.isBefore(today)

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TimelineHeader(
                dateLabel = DateTimeUtils.formatFullDate(selectedDate),
                canGoForward = canGoForward,
                onBack = onBack,
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
        }
    }
}

/** Centered dated header with back navigation and day-navigation chevrons. */
@Composable
private fun TimelineHeader(
    dateLabel: String,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        Text(
            dateLabel,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
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
        Column(
            Modifier.width(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isFirst) {
                Spacer(Modifier.height(14.dp))
            } else {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(14.dp)
                        .background(DiaryHairline, RoundedCornerShape(50))
                )
            }
            NodeBadge(item.icon)
            if (isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(DiaryHairline.copy(alpha = 0.8f), RoundedCornerShape(50))
                )
            } else {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(DiaryHairline, RoundedCornerShape(50))
                )
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
        color = DiaryPaperCard,
        shape = CircleShape,
        border = BorderStroke(1.dp, DiaryHairline),
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
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                item.subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    DateTimeUtils.formatMinutes(item.timeMinutes),
                    style = MaterialTheme.typography.labelSmall,
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
            Icon(Icons.Filled.Timeline, contentDescription = null, tint = DiaryInkViolet, modifier = Modifier.size(36.dp))
        }
        Text(
            "No timeline entries",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            "Tasks, habits, expenses and diary entries from this day will appear here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, start = 32.dp, end = 32.dp),
            textAlign = TextAlign.Center
        )
    }
}