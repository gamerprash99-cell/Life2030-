package com.lifeos.app.ui.diary.new

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

data class DiaryCalendarState(
    val year: Int,
    val month: Int,
    val daysWithEntries: Set<Long> = emptySet(),
    val moodByDay: Map<Long, String> = emptyMap(),
    val entriesForSelectedDay: List<DiaryEntity> = emptyList(),
    val selectedDay: Long = 0L,
    val isLoading: Boolean = true,
    val todayEpochDay: Long = 0L
) {
    val monthLabel: String get() = YearMonth.of(year, month).month.getDisplayName(
        java.time.format.TextStyle.FULL, java.util.Locale.getDefault()
    )

    val gridCells: List<Long?> by lazy {
        val first = LocalDate.of(year, month, 1)
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

    val selectedDayCount: Int get() = entriesForSelectedDay.size
}

class DiaryCalendarViewModel(
    private val getCalendar: GetDiaryCalendarUseCase,
    private val today: () -> Long = { DateTimeUtils.today().toEpochDay() }
) : ViewModel() {

    private var loadToken = 0L

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
            if (token != loadToken) return@launch
            _state.value = _state.value.copy(
                daysWithEntries = month.daysWithEntries,
                moodByDay = dominantMoods(month.entries),
                entriesForSelectedDay = month.entriesForDay(current.selectedDay),
                isLoading = false
            )
        }
    }

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

private fun DiaryMonth.entriesForDay(epochDay: Long): List<DiaryEntity> =
    entries.filter { it.dateEpochDay == epochDay }.sortedByDescending { it.timeMinutes }
