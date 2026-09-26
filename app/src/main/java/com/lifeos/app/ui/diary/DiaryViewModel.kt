package com.lifeos.app.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.domain.model.DiaryAttachments
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Diary screen state. Everything flows from `DiaryRepository.observeAll()` —
 * the day list, the date filter and the composer are all Room-backed, no
 * simulated data.
 *
 * The screen is day-scoped, matching the reference: a header naming the real
 * selected day, a week strip to move between days, and that day's entries
 * listed in time order.
 */
class DiaryViewModel(
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    val entries: StateFlow<List<DiaryEntity>> = diaryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The day the user is looking at. Seeded from the real current date so the
     * screen opens on today rather than on a hardcoded day.
     */
    private val _selectedDay = MutableStateFlow(DateTimeUtils.today().toEpochDay())
    val selectedDay: StateFlow<Long> = _selectedDay

    /** True while the very first database read is still in flight. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * The selected day's entries, newest time first — the day timeline.
     *
     * Favourites are surfaced separately so the reference's favourite star can
     * be rendered without re-querying the database per row.
     */
    val dayEntries: StateFlow<List<DiaryEntity>> = combine(entries, _selectedDay) { all, day ->
        all.filter { it.dateEpochDay == day }
            .sortedByDescending { it.timeMinutes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Entries flagged as favourites, newest first. */
    val favorites: StateFlow<List<DiaryEntity>> = entries
        .map { all -> all.filter { it.isFavorite }.sortedByDescending { it.dateEpochDay * 1440 + it.timeMinutes } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The seven days of the week containing [selectedDay], oldest first. */
    val weekDays: StateFlow<List<LocalDate>> = _selectedDay
        .map { epochDay ->
            val date = DateTimeUtils.epochDayToLocalDate(epochDay)
            val startOfWeek = date.minusDays((date.dayOfWeek.value - 1).toLong())
            (0L..6L).map { startOfWeek.plusDays(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Days that actually contain at least one entry, for the strip's dot marker. */
    val daysWithEntries: StateFlow<Set<Long>> = entries
        .map { all -> all.map { it.dateEpochDay }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _entryToDelete = MutableStateFlow<DiaryEntity?>(null)
    val entryToDelete: StateFlow<DiaryEntity?> = _entryToDelete

    init {
        // One read of the flow is enough to know the database has answered.
        viewModelScope.launch {
            entries.collect { _isLoading.value = false }
        }
    }

    fun selectDay(epochDay: Long) {
        _selectedDay.value = epochDay
    }

    fun requestDelete(entry: DiaryEntity) { _entryToDelete.value = entry }
    fun dismissDelete() { _entryToDelete.value = null }

    fun deleteEntry(id: String) {
        _entryToDelete.value = null
        viewModelScope.launch { diaryRepository.delete(id) }
    }

    /** Attachments for one entry, decoded from the persisted JSON column. */
    fun attachmentsOf(entry: DiaryEntity): List<com.lifeos.app.domain.model.DiaryAttachment> =
        DiaryAttachments.decode(entry.attachmentsJson)
}
