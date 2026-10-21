package com.lifeos.app.feature.insights

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.ai.AiConsent
import com.lifeos.app.core.designsystem.theme.ControlHeight
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.Radii
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.domain.formatMoney
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.EmptyState
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import com.lifeos.app.ui.components.Pill
import com.lifeos.app.ui.components.SectionHeader

/**
 * Insights: aggregates computed from the user's own rows.
 *
 * Every figure on this screen is a direct projection of stored data — there are no
 * estimates, benchmarks or comparison numbers. The summary card only appears when
 * the user has granted at least level-1 AI consent.
 */
@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(title = "Insights", onBack = null)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(InsightsViewModel.Period.entries) { period ->
                        Pill(
                            text = period.label,
                            selected = state.period == period,
                            onClick = { viewModel.selectPeriod(period) },
                        )
                    }
                }
            }

            if (!state.hasAnyData) {
                item {
                    EmptyState(
                        title = "Nothing to summarise yet",
                        subtitle = "Once you add expenses, tasks, habits or diary entries, " +
                            "this screen shows real totals for the period you pick.",
                    )
                }
            } else {
                item {
                    LifeOsCard(containerColor = LifeOs.tokens.softPinkSurface) {
                        Column {
                            Text(
                                text = "Spent · ${state.period.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = state.totalDisplay(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${state.currencyCode} · exact minor units, no rounding",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        MetricCard(
                            label = "Tasks done",
                            value = state.tasksCompleted.toString(),
                            hint = "${state.tasksOpen} open",
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            label = "Habit rate",
                            value = "${state.habitCompletionRate}%",
                            hint = "${state.habitsActive} habits",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        MetricCard(
                            label = "Diary days",
                            value = state.diaryEntries.toString(),
                            hint = "avg mood ${formatMood(state.averageMood)}",
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            label = "Captures",
                            value = state.captures.toString(),
                            hint = "${state.timelineEvents} events",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (state.spendingByCategory.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Spending by category",
                            subtitle = state.period.label,
                        )
                    }
                    items(state.spendingByCategory) { bar ->
                        CategoryBar(
                            label = bar.label,
                            value = bar.value,
                            max = state.spendingByCategory.firstOrNull()?.value ?: 1L,
                            currencyCode = state.currencyCode,
                        )
                    }
                }

                if (state.moodSeries.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Mood",
                            subtitle = "1 = awful · 5 = great, from your diary",
                        )
                    }
                    item {
                        LifeOsCard {
                            Column {
                                state.moodSeries.forEach { (date, score) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = Spacing.xxs),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = viewModel.dayLabel(date),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(end = Spacing.sm),
                                        )
                                        MoodBar(score = score, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                LifeOsCard {
                    Column {
                        Text(
                            text = "AI summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        when {
                            state.aiLevel < AiConsent.SUMMARIES -> Text(
                                text = "Off. Turn on on-device summaries in Settings to see a " +
                                    "short, computed summary here. Nothing is sent anywhere.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            state.summary.isNullOrBlank() -> Text(
                                text = "Not enough data yet for a summary.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            else -> Text(
                                text = state.summary!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = viewModel.todaySummaryHint(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatMood(value: Double): String =
    if (value <= 0.0) "—" else String.format("%.1f", value)

@Composable
private fun MetricCard(
    label: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    LifeOsCard(modifier = modifier) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryBar(
    label: String,
    value: Long,
    max: Long,
    currencyCode: String,
) {
    Column(Modifier.padding(vertical = Spacing.xxs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = formatMoney(value, currencyCode),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(Spacing.xxs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ControlHeight.chip / 2)
                .background(LifeOs.tokens.softPinkSurface, RoundedCornerShape(Radii.pill)),
        ) {
            val fraction = if (max <= 0L) 0f else (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(ControlHeight.chip / 2)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(Radii.pill)),
            )
        }
    }
}

@Composable
private fun MoodBar(score: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(12.dp)
            .background(LifeOs.tokens.softPinkSurface, RoundedCornerShape(Radii.pill)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((score.coerceIn(1, 5) / 5f))
                .height(12.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(Radii.pill)),
        )
    }
}
