package com.lifeos.app.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.repository.HabitRepository
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

private val PRESET_HABIT_ICONS = listOf("🔥", "💧", "🏃", "📖", "🧘", "😴", "🥗", "✅")

class HabitsViewModel(private val habitRepository: HabitRepository) : ViewModel() {
    val habits: StateFlow<List<HabitEntity>> = habitRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun addHabit(name: String, icon: String, reminderTime: LocalTime?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val today = DateTimeUtils.today()
            val reminderMillis = reminderTime?.let { today.atTime(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
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
    val completions by locator.habitRepository.observeAllForDay(DateTimeUtils.today().toEpochDay()).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🔥") }
    var reminderTime by remember { mutableStateOf<LocalTime?>(null) }
    val completionByHabit = completions.associateBy { it.habitId }
    val todayDone = habits.count { habit -> (completionByHabit[habit.id]?.progressCount ?: 0) >= habit.goalCount }
    val completionPercent = if (habits.isEmpty()) 0 else (todayDone * 100 / habits.size)
    Scaffold(floatingActionButton = { androidx.compose.material3.FloatingActionButton(onClick = { showAddDialog = true }, containerColor = LifeOSPrimary) { Icon(Icons.Filled.Add, contentDescription = "New habit", tint = Color.White) } }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(start = LifeOSSpacing.screenPadding, end = LifeOSSpacing.screenPadding, top = 6.dp, bottom = LifeOSSpacing.fabContentClearance),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            item { LifeOSTopBar(title = "Habits & Routines", subtitle = "Build a rhythm that feels sustainable") }
            item {
                LifeOSCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("Weekly Rhythm", style = MaterialTheme.typography.titleLarge); Text("Live from your habit check-ins", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Surface(color = LifeOSAccentLavender, shape = androidx.compose.foundation.shape.RoundedCornerShape(50)) { Text("$completionPercent% today", color = LifeOSPrimary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, day ->
                                val selected = index == 6
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(7.dp))
                                    Surface(color = if (selected) LifeOSPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp), modifier = Modifier.size(30.dp)) { BoxCentered { if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) } }
                                }
                            }
                        }
                    }
                }
            }
            item { LifeOSSectionHeader("Active routines", "${habits.size} total") }
            if (habits.isEmpty()) item { EmptyHabits(onAdd = { showAddDialog = true }) }
            else items(habits, key = { it.id }) { habit -> HabitCard(habit, locator.habitRepository, onOpenHabit) { progress -> viewModel.logToday(habit.id, progress, habit.goalCount) } }
        }
    }
    if (showAddDialog) {
        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("New habit") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose an icon", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(PRESET_HABIT_ICONS) { preset -> Surface(onClick = { icon = preset }, color = if (icon == preset) LifeOSAccentLavender else MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) { Text(preset, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.titleMedium) } } }
                OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Habit name") }, singleLine = true)
                TextButton(onClick = { showTimePicker = true }) { Icon(Icons.Filled.Notifications, contentDescription = null); Text(reminderTime?.let { "Remind at $it" } ?: "Set a daily reminder") }
            }
        }, confirmButton = { TextButton(onClick = { viewModel.addHabit(name, icon, reminderTime); name = ""; reminderTime = null; showAddDialog = false }) { Text("Add") } }, dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } })
    }
    if (showTimePicker) ReminderTimePickerDialog(onDismiss = { showTimePicker = false }, onConfirm = { reminderTime = it; showTimePicker = false })
}

@Composable
private fun HabitCard(habit: HabitEntity, repository: HabitRepository, onOpen: (String) -> Unit, onLogToday: (Int) -> Unit) {
    val today = remember { DateTimeUtils.today().toEpochDay() }
    val completion by repository.observeCompletion(habit.id, today).collectAsState(initial = null)
    val progress = completion?.progressCount ?: 0
    val done = progress >= habit.goalCount
    val analytics by produceState<com.lifeos.app.domain.model.HabitAnalytics?>(null, habit.id) { value = runCatching { repository.computeAnalytics(habit) }.getOrNull() }
    LifeOSCard(Modifier.fillMaxWidth(), onClick = { onOpen(habit.id) }, tint = if (done) LifeOSAccentLavender else MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (done) LifeOSPrimary else MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) { Text(habit.icon, modifier = Modifier.padding(11.dp), style = MaterialTheme.typography.titleLarge) }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium)
                Text(buildString { append("${habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() }} · "); append(if (habit.goalCount > 1) "$progress/${habit.goalCount} today" else if (done) "Completed today" else "Tap to check in") }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                analytics?.let { Text("${it.currentStreak} day streak · ${it.longestStreak} best", style = MaterialTheme.typography.labelSmall, color = LifeOSPrimary, modifier = Modifier.padding(top = 4.dp)) }
            }
            IconButton(onClick = { onLogToday(progress) }) { Icon(if (done) Icons.Filled.Check else Icons.Filled.RadioButtonUnchecked, contentDescription = if (done) "Completed today" else "Check in today", tint = if (done) LifeOSPrimary else MaterialTheme.colorScheme.outline) }
        }
    }
}

@Composable
private fun EmptyHabits(onAdd: () -> Unit) {
    LifeOSCard(Modifier.fillMaxWidth()) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("No routines yet", style = MaterialTheme.typography.titleMedium); Text("Create a small daily ritual and LifeOS will track its streak locally.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); LifeOSGradientButton("Create first habit", Modifier.fillMaxWidth(), onAdd) } }
}

@Composable
private fun BoxCentered(content: @Composable () -> Unit) { androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.Center) { content() } }
