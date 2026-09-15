package com.lifeos.app.core.intelligence

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.NoteRepository
import com.lifeos.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.first

/**
 * Simple if-then rules that turn already-computed analysis into short,
 * actionable suggestions. Every rule here is explicit and readable —
 * there is no hidden model, just a checklist of conditions.
 */
object RecommendationEngine {

    fun generate(
        taskAnalysis: TaskAnalysis,
        habits: List<HabitAnalysisSummary>,
        productivity: ProductivityScore
    ): List<Recommendation> {
        val recs = mutableListOf<Recommendation>()

        if (taskAnalysis.overdueCount >= 3) {
            recs += Recommendation(
                title = "Clear overdue tasks",
                detail = "You have ${taskAnalysis.overdueCount} overdue tasks. Consider rescheduling or completing a few today.",
                priority = 1
            )
        }

        if (taskAnalysis.highPriorityOpenCount > 0) {
            recs += Recommendation(
                title = "High-priority items waiting",
                detail = "You have ${taskAnalysis.highPriorityOpenCount} high-priority task(s) still open.",
                priority = 1
            )
        }

        val strugglingHabits = habits.filter { it.currentStreak == 0 && it.completionPercentThisMonth < 40 }
        if (strugglingHabits.isNotEmpty()) {
            val name = strugglingHabits.first().habitName
            recs += Recommendation(
                title = "A habit could use attention",
                detail = "\"$name\" has fallen off track this month. A small restart today can rebuild momentum.",
                priority = 2
            )
        }

        val strongHabits = habits.filter { it.currentStreak >= 7 }
        if (strongHabits.isNotEmpty()) {
            val best = strongHabits.maxByOrNull { it.currentStreak }
            if (best != null) {
                recs += Recommendation(
                    title = "Keep the streak going",
                    detail = "\"${best.habitName}\" is on a ${best.currentStreak}-day streak — don't break it today!",
                    priority = 3
                )
            }
        }

        if (productivity.score < 40) {
            recs += Recommendation(
                title = "Start small",
                detail = "Things look a bit quiet lately. Completing just one task or habit today can rebuild momentum.",
                priority = 2
            )
        }

        if (recs.isEmpty()) {
            recs += Recommendation(
                title = "You're on track",
                detail = "No urgent flags right now — nice work keeping things steady.",
                priority = 5
            )
        }

        return recs.sortedBy { it.priority }
    }
}

/**
 * Aggregate personal statistics — plain counts/sums read through the
 * existing repositories, no new database access patterns.
 */
class StatisticsEngine(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val diaryRepository: DiaryRepository,
    private val captureRepository: CaptureRepository,
    private val expenseRepository: ExpenseRepository
) {
    suspend fun computeAllTime(): PersonalStatistics {
        val notes = noteRepository.observeAll().first().size
        val allTasks = taskRepository.observeAll().first()
        val tasksCompleted = allTasks.count { it.isCompleted }
        val habits = habitRepository.observeAll().first().count { !it.isArchived }
        val diaryEntries = diaryRepository.observeAll().first()
        val captures = captureRepository.observeAll().first().size
        val totalSpend = diaryEntries.let {
            // Sum all-time spend via the existing range query, using the widest safe range.
            val today = DateTimeUtils.today().toEpochDay()
            expenseRepository.getInRange(0, today).sumOf { it.amount }
        }

        val diaryStreak = computeDiaryStreak(diaryEntries.map { it.dateEpochDay }.toSet())

        return PersonalStatistics(
            totalNotes = notes,
            totalTasksCompleted = tasksCompleted,
            totalHabitsTracked = habits,
            totalDiaryEntries = diaryEntries.size,
            totalCaptures = captures,
            totalSpend = totalSpend,
            diaryStreakDays = diaryStreak
        )
    }

    private fun computeDiaryStreak(entryDays: Set<Long>): Int {
        if (entryDays.isEmpty()) return 0
        var streak = 0
        var cursor = DateTimeUtils.today().toEpochDay()
        while (entryDays.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }
}
