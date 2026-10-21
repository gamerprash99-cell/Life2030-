package com.lifeos.app.feature.life

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.data.AppContainer
import com.lifeos.app.core.data.TrashContent
import com.lifeos.app.core.database.entity.LifeEventEntity
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.SearchGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Life section: Timeline, Calendar, Trash, Settings entry points.
 * Timeline and Trash are live database projections; the Calendar shows real
 * activity density per day (no synthetic markers).
 */
class LifeViewModel(private val container: AppContainer) : ViewModel() {

    // ------------------------------------------------------------- timeline

    data class TimelineState(
        val events: List<LifeEventEntity> = emptyList(),
        val filter: String? = null,
        val dayCounts: Map<String, Int> = emptyMap(),
        val total: Int = 0,
    ) {
        val visible: List<LifeEventEntity>
            get() = filter?.let { type -> events.filter { it.type == type } } ?: events
    }

    private val eventFilter = MutableStateFlow<String?>(null)

    val timelineState: StateFlow<TimelineState> = combine(
        container.timeline.observeTimeline(),
        eventFilter,
    ) { events, filter ->
        TimelineState(
            events = events,
            filter = filter,
            dayCounts = events.groupingBy { it.occurredOn }.eachCount(),
            total = events.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineState())

    fun setEventFilter(type: String?) {
        eventFilter.value = type
    }

    // ------------------------------------------------------------ calendar

    data class CalendarState(
        val month: java.time.YearMonth = java.time.YearMonth.now(),
        val dayCounts: Map<String, Int> = emptyMap(),
        val selectedDate: String? = null,
        val selectedEvents: List<LifeEventEntity> = emptyList(),
    )

    private val month = MutableStateFlow(java.time.YearMonth.now())
    private val selectedDate = MutableStateFlow<String?>(null)

    val calendarState: StateFlow<CalendarState> = combine(
        container.timeline.observeTimeline(),
        month,
        selectedDate,
    ) { events, currentMonth, selected ->
        val start = Dates.iso(currentMonth.atDay(1))
        val end = Dates.iso(currentMonth.atEndOfMonth())
        val inMonth = events.filter { it.occurredOn in start..end }
        CalendarState(
            month = currentMonth,
            dayCounts = inMonth.groupingBy { it.occurredOn }.eachCount(),
            selectedDate = selected,
            selectedEvents = selected?.let { day -> events.filter { it.occurredOn == day } }
                ?: emptyList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarState())

    fun selectMonth(value: java.time.YearMonth) {
        month.value = value
        selectedDate.value = null
    }

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
        selectedDate.value = null
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
        selectedDate.value = null
    }

    fun selectDay(iso: String) {
        selectedDate.value = if (selectedDate.value == iso) null else iso
    }

    // --------------------------------------------------------------- trash

    val trashState: StateFlow<TrashContent> = container.timeline.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrashContent(
            notes = emptyList(),
            tasks = emptyList(),
            habits = emptyList(),
            expenses = emptyList(),
            diary = emptyList(),
            captures = emptyList(),
            totalCount = 0,
        ))

    fun restore(group: SearchGroup, id: String) {
        viewModelScope.launch {
            when (group) {
                SearchGroup.NOTES -> container.notes.restore(id)
                SearchGroup.TASKS -> container.tracking.restoreTask(id)
                SearchGroup.HABITS -> container.tracking.restoreHabit(id)
                SearchGroup.EXPENSES -> container.tracking.restoreExpense(id)
                SearchGroup.DIARY -> container.tracking.restoreDiary(id)
                SearchGroup.CAPTURES -> container.captures.restore(id)
                SearchGroup.TIMELINE -> Unit
            }
        }
    }

    fun deleteForever(group: SearchGroup, id: String) {
        viewModelScope.launch {
            when (group) {
                SearchGroup.NOTES -> container.notes.deleteForever(id)
                SearchGroup.TASKS -> container.tracking.deleteTaskForever(id)
                SearchGroup.HABITS -> container.tracking.deleteHabitForever(id)
                SearchGroup.EXPENSES -> container.tracking.deleteExpenseForever(id)
                SearchGroup.DIARY -> container.tracking.deleteDiaryForever(id)
                SearchGroup.CAPTURES -> container.captures.deleteForever(id)
                SearchGroup.TIMELINE -> Unit
            }
        }
    }
}
