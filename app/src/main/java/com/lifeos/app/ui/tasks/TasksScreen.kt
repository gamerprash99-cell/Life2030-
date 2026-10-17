package com.lifeos.app.ui.tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.RepeatRule
import com.lifeos.app.data.db.entities.ReminderRepeatType
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.data.db.entities.TaskPriority
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.ui.components.ExactAlarmPermissionHost
import com.lifeos.app.ui.components.HeaderActionCircle
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSGradientButton
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.components.LiquidGlassSurface
import com.lifeos.app.ui.components.ReminderPermissionHost
import com.lifeos.app.ui.components.ReminderRepeatSelector
import com.lifeos.app.ui.components.ReminderTimePickerDialog
import com.lifeos.app.ui.components.rememberExactAlarmPermissionHost
import com.lifeos.app.ui.components.rememberReminderPermissionHost
import com.lifeos.app.ui.theme.FigmaAccent
import com.lifeos.app.ui.theme.FigmaAccentStroke
import com.lifeos.app.ui.theme.FigmaCard
import com.lifeos.app.ui.theme.FigmaOnAccent
import com.lifeos.app.ui.theme.FigmaStroke
import com.lifeos.app.ui.theme.FigmaText
import com.lifeos.app.ui.theme.FigmaTertiary
import com.lifeos.app.ui.theme.FigmaTrack
import com.lifeos.app.ui.theme.FigmaWash
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant
import kotlin.math.roundToInt

class TasksViewModel(private val taskRepository: TaskRepository) : ViewModel() {
    private val today = DateTimeUtils.today().toEpochDay()
    val tasksToday: StateFlow<List<TaskEntity>> = taskRepository.observeForDay(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val overdue: StateFlow<List<TaskEntity>> = taskRepository.observeOverdue(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Unfinished work due after today. Read only while the "Upcoming" filter is
     * actually selected, so the query's subscription is not paid for on every
     * visit to a screen most people use on today's plan.
     */
    val upcoming: StateFlow<List<TaskEntity>> = taskRepository.observeUpcoming(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleTask(id: String, completed: Boolean) = viewModelScope.launch { taskRepository.setCompleted(id, completed) }

    fun addTask(
        title: String,
        description: String?,
        category: String?,
        priority: TaskPriority,
        repeatRule: RepeatRule,
        reminderTime: LocalTime?,
        reminderRepeatType: ReminderRepeatType
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
                reminderRepeatType = reminderRepeatType,
                repeatRule = repeatRule
            )
        }
    }

    fun keepForTomorrow(id: String) = viewModelScope.launch { taskRepository.keepForTomorrow(id, today) }

    /** Updates (or clears) a task's reminder time/cadence via the shared reminder pipeline. */
    fun updateReminder(id: String, reminderEpochMillis: Long?, repeatType: ReminderRepeatType) = viewModelScope.launch {
        taskRepository.setReminder(id, reminderEpochMillis, repeatType)
    }

    /** The task's current reminder repeat cadence (for the edit dialog's initial state). */
    fun reminderRepeatFor(taskId: String, onResult: (ReminderRepeatType) -> Unit) = viewModelScope.launch {
        onResult(taskRepository.getReminderFor(taskId)?.repeatType ?: ReminderRepeatType.ONCE)
    }
}

/** The three scopes the task list can be viewed through. */
private enum class TaskScope(val label: String) { ALL("All"), TODAY("Today"), UPCOMING("Upcoming") }

/** The wording the Figma scope tags use, which reads better than the bare label. */
private val TaskScope.tagLabel: String
    get() = when (this) {
        TaskScope.TODAY -> "Today"
        TaskScope.UPCOMING -> "Upcoming"
        TaskScope.ALL -> "All tasks"
    }

@Composable
fun TasksScreen() {
    val locator = LocalServiceLocator.current
    val viewModel: TasksViewModel = viewModel(factory = LambdaViewModelFactory { TasksViewModel(locator.taskRepository) })
    val today by viewModel.tasksToday.collectAsState()
    val overdue by viewModel.overdue.collectAsState()
    // TODAY is the landing scope: it is the list the screen was built around, and
    // keeping it the default means the view a user first sees does not change when
    // `All` starts carrying the future as well.
    var scope by rememberSaveable { mutableStateOf(TaskScope.TODAY) }
    // Read only when a scope that shows it is on screen — see TasksViewModel.upcoming.
    val upcoming by remember(scope) {
        if (scope == TaskScope.ALL || scope == TaskScope.UPCOMING) viewModel.upcoming else flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var editReminderTask by remember { mutableStateOf<TaskEntity?>(null) }
    var editRepeatType by remember { mutableStateOf(ReminderRepeatType.ONCE) }
    val coroutineScope = rememberCoroutineScope()

    // `All` was identical to `Today` — both read the same `today` list — so the
    // chip looked like a filter and did nothing. `All` now also carries everything
    // still ahead; overdue work keeps its own section above the list instead of
    // being listed twice, exactly as it already was under `Today`.
    val inScope = when (scope) {
        TaskScope.TODAY -> today
        TaskScope.ALL -> today + upcoming
        TaskScope.UPCOMING -> upcoming
    }
    val categories = remember(inScope) {
        inScope.mapNotNull { it.category }.distinct().sorted()
    }
    // A category chip for a category that no longer exists in the current scope
    // would silently show nothing, so an out-of-scope selection falls back to All.
    val activeCategory = category?.takeIf { it in categories }
    val filtered = activeCategory?.let { c -> inScope.filter { it.category == c } } ?: inScope
    val showOverdue = overdue.isNotEmpty() && scope != TaskScope.UPCOMING
    // The header counts what is actually on screen — the list plus the overdue
    // section when it is shown. De-duplicated because a task due today with a
    // time already past is reported by both the day query and the overdue query.
    val visible = if (showOverdue) (inScope + overdue).distinctBy { it.id } else inScope
    val pending = visible.count { !it.isCompleted }
    val completed = visible.count { it.isCompleted }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        // The parent LifeOSNavHost Scaffold has already inset this destination
        // for the status bar, the bottom navigation and the system navigation
        // area. `Modifier.padding` does not *consume* insets, so a nested
        // Scaffold asking for `systemBars` again added the status bar and the
        // navigation bar a second time — a blank band above the header and below
        // the last card, and a FAB pushed down under the bottom navigation.
        // Zeroing the nested content insets leaves the destination's own real
        // safe area exactly as the NavHost computed it.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(
                start = LifeOSSpacing.screenPadding,
                end = LifeOSSpacing.screenPadding,
                top = 0.dp,
                bottom = LifeOSSpacing.screenPadding
            ),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            item { TasksHeader(onAddTask = { showAddDialog = true }) }
            if (pending + completed > 0) {
                item { TasksProgressHero(pending = pending, completed = completed) }
            }
            item {
                TaskScopeRow(
                    selected = scope,
                    counts = mapOf(
                        TaskScope.TODAY to today.count { !it.isCompleted },
                        TaskScope.UPCOMING to upcoming.count { !it.isCompleted },
                        TaskScope.ALL to (today + upcoming + overdue).distinctBy { it.id }.count { !it.isCompleted }
                    ),
                    onSelect = { scope = it }
                )
            }
            // The category chips only earn their row once there is more than one
            // real category to switch between; with none, they duplicated the
            // scope row and made a single "All" chip look like a broken filter.
            if (categories.size > 1) {
                item {
                    CategoryFilterRow(
                        categories = categories,
                        selected = activeCategory,
                        onSelect = { category = it }
                    )
                }
            }
            if (showOverdue) {
                item { TasksSectionHeader(title = "Overdue", count = overdue.size) }
                items(overdue, key = { "overdue-${it.id}" }) { task ->
                    TaskCard(
                        task = task,
                        onToggle = { viewModel.toggleTask(task.id, it) },
                        onKeepForTomorrow = { viewModel.keepForTomorrow(task.id) },
                        onEditReminder = {
                            coroutineScope.launch {
                                viewModel.reminderRepeatFor(task.id) { editRepeatType = it }
                                editReminderTask = task
                            }
                        }
                    )
                }
            }
            item {
                TasksSectionHeader(
                    title = if (activeCategory != null) activeCategory else scope.focusTitle(),
                    count = filtered.count { !it.isCompleted }
                )
            }
            val pendingTasks = filtered.filter { !it.isCompleted }
            if (pendingTasks.isEmpty()) {
                item {
                    EmptyFocusCard(
                        scope = scope,
                        currentFilter = activeCategory,
                        onAddTask = { showAddDialog = true }
                    )
                }
            } else {
                items(pendingTasks, key = { "${scope.name}-${it.id}" }) { task ->
                    TaskCard(
                        task = task,
                        onToggle = { viewModel.toggleTask(task.id, it) },
                        onKeepForTomorrow = { viewModel.keepForTomorrow(task.id) },
                        onEditReminder = {
                            coroutineScope.launch {
                                viewModel.reminderRepeatFor(task.id) { editRepeatType = it }
                                editReminderTask = task
                            }
                        }
                    )
                }
            }
            if (scope != TaskScope.UPCOMING && completed > 0) {
                item { TasksSectionHeader(title = "Completed", count = completed) }
                items(filtered.filter { t -> t.isCompleted }, key = { t -> "done-${t.id}" }) { task ->
                    TaskCard(
                        task = task,
                        onToggle = { viewModel.toggleTask(task.id, it) },
                        onKeepForTomorrow = {},
                        onEditReminder = {
                            coroutineScope.launch {
                                viewModel.reminderRepeatFor(task.id) { editRepeatType = it }
                                editReminderTask = task
                            }
                        }
                    )
                }
            }
            item {
                LifeOSPrimaryButton(
                    text = "Add a task",
                    icon = Icons.Filled.Add,
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showAddDialog) {
        NewTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, description, category, priority, repeatRule, reminderTime, reminderRepeatType ->
                viewModel.addTask(
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    repeatRule = repeatRule,
                    reminderTime = reminderTime,
                    reminderRepeatType = reminderRepeatType
                )
                showAddDialog = false
            }
        )
    }

    editReminderTask?.let { task ->
        TaskReminderEditorDialog(
            task = task,
            initialRepeatType = editRepeatType,
            onDismiss = { editReminderTask = null },
            onSave = { millis, repeatType ->
                viewModel.updateReminder(task.id, millis, repeatType)
                editReminderTask = null
            }
        )
    }
}

@Composable
private fun TasksHeader(onAddTask: () -> Unit) {
    val today = remember { DateTimeUtils.today() }
    LifeOSTopBar(
        title = "Make room for focus",
        subtitle = "Tasks / ${DateTimeUtils.formatDayOfWeek(today)}, ${DateTimeUtils.formatFullDate(today)}",
        actions = {
            // The Figma frame places a 42×42 action circle here. It carries the
            // add affordance because Tasks has no calendar destination to open;
            // the bottom "Add a task" primary is the second, equally working entry.
            HeaderActionCircle(Icons.Filled.Add, "Add a task", onClick = onAddTask)
        }
    )
}

/**
 * The one thing that leads Tasks: the Figma "Today's focus" card. `N done. M to
 * go.` states the visible list's progress, and the row of segments below is that
 * same ratio drawn one segment per visible task — so the card's words and its
 * bar are the same two numbers the list is built from and can never disagree.
 */
@Composable
private fun TasksProgressHero(pending: Int, completed: Int) {
    val total = pending + completed
    val fraction = if (total > 0) completed.toFloat() / total else 0f
    val progress by animateFloatAsState(fraction, tween(600), label = "tasksProgress")
    // One segment per task, capped so a long list stays a readable meter. Above
    // the cap the segments track the exact fraction instead of the raw count.
    val segments = total.coerceIn(1, 10)
    val filled = if (total <= 10) completed else (fraction * segments).roundToInt()
    LifeOSCard(Modifier.fillMaxWidth(), tint = FigmaCard) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "TODAY'S FOCUS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        ),
                        color = FigmaTertiary
                    )
                    Text(
                        when {
                            pending == 0 && completed > 0 -> "Every task cleared."
                            completed == 0 -> "$pending to go today."
                            else -> "$completed done. $pending to go."
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                        color = FigmaText
                    )
                }
                Text(
                    "${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
                    color = FigmaAccent
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(segments) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(LifeOSRadius.pill))
                            .background(if (index < filled) FigmaAccent else FigmaTrack)
                    )
                }
            }
        }
    }
}

/**
 * Scope tags, restyled to the Figma pills: the selected scope is a mint-tinted
 * plate with mint ink, the others sit on the faint wash with tertiary ink. A
 * scope that has work carries its live open count, read from the same lists the
 * screen renders, so a tag's number is never decorative.
 */
@Composable
private fun TaskScopeRow(
    selected: TaskScope,
    counts: Map<TaskScope, Int>,
    onSelect: (TaskScope) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskScope.entries.forEach { scope ->
            val isSelected = scope == selected
            val shape = RoundedCornerShape(LifeOSRadius.pill)
            val count = counts[scope] ?: 0
            val label = scope.tagLabel + if (count > 0) " $count" else ""
            Box(
                Modifier
                    .clip(shape)
                    .background(if (isSelected) FigmaAccent.copy(alpha = 0.16f) else FigmaWash)
                    .border(BorderStroke(1.dp, if (isSelected) FigmaAccentStroke else FigmaStroke), shape)
                    .clickable(role = Role.Button) { onSelect(scope) }
                    .semantics { contentDescription = label; role = Role.Button }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) FigmaAccent else FigmaTertiary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(categories: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All categories") },
                shape = RoundedCornerShape(11.dp)
            )
        }
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
/** The heading for whichever scope is on screen, or for a single category within it. */
/** Reads naturally after "today"/"tomorrow"-style wording, e.g. "No work tasks today". */
private fun TaskScope.adverb(): String = when (this) {
    TaskScope.ALL -> "yet"
    TaskScope.TODAY -> "today"
    TaskScope.UPCOMING -> "coming up"
}

private fun TaskScope.focusTitle(): String = when (this) {
    TaskScope.ALL -> "All Tasks"
    TaskScope.TODAY -> "Today's Focus"
    TaskScope.UPCOMING -> "Upcoming"
}

@Composable
private fun TasksSectionHeader(title: String, count: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            color = FigmaText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$count",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = FigmaAccent
        )
    }
}

@Composable
private fun EmptyFocusCard(scope: TaskScope, currentFilter: String?, onAddTask: () -> Unit) {
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
            // Say which scope produced the emptiness. "Nothing scheduled today"
            // was wrong for an empty Upcoming list and for an empty category.
            Text(
                when {
                    currentFilter != null -> "No $currentFilter tasks ${scope.adverb()}"
                    scope == TaskScope.UPCOMING -> "Nothing coming up"
                    scope == TaskScope.TODAY -> "Nothing scheduled today"
                    else -> "No tasks yet"
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                when {
                    currentFilter != null -> "Try another category, or add one here."
                    scope == TaskScope.UPCOMING -> "Add a task with a future due date and it will show up here."
                    else -> "Keep your plan small and intentional."
                },
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
    onKeepForTomorrow: () -> Unit,
    onEditReminder: () -> Unit
) {
    LifeOSCard(Modifier.fillMaxWidth(), tint = FigmaCard) {
        Row(verticalAlignment = Alignment.Top) {
            // The Figma row leads with a circular check/empty marker rather than
            // a boxed checkbox. The visible 24dp circle keeps a 44dp target.
            Box(
                Modifier
                    .size(44.dp)
                    .clickable(role = Role.Checkbox) { onToggle(!task.isCompleted) }
                    .semantics {
                        contentDescription = if (task.isCompleted) "Mark ${task.title} not done" else "Mark ${task.title} done"
                        role = Role.Checkbox
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (task.isCompleted) FigmaAccent else Color.Transparent)
                        .border(BorderStroke(1.dp, if (task.isCompleted) FigmaAccent else FigmaStroke), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = FigmaOnAccent, modifier = Modifier.size(15.dp))
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isCompleted) FigmaText.copy(alpha = 0.55f) else FigmaText,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (!task.description.isNullOrBlank()) {
                    Text(
                        task.description!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FigmaText.copy(alpha = 0.55f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(Modifier.height(9.dp))
                TaskMetaRow(task, onEditReminder)
            }
            if (!task.isCompleted) {
                IconButton(
                    onClick = onKeepForTomorrow,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Move to tomorrow",
                        tint = FigmaText.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskMetaRow(task: TaskEntity, onEditReminder: () -> Unit) {
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
        task.reminderEpochMillis?.let { millis ->
            // Tappable reminder chip — opens the same full-screen aware
            // reminder editor used by habits (parity requirement).
            Surface(
                onClick = onEditReminder,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        reminderTimeLabel(millis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        dueDateLabel(task)?.let { time ->
            MetaItem(icon = Icons.Filled.Schedule, text = time)
        }
    }
}

@Composable
private fun reminderTimeLabel(epochMillis: Long): String {
    val time = LocalTime.from(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    return "Remind at ${DateTimeUtils.formatMinutes(time.hour * 60 + time.minute)}"
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
private fun TaskReminderEditorDialog(
    task: TaskEntity,
    initialRepeatType: ReminderRepeatType,
    onDismiss: () -> Unit,
    onSave: (Long?, ReminderRepeatType) -> Unit
) {
    var time by remember(task.id) {
        mutableStateOf(task.reminderEpochMillis?.let { LocalTime.from(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())) })
    }
    var repeatType by remember(task.id) { mutableStateOf(initialRepeatType) }
    var showTimePicker by remember { mutableStateOf(false) }
    // Only reached when the user actually arms a timed reminder — never at startup.
    val permissionHost: ReminderPermissionHost = rememberReminderPermissionHost()

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
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Task reminder", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = if (time != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text("Reminder", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    time?.let { "Remind at ${DateTimeUtils.formatMinutes(it.hour * 60 + it.minute)}" } ?: "Set a reminder",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (time != null) {
                                Surface(
                                    onClick = { time = null },
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
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (time != null) {
                        Text("Repeat reminder", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ReminderRepeatSelector(selected = repeatType, onSelect = { repeatType = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDismiss, shape = RoundedCornerShape(16.dp)) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                val millis = time?.let {
                                    DateTimeUtils.today().atTime(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                }
                                val submit = { onSave(millis, repeatType); onDismiss() }
                                // Arming must wait for notification permission; clearing does not.
                                if (millis != null) permissionHost.runProtected(submit) else submit()
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) { Text("Save", modifier = Modifier.padding(horizontal = 6.dp)) }
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initial = time ?: LocalTime.now(),
            onDismiss = { showTimePicker = false },
            onConfirm = {
                time = it
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun NewTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String?, String?, TaskPriority, RepeatRule, LocalTime?, ReminderRepeatType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var repeatRule by remember { mutableStateOf(RepeatRule.NONE) }
    var reminderTime by remember { mutableStateOf<LocalTime?>(null) }
    var reminderRepeatType by remember { mutableStateOf(ReminderRepeatType.ONCE) }
    var showTimePicker by remember { mutableStateOf(false) }
    var priorityMenuOpen by remember { mutableStateOf(false) }
    var repeatMenuOpen by remember { mutableStateOf(false) }
    // Only reached when the user actually sets a timed reminder — never at startup.
    val permissionHost: ReminderPermissionHost = rememberReminderPermissionHost()

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
                        if (reminderTime != null) {
                            Text("Repeat reminder", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ReminderRepeatSelector(selected = reminderRepeatType, onSelect = { reminderRepeatType = it })
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
                                val submit = {
                                    onAdd(title, description, category, priority, repeatRule, reminderTime, reminderRepeatType)
                                }
                                // Reminder creation must wait for notification permission;
                                // a reminder-less task is unaffected.
                                if (reminderTime != null) permissionHost.runProtected(submit) else submit()
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

@Composable
private fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.HIGH -> MaterialTheme.colorScheme.error
    TaskPriority.MEDIUM -> LocalLifeOSSemanticColors.current.warning
    TaskPriority.LOW -> LocalLifeOSSemanticColors.current.success
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