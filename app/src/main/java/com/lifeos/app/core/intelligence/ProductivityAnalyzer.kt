package com.lifeos.app.core.intelligence

/**
 * A deliberately simple, explainable productivity score: average of task
 * completion rate and habit consistency, both already 0-100 percentages.
 * No hidden weighting, no ML — a straightforward, auditable formula.
 */
object ProductivityAnalyzer {

    fun compute(taskAnalysis: TaskAnalysis, habits: List<HabitAnalysisSummary>): ProductivityScore {
        val taskComponent = taskAnalysis.completionRatePercent
        val habitComponent = if (habits.isNotEmpty()) {
            habits.map { it.completionPercentThisMonth }.average().toInt()
        } else {
            taskComponent
        }

        val score = ((taskComponent + habitComponent) / 2).coerceIn(0, 100)
        val label = when {
            score >= 80 -> "Strong momentum"
            score >= 60 -> "Steady progress"
            score >= 40 -> "Building consistency"
            score >= 20 -> "Getting started"
            else -> "A fresh start"
        }

        return ProductivityScore(score = score, taskComponent = taskComponent, habitComponent = habitComponent, label = label)
    }
}
