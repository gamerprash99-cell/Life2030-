package com.lifeos.app.core.intelligence

import com.lifeos.app.data.db.dao.CategoryTotal
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Rule-based pattern detection over data the caller has already fetched
 * (via the repository layer) — no new queries here, just analysis of
 * existing lists. Deliberately simple, explainable rules rather than a
 * generic pattern-mining algorithm.
 */
object PatternDetector {

    /** Finds the day of week with the most missed (0-progress) completions, if any pattern exists. */
    fun mostMissedDayOfWeek(completions: List<HabitCompletionEntity>, activeDays: List<Long>): PatternInsight? {
        if (activeDays.isEmpty()) return null
        val doneDays = completions.filter { it.progressCount > 0 }.map { it.dateEpochDay }.toSet()
        val missedByDow = activeDays
            .filter { it !in doneDays }
            .groupingBy { LocalDate.ofEpochDay(it).dayOfWeek }
            .eachCount()
        val worst = missedByDow.maxByOrNull { it.value } ?: return null
        if (worst.value < 2) return null
        return PatternInsight(
            title = "Pattern noticed",
            detail = "You tend to miss more often on ${worst.key.dayOfWeekName()}s (${worst.value} times recently)."
        )
    }

    /** Highest-spend category as a simple pattern callout. */
    fun topSpendCategory(categoryTotals: List<CategoryTotal>): PatternInsight? {
        val top = categoryTotals.maxByOrNull { it.total } ?: return null
        if (top.total <= 0.0) return null
        return PatternInsight(
            title = "Top spending category",
            detail = "${top.category} was your biggest spend area this period (₹${"%.0f".format(top.total)})."
        )
    }

    private fun DayOfWeek.dayOfWeekName(): String =
        name.lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * Very lightweight correlation: compares average mood on days a habit was
 * completed vs. days it wasn't. This is a mean comparison, not a formal
 * statistical correlation coefficient — kept intentionally simple and
 * clearly labeled as such so it's never overstated to the user.
 */
object CorrelationAnalyzer {

    fun moodVsHabitCompletion(
        diaryEntries: List<DiaryEntity>,
        habitCompletions: List<HabitCompletionEntity>,
        goalCount: Int
    ): PatternInsight? {
        if (diaryEntries.size < 4) return null

        val moodByDay = diaryEntries.groupBy { it.dateEpochDay }
            .mapValues { (_, entries) -> DiaryAnalyzer.averageMood(entries) }
        val doneDays = habitCompletions.filter { it.progressCount >= goalCount }.map { it.dateEpochDay }.toSet()

        val moodOnDoneDays = moodByDay.filterKeys { it in doneDays }.values
        val moodOnOtherDays = moodByDay.filterKeys { it !in doneDays }.values
        if (moodOnDoneDays.isEmpty() || moodOnOtherDays.isEmpty()) return null

        val avgDone = moodOnDoneDays.average()
        val avgOther = moodOnOtherDays.average()
        val diff = avgDone - avgOther
        if (kotlin.math.abs(diff) < 0.5) return null

        return if (diff > 0) {
            PatternInsight(
                title = "Possible connection",
                detail = "Your diary mood tends to be higher on days you complete this habit."
            )
        } else {
            PatternInsight(
                title = "Worth noticing",
                detail = "Your diary mood tends to be lower on days you complete this habit — might be worth reflecting on."
            )
        }
    }
}
