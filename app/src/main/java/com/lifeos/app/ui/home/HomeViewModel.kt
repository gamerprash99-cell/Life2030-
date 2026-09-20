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

    /** Toggles the focus task's real completion state in Room. */
    fun toggleFocusTask() {
        val current = summary.value ?: return
        val task = current.focusTask ?: return
        toggleTask(task.id, !current.focusTaskIsDone)
    }

    /**
     * Toggles a habit's real completion for today: completing writes progress
     * up to the goal, undoing clears today's record. Both are persisted, so the
     * UI (and weekly consistency) updates from Room via the summary flow.
     */
    fun toggleHabit(habitId: String, isDone: Boolean, goalCount: Int) {
        viewModelScope.launch {
            val today = DateTimeUtils.today().toEpochDay()
            if (isDone) {
                habitRepository.clearProgress(habitId, today)
            } else {
                habitRepository.logProgress(habitId, today, goalCount)
            }
        }
    }
}
