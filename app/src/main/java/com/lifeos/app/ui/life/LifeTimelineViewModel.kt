package com.lifeos.app.ui.life

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.usecase.BuildLifeTimelineUseCase
import com.lifeos.app.domain.usecase.TimelineDayGrouping
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * State of the Life timeline for one frame.
 *
 * [sections] is already grouped and ordered (newest day first, newest entry
 * first inside a day) so the list can render straight from it and a
 * recomposition never re-sorts anything.
 */
data class LifeTimelineUiState(
    val loading: Boolean = true,
    val loadingOlder: Boolean = false,
    val sections: List<TimelineDaySection> = emptyList(),
    val query: String = "",
    val hasMoreHistory: Boolean = true,
    val windowDays: Long = BuildLifeTimelineUseCase.DEFAULT_WINDOW_DAYS,
    val todayEpochDay: Long = 0L
) {
    /** Total rows currently loaded, before any search filter. */
    val totalEntries: Int get() = sections.sumOf { it.items.size }

    /** True when the window holds nothing at all — a brand-new LifeOS. */
    val isEmpty: Boolean get() = !loading && sections.isEmpty()

    /** True when a search is filtering everything out. */
    val isFilteredEmpty: Boolean get() = !loading && sections.isEmpty() && query.isNotBlank()
}

/** One dated section of the timeline, labelled for display. */
data class TimelineDaySection(
    val epochDay: Long,
    val label: String,
    val items: List<TimelineItem>
)

/**
 * Owns the Life tab's memory timeline.
 *
 * The timeline is a read-only aggregation of records that already exist (see
 * [BuildLifeTimelineUseCase]) — nothing is stored twice and the ViewModel never
 * mutates user data. It only decides how far back to read: the first page is a
 * 90-day window, and [loadOlder] widens it by the same span, one page at a time.
 *
 * Every load assigns a new [Job]; a newer load cancels the older one so a fast
 * "load older / jump to date" pair can never race two results into the state.
 */
class LifeTimelineViewModel(
    private val buildTimeline: BuildLifeTimelineUseCase,
    private val today: LocalDate = DateTimeUtils.today()
) : ViewModel() {

    private val _state = MutableStateFlow(
        LifeTimelineUiState(todayEpochDay = today.toEpochDay())
    )
    val state: StateFlow<LifeTimelineUiState> = _state.asStateFlow()

    private var loaded: List<TimelineItem> = emptyList()
    private var loadJob: Job? = null

    init {
        load()
    }

    /** Re-reads the whole loaded window. Called on first show and on resume. */
    fun refresh() = load()

    /**
     * Widens the window by another page of history. No-op while a load is in
     * flight so double taps cannot stack pages.
     */
    fun loadOlder() {
        val current = _state.value
        if (current.loadingOlder || current.loading || !current.hasMoreHistory) return
        val nextWindow = current.windowDays + BuildLifeTimelineUseCase.DEFAULT_WINDOW_DAYS
        load(windowDays = nextWindow, older = true)
    }

    /**
     * Guarantees [epochDay] is inside the loaded window, so "jump to date" can
     * scroll to a day that is older than the current page without a second
     * screen: the window grows until it covers the requested day.
     */
    fun ensureDayLoaded(epochDay: Long) {
        val current = _state.value
        val oldestLoaded = current.todayEpochDay - current.windowDays
        if (epochDay < oldestLoaded) {
            val needed = (current.todayEpochDay - epochDay) + BuildLifeTimelineUseCase.DEFAULT_WINDOW_DAYS
            load(windowDays = needed, older = true)
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, sections = sectionsFor(loaded, query, it.todayEpochDay)) }
    }

    fun clearQuery() = onQueryChange("")

    private fun load(
        windowDays: Long = _state.value.windowDays,
        older: Boolean = false
    ) {
        loadJob?.cancel()
        val from = today.toEpochDay() - windowDays
        _state.update {
            it.copy(
                loading = !older && it.sections.isEmpty(),
                loadingOlder = older,
                windowDays = windowDays
            )
        }
        loadJob = viewModelScope.launch {
            val items = buildTimeline(fromEpochDay = from, toEpochDay = today.toEpochDay())
            loaded = items
            _state.update { state ->
                state.copy(
                    loading = false,
                    loadingOlder = false,
                    sections = sectionsFor(items, state.query, state.todayEpochDay),
                    // Only offered while the window can still grow and there is
                    // history to widen into. A brand-new LifeOS never shows a
                    // "load older" affordance over an empty timeline.
                    hasMoreHistory = state.windowDays < MAX_WINDOW_DAYS && items.isNotEmpty()
                )
            }
        }
    }

    private fun sectionsFor(
        items: List<TimelineItem>,
        query: String,
        todayEpochDay: Long
    ): List<TimelineDaySection> {
        val needle = query.trim()
        val filtered = if (needle.isEmpty()) items else items.filter { it.matches(needle) }
        return TimelineDayGrouping.group(filtered).map { section ->
            TimelineDaySection(
                epochDay = section.epochDay,
                label = TimelineDayGrouping.labelFor(section.epochDay, todayEpochDay),
                items = section.items
            )
        }
    }

    private companion object {
        /** Five years back is the practical floor; nobody journals past it. */
        const val MAX_WINDOW_DAYS = 365L * 5
    }
}

/**
 * Client-side match over the rows the window already holds. This is a filter
 * over real loaded records — deliberately not a second search index, and not a
 * claim to search everything ever written (the Global search screen does that
 * across every module).
 */
private fun TimelineItem.matches(query: String): Boolean {
    val haystack = listOfNotNull(title, subtitle, moodOrCategory, statusLabel)
        .joinToString(" ")
    return haystack.contains(query, ignoreCase = true)
}
