package com.lifeos.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.LifeOSBadge
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSIntelligenceCard
import com.lifeos.app.ui.components.LifeOSSectionHeader
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
    onOpenSearch: () -> Unit = {}
) {
    val locator = LocalServiceLocator.current
    val viewModel: HomeViewModel = viewModel(factory = LambdaViewModelFactory {
        HomeViewModel(locator.getHomeSummaryUseCase, locator.taskRepository, locator.habitRepository)
    })
    val summary by viewModel.summary.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenCapture,
                icon = { Icon(Icons.Filled.Add, contentDescription = "Capture") },
                text = { Text("Capture") },
                containerColor = LifeOSPrimary,
                contentColor = Color.White
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(
                horizontal = LifeOSSpacing.compactPadding,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.cardSpacing)
        ) {
            item {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    Text(summary?.greeting ?: "Welcome", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(3.dp))
                    Text(summary?.dateLabel.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    item { LifeOSBadge("Notes"); }
                    item { LifeOSBadge("Diary", Modifier); }
                    item { LifeOSBadge("Expenses", Modifier); }
                    item { LifeOSBadge("Timeline", Modifier); }
                }
            }

            item {
                LifeOSCard(modifier = Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.animateContentSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Today", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                AnimatedContent(targetState = "${summary?.tasksCompletedToday ?: 0} of ${summary?.tasksTotalToday ?: 0} tasks", label = "taskCount") { value ->
                                    Text(value, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                            LifeOSBadge("Today")
                        }
                        val total = summary?.tasksTotalToday ?: 0
                        val completed = summary?.tasksCompletedToday ?: 0
                        val target = if (total == 0) 0f else completed.toFloat() / total
                        val progress by animateFloatAsState(target.coerceIn(0f, 1f), spring(stiffness = 500f), label = "homeProgress")
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(9.dp),
                            color = LifeOSPrimary,
                            trackColor = Color(0xFFEEE7F7)
                        )
                    }
                }
            }

            item { LifeOSSectionHeader("Tasks", "View all", onOpenTasks) }
            summary?.tasksToday?.take(4)?.let { tasks ->
                items(tasks, key = { it.id }) { task ->
                    LifeOSCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenTasks) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { viewModel.toggleTask(task.id, it) }
                            )
                            Text(task.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }

            item { LifeOSSectionHeader("Habits", "Open habits", onOpenHabits) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(summary?.habitsToday ?: emptyList(), key = { it.habit.id }) { row ->
                        LifeOSCard(modifier = Modifier.size(width = 148.dp, height = 128.dp), onClick = onOpenHabits) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(row.habit.icon, style = MaterialTheme.typography.headlineMedium)
                                Text(row.habit.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                Icon(
                                    if (row.isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = if (row.isDone) "Completed" else "Not completed",
                                    tint = if (row.isDone) LifeOSPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp).size(20.dp)
                                )
                                Text("${row.progressCount}/${row.goalCount}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                }
            }

            item {
                LifeOSIntelligenceCard(onClick = onOpenAiAssistant)
            }
        }
    }
}
