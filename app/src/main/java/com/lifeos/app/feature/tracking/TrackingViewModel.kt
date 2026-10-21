package com.lifeos.app.feature.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.data.AppContainer
import com.lifeos.app.core.data.HabitTodayItem
import com.lifeos.app.core.database.entity.DiaryEntryEntity
import com.lifeos.app.core.database.entity.ExpenseEntity
import com.lifeos.app.core.database.entity.TaskEntity
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.HabitProgress
import com.lifeos.app.core.domain.Money
import com.lifeos.app.core.domain.Priority
import com.lifeos.app.core.domain.Recurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * One ViewModel for the four tracking surfaces (Tasks / Habits / Expenses /
 * Diary). They are separate screens but share a single data source and the same
 * add-form pattern, so one state holder avoids four near-identical classes while
 * keeping each screen's exposed state explicit.
 */
class TrackingViewModel(private val container: AppContainer) : ViewModel() {

    // ------------------------------------------------------------------ tasks

    data class TasksState(
        val open: List<TaskEntity> = emptyList(),
        val done: List<TaskEntity> = emptyList(),
        val showDone: Boolean = false,
        val overdueCount: Int = 0,
    ) {
        val visible: List<TaskEntity> get() = if (showDone) done else open
    }

    private val showDone = MutableStateFlow(false)

    val tasksState: StateFlow<TasksState> = combine(
        container.tracking.observeOpenTasks(),
        container.tracking.observeDoneTasks(),
        showDone,
    ) { open, done, showingDone ->
        val todayIso = Dates.iso(Dates.today())
        TasksState(
            open = open,
            done = done,
            showDone = showingDone,
            overdueCount = open.count { it.dueDate != null && it.dueDate < todayIso },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksState())

    fun toggleShowDone() {
        showDone.value = !showDone.value
    }

    fun addTask(
        title: String,
        dueDate: String?,
        priority: Priority,
        recurrence: Recurrence,
        recurrenceParameter: String?,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            container.tracking.createTask(
                title = title,
                dueDate = dueDate,
                priority = priority,
                recurrence = recurrence,
                recurrenceParameter = recurrenceParameter,
            )
        }
    }

    fun setTaskDone(id: String, done: Boolean) {
        viewModelScope.launch { container.tracking.setTaskDone(id, done) }
    }

    fun trashTask(id: String) {
        viewModelScope.launch { container.tracking.moveToTrashTask(id) }
    }

    // ----------------------------------------------------------------- habits

    data class HabitsState(
        val items: List<HabitTodayItem> = emptyList(),
        val heatmapWeeks: Int = 12,
        val expandedHabitId: String? = null,
        val expandedCells: List<HabitProgress.HeatmapCell> = emptyList(),
        val expandedStreak: HabitProgress.StreakResult = HabitProgress.StreakResult(0, 0),
        val expandedHistory: Set<String> = emptySet(),
    ) {
        val doneCount: Int get() = items.count { it.isDoneToday }
    }

    private val expandedHabit = MutableStateFlow<String?>(null)
    private val expandedCells = MutableStateFlow<List<HabitProgress.HeatmapCell>>(emptyList())
    private val expandedStreak = MutableStateFlow(HabitProgress.StreakResult(0, 0))
    private val expandedHistory = MutableStateFlow<Set<String>>(emptySet())

    val habitsState: StateFlow<HabitsState> = combine(
        container.tracking.observeToday(),
        container.settings.settings,
        expandedHabit,
    ) { items, settings, expanded ->
        HabitsState(
            items = items,
            heatmapWeeks = settings.heatmapWeeks,
            expandedHabitId = expanded,
            expandedCells = expandedCells.value,
            expandedStreak = expandedStreak.value,
            expandedHistory = expandedHistory.value,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsState())

    fun addHabit(name: String, emoji: String, recurrence: Recurrence, parameter: String?, target: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            container.tracking.createHabit(
                name = name,
                emoji = emoji,
                recurrence = recurrence,
                recurrenceParameter = parameter,
                mode = if (target > 1) "count" else "binary",
                targetPerDay = target.coerceAtLeast(1),
            )
        }
    }

    fun toggleHabit(id: String, currentlyDone: Boolean) {
        viewModelScope.launch {
            val today = Dates.today()
            if (currentlyDone) {
                container.tracking.uncompleteHabit(id, today)
            } else {
                container.tracking.completeHabit(id, today, amount = 1)
            }
        }
    }

    fun trashHabit(id: String) {
        viewModelScope.launch { container.tracking.moveToTrashHabit(id) }
    }

    /** Loads the real 12-week heatmap + streak + completion history for one habit. */
    fun expandHabit(id: String, weeks: Int) {
        expandedHabit.value = id
        viewModelScope.launch {
            val habit = container.database.habitDao().getById(id) ?: return@launch
            val completions = container.tracking.completions(id)
            val completedDates = completions.mapNotNull { Dates.parseOrNull(it.date) }
            val byDate = completedDates.toSet()
            val target = container.tracking.habitTarget(habit)
            expandedCells.value = HabitProgress.heatmap(Dates.today(), weeks) { date ->
                val iso = Dates.iso(date)
                val amount = completions.firstOrNull { it.date == iso }?.amount ?: 0
                amount to target
            }
            val recurrence = Recurrence.fromId(habit.recurrence)
            val start = minOf(
                Dates.toLocalDate(habit.createdAt),
                completedDates.minOrNull() ?: Dates.today(),
            )
            expandedStreak.value = HabitProgress.streaks(
                completedDates = byDate,
                scheduledPredicate = { date ->
                    com.lifeos.app.core.domain.RecurrenceRules.occursOn(
                        date = date,
                        startDate = start,
                        recurrence = recurrence,
                        parameter = habit.recurrenceParameter,
                    )
                },
                today = Dates.today(),
            )
            expandedHistory.value = byDate.map { Dates.iso(it) }.toSet()
        }
    }

    fun collapseHabit() {
        expandedHabit.value = null
        expandedCells.value = emptyList()
        expandedHistory.value = emptySet()
    }

    // --------------------------------------------------------------- expenses

    data class ExpensesState(
        val expenses: List<ExpenseEntity> = emptyList(),
        val currencyCode: String = "USD",
        val monthTotalMinor: Long = 0L,
        val categoryTotals: List<Pair<String, Long>> = emptyList(),
    ) {
        fun totalDisplay(): String = com.lifeos.app.core.domain.formatMoney(monthTotalMinor, currencyCode)
    }

    val expensesState: StateFlow<ExpensesState> = combine(
        container.tracking.observeExpenses(),
        container.settings.settings,
    ) { expenses, settings ->
        val today = Dates.today()
        val from = Dates.iso(today.withDayOfMonth(1))
        val to = Dates.iso(today.withDayOfMonth(today.lengthOfMonth()))
        val currency = settings.currencyCode
        val inRange = expenses.filter { it.expenseDate in from..to && it.currencyCode == currency }
        ExpensesState(
            expenses = expenses,
            currencyCode = currency,
            monthTotalMinor = inRange.sumOf { it.amountMinor },
            categoryTotals = inRange.groupBy { it.category }
                .map { (category, rows) -> category to rows.sumOf { it.amountMinor } }
                .sortedByDescending { it.second },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesState())

    fun addExpense(amountText: String, category: String, note: String, paymentMethod: String, date: LocalDate) {
        if (amountText.isBlank()) return
        viewModelScope.launch {
            val code = currentCurrency()
            val money = Money.parse(amountText, code)
            if (money.minorUnits <= 0) return@launch
            container.tracking.addExpense(
                money = money,
                category = category.ifBlank { "Other" },
                note = note,
                paymentMethod = paymentMethod,
                date = date,
            )
        }
    }

    private suspend fun currentCurrency(): String =
        container.settings.settings.first().currencyCode

    fun trashExpense(id: String) {
        viewModelScope.launch { container.tracking.moveToTrashExpense(id) }
    }

    /** Category list for the picker: previously used first, then defaults. */
    fun knownCategories(): List<String> {
        val used = expensesState.value.expenses.map { it.category }.distinct().sorted()
        return (used + DEFAULT_CATEGORIES).distinct()
    }

    // ------------------------------------------------------------------ diary

    data class DiaryState(
        val selectedDate: LocalDate = Dates.today(),
        val entry: DiaryEntryEntity? = null,
        val all: List<DiaryEntryEntity> = emptyList(),
        val draft: String = "",
        val draftTitle: String = "",
        val draftMood: String? = null,
        val loaded: Boolean = false,
    ) {
        val hasEntry: Boolean get() = entry != null
    }

    private val selectedDate = MutableStateFlow(Dates.today())
    private val diaryDraft = MutableStateFlow(DiaryDraft())

    val diaryState: StateFlow<DiaryState> = combine(
        selectedDate,
        container.tracking.observeDiary(),
        diaryDraft,
    ) { date, all, draft ->
        val iso = Dates.iso(date)
        DiaryState(
            selectedDate = date,
            entry = all.firstOrNull { it.diaryDate == iso },
            all = all,
            draft = draft.body,
            draftTitle = draft.title,
            draftMood = draft.mood,
            loaded = draft.loadedFor == iso,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryState())

    fun selectDiaryDate(date: LocalDate) {
        selectedDate.value = date
        diaryDraft.value = DiaryDraft(loadedFor = null)
    }

    fun setDiaryDraft(title: String? = null, body: String? = null, mood: String? = null, clearMood: Boolean = false) {
        val current = diaryDraft.value
        diaryDraft.value = current.copy(
            title = title ?: current.title,
            body = body ?: current.body,
            mood = if (clearMood) null else (mood ?: current.mood),
        )
    }

    fun saveDiary() {
        val draft = diaryDraft.value
        val date = selectedDate.value
        if (draft.body.isBlank() && draft.title.isBlank()) return
        viewModelScope.launch {
            container.tracking.writeDiary(
                date = date,
                title = draft.title,
                body = draft.body,
                mood = draft.mood,
            )
            diaryDraft.value = DiaryDraft(loadedFor = Dates.iso(date))
        }
    }

    fun trashDiary(id: String) {
        viewModelScope.launch { container.tracking.moveToTrashDiary(id) }
    }

    private data class DiaryDraft(
        val title: String = "",
        val body: String = "",
        val mood: String? = null,
        val loadedFor: String? = null,
    )

    private companion object {
        val DEFAULT_CATEGORIES = listOf(
            "Food", "Transport", "Groceries", "Health", "Bills", "Rent",
            "Shopping", "Entertainment", "Education", "Family", "Other",
        )
    }
}
