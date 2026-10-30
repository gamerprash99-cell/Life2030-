package com.lifeos.app.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.HabitStatsCalculator
import com.lifeos.app.core.util.completionTarget
import com.lifeos.app.core.util.isCompletionDone
import com.lifeos.app.core.util.toSchedule
import com.lifeos.app.data.db.entities.HabitChecklistItemEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitTrackingMode
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.domain.model.HabitAnalytics
import com.lifeos.app.domain.model.HeatmapCell
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Everything one habit card needs, all derived from persisted rows. */
data class HabitCardState(
    val habit: HabitEntity,
    val completion: HabitCompletionEntity?,
    val isDone: Boolean,
    val progress: Int,
    val target: Int,
    val analytics: HabitAnalytics,
    val miniHeatmap: List<HeatmapCell>,
    val checklistItemCount: Int,
    /** True when the habit is scheduled/due on the selected day. */
    val isDue: Boolean
)

data class HabitsUiState(
    val selectedEpochDay: Long,
    val todayEpochDay: Long,
    val cards: List<HabitCardState>,
    val archived: List<HabitEntity> = emptyList()
) {
    val scheduledCount: Int get() = cards.count { it.isDue }
    val completedCount: Int get() = cards.count { it.isDue && it.isDone }
    val hasHabits: Boolean get() = cards.isNotEmpty()
    val isRestDay: Boolean get() = cards.isNotEmpty() && scheduledCount == 0
    val scorePercent: Int
        get() = if (scheduledCount == 0) 0 else (completedCount * 100 / scheduledCount).coerceIn(0, 100)
}

class HabitsViewModel(private val habitRepository: HabitRepository) : ViewModel() {

    private val todayEpochDay = DateTimeUtils.today().toEpochDay()
    private val _selectedEpochDay = MutableStateFlow(todayEpochDay)
    val selectedEpochDay: StateFlow<Long> = _selectedEpochDay

    private val _celebrations = MutableSharedFlow<HabitCelebration>(extraBufferCapacity = 1)
    val celebrations = _celebrations.asSharedFlow()

    val uiState: StateFlow<HabitsUiState> = combine(
        habitRepository.observeAll(),
        habitRepository.observeAllCompletions(),
        habitRepository.observeAllChecklistItems(),
        habitRepository.observeArchived(),
        _selectedEpochDay
    ) { habits, completions, checklistItems, archived, selectedDay ->
        buildState(habits, completions, checklistItems, archived, selectedDay)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HabitsUiState(selectedEpochDay = todayEpochDay, todayEpochDay = todayEpochDay, cards = emptyList())
    )

    private fun buildState(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        checklistItems: List<HabitChecklistItemEntity>,
        archived: List<HabitEntity>,
        selectedDay: Long
    ): HabitsUiState {
        val today = DateTimeUtils.today().toEpochDay()
        val dayDate = LocalDate.ofEpochDay(selectedDay)
        val byHabit = completions.groupBy { it.habitId }
        val itemsByHabit = checklistItems.groupBy { it.habitId }

        val cards = habits.map { habit ->
            val allForHabit = byHabit[habit.id].orEmpty()
            val upToDay = allForHabit.filter { it.dateEpochDay <= selectedDay }
            val completion = upToDay.firstOrNull { it.dateEpochDay == selectedDay }
            val doneDays = upToDay.filter { habit.isCompletionDone(it) }
                .mapTo(mutableSetOf()) { it.dateEpochDay }
            val schedule = habit.toSchedule()
            val analytics = habitRepository.analyticsFor(habit, upToDay, dayDate)
            val miniStart = selectedDay - 27
            val mini = habitRepository.heatmapFor(
                habit,
                allForHabit.filter { it.dateEpochDay in miniStart..selectedDay },
                miniStart,
                selectedDay,
                today
            )
            val itemCount = itemsByHabit[habit.id].orEmpty().size
            val target = when {
                completion != null -> habit.completionTarget(completion)
                habit.trackingMode == HabitTrackingMode.CHECKLIST -> itemCount.coerceAtLeast(1)
                else -> habit.goalCount.coerceAtLeast(1)
            }
            val isDone = completion != null && habit.isCompletionDone(completion)
            val due = HabitStatsCalculator.isScheduled(schedule, selectedDay) &&
                (HabitStatsCalculator.isDue(schedule, selectedDay, doneDays) || isDone)
            HabitCardState(
                habit = habit,
                completion = completion,
                isDone = isDone,
                progress = completion?.progressCount ?: 0,
                target = target,
                analytics = analytics,
                miniHeatmap = mini,
                checklistItemCount = itemCount,
                isDue = due
            )
        }.sortedWith(
            compareByDescending<HabitCardState> { it.isDue && !it.isDone }
                .thenByDescending { it.isDue }
                .thenBy { it.habit.createdAt }
        )

        return HabitsUiState(
            selectedEpochDay = selectedDay,
            todayEpochDay = today,
            cards = cards,
            archived = archived
        )
    }

    fun selectDate(epochDay: Long) {
        _selectedEpochDay.value = epochDay.coerceIn(0L, DateTimeUtils.today().toEpochDay())
    }

    fun goToToday() = selectDate(DateTimeUtils.today().toEpochDay())

    fun shiftWeek(days: Long) = selectDate(_selectedEpochDay.value + days)

    // ---- Logging ----------------------------------------------------------

    fun setTick(card: HabitCardState, done: Boolean) {
        viewModelScope.launch {
            val day = _selectedEpochDay.value
            val wasDone = card.isDone
            habitRepository.setTick(card.habit.id, day, done)
            if (done && !wasDone) celebrate(card.habit.id, day)
        }
    }

    fun logSlip(card: HabitCardState) {
        viewModelScope.launch {
            habitRepository.setSlipped(card.habit.id, _selectedEpochDay.value)
        }
    }

    fun setCount(card: HabitCardState, count: Int) {
        viewModelScope.launch {
            val day = _selectedEpochDay.value
            val wasDone = card.isDone
            habitRepository.setCount(card.habit.id, day, count)
            if (!wasDone && count >= card.target.coerceAtLeast(1)) celebrate(card.habit.id, day)
        }
    }

    fun increment(card: HabitCardState) = setCount(card, (card.progress + 1).coerceAtMost(card.target + 50))

    fun decrement(card: HabitCardState) = setCount(card, (card.progress - 1).coerceAtLeast(0))

    fun archive(habitId: String) {
        viewModelScope.launch { habitRepository.archive(habitId) }
    }

    fun unarchive(habitId: String) {
        viewModelScope.launch { habitRepository.unarchive(habitId) }
    }

    fun delete(habitId: String) {
        viewModelScope.launch { habitRepository.delete(habitId) }
    }

    private suspend fun celebrate(habitId: String, epochDay: Long) {
        val habit = habitRepository.getById(habitId) ?: return
        val completions = habitRepository.getCompletions(habitId, habit.startDateEpochDay, epochDay)
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
