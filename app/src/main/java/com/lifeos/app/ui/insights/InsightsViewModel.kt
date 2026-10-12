package com.lifeos.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The period the dashboard is summarizing. */
enum class InsightsPeriod(val label: String) { WEEK("Week"), MONTH("Month"), YEAR("Year") }

/** Everything the Insights dashboard shows — all of it computed from stored rows. */
data class InsightsUiState(
    val loading: Boolean = true,
    val period: InsightsPeriod = InsightsPeriod.MONTH,
    val periodLabel: String = "",
    val tasksCompleted: Int = 0,
    val habitCompletions: Int = 0,
    val habitsTracked: Int = 0,
    val spendTotal: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val diaryEntries: Int = 0,
    val captures: Int = 0,
    val topCategory: String? = null,
    val topCategoryAmount: Double = 0.0
)

/**
 * Backs Insights. Every number is read from the local database through the same
 * repositories the rest of the app writes to — there is no analytics SDK, no
 * network and no fabricated figure. Switching the period recomputes over the
 * matching date range.
 */
class InsightsViewModel(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val expenseRepository: ExpenseRepository,
    private val diaryRepository: DiaryRepository,
    private val captureRepository: CaptureRepository
) : ViewModel() {

    private val period = MutableStateFlow(InsightsPeriod.MONTH)
    private val reload = MutableStateFlow(0)

    val state: StateFlow<InsightsUiState> = combine(period, reload) { p, _ -> p }
        .map { compute(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    fun setPeriod(next: InsightsPeriod) { period.value = next }
    fun refresh() { reload.value += 1 }

    private suspend fun compute(period: InsightsPeriod): InsightsUiState {
        val today = LocalDate.now()
        val start = when (period) {
            InsightsPeriod.WEEK -> today.minusDays(6)
            InsightsPeriod.MONTH -> today.withDayOfMonth(1)
            InsightsPeriod.YEAR -> today.withDayOfYear(1)
        }
        val startEpoch = start.toEpochDay()
        val endEpoch = today.toEpochDay()
        val zone = ZoneId.systemDefault()
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val days = (endEpoch - startEpoch + 1).coerceAtLeast(1)

        val tasksCompleted = runCatching { taskRepository.getCompletedBetween(startMillis, endMillis).size }.getOrDefault(0)
        val expenses = runCatching { expenseRepository.observeInRange(startEpoch, endEpoch).first() }.getOrDefault(emptyList())
        val spend = expenses.sumOf { it.amount }
        val top = expenses.groupBy { it.category }
            .maxByOrNull { entry -> entry.value.sumOf { it.amount } }
        val diary = runCatching { diaryRepository.getAllInRange(startEpoch, endEpoch).size }.getOrDefault(0)
        val captures = runCatching { captureRepository.getAllInRange(startMillis, endMillis).size }.getOrDefault(0)
        val habits = runCatching { habitRepository.observeAll().first().size }.getOrDefault(0)
        val completions = runCatching { habitRepository.observeAllInRange(startEpoch, endEpoch).first().size }.getOrDefault(0)

        return InsightsUiState(
            loading = false,
            period = period,
            periodLabel = periodLabelFor(period, start, today),
            tasksCompleted = tasksCompleted,
            habitCompletions = completions,
            habitsTracked = habits,
            spendTotal = spend,
            dailyAverage = spend / days,
            diaryEntries = diary,
            captures = captures,
            topCategory = top?.key,
            topCategoryAmount = top?.value?.sumOf { it.amount } ?: 0.0
        )
    }

    private fun periodLabelFor(period: InsightsPeriod, start: LocalDate, today: LocalDate): String = when (period) {
        InsightsPeriod.WEEK -> "Last 7 days"
        InsightsPeriod.MONTH -> start.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
        InsightsPeriod.YEAR -> today.year.toString()
    }
}
