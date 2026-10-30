package com.lifeos.app.ui.habits

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.HabitStatsCalculator
import com.lifeos.app.data.db.entities.HabitChecklistItemEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitScheduleType
import com.lifeos.app.data.db.entities.HabitTrackingMode
import com.lifeos.app.data.db.entities.HabitType
import com.lifeos.app.data.db.entities.ReminderRepeatType
import com.lifeos.app.data.repository.HabitChecklistDraft
import com.lifeos.app.data.repository.HabitDraft
import com.lifeos.app.data.repository.HabitRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HabitEditorUiState(
    val draft: HabitDraft = HabitDraft(name = ""),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val step: Int = 0,
    val reminderTime: LocalTime? = null,
    val reminderRepeatType: ReminderRepeatType = ReminderRepeatType.DAILY,
    val dirty: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
) {
    fun scheduleIsValid(): Boolean = when (draft.scheduleType) {
        HabitScheduleType.WEEKDAYS -> HabitStatsCalculator.parseCustomDays(draft.daysOfWeekCsv).isNotEmpty()
        HabitScheduleType.MONTH_DAYS -> HabitStatsCalculator.parseMonthDays(draft.daysOfMonthCsv).isNotEmpty()
        HabitScheduleType.TIMES_PER_WEEK -> (draft.timesPerPeriod ?: 0) in 1..7
        HabitScheduleType.TIMES_PER_MONTH -> (draft.timesPerPeriod ?: 0) in 1..31
        else -> true
    }

    fun trackingIsValid(): Boolean = when (draft.trackingMode) {
        HabitTrackingMode.COUNT -> draft.goalCount >= 1
        HabitTrackingMode.CHECKLIST -> draft.checklistItems.count { it.label.isNotBlank() } >= 1
        HabitTrackingMode.TICK -> true
    }

    fun stepIsValid(step: Int): Boolean = when (step) {
        1 -> draft.name.trim().isNotEmpty()
        5 -> scheduleIsValid()
        6 -> trackingIsValid()
        else -> true
    }

    fun isValidToSave(): Boolean =
        draft.name.trim().isNotEmpty() && scheduleIsValid() && trackingIsValid()
}

class HabitEditorViewModel(
    private val habitRepository: HabitRepository,
    private val habitId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(
        HabitEditorUiState(isEditing = habitId != null, isLoading = habitId != null)
    )
    val state: StateFlow<HabitEditorUiState> = _state.asStateFlow()

    init {
        if (habitId != null) {
            viewModelScope.launch { load(habitId) }
        }
    }

    private suspend fun load(id: String) {
        val habit = habitRepository.getById(id) ?: run {
            _state.update { it.copy(isLoading = false, error = "This habit no longer exists.") }
            return
        }
        val items = habitRepository.getChecklistItems(id)
        val reminder = habit.reminderEpochMillis?.let {
            java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
        }
        val repeat = habitRepository.getReminderRepeat(id) ?: ReminderRepeatType.DAILY
        val draft = habit.toDraft(items)
        _state.value = HabitEditorUiState(
            draft = draft,
            isEditing = true,
            isLoading = false,
            reminderTime = reminder,
            reminderRepeatType = repeat,
            dirty = false
        )
    }

    private fun HabitEntity.toDraft(items: List<HabitChecklistItemEntity>) = HabitDraft(
        name = name,
        description = description,
        icon = icon,
        colorArgb = colorArgb,
        category = category,
        habitType = habitType,
        trackingMode = trackingMode,
        scheduleType = scheduleType,
        daysOfWeekCsv = daysOfWeekCsv ?: customDaysCsv,
        daysOfMonthCsv = daysOfMonthCsv,
        timesPerPeriod = timesPerPeriod,
        goalCount = goalCount,
        reminderEpochMillis = reminderEpochMillis,
        reminderRepeatType = ReminderRepeatType.DAILY,
        showOnTimeline = showOnTimeline,
        startDateEpochDay = startDateEpochDay,
        checklistItems = items
            .filter { !it.isArchived }
            .sortedBy { it.position }
            .map { HabitChecklistDraft(id = it.id, label = it.label, position = it.position) }
    )

    fun nextStep() {
        val current = _state.value
        if (!current.stepIsValid(current.step)) {
            _state.update { it.copy(error = errorForStep(current.step)) }
            return
        }
        _state.update { it.copy(step = (it.step + 1).coerceAtMost(WIZARD_STEPS - 1), error = null) }
    }

    fun previousStep() {
        _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0), error = null) }
    }

    private fun errorForStep(step: Int): String = when (step) {
        1 -> "Give the habit a name before continuing."
        5 -> "Choose at least one day, or a valid number of times per period."
        6 -> "Add at least one checklist item, or choose a different tracking mode."
        else -> "Check this step before continuing."
    }

    fun update(transform: (HabitDraft) -> HabitDraft) {
        _state.update { current ->
            current.copy(draft = transform(current.draft), dirty = true, error = null)
        }
    }

    fun setName(value: String) = update { it.copy(name = value.take(80)) }
    fun setDescription(value: String) = update { it.copy(description = value.take(500)) }
    fun setIcon(value: String) = update { it.copy(icon = value) }
    fun setColor(value: Long) = update { it.copy(colorArgb = value) }
    fun setType(value: HabitType) = update { it.copy(habitType = value) }
    fun setCategory(value: String?) = update { it.copy(category = value) }
    fun setShowOnTimeline(value: Boolean) = update { it.copy(showOnTimeline = value) }

    fun setScheduleType(value: HabitScheduleType) = update { draft ->
        when (value) {
            HabitScheduleType.WEEKDAYS -> {
                val days = draft.daysOfWeekCsv?.takeIf { it.isNotBlank() }
                    ?: DayOfWeek.from(LocalDate.ofEpochDay(draft.startDateEpochDay)).value.toString()
                draft.copy(scheduleType = value, daysOfWeekCsv = days)
            }
            HabitScheduleType.MONTH_DAYS -> {
                val days = draft.daysOfMonthCsv?.takeIf { it.isNotBlank() }
                    ?: LocalDate.ofEpochDay(draft.startDateEpochDay).dayOfMonth.toString()
                draft.copy(scheduleType = value, daysOfMonthCsv = days)
            }
            HabitScheduleType.TIMES_PER_WEEK -> draft.copy(
                scheduleType = value,
                timesPerPeriod = draft.timesPerPeriod?.takeIf { it in 1..7 } ?: 3
            )
            HabitScheduleType.TIMES_PER_MONTH -> draft.copy(
                scheduleType = value,
                timesPerPeriod = draft.timesPerPeriod?.takeIf { it in 1..31 } ?: 10
            )
            else -> draft.copy(
                scheduleType = value,
                // The chosen schedule is authoritative; a stale legacy mirror
                // would otherwise keep an old CUSTOM row alive in the database.
                frequencyOverride = null,
                legacyCustomDaysCsv = null
            )
        }
    }

    fun toggleWeekday(day: DayOfWeek) = update { draft ->
        val days = HabitStatsCalculator.parseCustomDays(draft.daysOfWeekCsv).toMutableSet()
        if (!days.add(day)) days.remove(day)
        draft.copy(daysOfWeekCsv = days.sortedBy { it.value }.joinToString(",") { it.value.toString() })
    }

    fun toggleMonthDay(day: Int) = update { draft ->
        val days = HabitStatsCalculator.parseMonthDays(draft.daysOfMonthCsv).toMutableSet()
        if (!days.add(day)) days.remove(day)
        draft.copy(daysOfMonthCsv = days.sorted().joinToString(","))
    }

    fun setTimesPerPeriod(value: Int) = update { draft ->
        val max = if (draft.scheduleType == HabitScheduleType.TIMES_PER_WEEK) 7 else 31
        draft.copy(timesPerPeriod = value.coerceIn(1, max))
    }

    fun setTrackingMode(value: HabitTrackingMode) = update { draft ->
        val items = if (value == HabitTrackingMode.CHECKLIST && draft.checklistItems.isEmpty()) {
            listOf(HabitChecklistDraft(label = ""), HabitChecklistDraft(label = ""))
        } else draft.checklistItems
        draft.copy(
            trackingMode = value,
            goalCount = if (value == HabitTrackingMode.COUNT) draft.goalCount.coerceAtLeast(1) else draft.goalCount,
            checklistItems = items
        )
    }

    fun setGoalCount(value: Int) = update { it.copy(goalCount = value.coerceIn(1, 999)) }

    fun updateChecklistItem(index: Int, label: String) = update { draft ->
        val items = draft.checklistItems.toMutableList()
        if (index in items.indices) items[index] = items[index].copy(label = label.take(120))
        draft.copy(checklistItems = items)
    }

    fun addChecklistItem() = update { draft ->
        draft.copy(checklistItems = draft.checklistItems + HabitChecklistDraft(label = ""))
    }

    fun removeChecklistItem(index: Int) = update { draft ->
        val items = draft.checklistItems.toMutableList()
        if (index in items.indices) items.removeAt(index)
        draft.copy(checklistItems = items)
    }

    fun setReminder(time: LocalTime?) {
        _state.update { it.copy(reminderTime = time, dirty = true) }
    }

    fun setReminderRepeat(type: ReminderRepeatType) {
        _state.update { it.copy(reminderRepeatType = type, dirty = true) }
    }

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        if (!current.isValidToSave()) {
            _state.update { it.copy(error = "A name, a valid schedule and valid tracking are required.") }
            return
        }
        if (current.isSaving) return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val reminderEpoch = current.reminderTime?.let { time ->
                LocalDate.now().atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            val draft = current.draft.copy(
                name = current.draft.name.trim(),
                description = current.draft.description?.trim()?.takeIf { it.isNotBlank() },
                category = current.draft.category?.takeIf { it.isNotBlank() },
                reminderEpochMillis = reminderEpoch,
                reminderRepeatType = current.reminderRepeatType,
                checklistItems = current.draft.checklistItems.filter { it.label.isNotBlank() }
            )
            runCatching {
                if (current.isEditing && habitId != null) {
                    habitRepository.updateHabit(habitId, draft)
                } else {
                    habitRepository.createHabit(draft)
                }
            }.onSuccess {
                _state.update { it.copy(isSaving = false, saved = true, dirty = false) }
                onSaved()
            }.onFailure {
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = "Could not save the habit. Nothing was changed."
                    )
                }
            }
        }
    }

    companion object {
        const val WIZARD_STEPS = 10
    }
}

@Composable
fun HabitEditorScreen(
    habitId: String?,
    startWithWizard: Boolean,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: HabitEditorViewModel = viewModel(
        factory = LambdaViewModelFactory {
            HabitEditorViewModel(locator.habitRepository, habitId)
        }
    )
    val state by viewModel.state.collectAsState()
    val wizard = startWithWizard && habitId == null
    var showDiscard by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val reminderHost: ReminderPermissionHost = rememberReminderPermissionHost()
    val exactAlarmHost: ExactAlarmPermissionHost = rememberExactAlarmPermissionHost()

    val save: () -> Unit = {
        val action = { viewModel.save(onSaved) }
        if (state.reminderTime != null) {
            reminderHost.runProtected { exactAlarmHost.runProtected(action) }
        } else {
            action()
        }
    }

    val requestClose: () -> Unit = {
        if (state.dirty && !state.saved) showDiscard = true else onBack()
    }

    BackHandler(enabled = true) {
        when {
            state.isSaving -> Unit
            wizard && state.step > 0 -> viewModel.previousStep()
            else -> requestClose()
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard this habit?") },
            text = { Text("Your changes have not been saved. Leaving now will lose them.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    onBack()
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) { Text("Keep editing") }
            }
        )
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initial = state.reminderTime ?: LocalTime.of(8, 0),
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                showTimePicker = false
                viewModel.setReminder(time)
            }
        )
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading habit…", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LifeOSTopBar(
                title = when {
                    state.isEditing -> "Edit habit"
                    wizard -> "New habit"
                    else -> "New habit"
                },
                subtitle = if (wizard) "Step ${state.step + 1} of ${HabitEditorViewModel.WIZARD_STEPS}" else null,
                onBack = requestClose,
                actions = {
                    if (state.isSaving) {
                        Text("Saving…", style = MaterialTheme.typography.labelMedium)
                    } else {
                        TextButton(onClick = save, enabled = state.isValidToSave()) {
                            Text("Save")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (wizard) {
                WizardBottomBar(
                    step = state.step,
                    canContinue = state.stepIsValid(state.step),
                    isSaving = state.isSaving,
                    onBack = viewModel::previousStep,
                    onContinue = viewModel::nextStep,
                    onSave = save
                )
            }
        }
    ) { padding ->
        if (wizard) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                LinearProgressIndicator(
                    progress = { (state.step + 1f) / HabitEditorViewModel.WIZARD_STEPS },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = HabitIris,
                    trackColor = HabitIris.copy(alpha = 0.12f)
                )
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = LifeOSSpacing.screenPadding, vertical = LifeOSSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.lg)
                ) {
                    WizardStep(
                        step = state.step,
                        state = state,
                        viewModel = viewModel,
                        onPickTime = { showTimePicker = true }
                    )
                    if (state.error != null) {
                        Text(
                            state.error.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        } else {
            LazyEditorForm(
                state = state,
                viewModel = viewModel,
                onPickTime = { showTimePicker = true },
                onSave = save,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun LazyEditorForm(
    state: HabitEditorUiState,
    viewModel: HabitEditorViewModel,
    onPickTime: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = LifeOSSpacing.screenPadding,
            end = LifeOSSpacing.screenPadding,
            top = LifeOSSpacing.md,
            bottom = LifeOSSpacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.lg)
    ) {
        item {
            EditorSection("Basics") {
                NameFields(state, viewModel::setName, viewModel::setDescription)
            }
        }
        item { EditorSection("Appearance") { AppearanceFields(state, viewModel) } }
        item { EditorSection("Habit type") { TypeFields(state, viewModel::setType) } }
        item { EditorSection("Frequency") { FrequencyFields(state, viewModel) } }
        item { EditorSection("Tracking") { TrackingFields(state, viewModel) } }
        item { EditorSection("Reminder") { ReminderFields(state, viewModel, onPickTime) } }
        item { EditorSection("Category & Timeline") { CategoryFields(state, viewModel) } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LifeOSPrimaryButton(
                    text = if (state.isSaving) "Saving…" else "Save habit",
                    onClick = onSave,
                    enabled = !state.isSaving && state.isValidToSave(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.error != null) {
                    Text(
                        state.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardStep(
    step: Int,
    state: HabitEditorUiState,
    viewModel: HabitEditorViewModel,
    onPickTime: () -> Unit
) {
    when (step) {
        0 -> WelcomeStep()
        1 -> {
            StepHeading("What do you want to build?", "A clear name is easier to keep.")
            NameFields(state, viewModel::setName, viewModel::setDescription)
        }
        2 -> {
            StepHeading("Pick an icon", "This is how the habit appears on cards and the timeline.")
            IconGrid(selected = state.draft.icon, onSelect = viewModel::setIcon)
        }
        3 -> {
            StepHeading("Choose a colour", "It tints the card, heatmap and celebration.")
            ColorGrid(selected = state.draft.colorArgb, onSelect = viewModel::setColor)
        }
        4 -> {
            StepHeading("Build or quit?", "Both are tracked explicitly. A blank day is never assumed.")
            TypeFields(state, viewModel::setType)
        }
        5 -> {
            StepHeading("How often?", "Unscheduled days stay neutral and never break a streak.")
            FrequencyFields(state, viewModel)
        }
        6 -> {
            StepHeading("How do you want to track it?", "Switch modes later without losing history.")
            TrackingFields(state, viewModel)
        }
        7 -> {
            StepHeading("Want a reminder?", "Optional, local and never uploaded.")
            ReminderFields(state, viewModel, onPickTime)
        }
        8 -> {
            StepHeading("Where does it belong?", "Categories and Timeline are optional.")
            CategoryFields(state, viewModel)
        }
        9 -> {
            StepHeading("Review", "Check the details, then save. You can edit everything later.")
            HabitReview(state)
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
        HabitWizardHero()
        Text(
            "A habit is a promise you keep to yourself.",
            style = MaterialTheme.typography.headlineSmall,
            color = HabitIrisDeep,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "This short setup takes about a minute. You will choose a name, icon, colour, schedule, reminder and tracking style.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        LifeOSSurface(Modifier.fillMaxWidth(), tint = HabitIrisWash, contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WelcomeBullet("🔥", "Build a habit or quit one — both are first-class.")
                WelcomeBullet("📅", "Every day, selected weekdays, month dates or N times per period.")
                WelcomeBullet("✅", "Tick, count or checklist tracking with a 12-week heatmap.")
                WelcomeBullet("🔒", "Everything stays on this device, offline.")
            }
        }
    }
}

@Composable
private fun WelcomeBullet(emoji: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 16.sp)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StepHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EditorSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        LifeOSSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}

@Composable
private fun NameFields(
    state: HabitEditorUiState,
    onName: (String) -> Unit,
    onDescription: (String) -> Unit
) {
    OutlinedTextField(
        value = state.draft.name,
        onValueChange = onName,
        label = { Text("Habit name") },
        placeholder = { Text("Read 10 pages") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LifeOSRadius.medium)
    )
    OutlinedTextField(
        value = state.draft.description.orEmpty(),
        onValueChange = onDescription,
        label = { Text("Description (optional)") },
        placeholder = { Text("Why this matters to you") },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LifeOSRadius.medium)
    )
}

@Composable
private fun AppearanceFields(state: HabitEditorUiState, viewModel: HabitEditorViewModel) {
    Text("Icon", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    IconGrid(selected = state.draft.icon, onSelect = viewModel::setIcon)
    Text("Colour", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    ColorGrid(selected = state.draft.colorArgb, onSelect = viewModel::setColor)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconGrid(selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        habitIconOptions.forEach { icon ->
            val isSelected = icon == selected
            Surface(
                onClick = { onSelect(icon) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) HabitIrisSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(46.dp)
                    .then(
                        if (isSelected) Modifier.border(1.5.dp, HabitIris, RoundedCornerShape(14.dp))
                        else Modifier
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(icon, fontSize = 21.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorGrid(selected: Long, onSelect: (Long) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        habitColorOptions.forEach { option ->
            val color = habitColorOf(option.argb)
            val isSelected = option.argb == selected || (selected == 0L && option == habitColorOptions.first())
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelect(option.argb) }
                    .then(
                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = option.name,
                        tint = onHabitColor(color),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeFields(state: HabitEditorUiState, onSelect: (HabitType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HabitChoiceCard(
            selected = state.draft.habitType == HabitType.BUILD,
            title = "Build a habit",
            subtitle = "Do something consistently: read, walk, meditate.",
            emoji = "🌱",
            onClick = { onSelect(HabitType.BUILD) }
        )
        HabitChoiceCard(
            selected = state.draft.habitType == HabitType.QUIT,
            title = "Quit a habit",
            subtitle = "Avoid something: smoking, late scrolling, sugar. Log slips explicitly.",
            emoji = "🚭",
            onClick = { onSelect(HabitType.QUIT) }
        )
    }
}

@Composable
private fun HabitChoiceCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    emoji: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(LifeOSRadius.large),
        color = if (selected) HabitIrisWash else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.border(1.5.dp, HabitIris, RoundedCornerShape(LifeOSRadius.large))
                else Modifier
            )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(emoji, fontSize = 24.sp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (selected) HabitIrisDeep else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = HabitIris)
            }
        }
    }
}

@Composable
private fun FrequencyFields(state: HabitEditorUiState, viewModel: HabitEditorViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.draft.scheduleType == HabitScheduleType.LEGACY) {
            HabitChoiceCard(
                selected = true,
                title = "Original schedule",
                subtitle = legacyScheduleLabel(state.draft),
                emoji = "📅",
                onClick = {}
            )
        }
        HabitChoiceCard(
            selected = state.draft.scheduleType == HabitScheduleType.EVERY_DAY,
            title = "Every day",
            subtitle = "A daily rhythm.",
            emoji = "☀️",
            onClick = { viewModel.setScheduleType(HabitScheduleType.EVERY_DAY) }
        )
        HabitChoiceCard(
            selected = state.draft.scheduleType == HabitScheduleType.WEEKDAYS,
            title = "Specific weekdays",
            subtitle = "Choose any combination of days.",
            emoji = "🗓️",
            onClick = { viewModel.setScheduleType(HabitScheduleType.WEEKDAYS) }
        )
        if (state.draft.scheduleType == HabitScheduleType.WEEKDAYS) {
            WeekdayPicker(state.draft.daysOfWeekCsv, viewModel::toggleWeekday)
        }
        HabitChoiceCard(
            selected = state.draft.scheduleType == HabitScheduleType.MONTH_DAYS,
            title = "Specific days of the month",
            subtitle = "e.g. the 1st and 15th. Short months are handled safely.",
            emoji = "📆",
            onClick = { viewModel.setScheduleType(HabitScheduleType.MONTH_DAYS) }
        )
        if (state.draft.scheduleType == HabitScheduleType.MONTH_DAYS) {
            MonthDayPicker(state.draft.daysOfMonthCsv, viewModel::toggleMonthDay)
        }
        HabitChoiceCard(
            selected = state.draft.scheduleType == HabitScheduleType.TIMES_PER_WEEK,
            title = "A number of times per week",
            subtitle = "Flexible days, with the target counted per week.",
            emoji = "🔁",
            onClick = { viewModel.setScheduleType(HabitScheduleType.TIMES_PER_WEEK) }
        )
        if (state.draft.scheduleType == HabitScheduleType.TIMES_PER_WEEK ||
            state.draft.scheduleType == HabitScheduleType.TIMES_PER_MONTH
        ) {
            TimesPerPeriodPicker(
                times = state.draft.timesPerPeriod ?: 0,
                max = if (state.draft.scheduleType == HabitScheduleType.TIMES_PER_WEEK) 7 else 31,
                onChange = viewModel::setTimesPerPeriod
            )
        }
        HabitChoiceCard(
            selected = state.draft.scheduleType == HabitScheduleType.TIMES_PER_MONTH,
            title = "A number of times per month",
            subtitle = "Useful for 8 gym visits or 4 deep-clean days.",
            emoji = "📊",
            onClick = { viewModel.setScheduleType(HabitScheduleType.TIMES_PER_MONTH) }
        )
    }
}

private fun legacyScheduleLabel(draft: HabitDraft): String = when (draft.scheduleType) {
    HabitScheduleType.LEGACY -> "Keeps the original DAILY / WEEKLY / CUSTOM meaning."
    else -> ""
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayPicker(csv: String?, onToggle: (DayOfWeek) -> Unit) {
    val selected = HabitStatsCalculator.parseCustomDays(csv)
    val labels = listOf(
        DayOfWeek.MONDAY to "Mon",
        DayOfWeek.TUESDAY to "Tue",
        DayOfWeek.WEDNESDAY to "Wed",
        DayOfWeek.THURSDAY to "Thu",
        DayOfWeek.FRIDAY to "Fri",
        DayOfWeek.SATURDAY to "Sat",
        DayOfWeek.SUNDAY to "Sun"
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEach { (day, label) ->
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthDayPicker(csv: String?, onToggle: (Int) -> Unit) {
    val selected = HabitStatsCalculator.parseMonthDays(csv)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..31).forEach { day ->
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = { Text("$day", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(34.dp)
            )
        }
    }
}

@Composable
private fun TimesPerPeriodPicker(times: Int, max: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LifeOSRadius.large))
            .background(HabitIrisWash)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Target per period",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        NumberStepper(value = times, min = 1, max = max, onChange = onChange)
    }
}

@Composable
private fun TrackingFields(state: HabitEditorUiState, viewModel: HabitEditorViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HabitChoiceCard(
            selected = state.draft.trackingMode == HabitTrackingMode.TICK,
            title = "Tick",
            subtitle = "One tap marks the day complete. Best for simple yes/no habits.",
            emoji = "✅",
            onClick = { viewModel.setTrackingMode(HabitTrackingMode.TICK) }
        )
        HabitChoiceCard(
            selected = state.draft.trackingMode == HabitTrackingMode.COUNT,
            title = "Count",
            subtitle = "Track progress toward a daily number: pages, glasses, minutes.",
            emoji = "🔢",
            onClick = { viewModel.setTrackingMode(HabitTrackingMode.COUNT) }
        )
        if (state.draft.trackingMode == HabitTrackingMode.COUNT) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LifeOSRadius.large))
                    .background(HabitIrisWash)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daily target", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                NumberStepper(
                    value = state.draft.goalCount,
                    min = 1,
                    max = 999,
                    onChange = viewModel::setGoalCount
                )
            }
        }
        HabitChoiceCard(
            selected = state.draft.trackingMode == HabitTrackingMode.CHECKLIST,
            title = "Checklist",
            subtitle = "Define reusable items; the day completes only when all are checked.",
            emoji = "📋",
            onClick = { viewModel.setTrackingMode(HabitTrackingMode.CHECKLIST) }
        )
        if (state.draft.trackingMode == HabitTrackingMode.CHECKLIST) {
            ChecklistEditor(state, viewModel)
        }
    }
}

@Composable
private fun ChecklistEditor(state: HabitEditorUiState, viewModel: HabitEditorViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Checklist items",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        state.draft.checklistItems.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = item.label,
                    onValueChange = { viewModel.updateChecklistItem(index, it) },
                    placeholder = { Text("Item ${index + 1}") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(LifeOSRadius.medium)
                )
                IconButton(onClick = { viewModel.removeChecklistItem(index) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove item")
                }
            }
        }
        TextButton(onClick = viewModel::addChecklistItem) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add item")
        }
    }
}

@Composable
private fun ReminderFields(
    state: HabitEditorUiState,
    viewModel: HabitEditorViewModel,
    onPickTime: () -> Unit
) {
    val enabled = state.reminderTime != null
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LifeOSRadius.large))
            .background(HabitIrisWash)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.Notifications, contentDescription = null, tint = HabitIris)
        Column(Modifier.weight(1f)) {
            Text("Remind me", style = MaterialTheme.typography.bodyMedium)
            Text(
                state.reminderTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "No reminder",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HabitSwitch(
            checked = enabled,
            onCheckedChange = { checked ->
                if (checked) onPickTime() else viewModel.setReminder(null)
            }
        )
    }
    if (enabled) {
        TextButton(onClick = onPickTime) { Text("Change time") }
        Text(
            "Repeat",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ReminderRepeatSelector(
            selected = state.reminderRepeatType,
            onSelect = viewModel::setReminderRepeat
        )
        Text(
            "LifeOS asks for notification access only when you save a reminder. Exact alarms may be limited by the system battery settings.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A habit-specific wrapper so the form reads consistently across both fields. */
@Composable
private fun HabitSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(checked = checked, onCheckedChange = onCheckedChange)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryFields(state: HabitEditorUiState, viewModel: HabitEditorViewModel) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        habitCategoryOptions.forEach { category ->
            FilterChip(
                selected = state.draft.category == category,
                onClick = {
                    viewModel.setCategory(if (state.draft.category == category) null else category)
                },
                label = { Text(category) }
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Show streaks on Timeline", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Successful days appear as Timeline events. Slips and blank days are never auto-published.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HabitSwitch(
            checked = state.draft.showOnTimeline,
            onCheckedChange = viewModel::setShowOnTimeline
        )
    }
}

@Composable
private fun HabitReview(state: HabitEditorUiState) {
    val draft = state.draft
    LifeOSSurface(Modifier.fillMaxWidth(), tint = HabitIrisWash, contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).background(habitColorOf(draft.colorArgb).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(draft.icon, fontSize = 27.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(draft.name.ifBlank { "Untitled habit" }, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (draft.habitType == HabitType.QUIT) "Quit habit" else "Build habit",
                        style = MaterialTheme.typography.labelSmall,
                        color = HabitIrisDeep
                    )
                }
            }
            ReviewRow("Schedule", scheduleSummary(draft))
            ReviewRow("Tracking", trackingSummary(draft))
            ReviewRow(
                "Reminder",
                state.reminderTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "None"
            )
            ReviewRow("Category", draft.category ?: "None")
            ReviewRow("Timeline", if (draft.showOnTimeline) "On" else "Off")
            if (draft.description?.isNotBlank() == true) {
                ReviewRow("Description", draft.description)
            }
        }
    }
}

private fun scheduleSummary(draft: HabitDraft): String = when (draft.scheduleType) {
    HabitScheduleType.LEGACY -> "Original schedule"
    HabitScheduleType.EVERY_DAY -> "Every day"
    HabitScheduleType.WEEKDAYS -> weekdayCsvLabel(draft.daysOfWeekCsv)
    HabitScheduleType.MONTH_DAYS -> monthDayCsvLabel(draft.daysOfMonthCsv)
    HabitScheduleType.TIMES_PER_WEEK -> "${draft.timesPerPeriod ?: 1}× per week"
    HabitScheduleType.TIMES_PER_MONTH -> "${draft.timesPerPeriod ?: 1}× per month"
}

private fun trackingSummary(draft: HabitDraft): String = when (draft.trackingMode) {
    HabitTrackingMode.TICK -> "Tick — one tap"
    HabitTrackingMode.COUNT -> "Count — ${draft.goalCount} per day"
    HabitTrackingMode.CHECKLIST -> "Checklist — ${draft.checklistItems.count { it.label.isNotBlank() }} items"
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(82.dp)
        )
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NumberStepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = { onChange((value - 1).coerceAtLeast(min)) },
            enabled = value > min,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                "−",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            "$value",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.Center
        )
        Surface(
            onClick = { onChange((value + 1).coerceAtMost(max)) },
            enabled = value < max,
            shape = CircleShape,
            color = HabitIris.copy(alpha = 0.14f)
        ) {
            Text(
                "+",
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                style = MaterialTheme.typography.titleMedium,
                color = HabitIris
            )
        }
    }
}

@Composable
private fun WizardBottomBar(
    step: Int,
    canContinue: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = LifeOSSpacing.screenPadding, vertical = LifeOSSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (step > 0) {
                TextButton(onClick = onBack, enabled = !isSaving) { Text("Back") }
            }
            Spacer(Modifier.weight(1f))
            if (step == HabitEditorViewModel.WIZARD_STEPS - 1) {
                LifeOSPrimaryButton(
                    text = if (isSaving) "Saving…" else "Create habit",
                    onClick = onSave,
                    enabled = !isSaving
                )
            } else {
                Surface(
                    onClick = onContinue,
                    enabled = canContinue,
                    shape = RoundedCornerShape(LifeOSRadius.pill),
                    color = if (canContinue) HabitIris else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (canContinue) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Continue", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
