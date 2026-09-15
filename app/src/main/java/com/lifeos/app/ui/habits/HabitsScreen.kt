package com.lifeos.app.ui.habits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.ui.components.GlassCard
import com.lifeos.app.ui.components.ReminderTimePickerDialog
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId

// A small set of ready-to-tap emoji so "New habit" doesn't force manual emoji
// typing — the underlying HabitEntity.icon field is unchanged (still just a
// String); this is purely a faster way to fill the same existing field.
private val PRESET_HABIT_ICONS = listOf("🔥", "💧", "🏃", "📖", "🧘", "😴", "🥗", "✅")

class HabitsViewModel(private val habitRepository: HabitRepository) : ViewModel() {
    val habits: StateFlow<List<HabitEntity>> = habitRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addHabit(name: String, icon: String, reminderTime: LocalTime?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val today = DateTimeUtils.today()
            val reminderMillis = reminderTime?.let {
                today.atTime(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            habitRepository.createHabit(name = name, icon = icon.ifBlank { "✅" }, reminderEpochMillis = reminderMillis)
        }
    }

    fun logToday(habitId: String, currentProgress: Int, goal: Int) {
        viewModelScope.launch {
            val today = DateTimeUtils.today().toEpochDay()
            habitRepository.logProgress(habitId, today, (currentProgress + 1).coerceAtMost(goal + 3))
        }
    }
}

@Composable
fun HabitsScreen(onOpenHabit: (String) -> Unit) {
    val locator = LocalServiceLocator.current
    val viewModel: HabitsViewModel = viewModel(factory = LambdaViewModelFactory { HabitsViewModel(locator.habitRepository) })
    val habits by viewModel.habits.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🔥") }
    var reminderTime by remember { mutableStateOf<LocalTime?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Habits") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, contentDescription = "New habit") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp,
                bottom = LifeOSSpacing.fabContentClearance
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (habits.isEmpty()) {
                item { Text("No habits yet. Tap + to start tracking one.", style = MaterialTheme.typography.bodySmall) }
            }
            items(habits, key = { it.id }) { habit ->
                HabitRow(
                    habit = habit,
                    habitRepository = locator.habitRepository,
                    onOpenDetail = { onOpenHabit(habit.id) },
                    onLogToday = { current -> viewModel.logToday(habit.id, current, habit.goalCount) }
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New habit") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Icon", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PRESET_HABIT_ICONS) { preset ->
                            val selected = preset == icon
                            Text(
                                preset,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier
                                    .clickable { icon = preset }
                                    .padding(6.dp)
                                    .then(
                                        if (selected) Modifier.size(LifeOSSpacing.minTouchTarget) else Modifier
                                    )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = icon,
                        onValueChange = { icon = it },
                        placeholder = { Text("Or type your own emoji") }
                    )
                    OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Habit name") })
                    TextButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(reminderTime?.let { "Remind at $it" } ?: "Set a daily reminder (optional)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addHabit(name, icon, reminderTime)
                    name = ""; reminderTime = null; showAddDialog = false
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

/**
 * One habit's row on the list screen. Shows today's completion state
 * (reusing the same HabitRepository.observeCompletion/logProgress pattern
 * already used on Home) so the completion control is obvious without
 * needing to open Detail — Detail still owns the full streak/heatmap view.
 */
@Composable
private fun HabitRow(
    habit: HabitEntity,
    habitRepository: HabitRepository,
    onOpenDetail: () -> Unit,
    onLogToday: (currentProgress: Int) -> Unit
) {
    val todayEpochDay = remember { DateTimeUtils.today().toEpochDay() }
    val completion by habitRepository.observeCompletion(habit.id, todayEpochDay).collectAsState(initial = null)
    val progress = completion?.progressCount ?: 0
    val isDone = progress >= habit.goalCount

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onOpenDetail() }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${habit.icon}  ${habit.name}", style = MaterialTheme.typography.titleMedium)
                val subtitleParts = listOfNotNull(
                    habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                    habit.category
                )
                Text(
                    subtitleParts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (habit.goalCount > 1) {
                    Text(
                        "$progress/${habit.goalCount} today",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            IconButton(onClick = { onLogToday(progress) }) {
                Icon(
                    if (isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (isDone) "Completed today" else "Mark today's progress",
                    tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
