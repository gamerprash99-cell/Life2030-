package com.lifeos.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.domain.usecase.GetHomeSummaryUseCase
import com.lifeos.app.domain.usecase.HomeSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    getHomeSummary: GetHomeSummaryUseCase,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository
) : ViewModel() {

    val summary: StateFlow<HomeSummary?> = getHomeSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleTask(id: String, completed: Boolean) {
        viewModelScope.launch { taskRepository.setCompleted(id, completed) }
    }

    fun incrementHabit(habitId: String, currentProgress: Int, goalCount: Int) {
        viewModelScope.launch {
            val today = DateTimeUtils.today().toEpochDay()
            val next = (currentProgress + 1).coerceAtMost(goalCount + 5)
            habitRepository.logProgress(habitId, today, next)
        }
    }
}
