package com.lifeos.app.feature.life

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.database.entity.LifeEventEntity
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.LifeEventType
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.EmptyState
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import com.lifeos.app.ui.components.Pill

/** Filter chips for the timeline, drawn from the real enum (no invented types). */
private val TIMELINE_FILTERS = listOf(
    LifeEventType.NOTE_CREATED,
    LifeEventType.TASK_CREATED,
    LifeEventType.TASK_COMPLETED,
    LifeEventType.HABIT_COMPLETED,
    LifeEventType.EXPENSE_ADDED,
    LifeEventType.DIARY_WRITTEN,
    LifeEventType.CAPTURE_ADDED,
)

/**
 * Timeline: every recorded action, grouped by day, newest first.
 * Rows exist because the user did something; nothing is synthesised.
 */
@Composable
fun TimelineScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LifeViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val state by viewModel.timelineState.collectAsStateWithLifecycle()
    val visible = state.visible

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(title = "Timeline", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    item {
                        Pill(
                            text = "All (${state.total})",
                            selected = state.filter == null,
                            onClick = { viewModel.setEventFilter(null) },
                        )
                    }
                    items(TIMELINE_FILTERS) { type ->
                        Pill(
                            text = type.label,
                            selected = state.filter == type.code,
                            onClick = { viewModel.setEventFilter(type.code) },
                        )
                    }
                }
            }

            if (visible.isEmpty()) {
                item {
                    EmptyState(
                        title = if (state.total == 0) "Your timeline is empty" else "Nothing matches this filter",
                        subtitle = if (state.total == 0) {
                            "Actions you take — notes, tasks, habits, expenses, diary entries, " +
                                "captures — are recorded here automatically."
                        } else {
                            "Pick “All” to see ${state.total} recorded events."
                        },
                    )
                }
            } else {
                var lastDay: String? = null
                visible.forEach { event ->
                    if (event.occurredOn != lastDay) {
                        lastDay = event.occurredOn
                        item(key = "day-${event.occurredOn}") {
                            DayHeader(iso = event.occurredOn, count = state.dayCounts[event.occurredOn] ?: 0)
                        }
                    }
                    item(key = event.id) { TimelineRow(event) }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(iso: String, count: Int) {
    Column(Modifier.padding(top = Spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = Dates.relative(iso),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (count == 1) "1 event" else "$count events",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = LifeOs.tokens.border)
    }
}

@Composable
private fun TimelineRow(event: LifeEventEntity) {
    LifeOsCard {
        Column {
            Text(
                text = event.title.ifBlank { LifeEventType.fromCode(event.type).label },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = LifeEventType.fromCode(event.type).label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (event.detail.isNotBlank()) {
                    Text(
                        text = " · ${event.detail}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
