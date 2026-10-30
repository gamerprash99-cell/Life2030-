package com.lifeos.app.ui.habits

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.HabitStatsCalculator
import com.lifeos.app.core.util.completionTarget
import com.lifeos.app.core.util.isCompletionDone
import com.lifeos.app.core.util.toSchedule
import com.lifeos.app.data.db.entities.HabitChecklistCheckEntity
import com.lifeos.app.data.db.entities.HabitChecklistItemEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitOutcome
import com.lifeos.app.data.db.entities.HabitTrackingMode
import com.lifeos.app.data.db.entities.HabitType
import com.lifeos.app.data.db.entities.ReminderRepeatType
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.domain.model.HabitAnalytics
import com.lifeos.app.domain.model.HeatmapCell
import com.lifeos.app.domain.model.HeatmapIntensity
import com.lifeos.app.ui.components.ExactAlarmPermissionHost
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.LifeOSSurface
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.components.ReminderPermissionHost
import com.lifeos.app.ui.components.ReminderRepeatSelector
import com.lifeos.app.ui.components.ReminderTimePickerDialog
import com.lifeos.app.ui.components.rememberExactAlarmPermissionHost
import com.lifeos.app.ui.components.rememberReminderPermissionHost
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HabitDetailUiState(
    val habit: HabitEntity? = null,
    val analytics: HabitAnalytics? = null,
    val heatmap: List<HeatmapCell> = emptyList(),
    val selectedEpochDay: Long = DateTimeUtils.today().toEpochDay(),
    val completion: HabitCompletionEntity? = null,
    val checklistItems: List<HabitChecklistItemEntity> = emptyList(),
    val checklistChecks: List<HabitChecklistCheckEntity> = emptyList(),
    val reminderRepeat: ReminderRepeatType = ReminderRepeatType.DAILY,
    val isLoading: Boolean = true
) {
    val isDone: Boolean
        get() {
            val h = habit ?: return false
            val c = completion ?: return false
            return h.isCompletionDone(c)
        }

    val isSlipped: Boolean get() = completion?.outcome == HabitOutcome.SLIPPED

    val target: Int
        get() {
            val h = habit ?: return 1
            return when {
                completion != null -> h.completionTarget(completion)
                h.trackingMode == HabitTrackingMode.CHECKLIST -> checklistItems.size.coerceAtLeast(1)
                else -> h.goalCount.coerceAtLeast(1)
            }
        }

    val progress: Int get() = completion?.progressCount ?: 0

    val checkedItemIds: Set<String>
        get() = checklistChecks.filter { it.isChecked }.mapTo(mutableSetOf()) { it.itemId }
}

class HabitDetailViewModel(
    private val habitRepository: HabitRepository,
    private val habitId: String
) : ViewModel() {

    private val todayEpochDay = DateTimeUtils.today().toEpochDay()
    private val heatmapStart: Long
    private val heatmapEnd: Long

    init {
        val today = LocalDate.ofEpochDay(todayEpochDay)
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        heatmapStart = monday.minusWeeks(11).toEpochDay()
        heatmapEnd = monday.plusDays(6).toEpochDay()
    }

    private val _state = MutableStateFlow(HabitDetailUiState(selectedEpochDay = todayEpochDay))
    val state: StateFlow<HabitDetailUiState> = _state.asStateFlow()

    private val _celebrations = MutableSharedFlow<HabitCelebration>(extraBufferCapacity = 1)
    val celebrations = _celebrations.asSharedFlow()

    init {
        viewModelScope.launch { refreshAll() }
    }

    fun selectDay(epochDay: Long) {
        val day = epochDay.coerceIn(0L, todayEpochDay)
        _state.update { it.copy(selectedEpochDay = day) }
        viewModelScope.launch { refreshDay(day) }
    }

    fun toggleTick(done: Boolean) {
        val habit = _state.value.habit ?: return
        val day = _state.value.selectedEpochDay
        val wasDone = _state.value.isDone
        viewModelScope.launch {
            habitRepository.setTick(habit.id, day, done)
            if (done && !wasDone) celebrate(habit, day)
            refreshAll()
        }
    }

    fun setCount(value: Int) {
        val habit = _state.value.habit ?: return
        val day = _state.value.selectedEpochDay
        val wasDone = _state.value.isDone
        viewModelScope.launch {
            habitRepository.setCount(habit.id, day, value)
            if (!wasDone && value >= _state.value.target.coerceAtLeast(1)) celebrate(habit, day)
            refreshAll()
        }
    }

    fun increment() = setCount((_state.value.progress + 1).coerceAtMost(_state.value.target + 50))

    fun decrement() = setCount((_state.value.progress - 1).coerceAtLeast(0))

    fun logSlip() {
        val habit = _state.value.habit ?: return
        val day = _state.value.selectedEpochDay
        viewModelScope.launch {
            habitRepository.setSlipped(habit.id, day)
            refreshAll()
        }
    }

    fun toggleChecklistItem(item: HabitChecklistItemEntity, checked: Boolean) {
        val habit = _state.value.habit ?: return
        val day = _state.value.selectedEpochDay
        val wasDone = _state.value.isDone
        viewModelScope.launch {
            habitRepository.setChecklistItemChecked(habit.id, day, item.id, checked)
            val completion = habitRepository.getCompletions(habit.id, day, day).firstOrNull()
            if (!wasDone && completion != null && habit.isCompletionDone(completion)) {
                celebrate(habit, day)
            }
            refreshAll()
        }
    }

    fun saveReminder(time: LocalTime?, repeatType: ReminderRepeatType) {
        val habit = _state.value.habit ?: return
        viewModelScope.launch {
            val epochMillis = time?.let {
                LocalDate.now().atTime(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            habitRepository.setReminder(habit.id, epochMillis, repeatType)
            refreshAll()
        }
    }

    fun archive(onDone: () -> Unit) {
        viewModelScope.launch {
            habitRepository.archive(habitId)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            habitRepository.delete(habitId)
            onDone()
        }
    }

    private suspend fun refreshAll() {
        val habit = habitRepository.getById(habitId) ?: return
        val day = _state.value.selectedEpochDay
        val analytics = habitRepository.computeAnalytics(habit)
        val heatmap = habitRepository.computeHeatmap(habit, heatmapStart, heatmapEnd)
        val completion = habitRepository.getCompletions(habitId, day, day).firstOrNull()
        val items = habitRepository.getChecklistItems(habitId)
        val checks = habitRepository.getChecklistChecks(habitId, day)
        val repeat = habitRepository.getReminderRepeat(habitId) ?: ReminderRepeatType.DAILY
        _state.value = HabitDetailUiState(
            habit = habit,
            analytics = analytics,
            heatmap = heatmap,
            selectedEpochDay = day,
            completion = completion,
            checklistItems = items,
            checklistChecks = checks,
            reminderRepeat = repeat,
            isLoading = false
        )
    }

    private suspend fun refreshDay(day: Long) {
        val completion = habitRepository.getCompletions(habitId, day, day).firstOrNull()
        val items = habitRepository.getChecklistItems(habitId)
        val checks = habitRepository.getChecklistChecks(habitId, day)
        _state.update {
            it.copy(
                selectedEpochDay = day,
                completion = completion,
                checklistItems = items,
                checklistChecks = checks
            )
        }
    }

    private suspend fun celebrate(habit: HabitEntity, epochDay: Long) {
        val completions = habitRepository.getCompletions(habit.id, habit.startDateEpochDay, epochDay)
        val analytics = habitRepository.analyticsFor(habit, completions, LocalDate.ofEpochDay(epochDay))
        _celebrations.emit(
            HabitCelebration(
                habitName = habit.name,
                icon = habitEmoji(habit),
                colorArgb = habit.colorArgb,
                streak = analytics.currentStreak,
                streakUnit = analytics.streakUnit,
                habitType = habit.habitType
            )
        )
    }
}

@Composable
fun HabitDetailScreen(
    habitId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: HabitDetailViewModel = viewModel(
        factory = LambdaViewModelFactory { HabitDetailViewModel(locator.habitRepository, habitId) }
    )
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val habit = state.habit

    var celebration by remember { mutableStateOf<HabitCelebration?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDayDialog by remember { mutableStateOf<Long?>(null) }
    var showCountDialog by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var reminderRepeat by remember { mutableStateOf(ReminderRepeatType.DAILY) }

    val reminderHost: ReminderPermissionHost = rememberReminderPermissionHost()
    val exactAlarmHost: ExactAlarmPermissionHost = rememberExactAlarmPermissionHost()

    LaunchedEffect(viewModel) {
        viewModel.celebrations.collect { celebration = it }
    }

    if (habit == null) {
        if (!state.isLoading) {
            LaunchedEffect(Unit) { onBack() }
        }
        return
    }

    val accent = habitColorOf(habit)
    val schedule = habit.toSchedule()
    val selectedDay = state.selectedEpochDay
    val selectedDate = LocalDate.ofEpochDay(selectedDay)
    val selectedScheduled = HabitStatsCalculator.isScheduled(schedule, selectedDay)
    val selectedIsFuture = selectedDay > DateTimeUtils.today().toEpochDay()

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = LifeOSSpacing.screenPadding,
                    end = LifeOSSpacing.screenPadding,
                    top = 8.dp,
                    bottom = 40.dp
                ),
                verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.lg)
            ) {
                item(key = "topbar") {
                    LifeOSTopBar(
                        title = habit.name,
                        subtitle = habit.category ?: habitTypeLabel(habit),
                        onBack = onBack,
                        actions = {
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit habit")
                            }
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Share progress") },
                                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                        onClick = {
                                            menuOpen = false
                                            shareHabitViaContext(context, habit, state.analytics)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Archive") },
                                        leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                                        onClick = { menuOpen = false; confirmArchive = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        },
                                        onClick = { menuOpen = false; confirmDelete = true }
                                    )
                                }
                            }
                        }
                    )
                }

                item(key = "hero") {
                    HabitDetailHero(habit = habit, analytics = state.analytics, accent = accent)
                }

                item(key = "stats") {
                    HabitStatsGrid(state.analytics)
                }

                item(key = "action") {
                    HabitDayActionCard(
                        state = state,
                        accent = accent,
                        selectedScheduled = selectedScheduled,
                        isFuture = selectedIsFuture,
                        onToggle = viewModel::toggleTick,
                        onSlip = viewModel::logSlip,
                        onIncrement = viewModel::increment,
                        onDecrement = viewModel::decrement,
                        onEditCount = { showCountDialog = true },
                        onToggleItem = viewModel::toggleChecklistItem,
                        onEditHabit = onEdit
                    )
                }

                item(key = "heatmap") {
                    HabitHeatmapCard(
                        cells = state.heatmap,
                        accent = accent,
                        selectedDay = selectedDay,
                        onSelectDay = { day ->
                            viewModel.selectDay(day)
                            showDayDialog = day
                        }
                    )
                }

                item(key = "schedule") {
                    HabitScheduleCard(
                        habit = habit,
                        accent = accent,
                        onEdit = onEdit,
                        onSetReminder = {
                            reminderRepeat = state.reminderRepeat
                            showTimePicker = true
                        },
                        onChangeReminder = {
                            reminderRepeat = state.reminderRepeat
                            showRepeatDialog = true
                        },
                        onClearReminder = {
                            viewModel.saveReminder(null, ReminderRepeatType.DAILY)
                        }
                    )
                }

                item(key = "history-note") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Filled.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            if (habit.showOnTimeline) {
                                "Successful days appear on the Life Timeline. A blank day is never treated as a failure."
                            } else {
                                "Timeline sharing is off for this habit. Change it from Edit."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        celebration?.let { event ->
            HabitCelebrationOverlay(
                celebration = event,
                onDismiss = { celebration = null },
                onShare = { shareHabitCelebration(context, it) }
            )
        }
    }

    if (showRepeatDialog && habit.reminderEpochMillis != null) {
        AlertDialog(
            onDismissRequest = { showRepeatDialog = false },
            title = { Text("Reminder repeat") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Choose how often this habit reminder repeats. The next notification is scheduled from the stored time.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    ReminderRepeatSelector(
                        selected = reminderRepeat,
                        onSelect = { reminderRepeat = it }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val previous = formatReminderTime(habit.reminderEpochMillis)
                    val time = previous?.let {
                        runCatching { LocalTime.parse(it, DateTimeFormatter.ofPattern("h:mm a")) }.getOrNull()
                    }
                    showRepeatDialog = false
                    showTimePicker = true
                }) { Text("Change time") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val previous = formatReminderTime(habit.reminderEpochMillis)
                    val time = previous?.let {
                        runCatching { LocalTime.parse(it, DateTimeFormatter.ofPattern("h:mm a")) }.getOrNull()
                    } ?: LocalTime.of(8, 0)
                    showRepeatDialog = false
                    reminderHost.runProtected {
                        exactAlarmHost.runProtected {
                            viewModel.saveReminder(time, reminderRepeat)
                        }
                    }
                }) { Text("Save repeat") }
            }
        )
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initial = habit.reminderEpochMillis?.let { epoch ->
                java.time.Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalTime()
            } ?: LocalTime.of(8, 0),
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                showTimePicker = false
                reminderHost.runProtected {
                    exactAlarmHost.runProtected {
                        viewModel.saveReminder(time, reminderRepeat)
                    }
                }
            }
        )
    }

    if (showCountDialog) {
        var value by remember { mutableStateOf(state.progress.toString()) }
        AlertDialog(
            onDismissRequest = { showCountDialog = false },
            title = { Text("Set count") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Target: ${state.target}. Counts can never go below zero.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = { raw -> value = raw.filter { it.isDigit() }.take(4) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Count") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCountDialog = false
                    viewModel.setCount(value.toIntOrNull() ?: 0)
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showCountDialog = false }) { Text("Cancel") }
            }
        )
    }

    showDayDialog?.let { day ->
        val dayIsFuture = day > DateTimeUtils.today().toEpochDay()
        val dayCompletion = if (day == selectedDay) state.completion else null
        val dayDone = dayCompletion != null && habit.isCompletionDone(dayCompletion)
        val dayScheduled = HabitStatsCalculator.isScheduled(schedule, day)
        val dayTarget = habit.completionTarget(dayCompletion)
        AlertDialog(
            onDismissRequest = { showDayDialog = null },
            title = { Text(fullDayLabel(day)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when {
                            dayIsFuture -> "Future day"
                            !dayScheduled -> "Not scheduled on this day"
                            dayDone && habit.habitType == HabitType.QUIT -> "Stayed on track"
                            dayDone -> "Completed"
                            dayCompletion?.outcome == HabitOutcome.SLIPPED -> "Slip recorded"
                            else -> "Not logged"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (dayScheduled && !dayIsFuture) {
                        Text(
                            when (habit.trackingMode) {
                                HabitTrackingMode.TICK -> if (dayDone) "One-tap habit · done" else "One-tap habit · open"
                                HabitTrackingMode.COUNT -> "${dayCompletion?.progressCount ?: 0} of $dayTarget"
                                HabitTrackingMode.CHECKLIST -> "${dayCompletion?.progressCount ?: 0} of $dayTarget items checked"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (dayIsFuture || !dayScheduled) {
                    TextButton(onClick = { showDayDialog = null }) { Text("Close") }
                } else {
                    when (habit.trackingMode) {
                        HabitTrackingMode.TICK -> TextButton(onClick = {
                            viewModel.selectDay(day)
                            showDayDialog = null
                            if (dayDone) viewModel.toggleTick(false) else viewModel.toggleTick(true)
                        }) { Text(if (dayDone) "Undo" else "Mark done") }
                        HabitTrackingMode.COUNT -> TextButton(onClick = {
                            viewModel.selectDay(day)
                            showDayDialog = null
                        }) { Text("Edit count") }
                        HabitTrackingMode.CHECKLIST -> TextButton(onClick = {
                            viewModel.selectDay(day)
                            showDayDialog = null
                        }) { Text("Open checklist") }
                    }
                }
            },
            dismissButton = {
                if (!dayIsFuture && dayScheduled && habit.habitType == HabitType.QUIT && !dayDone) {
                    TextButton(onClick = {
                        viewModel.selectDay(day)
                        showDayDialog = null
                        viewModel.logSlip()
                    }) { Text("Log slip") }
                } else {
                    TextButton(onClick = { showDayDialog = null }) { Text("Cancel") }
                }
            }
        )
    }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("Archive ${habit.name}?") },
            text = { Text("Every completion and checklist row is kept. You can restore it from the archived list.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmArchive = false
                    viewModel.archive(onBack)
                }) { Text("Archive") }
            },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancel") } }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${habit.name}?") },
            text = { Text("This permanently removes the habit, all completions and every checklist answer. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HabitDetailHero(habit: HabitEntity, analytics: HabitAnalytics?, accent: Color) {
    LifeOSSurface(
        Modifier.fillMaxWidth(),
        tint = accent.copy(alpha = 0.085f),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(60.dp).clip(RoundedCornerShape(19.dp)).background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(habitEmoji(habit), fontSize = 30.sp)
                }
                Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        habit.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOfNotNull(
                            habit.category,
                            habitTypeLabel(habit),
                            habitScheduleLabel(habit)
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!habit.description.isNullOrBlank()) {
                Text(
                    habit.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(LifeOSRadius.pill), color = accent) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = onHabitColor(accent),
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            "${analytics?.currentStreak ?: 0} ${analytics?.streakUnit ?: "day"} streak",
                            style = MaterialTheme.typography.labelMedium,
                            color = onHabitColor(accent),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    "${analytics?.totalCompletionsAllTime ?: 0} successful days all time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HabitStatsGrid(analytics: HabitAnalytics?) {
    if (analytics == null) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Current streak", "${analytics.currentStreak}", analytics.streakUnit + "s", Modifier.weight(1f))
            StatTile("Longest streak", "${analytics.longestStreak}", analytics.streakUnit + "s", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("This week", "${analytics.completionPercentThisWeek}%", "${analytics.completionsThisWeek} of ${analytics.totalDaysThisWeek}", Modifier.weight(1f))
            StatTile("This month", "${analytics.completionPercentThisMonth}%", "${analytics.completionsThisMonth} of ${analytics.totalDaysThisMonth}", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, support: String, modifier: Modifier = Modifier) {
    LifeOSSurface(modifier, contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(
                support,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HabitDayActionCard(
    state: HabitDetailUiState,
    accent: Color,
    selectedScheduled: Boolean,
    isFuture: Boolean,
    onToggle: (Boolean) -> Unit,
    onSlip: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEditCount: () -> Unit,
    onToggleItem: (HabitChecklistItemEntity, Boolean) -> Unit,
    onEditHabit: () -> Unit
) {
    val habit = state.habit ?: return
    val dayLabel = relativeDayLabel(state.selectedEpochDay, DateTimeUtils.today().toEpochDay())
    LifeOSSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(dayLabel, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (isFuture) "Future days cannot be logged" else "Logging for this specific day",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.isDone) {
                    Surface(shape = RoundedCornerShape(LifeOSRadius.pill), color = accent.copy(alpha = 0.14f)) {
                        Text(
                            "Done",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (state.isSlipped) {
                    Surface(shape = RoundedCornerShape(LifeOSRadius.pill), color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)) {
                        Text(
                            "Slipped",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            when {
                isFuture -> Text(
                    "Choose today or a past date to record progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                !selectedScheduled -> Text(
                    "This habit is not scheduled on this date, so it cannot be marked as missed or complete.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> when (habit.trackingMode) {
                    HabitTrackingMode.TICK -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (habit.habitType == HabitType.QUIT) {
                            Text(
                                "Record exactly what happened. A blank day is never silently counted as a success.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LifeOSPrimaryButton(
                                    text = if (state.isDone) "Undo" else "Stayed on track",
                                    onClick = { onToggle(!state.isDone) },
                                    modifier = Modifier.weight(1f)
                                )
                                if (!state.isDone) {
                                    TextButton(onClick = onSlip) {
                                        Text("I slipped", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            if (state.isSlipped) {
                                Text(
                                    "A slip is recorded for this date. You can still mark the day as stayed on track if it was logged by mistake.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LifeOSPrimaryButton(
                                text = if (state.isDone) "Undo completion" else "Mark done",
                                onClick = { onToggle(!state.isDone) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    HabitTrackingMode.COUNT -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${state.progress} / ${state.target}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = accent
                            )
                            Spacer(Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StepperButton(
                                    icon = Icons.Filled.Remove,
                                    description = "Decrease count",
                                    enabled = state.progress > 0,
                                    onClick = onDecrement
                                )
                                StepperButton(
                                    icon = Icons.Filled.Add,
                                    description = "Increase count",
                                    enabled = true,
                                    onClick = onIncrement
                                )
                            }
                        }
                        HabitDetailProgress(
                            fraction = state.progress.toFloat() / state.target.coerceAtLeast(1),
                            accent = accent
                        )
                        TextButton(onClick = onEditCount) { Text("Enter a value directly") }
                    }
                    HabitTrackingMode.CHECKLIST -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.checklistItems.isEmpty()) {
                            Text(
                                "No checklist items yet. Add a few reusable items to make this habit checkable.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = onEditHabit) { Text("Add checklist items") }
                        } else {
                            val checkedCount = state.checklistItems.count { it.id in state.checkedItemIds }
                            Text(
                                "$checkedCount of ${state.checklistItems.size} checked. Complete only when every item is checked.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HabitDetailProgress(
                                fraction = checkedCount.toFloat() / state.checklistItems.size.coerceAtLeast(1),
                                accent = accent
                            )
                            state.checklistItems.forEach { item ->
                                val checked = item.id in state.checkedItemIds
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onToggleItem(item, !checked) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = { onToggleItem(item, it) })
                                    Text(
                                        item.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (checked) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Icon(
            icon,
            contentDescription = description,
            modifier = Modifier.padding(10.dp).size(20.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun HabitDetailProgress(fraction: Float, accent: Color) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "detailProgress")
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
    }
}

@Composable
private fun HabitHeatmapCard(
    cells: List<HeatmapCell>,
    accent: Color,
    selectedDay: Long,
    onSelectDay: (Long) -> Unit
) {
    val weeks = cells.chunked(7)
    val neutral = MaterialTheme.colorScheme.surfaceVariant
    val weekdayLabels = listOf("Mon", "", "Wed", "", "Fri", "", "Sun")
    LifeOSSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Last 12 weeks", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (cells.isEmpty()) "No history yet" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HeatmapLegend(accent = accent)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    weekdayLabels.forEach { label ->
                        Box(Modifier.height(12.dp).width(22.dp), contentAlignment = Alignment.CenterStart) {
                            if (label.isNotEmpty()) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        week.forEach { cell ->
                            val isSelected = cell.epochDay == selectedDay
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(habitHeatColor(cell.intensity, accent, neutral))
                                    .clickable { onSelectDay(cell.epochDay) }
                                    .semantics {
                                        contentDescription = heatmapDescription(cell)
                                    }
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(1.5.dp, accent, RoundedCornerShape(3.dp))
                                        } else Modifier
                                    )
                            )
                        }
                    }
                }
            }
            Text(
                "Tap any day to inspect or backfill it. Future days stay blank.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun heatmapDescription(cell: HeatmapCell): String {
    val date = LocalDate.ofEpochDay(cell.epochDay).format(DateTimeFormatter.ofPattern("d MMMM"))
    val status = when (cell.intensity) {
        HeatmapIntensity.NO_DATA -> "no data"
        HeatmapIntensity.MISSED -> "missed"
        HeatmapIntensity.PARTIAL -> "${cell.progressCount} of ${cell.goalCount}"
        HeatmapIntensity.COMPLETED -> "completed"
        HeatmapIntensity.EXCEPTIONAL -> "extra progress"
    }
    return "$date: $status"
}

@Composable
private fun HeatmapLegend(accent: Color) {
    val neutral = MaterialTheme.colorScheme.surfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(
            HeatmapIntensity.MISSED,
            HeatmapIntensity.PARTIAL,
            HeatmapIntensity.COMPLETED,
            HeatmapIntensity.EXCEPTIONAL
        ).forEach { intensity ->
            Box(
                Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(habitHeatColor(intensity, accent, neutral))
            )
        }
        Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HabitScheduleCard(
    habit: HabitEntity,
    accent: Color,
    onEdit: () -> Unit,
    onSetReminder: () -> Unit,
    onChangeReminder: () -> Unit,
    onClearReminder: () -> Unit
) {
    val reminder = formatReminderTime(habit.reminderEpochMillis)
    LifeOSSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Schedule & reminder", style = MaterialTheme.typography.titleMedium)
            ScheduleRow(Icons.Filled.Repeat, "Frequency", habitScheduleLabel(habit))
            ScheduleRow(
                Icons.Filled.Check,
                "Tracking",
                habitTrackingLabel(habit)
            )
            ScheduleRow(
                Icons.Filled.Notifications,
                "Reminder",
                reminder?.let { "At $it" } ?: "No reminder"
            )
            if (reminder == null) {
                TextButton(onClick = onSetReminder) { Text("Set a reminder", color = accent) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onChangeReminder) { Text("Change", color = accent) }
                    TextButton(onClick = onClearReminder) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            TextButton(onClick = onEdit) { Text("Edit habit, schedule or checklist", color = accent) }
        }
    }
}

@Composable
private fun ScheduleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(74.dp)
        )
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}

private fun shareHabitViaContext(context: android.content.Context, habit: HabitEntity, analytics: HabitAnalytics?) {
    val streak = analytics?.currentStreak ?: 0
    val text = buildString {
        append("${habitEmoji(habit)} ${habit.name}")
        if (streak > 0) append(" — $streak ${analytics?.streakUnit ?: "day"} streak")
        append("\nTracked privately on LifeOS.")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share habit")) }
}
