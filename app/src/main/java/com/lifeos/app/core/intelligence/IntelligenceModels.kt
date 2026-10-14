package com.lifeos.app.core.intelligence

/**
 * LifeOS Intelligence Engine — shared output models.
 *
 * Everything in core/intelligence/ is 100% deterministic, on-device logic
 * (lexicon lookups, keyword frequency counts, arithmetic over data already
 * fetched via the existing repositories). Nothing here makes a network
 * call, and none of these types are ever sent anywhere — they're computed
 * from local Room data and rendered directly in the UI.
 */

enum class Mood { VERY_POSITIVE, POSITIVE, NEUTRAL, NEGATIVE, VERY_NEGATIVE, UNKNOWN }

data class MoodResult(
    val mood: Mood,
    val score: Int,               // -5..+5, sum of matched lexicon weights, clamped
    val positiveHits: List<String>,
    val negativeHits: List<String>
)

data class KeywordResult(val keyword: String, val count: Int)

data class DiaryAnalysis(
    val entryId: String,
    val mood: MoodResult,
    val topKeywords: List<KeywordResult>,
    val wordCount: Int
)

data class TaskAnalysis(
    val totalInPeriod: Int,
    val completedInPeriod: Int,
    val completionRatePercent: Int,
    val overdueCount: Int,
    val highPriorityOpenCount: Int
)

data class HabitAnalysisSummary(
    val habitId: String,
    val habitName: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionPercentThisMonth: Int
)

/** 0-100 composite score. See ProductivityAnalyzer for exactly how it's built. */
data class ProductivityScore(
    val score: Int,
    val taskComponent: Int,
    val habitComponent: Int,
    val label: String // e.g. "Strong week", "Steady", "Room to grow"
)

data class TrendPoint(val label: String, val value: Double)

data class TrendResult(
    val metric: String,
    val currentPeriodValue: Double,
    val previousPeriodValue: Double,
    val percentChange: Int?, // null if previous period had no data to compare against
    val direction: TrendDirection
)

enum class TrendDirection { UP, DOWN, FLAT, UNKNOWN }

data class PatternInsight(val title: String, val detail: String)

data class Recommendation(val title: String, val detail: String, val priority: Int)

data class PersonalStatistics(
    val totalNotes: Int,
    val totalTasksCompleted: Int,
    val totalHabitsTracked: Int,
    val totalDiaryEntries: Int,
    val totalCaptures: Int,
    val totalSpend: Double,
    val diaryStreakDays: Int
)

data class PeriodReport(
    val periodLabel: String,          // "This week" / "This month"
    val productivity: ProductivityScore,
    val taskAnalysis: TaskAnalysis,
    val topHabits: List<HabitAnalysisSummary>,
    val moodTrend: TrendResult?,
    val spendTrend: TrendResult?,
    val patterns: List<PatternInsight>,
    val recommendations: List<Recommendation>,
    val narrative: String              // template-generated natural-language summary
)

/** Result of a single question asked to the LocalQuestionEngine ("Ask LifeOS AI"). */
data class AnswerResult(val answer: String, val followUpSuggestions: List<String> = emptyList())
