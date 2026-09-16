package com.lifeos.app.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSGradientButton
import com.lifeos.app.ui.components.LifeOSSectionHeader
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.components.ReminderTimePickerDialog
import com.lifeos.app.ui.theme.LifeOSAccentLavender
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId

class TasksViewModel(private val taskRepository: TaskRepository) : ViewModel() {
    private val today = DateTimeUtils.today().toEpochDay()
    val tasksToday: StateFlow<List<TaskEntity>> = taskRepository.observeForDay(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val overdue: StateFlow<List<TaskEntity>> = taskRepository.observeOverdue(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTask(id: String, completed: Boolean) = viewModelScope.launch { taskRepository.setCompleted(id, completed) }
    fun addQuickTask(title: String, reminderTime: LocalTime?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val reminder = reminderTime?.let { DateTimeUtils.today().atTime(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            taskRepository.createTask(title = title.trim(), dueDateEpochDay = today, reminderEpochMillis = reminder)
        }
    }
    fun keepForTomorrow(id: String) = viewModelScope.launch { taskRepository.keepForTomorrow(id, today) }
}

@Composable
fun TasksScreen() {
    val locator = LocalServiceLocator.current
    val viewModel: TasksViewModel = viewModel(factory = LambdaViewModelFactory { TasksViewModel(locator.taskRepository) })
    val today by viewModel.tasksToday.collectAsState()
    val overdue by viewModel.overdue.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var newTaskText by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf<LocalTime?>(null) }
    var filter by remember { mutableStateOf("All") }

    val categories = listOf("All") + today.mapNotNull { it.category }.distinct().take(4)
    val filtered = if (filter == "All") today else today.filter { it.category == filter }
    val pending = today.count { !it.isCompleted }
    val completed = today.count { it.isCompleted }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = LifeOSPrimary) {
                Icon(Icons.Filled.Add, contentDescription = "Add task", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(start = LifeOSSpacing.screenPadding, end = LifeOSSpacing.screenPadding, top = 6.dp, bottom = LifeOSSpacing.fabContentClearance),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            item { LifeOSTopBar("Tasks & Projects", "$pending pending · $completed completed") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        Surface(
                            onClick = { filter = category },
                            color = if (filter == category) LifeOSAccentLavender else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(50),
                            shadowElevation = if (filter == category) 1.dp else 0.dp
                        ) {
                            Text(category, style = MaterialTheme.typography.labelMedium, color = if (filter == category) LifeOSPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp))
                        }
                    }
                }
            }
            if (overdue.isNotEmpty()) {
                item { LifeOSSectionHeader("Overdue", "${overdue.size}") }
                items(overdue, key = { "overdue-${it.id}" }) { task -> TaskCard(task, viewModel) }
            }
            item { LifeOSSectionHeader("Today's Focus", "$pending") }
            if (filtered.isEmpty()) {
                item {
                    LifeOSCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (today.isEmpty()) "Nothing scheduled today" else "No tasks in $filter", style = MaterialTheme.typography.titleMedium)
                            Text("Keep your plan small and intentional.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LifeOSGradientButton("Add a task", Modifier.fillMaxWidth(), onClick = { showAddDialog = true })
                        }
                    }
                }
            } else {
                items(filtered.filter { !it.isCompleted }, key = { it.id }) { task -> TaskCard(task, viewModel) }
            }
            if (completed > 0) {
                item { LifeOSSectionHeader("Completed", "$completed") }
                items(filtered.filter { it.isCompleted }, key = { "done-${it.id}" }) { task -> TaskCard(task, viewModel) }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newTaskText, onValueChange = { newTaskText = it }, placeholder = { Text("What do you need to do?") }, singleLine = true)
                    TextButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Filled.Notifications, contentDescription = null)
                        Text(reminderTime?.let { "Remind at $it" } ?: "Set a reminder")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.addQuickTask(newTaskText, reminderTime); newTaskText = ""; reminderTime = null; showAddDialog = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
    if (showTimePicker) ReminderTimePickerDialog(onDismiss = { showTimePicker = false }, onConfirm = { reminderTime = it; showTimePicker = false })
}

@Composable
private fun TaskCard(task: TaskEntity, viewModel: TasksViewModel) {
    LifeOSCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { viewModel.toggleTask(task.id, it) })
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(task.title, style = MaterialTheme.typography.bodyLarge, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    task.category?.let { Text("#$it", style = MaterialTheme.typography.labelSmall, color = LifeOSPrimary) }
                    dueDateLabel(task)?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (!task.isCompleted) {
                TextButton(onClick = { viewModel.keepForTomorrow(task.id) }) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Move to tomorrow")
                }
            } else {
                Icon(Icons.Filled.Check, contentDescription = "Completed", tint = LifeOSPrimary)
            }
        }
    }
}

private fun dueDateLabel(task: TaskEntity): String? {
    val dueDay = task.dueDateEpochDay ?: return null
    val today = DateTimeUtils.today().toEpochDay()
    val day = when (dueDay) {
        today -> "Today"
        today + 1 -> "Tomorrow"
        else -> DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(dueDay))
    }
    return task.dueTimeMinutes?.let { "$day · ${DateTimeUtils.formatMinutes(it)}" } ?: day
}
