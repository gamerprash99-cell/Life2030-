package com.lifeos.app.feature.life

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import com.lifeos.app.ui.components.SectionHeader

/**
 * Life: the index of everything that is not a daily surface — timeline, calendar,
 * trash, search and settings — plus a Trash badge that reflects real deleted rows.
 */
@Composable
fun LifeScreen(
    onOpenTimeline: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LifeViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val trash by viewModel.trashState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(title = "Life", onBack = null)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item { SectionHeader(title = "Your record", subtitle = "Everything stays on this device") }
            items(
                listOf(
                    LifeEntry("Timeline", "Every real action, newest first", onOpenTimeline),
                    LifeEntry("Calendar", "Activity density per day", onOpenCalendar),
                    LifeEntry("Search", "Find anything you recorded", onOpenSearch),
                ),
            ) { entry ->
                LifeOsCard(onClick = entry.onClick) {
                    Column {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = entry.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { SectionHeader(title = "Trackers") }
            items(
                listOf(
                    LifeEntry("Tasks", "Open, done and recurring", onOpenTasks),
                    LifeEntry("Habits", "Streaks and heatmap", onOpenHabits),
                    LifeEntry("Expenses", "Exact amounts by category", onOpenExpenses),
                    LifeEntry("Diary", "One entry per day, mood optional", onOpenDiary),
                ),
            ) { entry ->
                LifeOsCard(onClick = entry.onClick) {
                    Column {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = entry.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { SectionHeader(title = "Data") }
            item {
                LifeOsCard(onClick = onOpenTrash) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Trash",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (trash.totalCount == 0) {
                                    "Empty — deleted items rest here"
                                } else {
                                    "${trash.totalCount} deleted items can be restored"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (trash.totalCount > 0) {
                            Text(
                                text = trash.totalCount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            item {
                LifeOsCard(onClick = onOpenSettings) {
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Appearance, currency, AI consent, backup",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(Modifier.size(Spacing.sm)) }
            item {
                Text(
                    text = "LifeOS keeps your data in a local database on this device. " +
                        "There is no account and no server.",
                    style = MaterialTheme.typography.labelSmall,
                    color = LifeOs.tokens.beigeAccent,
                )
            }
        }
    }
}

private data class LifeEntry(
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)
