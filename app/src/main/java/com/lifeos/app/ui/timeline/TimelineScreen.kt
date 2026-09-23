package com.lifeos.app.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventBusy
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.usecase.BuildTimelineUseCase
import com.lifeos.app.ui.components.GlassCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimelineViewModel(private val buildTimeline: BuildTimelineUseCase) : ViewModel() {
    private val _items = MutableStateFlow<List<TimelineItem>>(emptyList())
    val items: StateFlow<List<TimelineItem>> = _items
    fun loadFor(epochDay: Long) { viewModelScope.launch { _items.value = buildTimeline(epochDay) } }
}

@Composable
fun TimelineScreen(onBack: () -> Unit = {}) {
    val locator = LocalServiceLocator.current
    val viewModel: TimelineViewModel = viewModel(factory = LambdaViewModelFactory { TimelineViewModel(locator.buildTimelineUseCase) })
    val today = remember { DateTimeUtils.today() }
    var selectedDate by remember { mutableStateOf(today) }
    val items by viewModel.items.collectAsState()
    LaunchedEffect(selectedDate) { viewModel.loadFor(selectedDate.toEpochDay()) }
    androidx.activity.compose.BackHandler(onBack = onBack)
    val canGoForward = selectedDate.isBefore(today)
    Scaffold(topBar = { TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day") }; Text(DateTimeUtils.formatFullDate(selectedDate), style = MaterialTheme.typography.titleMedium); IconButton(enabled = canGoForward, onClick = { if (selectedDate.isBefore(today)) selectedDate = selectedDate.plusDays(1) }) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next day") } } }) }) { padding ->
        if (items.isEmpty()) Column(Modifier.fillMaxWidth().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Filled.EventBusy, contentDescription = null, modifier = Modifier.size(40.dp).padding(bottom = 8.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)); Text("Nothing recorded for this day yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
        else LazyColumn(Modifier.padding(padding).fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(items, key = { it.id }) { item -> TimelineRow(item) } }
    }
}

@Composable
private fun TimelineRow(item: TimelineItem) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.icon, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) { Text(item.title, style = MaterialTheme.typography.bodyLarge); item.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) } }
            Text(DateTimeUtils.formatMinutes(item.timeMinutes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}
