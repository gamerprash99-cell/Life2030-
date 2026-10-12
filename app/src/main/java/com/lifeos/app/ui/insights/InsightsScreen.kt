package com.lifeos.app.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.domain.model.ExpenseCategories
import com.lifeos.app.ui.components.LifeOSBanner
import com.lifeos.app.ui.components.LifeOSEyebrow
import com.lifeos.app.ui.components.LifeOSHeroCard
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.LifeOSSegmentedControl
import com.lifeos.app.ui.components.LifeOSStatTile
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSecondary
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LifeOSSuccess
import com.lifeos.app.ui.theme.LifeOSWarning
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors
import java.util.Locale

/**
 * Insights — the honest monthly (or weekly/yearly) mirror. One hero number, a
 * grid of real metrics, the top spending category, and a single entry point to
 * LifeOS AI. Nothing here is estimated; an empty period shows zeroes that mean
 * zero.
 */
@Composable
fun InsightsScreen(
    onOpenAi: () -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: InsightsViewModel = viewModel(
        factory = LambdaViewModelFactory {
            InsightsViewModel(
                locator.taskRepository,
                locator.habitRepository,
                locator.expenseRepository,
                locator.diaryRepository,
                locator.captureRepository
            )
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = LifeOSSpacing.screenPadding,
            end = LifeOSSpacing.screenPadding,
            top = LifeOSSpacing.sm,
            bottom = LifeOSSpacing.sectionSpacing
        ),
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.xs)) {
                Text("Insights", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(state.periodLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            LifeOSSegmentedControl(
                options = InsightsPeriod.entries.toList(),
                selected = state.period,
                onSelect = viewModel::setPeriod,
                label = { it.label }
            )
        }
        item {
            LifeOSHeroCard {
                Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
                    Text(
                        "DID YOU MAKE PROGRESS?",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        "${state.tasksCompleted} task${if (state.tasksCompleted == 1) "" else "s"} done",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                    Text(
                        when {
                            state.tasksCompleted == 0 && state.spendTotal == 0.0 ->
                                "A clean slate. Log a task or an expense and this fills in."
                            state.spendTotal > 0 ->
                                "And ${formatMoney(state.spendTotal)} spent across the period."
                            else -> "Keep the rhythm going."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
        item {
            LifeOSEyebrow("Your period by the numbers")
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                LifeOSStatTile(
                    value = "${state.tasksCompleted}",
                    label = "Tasks done",
                    icon = Icons.Filled.CheckCircle,
                    tint = LifeOSSuccess,
                    modifier = Modifier.weight(1f)
                )
                LifeOSStatTile(
                    value = "${state.habitCompletions}",
                    label = "Habit check-ins",
                    icon = Icons.Filled.LocalFireDepartment,
                    tint = LifeOSWarning,
                    supporting = if (state.habitsTracked > 0) "${state.habitsTracked} habits tracked" else "No habits yet",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                LifeOSStatTile(
                    value = formatMoney(state.spendTotal),
                    label = "Spent",
                    icon = Icons.Filled.Payments,
                    tint = LifeOSPrimary,
                    supporting = "≈ ${formatMoney(state.dailyAverage)} / day",
                    modifier = Modifier.weight(1f)
                )
                LifeOSStatTile(
                    value = "${state.diaryEntries}",
                    label = "Diary entries",
                    icon = Icons.Filled.CalendarMonth,
                    tint = LifeOSSecondary,
                    supporting = "${state.captures} captures",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        state.topCategory?.let { category ->
            item {
                LifeOSStatTile(
                    value = formatMoney(state.topCategoryAmount),
                    label = "Biggest category",
                    icon = null,
                    tint = LifeOSWarning,
                    supporting = "${ExpenseCategories.emojiFor(category)}  $category",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            LifeOSBanner(
                icon = Icons.Filled.Lock,
                text = "These numbers are computed on your device from your own records. Nothing leaves the phone.",
                tint = LocalLifeOSSemanticColors.current.success
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
                LifeOSEyebrow("Ask about your life")
                Text(
                    "LifeOS AI can read the same data and answer questions like “How was my week?”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(LifeOSSpacing.xs))
                LifeOSPrimaryButton(text = "Ask LifeOS AI", onClick = onOpenAi, icon = Icons.Filled.Bolt)
            }
        }
    }
}

private fun formatMoney(amount: Double): String {
    val digits = if (amount % 1.0 == 0.0) 0 else 2
    return "₹" + String.format(Locale.getDefault(), "%,.${digits}f", amount)
}
