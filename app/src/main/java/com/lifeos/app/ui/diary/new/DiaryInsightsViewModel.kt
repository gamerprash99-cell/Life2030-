package com.lifeos.app.ui.diary.new

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

data class DiaryInsightsState(
    val insights: DiaryInsights? = null,
    val streak: WritingStreak = WritingStreak(currentLength = 0, longestLength = 0, isAlive = false),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val todayEpochDay: Long = 0L
) {
    val showOnboarding: Boolean
        get() = insights != null && insights.isSparse && !isLoading
    val showFigures: Boolean
        get() = insights != null && !insights.isSparse && !isLoading
}

class DiaryInsightsViewModel(
    private val getInsights: GetDiaryInsightsUseCase,
    private val today: () -> Long = { DateTimeUtils.today().toEpochDay() }
) : ViewModel() {

    private val _state = MutableStateFlow(DiaryInsightsState())
    val state: StateFlow<DiaryInsightsState> = _state.asStateFlow()

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
