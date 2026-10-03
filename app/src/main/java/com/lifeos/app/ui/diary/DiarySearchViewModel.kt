package com.lifeos.app.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.domain.usecase.SearchDiaryEntriesUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * What the search screen renders.
 *
 * [isSearching] and [results] are separate rather than one nullable list,
 * because "searching" and "found nothing" are different answers and collapsing
 * them makes an empty result flash past before the query settles.
 */
data class DiarySearchState(
    val query: String = "",
    val moodKey: String? = null,
    val results: List<DiaryEntity> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null
) {
    /** True only once a real query has completed, so the empty state is honest. */
    val showEmptyResult: Boolean
        get() = hasSearched && !isSearching && results.isEmpty() && query.isNotBlank()

    /** Nothing typed yet — an invitation, not a "no matches" message. */
    val showPrompt: Boolean get() = query.isBlank()
}

/**
 * Drives search across the user's own entries.
 *
 * The query is debounced because every keystroke would otherwise be a full
 * table scan; [MoodAnalyzer.TREND_WINDOW_DAYS]-many characters of typing would
 * mean thirty scans for one result. The scan is cheap at this size, but "cheap
 * enough" is not a reason to run work the user did not ask for.
 *
 * Results are keyed on the query *and* the mood filter, and a stale response
 * cannot overwrite a newer one: each search carries a token, so a slow early
 * query that lands after a fast later one is discarded rather than shown. That
 * race is real — the two queries run on different threads and SQLite gives no
 * ordering guarantee between them.
 */
@OptIn(FlowPreview::class)
class DiarySearchViewModel(
    private val searchEntries: SearchDiaryEntriesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DiarySearchState())
    val state: StateFlow<DiarySearchState> = _state.asStateFlow()

    private val queries = MutableStateFlow("")

    /** Guards against an older, slower search landing after a newer one. */
    private var searchToken = 0L

    init {
        viewModelScope.launch {
            // No `drop(1)` on the initial value. It looks like a tidy way to skip
            // the empty string, but this collector starts lazily: by the time it
            // subscribes, `queries` already holds the first character the user
            // typed, so dropping the first emission silently discarded the first
            // real query and left the field apparently doing nothing. runSearch()
            // already ignores a blank query, which is what made the drop look
            // redundant in the first place.
            queries
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { runSearch() }
        }
    }

    fun onQueryChange(value: String) {
        _state.value = _state.value.copy(query = value, errorMessage = null)
        queries.value = value
    }

    /**
     * Select a mood filter, or clear it by naming the one already active.
     *
     * The toggle lives here rather than in the screen. "Tapping the thing that is
     * already on turns it off" is a rule of the control, and the editor's mood
     * selector already behaves that way — having each screen rediscover it is how
     * two identical-looking chips end up meaning different things. Passing `null`
     * still clears unconditionally.
     */
    fun onMoodFilterChange(moodKey: String?) {
        val current = _state.value.moodKey
        val next = when {
            moodKey == null -> null
            moodKey.equals(current, ignoreCase = true) -> null
            else -> moodKey
        }
        _state.value = _state.value.copy(moodKey = next, errorMessage = null)
        runSearch()
    }

    /** Re-runs immediately, for the keyboard's search action. */
    fun onSubmit() {
        runSearch()
    }

    fun onClearQuery() {
        onQueryChange("")
        // Reset to the prompt rather than leaving stale results behind a blank
        // field, which reads as "your journal is empty".
        _state.value = _state.value.copy(results = emptyList(), hasSearched = false, isSearching = false)
    }

    private fun runSearch() {
        val query = _state.value.query
        val moodKey = _state.value.moodKey
        if (query.isBlank()) {
            _state.value = _state.value.copy(results = emptyList(), hasSearched = false, isSearching = false)
            return
        }

        val token = ++searchToken
        _state.value = _state.value.copy(isSearching = true, errorMessage = null)

        viewModelScope.launch {
            runCatching { searchEntries(query, moodKey) }
                .onSuccess { results ->
                    if (token != searchToken) return@onSuccess
                    _state.value = _state.value.copy(
                        results = results,
                        isSearching = false,
                        hasSearched = true
                    )
                }
                .onFailure { error ->
                    if (token != searchToken) return@onFailure
                    _state.value = _state.value.copy(
                        results = emptyList(),
                        isSearching = false,
                        hasSearched = true,
                        errorMessage = error.message ?: "Your journal could not be searched."
                    )
                }
        }
    }

    /** Moods offered as filters — the same eight the composer writes. */
    val moodFilters: List<com.lifeos.app.domain.model.DiaryMood> get() = DiaryMoods.OPTIONS

    companion object {
        /**
         * Long enough that ordinary typing fires one search, short enough that
         * the field does not feel like it is lagging behind the keyboard.
         */
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}