package com.lifeos.app.ui.habits

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.lifeos.app.ui.components.HeaderActionCircle
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSEmptyState
import com.lifeos.app.ui.components.LifeOSSecondaryButton
import com.lifeos.app.ui.components.LifeOSSectionHeader
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.components.LifeOSWeeklyRhythm
import com.lifeos.app.ui.components.ReminderPermissionHost
import com.lifeos.app.ui.components.ReminderRepeatSelector
import com.lifeos.app.ui.components.ReminderTimePickerDialog
import com.lifeos.app.ui.components.rememberExactAlarmPermissionHost
import com.lifeos.app.ui.components.rememberReminderPermissionHost
import com.lifeos.app.ui.theme.FigmaAccent
import com.lifeos.app.ui.theme.FigmaCard
import com.lifeos.app.ui.theme.FigmaOnAccent
import com.lifeos.app.ui.theme.FigmaStroke
import com.lifeos.app.ui.theme.FigmaText
import com.lifeos.app.ui.theme.FigmaTrack
import com.lifeos.app.ui.theme.LifeOSRadius
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

/**
 * HABITS · DAILY RHYTHM — the Figma frame `#5:36536`.
 *
 * Chrome is the shared 86dp page header (mint date kicker + "Your daily rhythm"
 * + a 42×42 action circle holding the add affordance). Below it: the week strip
 * with today picked out in mint, the "Today's habits" card of rows, the rhythm
 * card, and a secondary "Create a habit" action.
 *
 * Everything below the header is real, persisted data: rows are the user's
 * habits, the counts are persisted completions against each habit's own goal,
 * and the strip is the same `buildWeeklyRhythm` rule Home uses. The add flow
 * (name/icon/reminder/repeat + notification and exact-alarm gating) and the
 * empty state are unchanged.
 */
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
    val today = LocalDate.ofEpochDay(todayEpochDay)
    Scaffold(
        containerColor = Color.Transparent,
        // The parent LifeOSNavHost Scaffold has already inset this destination for
        // the status bar, the bottom navigation and the system navigation area.
        // `Modifier.padding` does not *consume* insets, so a nested Scaffold asking
        // for `systemBars` again added them a second time — a blank band above the
        // header and below the last card, and a FAB pushed under the bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(start = LifeOSSpacing.screenPadding, end = LifeOSSpacing.screenPadding, top = 0.dp, bottom = LifeOSSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            item {
                LifeOSTopBar(
                    title = "Your daily rhythm",
                    subtitle = "Habits / ${DateTimeUtils.formatDayOfWeek(today)}, ${DateTimeUtils.formatFullDate(today)}",
                    actions = {
                        HeaderActionCircle(Icons.Filled.Add, "Create a habit", onClick = { showAddDialog = true })
                    }
                )
            }
            item { HabitsWeekStrip(rhythm) }
            // The counts are skipped on an empty library: with no routines there
            // is nothing to report, and the invitation reads better on its own
            // than under two zeroed statistics.
            if (habits.isNotEmpty()) {
                item { LifeOSSectionHeader("Today's habits", "$doneToday of ${scheduledToday.size} complete") }
                item {
                    LifeOSCard(Modifier.fillMaxWidth(), tint = FigmaCard) {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            scheduledToday.forEachIndexed { index, habit ->
                                if (index > 0) HabitRowDivider()
                                HabitRow(
                                    habit = habit,
                                    repository = locator.habitRepository,
                                    progress = progressByHabit[habit.id]?.progressCount ?: 0,
                                    onOpen = { onOpenHabit(habit.id) },
                                    onLogToday = { p -> viewModel.logToday(habit.id, p, habit.goalCount) }
                                )
                            }
                        }
                    }
                }
                item { LifeOSSectionHeader("Weekly rhythm", "${rhythm.count { it.isDone }} of 7 days") }
                item { WeeklyRhythmCard(rhythm) }
            } else {
                item { EmptyHabits(onAdd = { showAddDialog = true }) }
            }
            item {
                LifeOSSecondaryButton(
                    text = "Create a habit",
                    icon = Icons.Filled.Add,
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    if (showAddDialog) {
        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("New habit") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose an icon", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(PRESET_HABIT_ICONS) { preset -> Surface(onClick = { icon = preset }, color = if (icon == preset) FigmaAccent else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) { Text(preset, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.titleMedium) } } }
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

/**
 * WEEK STRIP: seven days ending today. The selected (today) cell is the mint
 * plate with dark ink; every other cell states its own day letter and number.
 * Each cell is a real 44dp target even though it is only a label here.
 */
@Composable
private fun HabitsWeekStrip(days: List<DayCheck>) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { day ->
            val date = LocalDate.ofEpochDay(day.epochDay)
            val letter = date.dayOfWeek.name.take(1).lowercase().replaceFirstChar { it.uppercase() }
            val number = date.dayOfMonth.toString()
            val selected = day.isToday
            Column(
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(LifeOSRadius.medium))
                    .background(if (selected) FigmaAccent else Color.Transparent)
                    .then(
                        if (selected) Modifier else Modifier.border(BorderStroke(1.dp, FigmaStroke), RoundedCornerShape(LifeOSRadius.medium))
                    )
                    .clickable(role = Role.Button, onClick = { /* Read-only marker: the strip reflects the persisted week. */ })
                    .semantics { contentDescription = "$letter ${date.dayOfMonth}${if (day.isDone) ", completed" else ""}"; role = Role.Button },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(letter, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold), color = if (selected) FigmaOnAccent else FigmaText)
                Spacer(Modifier.height(2.dp))
                Text(number, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = if (selected) FigmaOnAccent else FigmaText)
            }
        }
    }
}

/** A single habit row: 46×46 feature icon, name + meta, and the live progress. */
@Composable
@Suppress("ProduceStateDoesNotAssignValue")
private fun HabitRow(
    habit: HabitEntity,
    repository: HabitRepository,
    progress: Int,
    onOpen: () -> Unit,
    onLogToday: (Int) -> Unit
) {
    val goal = habit.goalCount
    val done = progress >= goal
    val frequency = habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() }
    val currentStreak by produceState(0, habit.id) { value = runCatching { repository.computeAnalytics(habit).currentStreak }.getOrDefault(0) }
    val meta = when {
        done -> "$currentStreak-day streak · $frequency · goal $goal"
        progress > 0 -> "${goal - progress} more to reach your goal"
        else -> "$currentStreak-day streak · $frequency · $goal a day"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics { contentDescription = "${habit.name}, $meta" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (done) FigmaAccent else FigmaTrack),
            contentAlignment = Alignment.Center
        ) {
            Text(habit.icon, style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.weight(1f)) {
            Text(habit.name, style = MaterialTheme.typography.titleMedium, color = FigmaText, maxLines = 1)
            Text(meta, style = MaterialTheme.typography.bodySmall, color = FigmaText.copy(alpha = 0.55f), maxLines = 1)
        }
        // Progress is a real control: it both states the count and logs the next
        // step, so the row stays one-handed and the value can never drift.
        TextButton(onClick = { onLogToday(progress) }) {
            Text(
                if (done) "Done" else if (progress > 0) "$progress / $goal" else "Start",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (done) FigmaOnAccent else FigmaAccent
            )
        }
    }
}

/** The 1px card-internal hairline between stacked rows. */
@Composable
private fun HabitRowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(FigmaStroke)
    )
}

/**
 * WEEKLY RHYTHM: the shared seven-day strip, with its rule stated in the card
 * so a row of dots is readable without guessing what "done" means.
 */
@Composable
private fun WeeklyRhythmCard(days: List<DayCheck>) {
    val doneDays = days.count { it.isDone }
    LifeOSCard(Modifier.fillMaxWidth(), tint = FigmaCard) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Last 7 days", style = MaterialTheme.typography.titleMedium, color = FigmaText)
                    Text(
                        "A day counts as done only when every routine scheduled for it reached its goal.",
                        style = MaterialTheme.typography.labelSmall,
                        color = FigmaText.copy(alpha = 0.55f)
                    )
                }
                Text(
                    "$doneDays/7",
                    style = MaterialTheme.typography.titleMedium,
                    color = FigmaAccent
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
