package com.lifeos.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.ai.AiConsent
import com.lifeos.app.core.ai.InsightRequest
import com.lifeos.app.core.data.AppContainer
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.Mood
import com.lifeos.app.core.domain.formatMoney
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Insights: real aggregates over the user's own rows only.
 *
 * Period selection is 7 / 30 / 90 days; every figure is computed from stored data,
 * and the "summary" panel only appears when the user has granted at least level 1
 * AI consent (spec: permission-scoped AI).
 */
class InsightsViewModel(private val container: AppContainer) : ViewModel() {

    enum class Period(val days: Long, val label: String) {
        WEEK(7, "7 days"),
        MONTH(30, "30 days"),
        QUARTER(90, "90 days"),
    }

    data class Bar(val label: String, val value: Long)

    data class InsightsState(
        val period: Period = Period.WEEK,
        val totalSpentMinor: Long = 0L,
        val currencyCode: String = "USD",
        val spendingByCategory: List<Bar> = emptyList(),
        val spendingByDay: List<Bar> = emptyList(),
        val tasksCompleted: Int = 0,
        val tasksOpen: Int = 0,
        val habitsActive: Int = 0,
        val habitCompletionRate: Int = 0,
        val moodSeries: List<Pair<String, Int>> = emptyList(),
        val averageMood: Double = 0.0,
        val diaryEntries: Int = 0,
        val captures: Int = 0,
        val timelineEvents: Int = 0,
        val summary: String? = null,
        val aiLevel: Int = AiConsent.OFF,
        val hasAnyData: Boolean = false,
    ) {
        fun totalDisplay(): String = formatMoney(totalSpentMinor, currencyCode)
    }

    private val period = kotlinx.coroutines.flow.MutableStateFlow(Period.WEEK)

    val state: StateFlow<InsightsState> = combine(
        container.tracking.observeExpenses(),
        container.tracking.observeOpenTasks(),
        container.tracking.observeHabits(),
        container.tracking.observeDiary(),
        container.captures.observeCaptures(),
        container.timeline.observeTimeline(limit = 2000),
        container.settings.settings,
        period,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val expenses = values[0] as List<com.lifeos.app.core.database.entity.ExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val openTasks = values[1] as List<com.lifeos.app.core.database.entity.TaskEntity>
        @Suppress("UNCHECKED_CAST")
        val habits = values[2] as List<com.lifeos.app.core.database.entity.HabitEntity>
        @Suppress("UNCHECKED_CAST")
        val diary = values[3] as List<com.lifeos.app.core.database.entity.DiaryEntryEntity>
        @Suppress("UNCHECKED_CAST")
        val captures = values[4] as List<com.lifeos.app.core.database.entity.CaptureEntity>
        @Suppress("UNCHECKED_CAST")
        val events = values[5] as List<com.lifeos.app.core.database.entity.LifeEventEntity>
        val settings = values[6] as com.lifeos.app.core.data.AppSettings
        val selected = values[7] as Period

        val today = Dates.today()
        val start = today.minusDays(selected.days - 1)
        val from = Dates.iso(start)
        val to = Dates.iso(today)
        val currency = settings.currencyCode

        val periodExpenses = expenses.filter { it.expenseDate in from..to && it.currencyCode == currency }
        val byCategory = periodExpenses.groupBy { it.category }
            .map { (category, rows) -> Bar(category, rows.sumOf { it.amountMinor }) }
            .sortedByDescending { it.value }
        val byDay = periodExpenses.groupBy { it.expenseDate }
            .map { (day, rows) -> Bar(day, rows.sumOf { it.amountMinor }) }
            .sortedBy { it.label }

        val periodDiary = diary.filter { it.diaryDate in from..to }
        val moods = periodDiary.mapNotNull { entry ->
            entry.mood?.let { Mood.fromCode(it).score }
        }
        val moodSeries = periodDiary
            .filter { it.mood != null }
            .sortedBy { it.diaryDate }
            .map { it.diaryDate to Mood.fromCode(it.mood).score }

        val periodEvents = events.filter { it.occurredOn in from..to }
        val completedTasks = periodEvents.count {
            it.type == com.lifeos.app.core.domain.LifeEventType.TASK_COMPLETED.code
        }
        val habitCompletions = periodEvents.count {
            it.type == com.lifeos.app.core.domain.LifeEventType.HABIT_COMPLETED.code
        }
        val habitScheduledDays = habits.size * selected.days
        val completionRate = if (habitScheduledDays == 0L) {
            0
        } else {
            (habitCompletions * 100.0 / habitScheduledDays).roundToInt().coerceIn(0, 100)
        }

        val hasAnyData = expenses.isNotEmpty() || openTasks.isNotEmpty() || habits.isNotEmpty() ||
            diary.isNotEmpty() || captures.isNotEmpty() || events.isNotEmpty()

        val summary = if (settings.aiConsentLevel >= AiConsent.SUMMARIES) {
            container.aiProvider.insight(
                InsightRequest(
                    expensesByCategory = byCategory.map { it.label to it.value },
                    currencyCode = currency,
                    openTasks = openTasks.size,
                    overdueTasks = openTasks.count { it.dueDate != null && it.dueDate < to },
                    habitsActive = habits.size,
                    habitsCompletedToday = 0,
                    diaryEntriesLast7Days = periodDiary.size,
                    eventsLast7Days = periodEvents.size,
                ),
            )
        } else {
            null
        }

        InsightsState(
            period = selected,
            totalSpentMinor = periodExpenses.sumOf { it.amountMinor },
            currencyCode = currency,
            spendingByCategory = byCategory,
            spendingByDay = byDay,
            tasksCompleted = completedTasks,
            tasksOpen = openTasks.size,
            habitsActive = habits.size,
            habitCompletionRate = completionRate,
            moodSeries = moodSeries,
            averageMood = if (moods.isEmpty()) 0.0 else moods.average(),
            diaryEntries = periodDiary.size,
            captures = captures.count { it.occurredOn in from..to },
            timelineEvents = periodEvents.size,
            summary = summary,
            aiLevel = settings.aiConsentLevel,
            hasAnyData = hasAnyData,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsState())

    fun selectPeriod(value: Period) {
        period.value = value
    }

    fun dayLabel(iso: String): String = Dates.parseOrNull(iso)?.let { Dates.human(it) } ?: iso

    fun todaySummaryHint(): String =
        "Insights are computed from your own data on this device (${LocalDate.now()})."
}
