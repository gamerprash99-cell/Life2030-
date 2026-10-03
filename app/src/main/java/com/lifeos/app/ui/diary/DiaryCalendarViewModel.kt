package com.lifeos.app.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.domain.usecase.DiaryMonth
import com.lifeos.app.domain.usecase.GetDiaryCalendarUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** What the calendar renders: the visible month, the grid, and the selected day. */
data class DiaryCalendarState(
    val year: Int,
    val month: Int,
    val daysWithEntries: Set<Long> = emptySet(),
    /** Dominant mood per day, for the dot's colour. Absent for a day with no mood. */
    val moodByDay: Map<Long, String> = emptyMap(),
    val entriesForSelectedDay: List<DiaryEntity> = emptyList(),
    val selectedDay: Long = 0L,
    val isLoading: Boolean = true,
    val todayEpochDay: Long = 0L
) {
    val monthLabel: String get() = YearMonth.of(year, month).month.getDisplayName(
        java.time.format.TextStyle.FULL, java.util.Locale.getDefault()
    )

    /**
     * The 42 cells of the grid, padded to whole weeks so every row has seven
     * days and the columns never jump between months.
     *
     * Padded cells are null, and are rendered blank rather than as the previous
     * or next month's days — a calendar that greys out a *real* date from
     * another month invites a tap that navigates somewhere unexpected.
     */
    val gridCells: List<Long?> by lazy {
        val first = LocalDate.of(year, month, 1)
        // Monday-first, matching the existing diary date strip.
        val leading = (first.dayOfWeek.value - 1)
        val start = first.minusDays(leading.toLong())
        (0 until 42).map { offset ->
            val day = start.plusDays(offset.toLong())
            if (day.monthValue == month && day.year == year) day.toEpochDay() else null
        }
    }

    val isCurrentMonth: Boolean
        get() = year == LocalDate.ofEpochDay(todayEpochDay).year &&
            month == LocalDate.ofEpochDay(todayEpochDay).monthValue

    /** How many entries the selected day holds. */
    val selectedDayCount: Int get() = entriesForSelectedDay.size
}

/**
 * Drives the month calendar.
 *
 * Selection defaults to today rather than the first of the month: someone
 * opening "October" on the 14th is asking about now, and starting them on the
 * 1st with an empty day panel reads as an empty journal.
 *
 * The month cannot be navigated past today. A future month has no entries by
 * construction — the composer clamps dates to the past — so offering it would
 * be a screen that can only ever show emptiness.
 */
class DiaryCalendarViewModel(
    private val getCalendar: GetDiaryCalendarUseCase,
    private val today: () -> Long = { DateTimeUtils.today().toEpochDay() }
) : ViewModel() {

    /** Guards against a slow month read landing after the user has paged on. */
    private var loadToken = 0L

    /**
     * The opening month comes from the *injected* clock, not [DateTimeUtils.today].
     * Reading the system date here while [today] supplied everything else meant
     * the grid opened on one month and the "is this the current month" check and
     * the forward-page guard used another — so under any injected clock the
     * calendar started somewhere the rest of the class disagreed with.
     */
    private val _state = MutableStateFlow(
        DateTimeUtils.today().let { LocalDate.ofEpochDay(today()) }.let { date ->
            DiaryCalendarState(
                year = date.year,
                month = date.monthValue,
                todayEpochDay = date.toEpochDay(),
                selectedDay = date.toEpochDay()
            )
        }
    )
    val state: StateFlow<DiaryCalendarState> = _state.asStateFlow()

    init {
        load()
    }

    fun selectDay(epochDay: Long) {
        _state.value = _state.value.copy(selectedDay = epochDay)
        load()
    }

    fun nextMonth() {
        val current = YearMonth.of(_state.value.year, _state.value.month)
        if (current >= YearMonth.from(LocalDate.ofEpochDay(today()))) return
        _state.value = _state.value.copy(year = current.plusMonths(1).year, month = current.plusMonths(1).monthValue)
        load()
    }

    /** A no-op once the calendar is showing the current month. */
    fun previousMonth() {
        val current = YearMonth.of(_state.value.year, _state.value.month)
        val previous = current.minusMonths(1)
        _state.value = _state.value.copy(year = previous.year, month = previous.monthValue)
        load()
    }

    fun jumpToToday() {
        val date = LocalDate.ofEpochDay(today())
        _state.value = _state.value.copy(
            year = date.year,
            month = date.monthValue,
            selectedDay = date.toEpochDay()
        )
        load()
    }

    private fun load() {
        val token = ++loadToken
        val current = _state.value
        _state.value = current.copy(isLoading = true)
        viewModelScope.launch {
            val month: DiaryMonth = getCalendar(current.year, current.month)
            // Guard against a slow month read landing after the user has already
            // paged somewhere else, which would repaint the grid they are
            // looking at with the month they just left.
            if (token != loadToken) return@launch
            _state.value = _state.value.copy(
                daysWithEntries = month.daysWithEntries,
                moodByDay = dominantMoods(month.entries),
                entriesForSelectedDay = month.entriesForDay(current.selectedDay),
                isLoading = false
            )
        }
    }

    /**
     * One mood per day for the dots.
     *
     * The rule matches `MoodAnalyzer.dominantMoodByDay` — most frequent wins,
     * ties broken toward the positive reading — so a day's dot and the same day's
     * dot on the insights chart can never disagree about what that day was.
     */
    private fun dominantMoods(entries: List<DiaryEntity>): Map<Long, String> =
        entries.filter { !it.mood.isNullOrBlank() }
            .groupBy { it.dateEpochDay }
            .mapValues { (_, dayEntries) ->
                dayEntries.groupingBy { it.mood!! }.eachCount()
                    .entries
                    .sortedWith(
                        compareByDescending<Map.Entry<String, Int>> { it.value }
                            .thenByDescending { DiaryMoods.valenceOf(it.key) == com.lifeos.app.domain.model.MoodValence.POSITIVE }
                            .thenBy { it.key }
                    )
                    .first().key
            }
}

/** Entries for one day, newest first — the same order the diary list uses. */
private fun DiaryMonth.entriesForDay(epochDay: Long): List<DiaryEntity> =
    entries.filter { it.dateEpochDay == epochDay }.sortedByDescending { it.timeMinutes }