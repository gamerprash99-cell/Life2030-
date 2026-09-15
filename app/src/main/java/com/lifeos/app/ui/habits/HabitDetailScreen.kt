package com.lifeos.app.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.domain.model.HabitAnalytics
import com.lifeos.app.domain.model.HeatmapCell
import com.lifeos.app.domain.model.HeatmapIntensity
import com.lifeos.app.ui.components.GlassCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HabitDetailViewModel(private val habitRepository: HabitRepository, private val habitId: String) : ViewModel() {
    private val _habit = MutableStateFlow<HabitEntity?>(null)
    val habit: StateFlow<HabitEntity?> = _habit

    private val _analytics = MutableStateFlow<HabitAnalytics?>(null)
    val analytics: StateFlow<HabitAnalytics?> = _analytics

    private val _heatmap = MutableStateFlow<List<HeatmapCell>>(emptyList())
    val heatmap: StateFlow<List<HeatmapCell>> = _heatmap

    fun load() {
        viewModelScope.launch {
            val h = habitRepository.getById(habitId) ?: return@launch
            _habit.value = h
            _analytics.value = habitRepository.computeAnalytics(h)
            val end = DateTimeUtils.today().toEpochDay()
            val start = end - 83 // 12 weeks
            _heatmap.value = habitRepository.computeHeatmap(h, start, end)
        }
    }

    fun logToday() {
        viewModelScope.launch {
            val h = _habit.value ?: return@launch
            val today = DateTimeUtils.today().toEpochDay()
            val current = _heatmap.value.find { it.epochDay == today }?.progressCount ?: 0
            habitRepository.logProgress(habitId, today, (current + 1).coerceAtMost(h.goalCount + 3))
            load()
        }
    }
}

@Composable
fun HabitDetailScreen(habitId: String, onBack: () -> Unit) {
    val locator = LocalServiceLocator.current
    val viewModel: HabitDetailViewModel = viewModel(
        factory = LambdaViewModelFactory { HabitDetailViewModel(locator.habitRepository, habitId) }
    )
    LaunchedEffect(habitId) { viewModel.load() }

    val habit by viewModel.habit.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val heatmap by viewModel.heatmap.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habit?.name ?: "Habit") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            analytics?.let { a ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatColumn("🔥 Streak", "${a.currentStreak} days")
                        StatColumn("🏆 Best", "${a.longestStreak} days")
                        StatColumn("This month", "${a.completionPercentThisMonth}%")
                    }
                }
            }

            Text("Last 12 weeks", style = MaterialTheme.typography.titleMedium)
            LazyVerticalGrid(columns = GridCells.Fixed(12), modifier = Modifier.fillMaxWidth()) {
                items(heatmap) { cell ->
                    HeatmapDot(cell)
                }
            }

            Button(onClick = viewModel::logToday, modifier = Modifier.fillMaxWidth()) {
                Text("Log today's progress")
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HeatmapDot(cell: HeatmapCell) {
    val color = when (cell.intensity) {
        HeatmapIntensity.NO_DATA -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        HeatmapIntensity.MISSED -> MaterialTheme.colorScheme.surfaceVariant
        HeatmapIntensity.PARTIAL -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        HeatmapIntensity.COMPLETED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        HeatmapIntensity.EXCEPTIONAL -> MaterialTheme.colorScheme.primary
    }
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .padding(2.dp)
            .size(14.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
    )
}
