package com.lifeos.app.ui.home

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSGradientButton
import com.lifeos.app.ui.components.LifeOSIntelligenceCard
import com.lifeos.app.ui.components.LifeOSSectionHeader
import com.lifeos.app.ui.theme.LifeOSAccentLavender
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSpacing

@Composable
fun HomeScreen(
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenCapture: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenNotes: () -> Unit = {},
    onOpenExpenses: () -> Unit = {},
    onOpenDiary: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenTimeline: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val locator = LocalServiceLocator.current
    val viewModel: HomeViewModel = viewModel(factory = LambdaViewModelFactory {
        HomeViewModel(locator.getHomeSummaryUseCase, locator.taskRepository, locator.habitRepository)
    })
    val summary by viewModel.summary.collectAsState()

    val totalTasks = summary?.tasksTotalToday ?: 0
    val completedTasks = summary?.tasksCompletedToday ?: 0
    val taskProgress = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks
    val verifiedHabits = summary?.habitsToday?.count { it.isDone } ?: 0
    val totalHabits = summary?.habitsToday?.size ?: 0
    val momentum = if (totalTasks + totalHabits == 0) 0 else {
        (((completedTasks + verifiedHabits).toFloat() / (totalTasks + totalHabits)) * 100).toInt()
    }
    val animatedProgress by animateFloatAsState(
        targetValue = taskProgress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 500f),
        label = "homeTaskProgress"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = LifeOSSpacing.screenPadding,
            end = LifeOSSpacing.screenPadding,
            top = 0.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
    ) {
        item {
            HomeHeader(
                dateLabel = summary?.dateLabel,
                onSearch = onOpenSearch,
                onSettings = onOpenSettings,
                onProfile = onOpenProfile
            )
        }

        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    Modifier
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = .42f),
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0f)
                                )
                            )
                        )
                )
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(summary?.greeting ?: "Welcome back", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "A calm plan for a focused day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            LifeOSCard(
                modifier = Modifier.fillMaxWidth(),
                tint = MaterialTheme.colorScheme.surface
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.padding(9.dp))
                        }
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text("Daily Momentum", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "$momentum% of your day targets achieved",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(color = LifeOSAccentLavender, shape = RoundedCornerShape(50)) {
                            Text(
                                if (momentum >= 50) "On Pace" else "Start",
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                color = LifeOSPrimary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                        color = LifeOSPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f)
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MomentumStat("Tasks", "$completedTasks/$totalTasks", Modifier.weight(1f))
                        MomentumStat("Habits", "$verifiedHabits/$totalHabits", Modifier.weight(1f))
                        MomentumStat("Spend", "₹${"%.0f".format(summary?.todaySpend ?: 0.0)}", Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item { QuickAction("New Note", Icons.Filled.EditNote, onOpenNotes) }
                item { QuickAction("Log Expense", Icons.Filled.Payments, onOpenExpenses) }
                item { QuickAction("Reflect", Icons.Filled.SelfImprovement, onOpenDiary) }
                item { QuickAction("Insights", Icons.Filled.AutoAwesome, onOpenInsights) }
                item { QuickAction("Timeline", Icons.Filled.Timeline, onOpenTimeline) }
            }
        }

        item { LifeOSSectionHeader("Today's Priorities", "See all", onOpenTasks) }
        if (summary?.tasksToday.isNullOrEmpty()) {
            item { EmptyStateCard("No tasks planned for today", "Add a task when you are ready to focus.", onOpenTasks) }
        } else {
            items(summary?.tasksToday?.take(4).orEmpty(), key = { it.id }) { task ->
                LifeOSCard(Modifier.fillMaxWidth(), onClick = onOpenTasks) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (task.isCompleted) LifeOSPrimary else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(task.title, style = MaterialTheme.typography.bodyLarge)
                            task.category?.let {
                                Text("#$it", style = MaterialTheme.typography.bodySmall, color = LifeOSPrimary)
                            }
                        }
                    }
                }
            }
        }

        item { LifeOSSectionHeader("Habits Streak", "Open all", onOpenHabits) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(summary?.habitsToday.orEmpty(), key = { it.habit.id }) { row ->
                    LifeOSCard(
                        modifier = Modifier.size(width = 148.dp, height = 126.dp),
                        tint = if (row.isDone) LifeOSAccentLavender else MaterialTheme.colorScheme.surface,
                        onClick = onOpenHabits
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(row.habit.icon, style = MaterialTheme.typography.headlineMedium)
                            Text(row.habit.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            Spacer(Modifier.height(7.dp))
                            Text(
                                "${row.progressCount}/${row.goalCount}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (row.isDone) LifeOSPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (row.isDone) "Completed" else "Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            LifeOSCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = LifeOSAccentLavender, shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.padding(10.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Passive Insights", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "₹${"%.0f".format(summary?.todaySpend ?: 0.0)} spent today · ${summary?.overdueTaskCount ?: 0} overdue tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { LifeOSIntelligenceCard(onClick = onOpenAiAssistant) }

        item {
            LifeOSGradientButton(
                text = "Capture a moment",
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenCapture
            )
        }
    }
}

@Composable
private fun HomeHeader(
    dateLabel: String?,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("LifeOS", style = MaterialTheme.typography.titleLarge)
                Text("•", color = LifeOSPrimary)
                Text("Today", style = MaterialTheme.typography.labelSmall, color = LifeOSPrimary)
            }
            Text(
                dateLabel ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onSearch) {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings")
        }
        Surface(
            onClick = onProfile,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .32f)
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = "Profile",
                tint = LifeOSPrimary,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
private fun MomentumStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .7f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 9.dp, horizontal = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.height(50.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.size(19.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, subtitle: String, onClick: () -> Unit) {
    LifeOSCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
