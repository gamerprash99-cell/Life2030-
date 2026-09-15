package com.lifeos.app.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.ui.components.GlassCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InsightsViewModel(
    private val taskRepository: TaskRepository,
    private val expenseRepository: ExpenseRepository,
    private val diaryRepository: DiaryRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _statsText = MutableStateFlow("Loading…")
    val statsText: StateFlow<String> = _statsText

    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    init { loadStats() }

    private fun loadStats() {
        viewModelScope.launch {
            val weekStart = DateTimeUtils.startOfWeekEpochDay()
            val weekEnd = DateTimeUtils.endOfWeekEpochDay()
            val weekStartMillis = weekStart * 86_400_000L
            val weekEndMillis = (weekEnd + 1) * 86_400_000L

            val tasksCompleted = taskRepository.countCompletedBetween(weekStartMillis, weekEndMillis)
            val spend = expenseRepository.getInRange(weekStart, weekEnd).sumOf { it.amount }
            val diaryCount = diaryRepository.countInRange(weekStart, weekEnd)

            _statsText.value = "Tasks completed: $tasksCompleted\nTotal spend: ₹${"%.0f".format(spend)}\nDiary entries: $diaryCount"
        }
    }

    fun generateAiSummary() {
        viewModelScope.launch {
            _busy.value = true
            when (val result = aiRepository.generateReviewSummary("week")) {
                is AiResult.Success -> _aiSummary.value = result.text
                is AiResult.Error -> _aiSummary.value = "Couldn't build your review: ${result.message}"
            }
            _busy.value = false
        }
    }
}

@Composable
fun InsightsScreen() {
    val locator = LocalServiceLocator.current
    val viewModel: InsightsViewModel = viewModel(
        factory = LambdaViewModelFactory {
            InsightsViewModel(locator.taskRepository, locator.expenseRepository, locator.diaryRepository, locator.aiRepository)
        }
    )
    val stats by viewModel.statsText.collectAsState()
    val aiSummary by viewModel.aiSummary.collectAsState()
    val busy by viewModel.busy.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Weekly Review") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column { Text("This week", style = MaterialTheme.typography.titleMedium); Text(stats) }
                }
            }
            item {
                Button(onClick = viewModel::generateAiSummary, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "Generating…" else "Generate AI review")
                }
            }
            aiSummary?.let { summary ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) { Text(summary) }
                }
            }
        }
    }
}
