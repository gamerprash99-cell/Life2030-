package com.lifeos.app.core.intelligence

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository

/**
 * Powers "Ask LifeOS AI" fully offline: matches the user's question text
 * against a small set of known intents (keyword patterns) and routes to
 * the appropriate analyzer. This is deliberately a pattern-matcher, not a
 * general-purpose chatbot — when nothing matches, it says so honestly and
 * lists what it can actually answer, rather than guessing.
 */
class LocalQuestionEngine(
    private val taskAnalyzer: TaskAnalyzer,
    private val habitAnalyzer: HabitAnalyzer,
    private val expenseRepository: ExpenseRepository,
    private val diaryRepository: DiaryRepository,
    private val statisticsEngine: StatisticsEngine,
    private val reportGenerator: ReportGenerator
) {
    private val capabilities = listOf(
        "\"How much did I spend this week?\"",
        "\"How am I doing with my habits?\"",
        "\"What's my mood been like lately?\"",
        "\"How productive have I been?\"",
        "\"Give me a weekly summary\"",
        "\"How many tasks are overdue?\""
    )

    suspend fun answer(question: String): AnswerResult {
        val q = question.lowercase()
        val today = DateTimeUtils.today()

        return when {
            containsAny(q, "spend", "spent", "expense", "money", "cost") -> answerSpending(q, today)
            containsAny(q, "habit", "streak") -> answerHabits()
            containsAny(q, "mood", "feeling", "diary", "journal") -> answerMood(today)
            containsAny(q, "productiv", "how am i doing", "how's it going", "score") -> answerProductivity(today)
            containsAny(q, "overdue", "behind") -> answerOverdue(today)
            containsAny(q, "weekly summary", "week in review", "this week") -> answerWeeklySummary(today)
            containsAny(q, "stat", "total", "all time", "overall") -> answerStatistics()
            else -> AnswerResult(
                answer = "I can't quite match that question yet. I'm a local, rule-based assistant, " +
                    "not a general chatbot, so I only understand a specific set of questions about your " +
                    "LifeOS data. Try one of these:",
                followUpSuggestions = capabilities
            )
        }
    }

    private fun containsAny(text: String, vararg needles: String) = needles.any { text.contains(it) }

    private suspend fun answerSpending(q: String, today: java.time.LocalDate): AnswerResult {
        val monthMode = containsAny(q, "month")
        val start = if (monthMode) DateTimeUtils.startOfMonthEpochDay(today) else DateTimeUtils.startOfWeekEpochDay(today)
        val end = if (monthMode) DateTimeUtils.endOfMonthEpochDay(today) else DateTimeUtils.endOfWeekEpochDay(today)
        val expenses = expenseRepository.getInRange(start, end)
        val total = expenses.sumOf { it.amount }
        val period = if (monthMode) "this month" else "this week"
        if (expenses.isEmpty()) return AnswerResult("You haven't logged any expenses $period yet.")
        val topCategory = expenses.groupBy { it.category }.maxByOrNull { entry -> entry.value.sumOf { it.amount } }
        val extra = topCategory?.let { " Your biggest category was ${it.key}." } ?: ""
        return AnswerResult("You've spent \u20B9${"%.0f".format(total)} $period across ${expenses.size} expense(s).$extra")
    }

    private suspend fun answerHabits(): AnswerResult {
        val habits = habitAnalyzer.summarizeAll()
        if (habits.isEmpty()) return AnswerResult("You don't have any habits set up yet. Add one from the Habits tab.")
        val lines = habits.take(5).joinToString("\n") { h ->
            "\u2022 ${h.habitName}: ${h.currentStreak}-day streak, ${h.completionPercentThisMonth}% this month"
        }
        return AnswerResult("Here's how your habits are looking:\n$lines")
    }

    private suspend fun answerMood(today: java.time.LocalDate): AnswerResult {
        val start = DateTimeUtils.startOfWeekEpochDay(today)
        val end = DateTimeUtils.endOfWeekEpochDay(today)
        val entries = diaryRepository.getInRange(start, end)
        if (entries.isEmpty()) return AnswerResult("You haven't written any diary entries this week yet.")
        val avg = DiaryAnalyzer.averageMood(entries)
        val moodLabel = when {
            avg >= 1.5 -> "quite positive"
            avg >= 0.5 -> "generally positive"
            avg >= -0.5 -> "fairly neutral"
            avg >= -1.5 -> "a bit low"
            else -> "quite low"
        }
        val entryWord = if (entries.size == 1) "entry" else "entries"
        return AnswerResult("Based on ${entries.size} diary $entryWord this week, your mood has been $moodLabel.")
    }

    private suspend fun answerProductivity(today: java.time.LocalDate): AnswerResult {
        val taskAnalysis = taskAnalyzer.analyzePeriod(
            DateTimeUtils.startOfWeekEpochDay(today), DateTimeUtils.endOfWeekEpochDay(today), today.toEpochDay()
        )
        val habits = habitAnalyzer.summarizeAll()
        val productivity = ProductivityAnalyzer.compute(taskAnalysis, habits)
        return AnswerResult("Your productivity this week: ${productivity.label} — ${productivity.score}/100 (tasks ${productivity.taskComponent}%, habits ${productivity.habitComponent}%).")
    }

    private suspend fun answerOverdue(today: java.time.LocalDate): AnswerResult {
        val taskAnalysis = taskAnalyzer.analyzePeriod(
            DateTimeUtils.startOfWeekEpochDay(today), DateTimeUtils.endOfWeekEpochDay(today), today.toEpochDay()
        )
        return if (taskAnalysis.overdueCount == 0) {
            AnswerResult("Nothing overdue right now. You're all caught up.")
        } else {
            AnswerResult("You have ${taskAnalysis.overdueCount} overdue task(s). Worth a look on the Tasks tab.")
        }
    }

    private suspend fun answerWeeklySummary(today: java.time.LocalDate): AnswerResult {
        val report = reportGenerator.weekly(today)
        return AnswerResult(report.narrative)
    }

    private suspend fun answerStatistics(): AnswerResult {
        val stats = statisticsEngine.computeAllTime()
        return AnswerResult(
            "All-time so far: ${stats.totalNotes} notes, ${stats.totalTasksCompleted} tasks completed, " +
                "${stats.totalHabitsTracked} active habits, ${stats.totalDiaryEntries} diary entries, " +
                "${stats.totalCaptures} captures, \u20B9${"%.0f".format(stats.totalSpend)} tracked spend."
        )
    }
}
