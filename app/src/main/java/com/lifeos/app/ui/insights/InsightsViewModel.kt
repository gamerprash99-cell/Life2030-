package com.lifeos.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.intelligence.LifeOSIntelligenceEngine
import com.lifeos.app.core.intelligence.PeriodReport
import com.lifeos.app.core.intelligence.PersonalStatistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Which window the dashboard is currently summarising. */
enum class InsightRange { WEEK, MONTH, ALL_TIME }

/**
 * What the Insights screen renders.
 *
 * [report] carries the weekly/monthly [PeriodReport] (productivity, tasks,
 * habits, mood and spend trends, patterns, recommendations, narrative);
 * [statistics] carries the all-time [PersonalStatistics]. Exactly one of them is
 * populated for the selected [range].
 */
data class InsightsState(
    val range: InsightRange = InsightRange.WEEK,
    val report: PeriodReport? = null,
    val statistics: PersonalStatistics? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Drives the Insights tab (spec §27 "Life Insights" and §28 "Productivity
 * Insights").
 *
 * Every figure is computed on demand from rows already on the device through
 * the offline [LifeOSIntelligenceEngine] — there is no cache and no network
 * path, so the numbers are never stale and never invented. Recomputing on each
 * open is cheap (arithmetic over local rows) and is the only way the screen can
 * be trusted immediately after the user adds a task or logs an expense.
 */
class InsightsViewModel(
    private val engine: LifeOSIntelligenceEngine
) : ViewModel() {

    private val _state = MutableStateFlow(InsightsState())
    val state: StateFlow<InsightsState> = _state.asStateFlow()

    /** Guards against an older load landing after a newer one. */
    private var loadToken = 0L

    init {
        select(InsightRange.WEEK)
    }

    fun select(range: InsightRange) {
        val token = ++loadToken
        _state.value = _state.value.copy(range = range, isLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { load(range) }
                .onSuccess { (report, statistics) ->
                    if (token != loadToken) return@onSuccess
                    _state.value = InsightsState(
                        range = range,
                        report = report,
                        statistics = statistics,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    if (token != loadToken) return@onFailure
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Your insights could not be calculated."
                    )
                }
        }
    }

    private suspend fun load(range: InsightRange): Pair<PeriodReport?, PersonalStatistics?> =
        when (range) {
            InsightRange.WEEK -> engine.weeklyReport() to null
            InsightRange.MONTH -> engine.monthlyReport() to null
            InsightRange.ALL_TIME -> null to engine.personalStatistics()
        }
}
