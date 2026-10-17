package com.lifeos.app.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.intelligence.HabitAnalysisSummary
import com.lifeos.app.core.intelligence.PeriodReport
import com.lifeos.app.core.intelligence.PersonalStatistics
import com.lifeos.app.core.intelligence.TrendDirection
import com.lifeos.app.core.intelligence.TrendResult
import com.lifeos.app.ui.components.LifeOSBanner
import com.lifeos.app.ui.components.LifeOSEyebrow
import com.lifeos.app.ui.components.LifeOSMetric
import com.lifeos.app.ui.components.LifeOSPageHeader
import com.lifeos.app.ui.components.LifeOSPill
import com.lifeos.app.ui.components.LifeOSProgressRing
import com.lifeos.app.ui.components.LifeOSSectionTitle
import com.lifeos.app.ui.components.LifeOSSegmentedControl
import com.lifeos.app.ui.components.LifeOSStatTile
import com.lifeos.app.ui.components.LifeOSSurface
import com.lifeos.app.ui.components.lifeOSCanvas
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors
import kotlin.math.abs

/**
 * Insights — spec §27 "Life Insights" and §28 "Productivity Insights".
 *
 * Turns the raw rows the user already has (tasks, habits, diary, expenses,
 * captures) into a weekly / monthly / all-time dashboard. Every number comes
 * from [InsightsViewModel] straight out of the offline engine — there are no
 * placeholder or sample figures anywhere on this screen.
 */
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize().lifeOSCanvas()) {
        LifeOSPageHeader(
            eyebrow = "LifeOS",
            title = "Insights",
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LifeOSSpacing.screenPadding,
                end = LifeOSSpacing.screenPadding,
                top = LifeOSSpacing.md,
                bottom = LifeOSSpacing.sectionSpacing
            ),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
        ) {
            item {
                LifeOSSegmentedControl(
                    options = InsightRange.entries.toList(),
                    selected = state.range,
                    onSelect = viewModel::select,
                    label = { rangeLabel(it) }
                )
            }

            when {
                state.errorMessage != null -> item {
                    LifeOSBanner(
                        icon = Icons.Filled.Info,
                        text = state.errorMessage.orEmpty(),
                        tint = LocalLifeOSSemanticColors.current.warning
                    )
                }

                state.isLoading && state.report == null && state.statistics == null -> item {
                    LifeOSSurface(Modifier.fillMaxWidth()) {
                        Text(
                            "Reading your data…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                state.range == InsightRange.ALL_TIME -> {
                    val stats = state.statistics
                    if (stats != null) allTimeItems(stats)
                }

                else -> {
                    val report = state.report
                    if (report != null) reportItems(report)
                }
            }
        }
    }
}

// ---- Weekly / monthly report ------------------------------------------------

private fun LazyListScope.reportItems(report: PeriodReport) {
    item {
        LifeOSSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                LifeOSEyebrow(report.periodLabel)
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.lg)
                ) {
                    LifeOSProgressRing(
                        progress = report.productivity.score / 100f,
                        ringSize = 118.dp,
                        strokeWidth = 11.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                report.productivity.score.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "/ 100",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.xs)
                    ) {
                        Text(
                            report.productivity.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Tasks ${report.productivity.taskComponent} · " +
                                "Habits ${report.productivity.habitComponent}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    report.narrative,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
            LifeOSStatTile(
                value = "${report.taskAnalysis.completedInPeriod}/${report.taskAnalysis.totalInPeriod}",
                label = "Tasks done",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CheckCircle,
                tint = LocalLifeOSSemanticColors.current.success,
                supporting = "${report.taskAnalysis.completionRatePercent}% completion"
            )
            LifeOSStatTile(
                value = report.taskAnalysis.overdueCount.toString(),
                label = "Overdue",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Timeline,
                tint = if (report.taskAnalysis.overdueCount > 0) {
                    LocalLifeOSSemanticColors.current.warning
                } else {
                    MaterialTheme.colorScheme.primary
                },
                supporting = "${report.taskAnalysis.highPriorityOpenCount} high priority open"
            )
        }
    }

    report.spendTrend?.let { spend ->
        item {
            LifeOSSurface(Modifier.fillMaxWidth()) {
                LifeOSMetric(
                    value = money(spend.currentPeriodValue),
                    label = "Spent this period",
                    supporting = "vs ${money(spend.previousPeriodValue)} last period",
                    delta = trendDelta(spend),
                    deltaPositive = spend.direction != TrendDirection.UP,
                    emphasize = true
                )
            }
        }
    }

    report.moodTrend?.let { mood ->
        item {
            LifeOSSurface(Modifier.fillMaxWidth()) {
                LifeOSMetric(
                    value = moodLabel(mood),
                    label = "Diary mood",
                    supporting = "average ${"%.1f".format(mood.currentPeriodValue)} of 5",
                    delta = trendDelta(mood),
                    deltaPositive = mood.direction != TrendDirection.DOWN
                )
            }
        }
    }

    if (report.topHabits.isNotEmpty()) {
        item { LifeOSSectionTitle("Habits") }
        items(report.topHabits, key = { it.habitId }) { habit -> HabitRow(habit) }
    }

    if (report.recommendations.isNotEmpty()) {
        item { LifeOSSectionTitle("Suggestions") }
        items(report.recommendations, key = { it.title }) { rec ->
            LifeOSBanner(
                icon = Icons.Filled.Lightbulb,
                text = "${rec.title} — ${rec.detail}",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (report.patterns.isNotEmpty()) {
        item { LifeOSSectionTitle("Patterns") }
        items(report.patterns, key = { it.title }) { pattern ->
            LifeOSBanner(
                icon = Icons.Filled.Info,
                text = "${pattern.title} — ${pattern.detail}",
                tint = LocalLifeOSSemanticColors.current.info
            )
        }
    }
}

// ---- All-time statistics ----------------------------------------------------

private fun LazyListScope.allTimeItems(stats: PersonalStatistics) {
    item { LifeOSSectionTitle("Everything so far") }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
            LifeOSStatTile(
                value = stats.totalNotes.toString(),
                label = "Notes",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Star,
                tint = MaterialTheme.colorScheme.primary
            )
            LifeOSStatTile(
                value = stats.totalTasksCompleted.toString(),
                label = "Tasks done",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CheckCircle,
                tint = LocalLifeOSSemanticColors.current.success
            )
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
            LifeOSStatTile(
                value = stats.totalHabitsTracked.toString(),
                label = "Habits",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.LocalFireDepartment,
                tint = LocalLifeOSSemanticColors.current.warning
            )
            LifeOSStatTile(
                value = stats.totalDiaryEntries.toString(),
                label = "Diary entries",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Timeline,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
            LifeOSStatTile(
                value = stats.totalCaptures.toString(),
                label = "Memories",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.PhotoLibrary,
                tint = MaterialTheme.colorScheme.primary
            )
            LifeOSStatTile(
                value = money(stats.totalSpend),
                label = "Total spent",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Payments,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
    if (stats.diaryStreakDays > 0) {
        item {
            LifeOSPill(
                text = "${stats.diaryStreakDays}-day diary streak",
                icon = Icons.Filled.LocalFireDepartment,
                tint = LocalLifeOSSemanticColors.current.gold
            )
        }
    }
}

// ---- Small pieces -----------------------------------------------------------

@Composable
private fun HabitRow(habit: HabitAnalysisSummary) {
    LifeOSSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(LifeOSSpacing.lg)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    habit.habitName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Best ${habit.longestStreak}d · ${habit.completionPercentThisMonth}% this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LifeOSPill(
                text = "${habit.currentStreak}d",
                icon = Icons.Filled.LocalFireDepartment,
                tint = if (habit.currentStreak > 0) {
                    LocalLifeOSSemanticColors.current.warning
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

// ---- Formatting helpers -----------------------------------------------------

private fun rangeLabel(range: InsightRange): String = when (range) {
    InsightRange.WEEK -> "This week"
    InsightRange.MONTH -> "This month"
    InsightRange.ALL_TIME -> "All time"
}

private fun money(value: Double): String = "₹${"%,.0f".format(value)}"

private fun trendDelta(trend: TrendResult): String? {
    val change = trend.percentChange ?: return null
    if (change == 0) return "no change"
    val sign = if (change > 0) "+" else "−"
    return "$sign${abs(change)}%"
}

private fun moodLabel(trend: TrendResult): String = when (trend.direction) {
    TrendDirection.UP -> "Brighter"
    TrendDirection.DOWN -> "Lower"
    TrendDirection.FLAT -> "Steady"
    TrendDirection.UNKNOWN -> "—"
}
