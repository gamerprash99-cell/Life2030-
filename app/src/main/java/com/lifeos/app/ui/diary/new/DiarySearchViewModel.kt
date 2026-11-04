package com.lifeos.app.ui.diary.new

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.domain.usecase.SearchDiaryEntriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class DiarySearchState(
    val query: String = "",
    val moodKey: String? = null,
    val results: List<DiaryEntity> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null
) {
    val showEmptyResult: Boolean
        get() = hasSearched && !isSearching && results.isEmpty() && query.isNotBlank()
    val showPrompt: Boolean get() = query.isBlank()
}

class DiarySearchViewModel(
    private val searchEntries: SearchDiaryEntriesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DiarySearchState())
    val state: StateFlow<DiarySearchState> = _state.asStateFlow()

    private val queries = MutableStateFlow("")
    private var searchToken = 0L

    init {
        viewModelScope.launch {
            queries
                .debounce(300L)
                .distinctUntilChanged()
                .collect { runSearch() }
        }
    }

    fun onQueryChange(value: String) {
        _state.value = _state.value.copy(query = value, errorMessage = null)
        queries.value = value
    }

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

    fun onSubmit() {
        runSearch()
    }

    fun onClearQuery() {
        onQueryChange("")
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

    val moodFilters: List<com.lifeos.app.domain.model.DiaryMood> get() = DiaryMoods.OPTIONS

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
