package com.lifeos.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.data.AppContainer
import com.lifeos.app.core.data.SearchResults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Global search. Queries shorter than two characters never hit the database, and
 * input is debounced so typing does not issue a query per keystroke.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(private val container: AppContainer) : ViewModel() {

    data class SearchState(
        val query: String = "",
        val results: SearchResults? = null,
        val searching: Boolean = false,
        val tooShort: Boolean = true,
    ) {
        val hasResults: Boolean get() = results?.isEmpty == false
    }

    private val query = MutableStateFlow("")

    private val results = query
        .debounce(220)
        .flatMapLatest { q ->
            if (q.trim().length < 2) flowOf(null) else container.timeline.observeSearch(q.trim())
        }

    val state: StateFlow<SearchState> = combine(query, results) { q, res ->
        SearchState(
            query = q,
            results = res,
            searching = false,
            tooShort = q.trim().length < 2,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun clear() {
        query.value = ""
    }
}
