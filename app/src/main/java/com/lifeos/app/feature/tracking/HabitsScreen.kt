package com.lifeos.app.feature.tracking

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.data.HabitTodayItem
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.HabitProgress
import com.lifeos.app.core.domain.Recurrence
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.EmptyState
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import com.lifeos.app.ui.components.Pill
import com.lifeos.app.ui.components.SectionHeader

/**
 * Habits: today's checklist with real progress, streak and a 5–26 week heatmap,
 * plus an add dialog. Tapping a habit opens its heatmap; the ring and streak are
 * computed by [HabitProgress] from stored completions only.
 */
@Composable
fun HabitsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val state by viewModel.habitsState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(
            title = "Habits",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add habit")
                }
            },
        )

        if (state.items.isEmpty()) {
            EmptyState(
                title = "No habits yet",
                subtitle = "Add a habit you want to keep. Streaks and history are computed " +
                    "from your own check-ins.",
                modifier = Modifier.fillMaxSize(),
                actionLabel = "Add habit",
                onAction = { showAdd = true },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                item {
                    TodaySummary(done = state.doneCount, total = state.items.size)
                }
                items(state.items, key = { it.habit.id }) { item ->
                    HabitRow(
                        item = item,
                        expanded = state.expandedHabitId == item.habit.id,
                        cells = state.expandedCells,
                        streak = state.expandedStreak,
                        onToggle = { viewModel.toggleHabit(item.habit.id, item.isDoneToday) },
                        onExpand = {
                            if (state.expandedHabitId == item.habit.id) {
                                viewModel.collapseHabit()
                            } else {
                                viewModel.expandHabit(item.habit.id, state.heatmapWeeks)
                            }
                        },
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddHabitDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, emoji, recurrence, target ->
                viewModel.addHabit(name, emoji, recurrence, null, target)
                showAdd = false
            },
        )
    }
}

@Composable
private fun TodaySummary(done: Int, total: Int) {
    val percent = if (total == 0) 0 else (done * 100) / total
    LifeOsCard(containerColor = LifeOs.tokens.softPinkSurface) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.size(Spacing.lg))
            Column {
                Text(
                    text = "$done of $total done today",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = Dates.humanLong(Dates.today()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HabitRow(
    item: HabitTodayItem,
    expanded: Boolean,
    cells: List<HabitProgress.HeatmapCell>,
    streak: HabitProgress.StreakResult,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
) {
    LifeOsCard(onClick = onExpand) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (item.isDoneToday) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                LifeOs.tokens.softPinkSurface
                            },
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.isDoneToday) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Done today",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = item.habit.emoji.ifBlank { item.habit.name.take(1) },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.size(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.habit.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = buildString {
                            append(Recurrence.fromId(item.habit.recurrence).label)
                            if (item.targetToday > 1) {
                                append(" · ${item.completedToday}/${item.targetToday}")
                            }
                            append(" · ${Dates.iso(Dates.today())}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onToggle) {
                    Text(if (item.isDoneToday) "Undo" else "Done")
                }
            }

            if (expanded) {
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    Text(
                        text = "Current streak: ${streak.current}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Best: ${streak.best}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                Heatmap(cells)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Each square is one day; darker means fully completed.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 7-row, N-column heatmap. Intensity 0..4 maps to the theme's pink ramp. */
@Composable
private fun Heatmap(cells: List<HabitProgress.HeatmapCell>) {
    val weeks = (cells.size + 6) / 7
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        for (week in 0 until weeks) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (day in 0 until 7) {
                    val index = week * 7 + day
                    val cell = cells.getOrNull(index)
                    val color = intensityColor(cell?.intensity ?: 0)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, MaterialTheme.shapes.extraSmall),
                    )
                }
            }
        }
    }
}

@Composable
private fun intensityColor(intensity: Int) = when (intensity) {
    0 -> LifeOs.tokens.border.copy(alpha = 0.5f)
    1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.74f)
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Recurrence, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf(Recurrence.DAILY) }
    var targetText by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = trackingFieldColors(),
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(2) },
                    label = { Text("Emoji (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = trackingFieldColors(),
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { input -> targetText = input.filter { it.isDigit() }.take(3) },
                    label = { Text("Times per day") },
                    supportingText = { Text("1 = simple done/not-done habit.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = trackingFieldColors(),
                )
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(
                        listOf(
                            Recurrence.DAILY,
                            Recurrence.WEEKLY,
                            Recurrence.EVERY_N_DAYS,
                        ),
                    ) { option ->
                        Pill(
                            text = option.label,
                            selected = recurrence == option,
                            onClick = { recurrence = option },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        emoji,
                        recurrence,
                        targetText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun trackingFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = LifeOs.tokens.border,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
)
