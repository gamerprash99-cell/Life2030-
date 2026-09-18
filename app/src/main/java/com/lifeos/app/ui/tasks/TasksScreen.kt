package com.lifeos.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.RepeatRule
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.data.db.entities.TaskPriority
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSGradientButton
import com.lifeos.app.ui.components.ReminderTimePickerDialog
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

    fun addTask(
        title: String,
        description: String?,
        category: String?,
        priority: TaskPriority,
        repeatRule: RepeatRule,
        reminderTime: LocalTime?
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val reminder = reminderTime?.let { DateTimeUtils.today().atTime(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            taskRepository.createTask(
                title = title.trim(),
                description = description?.trim()?.takeIf { it.isNotBlank() },
                dueDateEpochDay = today,
                dueTimeMinutes = reminderTime?.let { it.hour * 60 + it.minute },
                priority = priority,
                category = category?.trim()?.takeIf { it.isNotBlank() },
                reminderEpochMillis = reminder,
                repeatRule = repeatRule
            )
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
    var filter by remember { mutableStateOf("All") }

    val categories = remember(today) { listOf("All") + today.mapNotNull { it.category }.distinct() }
    val activeFilter = if (categories.any { it == filter }) filter else "All"
    val filtered = if (activeFilter == "All") today else today.filter { it.category == activeFilter }
    val pending = today.count { !it.isCompleted }
    val completed = today.count { it.isCompleted }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(
                start = LifeOSSpacing.screenPadding,
                end = LifeOSSpacing.screenPadding,
                top = 8.dp,
                bottom = LifeOSSpacing.fabContentClearance
            ),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            item { TasksHeader(pending = pending, completed = completed) }
            item { CategoryFilterRow(categories = categories, selected = activeFilter, onSelect = { filter = it }) }
            if (overdue.isNotEmpty()) {
                item { TasksSectionHeader(title = "Overdue", count = overdue.size) }
                items(overdue, key = { "overdue-${it.id}" }) { task ->
                    TaskCard(
                        task = task,
                        onToggle = { viewModel.toggleTask(task.id, it) },
                        onKeepForTomorrow = { viewModel.keepForTomorrow(task.id) }
                    )
                }
            }
            item { TasksSectionHeader(title = "Today's Focus", count = pending) }
            val pendingTasks = filtered.filter { !it.isCompleted }
            if (pendingTasks.isEmpty()) {
                item {
                    EmptyFocusCard(
                        currentFilter = activeFilter.takeIf { it != "All" },
                        onAddTask = { showAddDialog = true }
                    )
                }
            } else {
                items(pendingTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggle = { viewModel.toggleTask(task.id, it) },
                        onKeepForTomorrow = { viewModel.keepForTomorrow(task.id) }
                    )
                }
            }
            if (completed > 0) {
                item { TasksSectionHeader(title = "Completed", count = completed) }
                items(filtered.filter { it.isCompleted }, key = { "done-${it.id}" }) { task ->
                    TaskCard(
                        task = task,
                        onToggle = { viewModel.toggleTask(task.id, it) },
                        onKeepForTomorrow = {}
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        NewTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, description, category, priority, repeatRule, reminderTime ->
                viewModel.addTask(
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    repeatRule = repeatRule,
                    reminderTime = reminderTime
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TasksHeader(pending: Int, completed: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            shadowElevation = 1.dp
        ) {
            Icon(
                Icons.Filled.Spa,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(11.dp).size(26.dp)
            )
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                "Tasks & Projects",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "$pending pending · $completed completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = PaddingValues(horizontal = LifeOSSpacing.screenPadding)
    ) {
        items(categories, key = { it }) { category ->
            val isSelected = category == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(category) },
                label = {
                    Text(category, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.primary,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    selectedTrailingIconColor = MaterialTheme.colorScheme.primary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    selectedBorderColor = Color.Transparent,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 0.dp
                )
            )
        }
    }
}

@Composable
private fun TasksSectionHeader(title: String, count: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(50)
        ) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun EmptyFocusCard(currentFilter: String?, onAddTask: () -> Unit) {
    LifeOSCard(
        modifier = Modifier.fillMaxWidth(),
        tint = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Spa,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                if (currentFilter == null) "Nothing scheduled today" else "No tasks in $currentFilter",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "Keep your plan small and intentional.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            LifeOSGradientButton("Add a task", Modifier.fillMaxWidth(), onClick = onAddTask)
        }
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    onToggle: (Boolean) -> Unit,
    onKeepForTomorrow: () -> Unit
) {
    LifeOSCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            Spacer(Modifier.width(2.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (!task.description.isNullOrBlank()) {
                    Text(
                        task.description!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(Modifier.height(9.dp))
                TaskMetaRow(task)
            }
            if (!task.isCompleted) {
                IconButton(
                    onClick = onKeepForTomorrow,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Move to tomorrow",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskMetaRow(task: TaskEntity) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        task.category?.let { category ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    "#$category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        task.repeatRule?.takeIf { it != RepeatRule.NONE }?.let { rule ->
            MetaItem(icon = Icons.Filled.Repeat, text = repeatLabel(rule))
        }
        dueDateLabel(task)?.let { time ->
            MetaItem(icon = Icons.Filled.Schedule, text = time)
        }
    }
}

@Composable
private fun MetaItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NewTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String?, String?, TaskPriority, RepeatRule, LocalTime?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var repeatRule by remember { mutableStateOf(RepeatRule.NONE) }
    var reminderTime by remember { mutableStateOf<LocalTime?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var priorityMenuOpen by remember { mutableStateOf(false) }
    var repeatMenuOpen by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 560.dp)
                    .heightIn(max = 640.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
                tonalElevation = 1.dp
            ) {
                Column(Modifier.padding(top = 18.dp, bottom = 12.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("New task", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("Add to today's plan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 24.dp, end = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        LavenderTextField(value = title, onValueChange = { title = it }, placeholder = "What do you need to do?")
                        LavenderTextField(value = description, onValueChange = { description = it }, placeholder = "Notes (optional)", maxLines = 3)
                        LavenderTextField(value = category, onValueChange = { category = it }, placeholder = "Category (optional)")

                        SelectField(
                            label = "Priority",
                            value = priorityLabel(priority),
                            expanded = priorityMenuOpen,
                            onToggle = { priorityMenuOpen = !priorityMenuOpen; repeatMenuOpen = false },
                            leadingContent = { PriorityDot(priority) }
                        ) {
                            TaskPriority.entries.forEach { option ->
                                MenuOption(
                                    text = priorityLabel(option),
                                    selected = option == priority,
                                    leading = { PriorityDot(option) },
                                    onClick = { priority = option; priorityMenuOpen = false }
                                )
                            }
                        }

                        SelectField(
                            label = "Repeat",
                            value = repeatLabel(repeatRule),
                            expanded = repeatMenuOpen,
                            onToggle = { repeatMenuOpen = !repeatMenuOpen; priorityMenuOpen = false }
                        ) {
                            RepeatRule.entries.forEach { rule ->
                                MenuOption(
                                    text = repeatLabel(rule),
                                    selected = rule == repeatRule,
                                    onClick = { repeatRule = rule; repeatMenuOpen = false }
                                )
                            }
                        }

                        Surface(
                            onClick = { showTimePicker = true },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint = if (reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text("Reminder", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        reminderTime?.let { "Remind at ${DateTimeUtils.formatMinutes(it.hour * 60 + it.minute)}" } ?: "Set a reminder",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (reminderTime != null) {
                                    Surface(
                                        onClick = { reminderTime = null },
                                        shape = CircleShape,
                                        color = Color.Transparent
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Clear reminder",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(8.dp).size(16.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDismiss, shape = RoundedCornerShape(16.dp)) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                onAdd(title, description, category, priority, repeatRule, reminderTime)
                            },
                            enabled = title.isNotBlank(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Add", modifier = Modifier.padding(horizontal = 6.dp))
                        }
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initial = reminderTime ?: LocalTime.now(),
            onDismiss = { showTimePicker = false },
            onConfirm = {
                reminderTime = it
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun LavenderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge) },
        singleLine = maxLines == 1,
        maxLines = maxLines,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** A tappable dropdown-style field with an anchored [DropdownMenu] rendered by [menu]. */
@Composable
private fun SelectField(
    label: String,
    value: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    leadingContent: @Composable () -> Unit = {},
    menu: @Composable () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        Surface(
            onClick = onToggle,
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                leadingContent()
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onToggle,
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.widthIn(min = 220.dp)
        ) {
            menu()
        }
    }
}

@Composable
private fun MenuOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f) else Color.Transparent)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leading()
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PriorityDot(priority: TaskPriority, size: Dp = 11.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(priorityColor(priority)))
}

private fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.HIGH -> Color(0xFFD94A5B)
    TaskPriority.MEDIUM -> Color(0xFFE39A28)
    TaskPriority.LOW -> Color(0xFF2E9D63)
}

private fun priorityLabel(priority: TaskPriority): String = when (priority) {
    TaskPriority.HIGH -> "High"
    TaskPriority.MEDIUM -> "Medium"
    TaskPriority.LOW -> "Low"
}

private fun repeatLabel(rule: RepeatRule): String = when (rule) {
    RepeatRule.NONE -> "Doesn't repeat"
    RepeatRule.DAILY -> "Repeat daily"
    RepeatRule.WEEKLY -> "Repeat weekly"
    RepeatRule.MONTHLY -> "Repeat monthly"
    RepeatRule.CUSTOM_DAYS -> "Custom days"
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