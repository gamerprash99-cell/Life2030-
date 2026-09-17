package com.lifeos.app.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

class InsightsViewModel(
    private val taskRepository: TaskRepository,
    private val expenseRepository: ExpenseRepository,
    private val diaryRepository: DiaryRepository,
    private val aiRepository: AiRepository
) : ViewModel() {
    private val _stats = MutableStateFlow(InsightStats())
    val stats: StateFlow<InsightStats> = _stats
    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    init { loadStats() }

    private fun loadStats() {
        viewModelScope.launch {
            val start = DateTimeUtils.startOfWeekEpochDay()
            val end = DateTimeUtils.endOfWeekEpochDay()
            val startMillis = start * 86_400_000L
            val endMillis = (end + 1) * 86_400_000L
            val tasksCompleted = taskRepository.countCompletedBetween(startMillis, endMillis)
            val spend = expenseRepository.getInRange(start, end).sumOf { it.amount }
            val diaryCount = diaryRepository.countInRange(start, end)
            val taskList = taskRepository.observeAll().first()
            val weekTasks = taskList.filter { it.dueDateEpochDay in start..end && !it.isDeleted }
            _stats.value = InsightStats(tasksCompleted, weekTasks.size, spend, diaryCount)
        }
    }

    fun generateLocalReview() {
        viewModelScope.launch {
            _busy.value = true
            _aiSummary.value = when (val result = aiRepository.generateReviewSummary("week")) {
                is AiResult.Success -> result.text
                is AiResult.Error -> "Couldn't build your local review: ${result.message}"
            }
            _busy.value = false
        }
    }
}

data class InsightStats(
    val tasksCompleted: Int = 0,
    val tasksTotal: Int = 0,
    val spend: Double = 0.0,
    val diaryEntries: Int = 0
) {
    val completionPercent: Int
        get() = if (tasksTotal == 0) 0 else (tasksCompleted * 100 / tasksTotal).coerceIn(0, 100)
}

@Composable
fun InsightsScreen() {
    val locator = LocalServiceLocator.current
    val viewModel: InsightsViewModel = viewModel(factory = LambdaViewModelFactory { InsightsViewModel(locator.taskRepository, locator.expenseRepository, locator.diaryRepository, locator.aiRepository) })
    val stats by viewModel.stats.collectAsState()
    val summary by viewModel.aiSummary.collectAsState()
    val busy by viewModel.busy.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().fillMaxWidth(),
        contentPadding = PaddingValues(start = LifeOSSpacing.screenPadding, end = LifeOSSpacing.screenPadding, top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
    ) {
        item { LifeOSTopBar("Insights & Flow", "Weekly overview · processed on-device") }
        item { CognitiveVitalityCard(stats) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InsightStatCard("Tasks", "${stats.tasksCompleted}/${stats.tasksTotal}", Icons.AutoMirrored.Filled.TrendingUp, Modifier.weight(1f))
                InsightStatCard("Spend", "₹${"%.0f".format(stats.spend)}", Icons.Filled.Payments, Modifier.weight(1f))
                InsightStatCard("Diary", "${stats.diaryEntries}", Icons.Filled.Book, Modifier.weight(1f))
            }
        }
        item { LifeOSSectionHeader("Pattern Intelligence", "Local engine") }
        item { PatternIntelligenceCard(onGenerate = viewModel::generateLocalReview, busy = busy) }
        summary?.let { report ->
            item { WeeklyNarrativeCard(report) }
        }
        item {
            LifeOSCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = LifeOSAccentLavender, shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.padding(9.dp))
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Private by design", style = MaterialTheme.typography.titleMedium)
                        Text("Analytics stay on this device. No telemetry is required for LifeOS Intelligence.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Cognitive vitality: animated ring + clear typography hierarchy, polished even at 0%. */
@Composable
private fun CognitiveVitalityCard(stats: InsightStats) {
    val percent = stats.completionPercent
    val animatedPercent by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = spring(stiffness = 400f),
        label = "vitalityProgress"
    )
    val hasActivity = stats.tasksTotal > 0
    LifeOSCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Cognitive Vitality", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (hasActivity) "A simple view of this week's activity"
                    else "No tasks this week yet — momentum starts small.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.padding(top = 14.dp))
                Text("$percent%", style = MaterialTheme.typography.displaySmall, color = LifeOSPrimary)
                Text("task completion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedPercent },
                    modifier = Modifier.size(104.dp),
                    strokeWidth = 9.dp,
                    color = LifeOSPrimary,
                    trackColor = LifeOSAccentLavender
                )
                Text("$percent%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = LifeOSPrimary)
            }
        }
    }
}

/** One metric: icon, prominent value, small label — no overlapping text. */
@Composable
private fun InsightStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    LifeOSCard(modifier) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f), shape = CircleShape) {
                Icon(icon, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.padding(7.dp).size(18.dp))
            }
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** Local, offline-only pattern intelligence. No cloud model is ever contacted. */
@Composable
private fun PatternIntelligenceCard(onGenerate: () -> Unit, busy: Boolean) {
    LifeOSCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Filled.Science, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.padding(10.dp))
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Pattern Intelligence", style = MaterialTheme.typography.titleMedium)
                    Text("Local engine · runs on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "Together with your tasks, diary and spending, LifeOS looks for weekly patterns — all inside the app, without uploading anything.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onGenerate, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "Analyzing…" else "Generate local review")
            }
        }
    }
}

/** Weekly narrative extracted from the on-device report, with a dedicated score row. */
@Composable
private fun WeeklyNarrativeCard(narrative: String) {
    val score = narrativeScore(narrative)
    LifeOSCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your Weekly Narrative", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Surface(color = LifeOSAccentLavender, shape = RoundedCornerShape(50)) {
                    Text("Local", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = LifeOSPrimary)
                }
            }
            Text(narrative, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Score", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$score / 100", style = MaterialTheme.typography.headlineSmall, color = LifeOSPrimary)
                }
                Text("Generated by the on-device LifeOS Intelligence Engine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

/** Pulls the productivity score earned from the narrative text; falls back safely to 0. */
internal fun narrativeScore(narrative: String): Int =
    Regex("""(?:score\s+)?(\d+)/100""").find(narrative)?.groupValues?.get(1)?.toIntOrNull() ?: 0