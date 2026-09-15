package com.lifeos.app.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.lifeos.app.ui.components.GlassCard
import com.lifeos.app.ui.components.ReminderTimePickerDialog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId

class TasksViewModel(private val taskRepository: TaskRepository) : ViewModel() {
    private val today = DateTimeUtils.today().toEpochDay()

    val tasksToday: StateFlow<List<TaskEntity>> = taskRepository.observeForDay(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdue: StateFlow<List<TaskEntity>> = taskRepository.observeOverdue(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTask(id: String, completed: Boolean) = viewModelScope.launch { taskRepository.setCompleted(id, completed) }

    fun addQuickTask(title: String, reminderTime: LocalTime?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val reminderMillis = reminderTime?.let {
                DateTimeUtils.epochDayToLocalDate(today).atTime(it)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            taskRepository.createTask(title = title, dueDateEpochDay = today, reminderEpochMillis = reminderMillis)
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tasks") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, contentDescription = "Add task") }
        }
    ) { padding ->
        val todayIncomplete = today.filter { !it.isCompleted }
        val todayCompleted = today.filter { it.isCompleted }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp,
                bottom = com.lifeos.app.ui.theme.LifeOSSpacing.fabContentClearance
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (overdue.isNotEmpty()) {
                item { SectionHeader("Overdue", color = MaterialTheme.colorScheme.error) }
                items(overdue, key = { "overdue-${it.id}" }) { task ->
                    TaskRow(task, onToggle = { viewModel.toggleTask(task.id, it) }, onKeepForTomorrow = { viewModel.keepForTomorrow(task.id) })
                }
            }

            item { SectionHeader("Today") }
            if (todayIncomplete.isEmpty() && todayCompleted.isEmpty()) {
                item { Text("No tasks for today. Tap + to add one.", style = MaterialTheme.typography.bodySmall) }
            } else if (todayIncomplete.isEmpty()) {
                item { Text("All done for today 🎉", style = MaterialTheme.typography.bodySmall) }
            }
            items(todayIncomplete, key = { it.id }) { task ->
                TaskRow(task, onToggle = { viewModel.toggleTask(task.id, it) }, onKeepForTomorrow = { viewModel.keepForTomorrow(task.id) })
            }

            if (todayCompleted.isNotEmpty()) {
                item { SectionHeader("Completed") }
                items(todayCompleted, key = { it.id }) { task ->
                    TaskRow(task, onToggle = { viewModel.toggleTask(task.id, it) }, onKeepForTomorrow = { viewModel.keepForTomorrow(task.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New task") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(value = newTaskText, onValueChange = { newTaskText = it }, placeholder = { Text("What do you need to do?") })
                    TextButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(reminderTime?.let { "Remind at $it" } ?: "Set a reminder (optional)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addQuickTask(newTaskText, reminderTime)
                    newTaskText = ""
                    reminderTime = null
                    showAddDialog = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { time -> reminderTime = time; showTimePicker = false }
        )
    }
}

@Composable
private fun SectionHeader(text: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = color,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

/**
 * A task's due-date label relative to today, e.g. "Today · 5:00 PM",
 * "Tomorrow", or a full date for anything further out. Built entirely from
 * existing TaskEntity fields (dueDateEpochDay/dueTimeMinutes) — no new data.
 */
@Composable
private fun dueDateLabel(task: com.lifeos.app.data.db.entities.TaskEntity): String? {
    val dueDay = task.dueDateEpochDay ?: return null
    val todayEpochDay = DateTimeUtils.today().toEpochDay()
    val dayLabel = when (dueDay) {
        todayEpochDay -> "Today"
        todayEpochDay + 1 -> "Tomorrow"
        else -> DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(dueDay))
    }
    val timeLabel = task.dueTimeMinutes?.let { DateTimeUtils.formatMinutes(it) }
    return if (timeLabel != null) "$dayLabel · $timeLabel" else dayLabel
}

@Composable
private fun TaskRow(task: TaskEntity, onToggle: (Boolean) -> Unit, onKeepForTomorrow: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!task.isCompleted) }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                // Checkbox keeps its own click so tapping it doesn't also trigger the card's ripple twice —
                // Compose lets a child clickable consume its own tap independently of the parent's.
                Checkbox(checked = task.isCompleted, onCheckedChange = onToggle)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    dueDateLabel(task)?.let { label ->
                        Text(
                            label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    task.description?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            // "Move to tomorrow" is an ACTION button, not a status — kept visually
            // separate (smaller, muted, with an icon) so it can never be mistaken
            // for the task's actual due date shown above.
            if (!task.isCompleted) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onKeepForTomorrow) {
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Move to tomorrow", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
