package com.lifeos.app.core.intelligence

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import java.time.LocalDate

/**
 * Builds a full weekly or monthly report by combining every analyzer above.
 * The "narrative" field is template-based natural language — sentences
 * built from if/else branches over already-computed numbers, not a
 * generative model. This is exactly the "template-based natural-language
 * responses" the product spec calls for.
 */
class ReportGenerator(
    private val taskAnalyzer: TaskAnalyzer,
    private val habitAnalyzer: HabitAnalyzer,
    private val diaryRepository: DiaryRepository,
    private val expenseRepository: ExpenseRepository
) {
    suspend fun weekly(today: LocalDate = DateTimeUtils.today()): PeriodReport =
        buildReport(
            periodLabel = "This week",
            startEpochDay = DateTimeUtils.startOfWeekEpochDay(today),
            endEpochDay = DateTimeUtils.endOfWeekEpochDay(today),
            previousStartEpochDay = DateTimeUtils.startOfWeekEpochDay(today) - 7,
            previousEndEpochDay = DateTimeUtils.startOfWeekEpochDay(today) - 1,
            today = today
        )

    suspend fun monthly(today: LocalDate = DateTimeUtils.today()): PeriodReport {
        val monthStart = DateTimeUtils.startOfMonthEpochDay(today)
        val monthEnd = DateTimeUtils.endOfMonthEpochDay(today)
        val lengthDays = monthEnd - monthStart + 1
        return buildReport(
            periodLabel = "This month",
            startEpochDay = monthStart,
            endEpochDay = monthEnd,
            previousStartEpochDay = monthStart - lengthDays,
            previousEndEpochDay = monthStart - 1,
            today = today
        )
    }

    private suspend fun buildReport(
        periodLabel: String,
        startEpochDay: Long,
        endEpochDay: Long,
        previousStartEpochDay: Long,
        previousEndEpochDay: Long,
        today: LocalDate
    ): PeriodReport {
        val todayEpochDay = today.toEpochDay()

        val taskAnalysis = taskAnalyzer.analyzePeriod(startEpochDay, endEpochDay, todayEpochDay)
        val habits = habitAnalyzer.summarizeAll()
        val productivity = ProductivityAnalyzer.compute(taskAnalysis, habits)

        val diaryThisPeriod = diaryRepository.getInRange(startEpochDay, endEpochDay)
        val diaryPrevPeriod = diaryRepository.getInRange(previousStartEpochDay, previousEndEpochDay)
        val moodTrend = if (diaryThisPeriod.isNotEmpty() || diaryPrevPeriod.isNotEmpty()) {
            TrendAnalyzer.compare(
                "mood",
                DiaryAnalyzer.averageMood(diaryThisPeriod),
                DiaryAnalyzer.averageMood(diaryPrevPeriod)
            )
        } else null

        val spendThis = expenseRepository.getInRange(startEpochDay, endEpochDay).sumOf { it.amount }
        val spendPrev = expenseRepository.getInRange(previousStartEpochDay, previousEndEpochDay).sumOf { it.amount }
        val spendTrend = TrendAnalyzer.compare("spend", spendThis, spendPrev)

        val categoryTotals = expenseRepository.getCategoryTotals(startEpochDay, endEpochDay)
        val patterns = listOfNotNull(
            PatternDetector.topSpendCategory(categoryTotals)
        )

        val recommendations = RecommendationEngine.generate(taskAnalysis, habits, productivity)

        val narrative = buildNarrative(periodLabel, taskAnalysis, habits, productivity, moodTrend, spendTrend)

        return PeriodReport(
            periodLabel = periodLabel,
            productivity = productivity,
            taskAnalysis = taskAnalysis,
            topHabits = habits.take(5),
            moodTrend = moodTrend,
            spendTrend = spendTrend,
            patterns = patterns,
            recommendations = recommendations,
            narrative = narrative
        )
    }

    private fun buildNarrative(
        periodLabel: String,
        taskAnalysis: TaskAnalysis,
        habits: List<HabitAnalysisSummary>,
        productivity: ProductivityScore,
        moodTrend: TrendResult?,
        spendTrend: TrendResult
    ): String {
        val sb = StringBuilder()
        sb.append("$periodLabel, you completed ${taskAnalysis.completedInPeriod} of ${taskAnalysis.totalInPeriod} tasks")
        sb.append(if (taskAnalysis.totalInPeriod > 0) " (${taskAnalysis.completionRatePercent}%). " else ". ")

        val strongest = habits.maxByOrNull { it.currentStreak }
        if (strongest != null && strongest.currentStreak > 0) {
            sb.append("Your strongest streak right now is \"${strongest.habitName}\" at ${strongest.currentStreak} day(s). ")
        }

        when (moodTrend?.direction) {
            TrendDirection.UP -> sb.append("Your diary mood looks a bit brighter than last period. ")
            TrendDirection.DOWN -> sb.append("Your diary mood has dipped a little compared to last period. ")
            else -> Unit
        }

        when (spendTrend.direction) {
            TrendDirection.UP -> sb.append("Spending is up compared to last period. ")
            TrendDirection.DOWN -> sb.append("Spending is down compared to last period. ")
            else -> Unit
        }

        sb.append("Overall: ${productivity.label.lowercase()} (score ${productivity.score}/100).")
        return sb.toString()
    }
}
