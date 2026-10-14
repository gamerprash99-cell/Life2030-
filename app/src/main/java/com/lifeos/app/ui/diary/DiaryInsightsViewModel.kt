package com.lifeos.app.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.domain.intelligence.DiaryInsights
import com.lifeos.app.domain.intelligence.WritingStreak
import com.lifeos.app.domain.usecase.GetDiaryInsightsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the insights screen renders. */
data class DiaryInsightsState(
    val insights: DiaryInsights? = null,
    val streak: WritingStreak = WritingStreak(currentLength = 0, longestLength = 0, isAlive = false),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /**
     * Carried on the state, not read from the clock by the screen, so the trend
     * chart's "today" marker is the same value the analyzers used. A screen
     * calling LocalDate.now() itself could straddle midnight between the two
     * and mark the wrong day.
     */
    val todayEpochDay: Long = 0L
) {
    /** Nothing analysed yet — the first-run state, not a failure. */
    val showOnboarding: Boolean
        get() = insights != null && insights.isSparse && !isLoading

    /** Real figures to render. */
    val showFigures: Boolean get() = insights != null && !insights.isSparse && !isLoading
}

/**
 * Drives the insights screen.
 *
 * Recomputes whenever the screen is opened rather than caching. The analysis is
 * deterministic and reads rows already on the device, so a stale cache would
 * only ever be wrong — and would be wrong in the one situation the user most
 * notices, which is immediately after saving a new entry.
 *
 * [reload] is public so the screen can pull to refresh, which is the honest
 * gesture here: the data is already local and always current, so the spinner
 * exists to reassure, not to fetch.
 */
class DiaryInsightsViewModel(
    private val getInsights: GetDiaryInsightsUseCase,
    private val today: () -> Long = { DateTimeUtils.today().toEpochDay() }
) : ViewModel() {

    private val _state = MutableStateFlow(DiaryInsightsState())
    val state: StateFlow<DiaryInsightsState> = _state.asStateFlow()

    /** Guards against an older analysis landing after a newer one. */
    private var loadToken = 0L

    init {
        reload()
    }

    fun reload() {
        val token = ++loadToken
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { getInsights() }
                .onSuccess { result ->
                    if (token != loadToken) return@onSuccess
                    _state.value = DiaryInsightsState(
                        insights = result.insights,
                        streak = result.streak,
                        isLoading = false,
                        todayEpochDay = today()
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
}