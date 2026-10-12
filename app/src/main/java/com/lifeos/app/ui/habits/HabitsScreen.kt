package com.lifeos.app.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.HabitStatsCalculator
import com.lifeos.app.core.util.toSchedule
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.ReminderRepeatType
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.domain.usecase.DayCheck
import com.lifeos.app.domain.usecase.buildWeeklyRhythm
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSEmptyState
import com.lifeos.app.ui.components.LifeOSProgressBar
import com.lifeos.app.ui.components.LifeOSSectionHeader
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.components.LifeOSWeeklyRhythm
import com.lifeos.app.ui.components.LiquidGlassFab
import com.lifeos.app.ui.components.ReminderPermissionHost
import com.lifeos.app.ui.components.ReminderRepeatSelector
import com.lifeos.app.ui.components.ReminderTimePickerDialog
import com.lifeos.app.ui.components.rememberExactAlarmPermissionHost
import com.lifeos.app.ui.components.rememberReminderPermissionHost
import com.lifeos.app.ui.theme.LifeOSAccentLavender
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val PRESET_HABIT_ICONS = listOf("🔥", "💧", "🏃", "📖", "🧘", "😴", "🥗", "✅")

class HabitsViewModel(private val habitRepository: HabitRepository) : ViewModel() {
    val habits: StateFlow<List<HabitEntity>> = habitRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val today = DateTimeUtils.today()
    private val todayEpochDay = today.toEpochDay()

    /**
     * The seven days ending today, feeding the shared weekly-rhythm strip. Kept
     * here rather than collected in the composition so the range is fixed to the
     * day the screen was opened, instead of silently re-basing every time the
     * composable re-subscribes after the date rolls over.
     */
    val weekCompletions: StateFlow<List<HabitCompletionEntity>> =
        habitRepository.observeAllInRange(todayEpochDay - 6, todayEpochDay)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addHabit(name: String, icon: String, reminderTime: LocalTime?, reminderRepeatType: ReminderRepeatType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val today = DateTimeUtils.today()
            val reminderMillis = reminderTime?.let { today.atTime(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            habitRepository.createHabit(name = name, icon = icon.ifBlank { "✅" }, reminderEpochMillis = reminderMillis, reminderRepeatType = reminderRepeatType)
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
    // The flow is remembered per composition (keyed on today) so a new
    // subscription isn't created on every recomposition of this screen.
    val todayEpochDay = remember { DateTimeUtils.today().toEpochDay() }
    val completions by remember(todayEpochDay) {
        locator.habitRepository.observeAllForDay(todayEpochDay)
    }.collectAsState(initial = emptyList())
    val weekCompletions by viewModel.weekCompletions.collectAsState()
    // One rule for the strip, shared with Home through buildWeeklyRhythm, so the
    // two screens can never disagree about what counts as a done day.
    val rhythm = remember(habits, completions, weekCompletions, todayEpochDay) {
        buildWeeklyRhythm(
            habits = habits,
            todayCompletions = completions,
            weekCompletions = weekCompletions,
            today = LocalDate.ofEpochDay(todayEpochDay)
        )
    }
    // Today's progress is stated against what is *scheduled* for today, so a
    // weekly or custom-days routine never reads as missed on its rest day.
    val scheduledToday = remember(habits, todayEpochDay) {
        habits.filter { HabitStatsCalculator.isScheduled(it.toSchedule(), todayEpochDay) }
    }
    val progressByHabit = remember(completions) { completions.associateBy { it.habitId } }
    val doneToday = remember(scheduledToday, progressByHabit) {
        scheduledToday.count { habit -> (progressByHabit[habit.id]?.progressCount ?: 0) >= habit.goalCount }
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🔥") }
    var reminderTime by remember { mutableStateOf<LocalTime?>(null) }
    var reminderRepeatType by remember { mutableStateOf(ReminderRepeatType.DAILY) }
    // Reached only when the user actually creates a habit with a timed reminder — never at startup.
    val permissionHost: ReminderPermissionHost = rememberReminderPermissionHost()
    // Exact alarms are a system special access (SCHEDULE_EXACT_ALARM); without it the
    // scheduler still warns/degrades, so arming a timed habit reminder waits for the grant.
    val exactAlarmHost = rememberExactAlarmPermissionHost()
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        // The parent LifeOSNavHost Scaffold has already inset this destination for
        // the status bar, the bottom navigation and the system navigation area.
        // `Modifier.padding` does not *consume* insets, so a nested Scaffold asking
        // for `systemBars` again added them a second time — a blank band above the
        // header and below the last card, and a FAB pushed under the bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = { LiquidGlassFab(icon = Icons.Filled.Add, contentDescription = "New habit", onClick = { showAddDialog = true }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(start = LifeOSSpacing.screenPadding, end = LifeOSSpacing.screenPadding, top = 6.dp, bottom = LifeOSSpacing.fabContentClearance),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            item { LifeOSTopBar(title = "Habits & Routines", subtitle = "Build a rhythm that feels sustainable", modifier = Modifier) }
            // Both summaries are skipped on an empty library: with no routines
            // there is nothing to report, and the invitation card reads better
            // directly under the header than under two zeroed statistics.
            if (habits.isNotEmpty()) {
                item { LifeOSSectionHeader("Today's progress", "$doneToday of ${scheduledToday.size} done") }
                item { TodaysProgressCard(done = doneToday, scheduled = scheduledToday.size) }
                item { LifeOSSectionHeader("Weekly rhythm", "${rhythm.count { it.isDone }} of 7 days") }
                item { WeeklyRhythmCard(rhythm) }
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
                if (reminderTime != null) {
                    Text("Repeat reminder", style = MaterialTheme.typography.labelMedium)
                    ReminderRepeatSelector(selected = reminderRepeatType, onSelect = { reminderRepeatType = it })
                }
            }
        }, confirmButton = { TextButton(onClick = {
                val submit = { viewModel.addHabit(name, icon, reminderTime, reminderRepeatType); name = ""; reminderTime = null; reminderRepeatType = ReminderRepeatType.DAILY; showAddDialog = false }
                // Habit reminder creation must wait for the notification permission
                // and, when a timed alarm is being armed, for exact-alarm access.
                if (reminderTime != null) permissionHost.runProtected { exactAlarmHost.runProtected(submit) } else submit()
            }) { Text("Add") } }, dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } })
    }
    if (showTimePicker) ReminderTimePickerDialog(onDismiss = { showTimePicker = false }, onConfirm = { reminderTime = it; showTimePicker = false })
}

@Composable
@Suppress("ProduceStateDoesNotAssignValue")
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

/**
 * TODAY'S PROGRESS: the share of today's *scheduled* routines already checked
 * in. Both numbers come from persisted completions and the bar is their exact
 * ratio, so it can never show progress the database does not hold.
 */
@Composable
private fun TodaysProgressCard(done: Int, scheduled: Int) {
    val fraction = if (scheduled > 0) done.toFloat() / scheduled else 0f
    LifeOSCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            scheduled == 0 -> "Nothing scheduled today"
                            done >= scheduled -> "Every routine done today"
                            else -> "$done of $scheduled routines done"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Checked in on this device",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = LifeOSPrimary
                )
            }
            LifeOSProgressBar(progress = fraction)
        }
    }
}

/**
 * WEEKLY RHYTHM: the shared seven-day strip, with its rule stated in the card
 * so a row of dots is readable without guessing what "done" means.
 */
@Composable
private fun WeeklyRhythmCard(days: List<DayCheck>) {
    val doneDays = days.count { it.isDone }
    LifeOSCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A day counts as done only when every routine scheduled for it reached its goal.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "$doneDays/7",
                    style = MaterialTheme.typography.titleMedium,
                    color = LifeOSPrimary
                )
            }
            LifeOSWeeklyRhythm(days)
        }
    }
}

@Composable
private fun EmptyHabits(onAdd: () -> Unit) {
    LifeOSEmptyState(
        icon = Icons.Filled.RadioButtonUnchecked,
        title = "No routines yet",
        body = "Create a small daily ritual and LifeOS will track its streak locally.",
        actionLabel = "Create first habit",
        onAction = onAdd
    )
}
