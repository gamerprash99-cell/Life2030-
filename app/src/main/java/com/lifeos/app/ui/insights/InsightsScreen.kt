package com.lifeos.app.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.ai.AiRepository
import com.lifeos.app.core.ai.AiResult
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSSectionHeader
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.theme.LifeOSAccentLavender
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class InsightsViewModel(private val taskRepository: TaskRepository, private val expenseRepository: ExpenseRepository, private val diaryRepository: DiaryRepository, private val aiRepository: AiRepository) : ViewModel() {
    private val _stats = MutableStateFlow(InsightStats())
    val stats: StateFlow<InsightStats> = _stats
    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    init { loadStats() }
    private fun loadStats() { viewModelScope.launch { val start = DateTimeUtils.startOfWeekEpochDay(); val end = DateTimeUtils.endOfWeekEpochDay(); val startMillis = start * 86_400_000L; val endMillis = (end + 1) * 86_400_000L; val tasksCompleted = taskRepository.countCompletedBetween(startMillis, endMillis); val spend = expenseRepository.getInRange(start, end).sumOf { it.amount }; val diaryCount = diaryRepository.countInRange(start, end); val taskList = taskRepository.observeAll().first(); val weekTasks = taskList.filter { it.dueDateEpochDay in start..end && !it.isDeleted }; _stats.value = InsightStats(tasksCompleted, weekTasks.size, spend, diaryCount) } }
    fun generateLocalReview() { viewModelScope.launch { _busy.value = true; _aiSummary.value = when (val result = aiRepository.generateReviewSummary("week")) { is AiResult.Success -> result.text; is AiResult.Error -> "Couldn't build your local review: ${result.message}" }; _busy.value = false } }
}

data class InsightStats(val tasksCompleted: Int = 0, val tasksTotal: Int = 0, val spend: Double = 0.0, val diaryEntries: Int = 0) { val completionPercent: Int get() = if (tasksTotal == 0) 0 else (tasksCompleted * 100 / tasksTotal).coerceIn(0, 100) }

@Composable
fun InsightsScreen() {
    val locator = LocalServiceLocator.current
    val viewModel: InsightsViewModel = viewModel(factory = LambdaViewModelFactory { InsightsViewModel(locator.taskRepository, locator.expenseRepository, locator.diaryRepository, locator.aiRepository) })
    val stats by viewModel.stats.collectAsState(); val summary by viewModel.aiSummary.collectAsState(); val busy by viewModel.busy.collectAsState()
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = LifeOSSpacing.screenPadding, end = LifeOSSpacing.screenPadding, top = 6.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)) {
        item { LifeOSTopBar("Insights & Flow", "Weekly overview · processed on-device") }
        item { LifeOSCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.surface) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Cognitive Vitality", style = MaterialTheme.typography.titleLarge); Text("A simple view of this week's activity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${stats.completionPercent}%", style = MaterialTheme.typography.displaySmall, color = LifeOSPrimary, modifier = Modifier.padding(top = 8.dp)); Text("task completion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; CircularProgressIndicator(progress = { stats.completionPercent / 100f }, modifier = Modifier.height(92.dp), strokeWidth = 9.dp, color = LifeOSPrimary, trackColor = LifeOSAccentLavender) } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { InsightStatCard("Tasks", "${stats.tasksCompleted}/${stats.tasksTotal}", Icons.Filled.TrendingUp, Modifier.weight(1f)); InsightStatCard("Spend", "₹${"%.0f".format(stats.spend)}", Icons.Filled.AutoAwesome, Modifier.weight(1f)); InsightStatCard("Diary", stats.diaryEntries.toString(), Icons.Filled.AutoAwesome, Modifier.weight(1f)) } }
        item { LifeOSSectionHeader("Pattern Intelligence", "Local engine") }
        item { LifeOSCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.surfaceContainerLow) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = LifeOSPrimary); Column(Modifier.weight(1f).padding(start = 12.dp)) { Text("Review your week", style = MaterialTheme.typography.titleMedium); Text("LifeOS analyzes your local tasks, diary and spending without a cloud model.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Button(onClick = viewModel::generateLocalReview, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { Text(if (busy) "Analyzing…" else "Generate local review") } } }
        summary?.let { report -> item { LifeOSCard(Modifier.fillMaxWidth()) { Text("Your weekly narrative", style = MaterialTheme.typography.titleMedium); Text(report, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp)) } } }
        item { LifeOSCard(Modifier.fillMaxWidth()) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Lock, contentDescription = null, tint = LifeOSPrimary); Column(Modifier.padding(start = 12.dp)) { Text("Private by design", style = MaterialTheme.typography.titleMedium); Text("Analytics stay on this device. No telemetry is required for LifeOS Intelligence.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
    }
}

@Composable
private fun InsightStatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) { LifeOSCard(modifier) { Icon(icon, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.height(20.dp)); Text(value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp)); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
