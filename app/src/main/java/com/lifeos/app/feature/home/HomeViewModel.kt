package com.lifeos.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.ai.AiConsent
import com.lifeos.app.core.ai.InsightRequest
import com.lifeos.app.core.ai.SuggestionRequest
import com.lifeos.app.core.data.AppContainer
import com.lifeos.app.core.domain.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime

/**
 * Home dashboard state — an honest projection of the database:
 * what is open, what is done today, what recently happened, and (only when the
 * user has opted into level 3) one rule-based suggestion.
 *
 * No placeholder/ demo values exist here: on a fresh install [HomeState.isEmpty]
 * is true and the UI shows the real empty state.
 */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    data class HomeState(
        val today: LocalDate = Dates.today(),
        val greeting: String = "",
        val openTasks: Int = 0,
        val overdueTasks: Int = 0,
        val habitsTotal: Int = 0,
        val habitsDone: Int = 0,
        val diaryWrittenToday: Boolean = false,
        val notesCount: Int = 0,
        val recentEvents: List<com.lifeos.app.core.database.entity.LifeEventEntity> = emptyList(),
        val suggestion: String? = null,
        val aiLevel: Int = AiConsent.OFF,
        val isEmpty: Boolean = true,
    ) {
        val habitPercent: Int
            get() = if (habitsTotal == 0) 0 else (habitsDone * 100) / habitsTotal
    }

    private data class Head(
        val openTasks: List<com.lifeos.app.core.database.entity.TaskEntity>,
        val habits: List<com.lifeos.app.core.data.HabitTodayItem>,
        val events: List<com.lifeos.app.core.database.entity.LifeEventEntity>,
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeState> = combine(
        combine(
            container.tracking.observeOpenTasks(),
            container.tracking.observeToday(),
            container.timeline.observeTimeline(limit = 8),
        ) { openTasks, habits, events -> Head(openTasks, habits, events) },
        container.settings.settings,
        container.notes.observeNotes(),
        container.tracking.observeDiary().map { entries ->
            entries.any { it.diaryDate == Dates.iso(Dates.today()) }
        },
    ) { head, settings, notes, diaryToday ->
        val openTasks = head.openTasks
        val habits = head.habits
        val events = head.events

        val today = Dates.today()
        val todayIso = Dates.iso(today)
        val overdue = openTasks.count { task -> task.dueDate != null && task.dueDate < todayIso }
        HomeState(
            today = today,
            greeting = greetingFor(),
            openTasks = openTasks.size,
            overdueTasks = overdue,
            habitsTotal = habits.size,
            habitsDone = habits.count { it.isDoneToday },
            diaryWrittenToday = diaryToday,
            notesCount = notes.size,
            recentEvents = events.take(5),
            aiLevel = settings.aiConsentLevel,
            suggestion = if (settings.aiConsentLevel >= AiConsent.SUGGESTIONS) {
                container.aiProvider.suggestion(
                    SuggestionRequest(overdueTasks = overdue, daysSinceDiary = 0),
                )
            } else {
                null
            },
            isEmpty = openTasks.isEmpty() && habits.isEmpty() && notes.isEmpty() && events.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private fun greetingFor(): String {
        val part = when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Hello"
        }
        return part
    }

    /** Insight string for the Insights hand-off; null when AI is off. */
    fun insightText(expensesByCategory: List<Pair<String, Long>>, currency: String): String? {
        val current = state.value
        if (current.aiLevel < AiConsent.SUMMARIES) return null
        return container.aiProvider.insight(
            InsightRequest(
                expensesByCategory = expensesByCategory,
                currencyCode = currency,
                openTasks = current.openTasks,
                overdueTasks = current.overdueTasks,
                habitsActive = current.habitsTotal,
                habitsCompletedToday = current.habitsDone,
            ),
        )
    }
}
