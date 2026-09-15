package com.lifeos.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.GlassCard
import com.lifeos.app.ui.home.HomeViewModel

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
    onOpenSearch: () -> Unit = {}
) {
    val locator = LocalServiceLocator.current
    val viewModel: HomeViewModel = viewModel(
        factory = LambdaViewModelFactory {
            HomeViewModel(locator.getHomeSummaryUseCase, locator.taskRepository, locator.habitRepository)
        }
    )
    val summary by viewModel.summary.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onOpenCapture, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Capture") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp,
                bottom = com.lifeos.app.ui.theme.LifeOSSpacing.extendedFabContentClearance
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(summary?.greeting ?: "Welcome", style = MaterialTheme.typography.headlineMedium)
                    Text(summary?.dateLabel ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val links = listOf(
                        "📝 Notes" to onOpenNotes,
                        "💰 Expenses" to onOpenExpenses,
                        "📔 Diary" to onOpenDiary,
                        "📊 Insights" to onOpenInsights,
                        "🔍 Search" to onOpenSearch,
                    )
                    items(links) { (label, action) ->
                        com.lifeos.app.ui.components.GlassChip(
                            modifier = Modifier.clickable { action() }
                        ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Today's spend", style = MaterialTheme.typography.labelLarge)
                        Text("₹${"%.0f".format(summary?.todaySpend ?: 0.0)}", style = MaterialTheme.typography.headlineLarge)
                        if ((summary?.overdueTaskCount ?: 0) > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${summary?.overdueTaskCount} overdue task(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Today's Tasks", style = MaterialTheme.typography.titleMedium)
                        Text("${summary?.tasksCompletedToday ?: 0}/${summary?.tasksTotalToday ?: 0}", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            val total = summary?.tasksTotalToday ?: 0
                            if (total == 0) 0f else (summary?.tasksCompletedToday ?: 0).toFloat() / total
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            summary?.tasksToday?.take(5)?.let { tasks ->
                items(tasks) { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(checked = task.isCompleted, onCheckedChange = { viewModel.toggleTask(task.id, it) })
                        Text(task.title, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            item {
                Text("Habits", style = MaterialTheme.typography.titleMedium)
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(summary?.habitsToday ?: emptyList()) { row ->
                        GlassCard(modifier = Modifier.height(110.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(row.habit.icon, style = MaterialTheme.typography.headlineMedium)
                                Text(row.habit.name, style = MaterialTheme.typography.labelMedium)
                                Icon(
                                    if (row.isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (row.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 4.dp).height(20.dp)
                                )
                                Text(
                                    "${row.progressCount}/${row.goalCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAiAssistant() }
                ) {
                    Column {
                        Text("Ask LifeOS AI", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Get a summary of your week, or ask about your notes, tasks and habits.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
