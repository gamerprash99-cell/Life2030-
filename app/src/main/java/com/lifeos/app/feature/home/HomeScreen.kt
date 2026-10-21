package com.lifeos.app.feature.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.LifeEventType
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.EmptyState
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import com.lifeos.app.ui.components.SectionHeader

/**
 * Home: today's date, today's real numbers, and the most recent timeline entries.
 * Every number comes from the database; nothing is estimated or seeded.
 */
@Composable
fun HomeScreen(
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenTimeline: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(title = "LifeOS", onBack = null)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Column {
                    Text(
                        text = state.greeting,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = Dates.humanLong(state.today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    StatCard(
                        label = "Open tasks",
                        value = state.openTasks.toString(),
                        hint = if (state.overdueTasks > 0) "${state.overdueTasks} overdue" else "none overdue",
                        emphasis = state.overdueTasks > 0,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTasks,
                    )
                    StatCard(
                        label = "Habits today",
                        value = "${state.habitsDone}/${state.habitsTotal}",
                        hint = if (state.habitsTotal == 0) "none yet" else "${state.habitPercent}% done",
                        emphasis = false,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenHabits,
                    )
                }
            }

            if (state.suggestion != null) {
                item {
                    LifeOsCard(containerColor = LifeOs.tokens.softPinkSurface) {
                        Column {
                            Text(
                                text = "Suggestion",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = state.suggestion.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Recent activity",
                    subtitle = if (state.recentEvents.isEmpty()) "Nothing recorded yet" else "From your timeline",
                    actionLabel = if (state.recentEvents.isEmpty()) null else "Timeline",
                    onAction = onOpenTimeline,
                )
            }

            if (state.recentEvents.isEmpty()) {
                item {
                    EmptyState(
                        title = "This is your starting point",
                        subtitle = "Capture a thought, write a note or add a task — " +
                            "everything you do shows up here as a real timeline entry.",
                    )
                }
            } else {
                itemsIndexed(state.recentEvents) { _, event ->
                    LifeOsCard {
                        Column {
                            Text(
                                text = event.title.ifBlank { LifeEventType.fromCode(event.type).label },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (event.detail.isNotBlank()) {
                                Text(
                                    text = event.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = Dates.relative(event.occurredOn),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    hint: String,
    emphasis: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    LifeOsCard(modifier = modifier, onClick = onClick) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = if (emphasis) LifeOs.tokens.warning else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
