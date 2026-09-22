package com.lifeos.app.core.intelligence

import com.lifeos.app.core.ai.NoteAiAction
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.NoteRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.core.util.DateTimeUtils
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * LifeOS Intelligence Engine — Level 1 (see docs/11_AI_SYSTEM.md for the
 * architecture writeup). This is the single facade wiring together every
 * analyzer below it:
 *
 *   LifeOSIntelligenceEngine
 *       |
 *       |-- DiaryAnalyzer           (DiaryAnalyzer.kt)
 *       |-- MoodAnalyzer            (MoodAnalyzer.kt)
 *       |-- KeywordExtractor        (MoodAnalyzer.kt)
 *       |-- TaskAnalyzer            (TaskAnalyzer.kt)
 *       |-- HabitAnalyzer           (HabitAnalyzer.kt)
 *       |-- ProductivityAnalyzer    (ProductivityAnalyzer.kt)
 *       |-- TrendAnalyzer           (TrendAnalyzer.kt)
 *       |-- PatternDetector         (PatternDetector.kt)
 *       |-- CorrelationAnalyzer     (PatternDetector.kt)
 *       |-- RecommendationEngine    (RecommendationAndStatistics.kt)
 *       |-- StatisticsEngine        (RecommendationAndStatistics.kt)
 *       |-- ReportGenerator         (Weekly + Monthly reports, ReportGenerator.kt)
 *       |-- NoteTextAnalyzer        (extractive text ops, NoteTextAnalyzer.kt)
 *       `-- LocalQuestionEngine     (LocalQuestionEngine.kt)
 *
 * 100% on-device: everything here reads through the existing repositories
 * (Room-backed), does arithmetic/lexicon lookups, and returns results.
 * No network client exists anywhere in this package. No diary/note/task/
 * habit content ever leaves the device via this engine.
 *
 * Modularity note: every method below returns a plain result type. If a
 * genuinely useful, small on-device generative model is added in the
 * future, it can be slotted in as an additional optional path (e.g. for
 * the actions NoteTextAnalyzer honestly can't do) without redesigning this
 * facade or any of its callers.
 */
class LifeOSIntelligenceEngine(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val diaryRepository: DiaryRepository,
    private val expenseRepository: ExpenseRepository,
    private val captureRepository: CaptureRepository
) {
    private val taskAnalyzer = TaskAnalyzer(taskRepository)
    private val habitAnalyzer = HabitAnalyzer(habitRepository)
    private val statisticsEngine = StatisticsEngine(
        noteRepository, taskRepository, habitRepository, diaryRepository, captureRepository, expenseRepository
    )
    private val reportGenerator = ReportGenerator(taskAnalyzer, habitAnalyzer, diaryRepository, expenseRepository)
    private val questionEngine = LocalQuestionEngine(
        taskAnalyzer, habitAnalyzer, expenseRepository, diaryRepository, statisticsEngine, reportGenerator
    )

    // ---- Note actions (extractive text operations; see NoteTextAnalyzer for what's genuinely offline-feasible) ----

    /** Every NoteAiAction that is genuinely achievable offline without a generative model. */
    private val supportedNoteActions = setOf(
        NoteAiAction.SUMMARIZE, NoteAiAction.GENERATE_TITLE, NoteAiAction.ORGANIZE,
        NoteAiAction.EXTRACT_POINTS, NoteAiAction.CREATE_CHECKLIST, NoteAiAction.MAKE_SHORTER
    )

    fun isNoteActionSupportedOffline(action: NoteAiAction): Boolean = action in supportedNoteActions

    fun runNoteAction(action: NoteAiAction, text: String): String {
        if (text.isBlank()) return "There's no text yet to work with."
        return when (action) {
            NoteAiAction.SUMMARIZE -> NoteTextAnalyzer.summarize(text)
            NoteAiAction.GENERATE_TITLE -> NoteTextAnalyzer.suggestTitle(text)
            NoteAiAction.ORGANIZE -> NoteTextAnalyzer.organizeAsBullets(text)
            NoteAiAction.EXTRACT_POINTS -> NoteTextAnalyzer.extractKeyPoints(text)
            NoteAiAction.CREATE_CHECKLIST -> NoteTextAnalyzer.createChecklist(text)
            NoteAiAction.MAKE_SHORTER -> NoteTextAnalyzer.summarize(text, maxSentences = 2)
            else -> "\"${action.label}\" needs a generative writing model, which LifeOS doesn't include " +
                "to stay fully offline and lightweight. Try Summarize, Organize text, Extract points, " +
                "Create checklist, or Make shorter instead. Those work fully offline."
        }
    }

    fun extractTasksFromText(text: String): List<String> = NoteTextAnalyzer.findActionItems(text)

    /**
     * A modest, honest reformatting of raw thoughts into diary-entry shape
     * (light templating + a mood reflection line), not a full generative
     * rewrite. See NoteTextAnalyzer.kt's doc comment for why.
     */
    fun draftDiaryEntry(rawThoughts: String): String {
        if (rawThoughts.isBlank()) return ""
        val mood = MoodAnalyzer.analyze(rawThoughts)
        val cleaned = rawThoughts.trim().replaceFirstChar { it.uppercase() }
        val moodLine = when (mood.mood) {
            Mood.VERY_POSITIVE, Mood.POSITIVE -> " It was a good one."
            Mood.NEGATIVE, Mood.VERY_NEGATIVE -> " Not the easiest day, but it's noted."
            else -> ""
        }
        return "$cleaned$moodLine"
    }

    // ---- Reports ----

    suspend fun weeklyReport(): PeriodReport = reportGenerator.weekly()
    suspend fun monthlyReport(): PeriodReport = reportGenerator.monthly()

    // ---- Statistics ----

    suspend fun personalStatistics() = statisticsEngine.computeAllTime()

    // ---- Diary insights (Diary screen) ----

    /**
     * Diary introspection for the Diary screen: weekly/monthly counts, an
     * honest average-mood trend (current vs previous week), per-day mood points
     * for the little chart, top keywords across the month, streak, a habit/mood
     * correlation and the existing weekly report's patterns + recommendations.
     * Everything here reads through the existing repositories — fully on-device.
     */
    suspend fun diaryInsights(today: LocalDate = DateTimeUtils.today()): DiaryInsights {
        val weekStart = DateTimeUtils.startOfWeekEpochDay(today)
        val weekEnd = DateTimeUtils.endOfWeekEpochDay(today)
        val weekEntries = diaryRepository.getInRange(weekStart, weekEnd)
        val prevWeekEntries = diaryRepository.getInRange(weekStart - 7, weekStart - 1)
        val monthStart = DateTimeUtils.startOfMonthEpochDay(today)
        val monthEnd = DateTimeUtils.endOfMonthEpochDay(today)
        val monthEntries = diaryRepository.getInRange(monthStart, monthEnd)

        val weeklyReport = reportGenerator.weekly(today)

        val moodAverage = DiaryAnalyzer.averageMood(weekEntries)
        val moodTrend = if (weekEntries.isNotEmpty() || prevWeekEntries.isNotEmpty()) {
            TrendAnalyzer.compare("mood", moodAverage, DiaryAnalyzer.averageMood(prevWeekEntries))
        } else null

        // Oldest → today, so the chart renders left-to-right in reading order.
        val moodByDay = (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            val dayEntries = weekEntries.filter { it.dateEpochDay == day.toEpochDay() }
            TrendPoint(
                DateTimeUtils.shortDayName(day),
                if (dayEntries.isEmpty()) 0.0 else DiaryAnalyzer.averageMood(dayEntries)
            )
        }

        val allEntryDays = diaryRepository.observeAll().first().map { it.dateEpochDay }.toSet()
        val streak = consecutiveDiaryDays(allEntryDays, today.toEpochDay())

        val habits = habitRepository.observeAll().first().filter { !it.isArchived }
        val completionsInRange = habitRepository.observeAllInRange(weekStart, weekEnd).first()
        val habitCorrelation = habits.firstNotNullOfOrNull { habit ->
            CorrelationAnalyzer.moodVsHabitCompletion(
                weekEntries,
                completionsInRange.filter { it.habitId == habit.id },
                habit.goalCount
            )
        }

        return DiaryInsights(
            weekCount = weekEntries.size,
            monthCount = monthEntries.size,
            averageMood = moodAverage,
            moodTrend = moodTrend,
            topKeywords = DiaryAnalyzer.topKeywordsAcross(monthEntries, max = 8),
            diaryStreakDays = streak,
            moodByDay = moodByDay,
            habitCorrelation = habitCorrelation,
            patterns = weeklyReport.patterns,
            recommendations = weeklyReport.recommendations,
            narrative = weeklyReport.narrative
        )
    }

    private fun consecutiveDiaryDays(entryDays: Set<Long>, todayEpochDay: Long): Int {
        if (entryDays.isEmpty()) return 0
        var streak = 0
        var cursor = todayEpochDay
        while (entryDays.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }

    // ---- Chat / Q&A ----

    suspend fun answerQuestion(question: String): AnswerResult = questionEngine.answer(question)
}
