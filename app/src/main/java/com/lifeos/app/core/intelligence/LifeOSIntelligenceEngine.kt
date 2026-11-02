package com.lifeos.app.core.intelligence

import com.lifeos.app.core.ai.TextAiAction
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.NoteRepository
import com.lifeos.app.data.repository.TaskRepository

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
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val diaryRepository: DiaryRepository,
    private val expenseRepository: ExpenseRepository,
    private val captureRepository: CaptureRepository
) {
    private val taskAnalyzer = TaskAnalyzer(taskRepository)
    private val habitAnalyzer = HabitAnalyzer(habitRepository)
    private val statisticsEngine = StatisticsEngine(
        taskRepository, habitRepository, diaryRepository, captureRepository, expenseRepository
    )
    private val reportGenerator = ReportGenerator(taskAnalyzer, habitAnalyzer, diaryRepository, expenseRepository)
    private val questionEngine = LocalQuestionEngine(
        taskAnalyzer, habitAnalyzer, expenseRepository, diaryRepository, statisticsEngine, reportGenerator
    )

    // ---- Text actions (extractive operations; see NoteTextAnalyzer for what's genuinely offline-feasible) ----
    //
    // These operate on text the user is editing. They are NOT the retired Notes
    // module: nothing here reads or writes the `notes` table, and the analyzer
    // is a pure string utility.

    /** Every action that is genuinely achievable offline without a generative model. */
    private val supportedTextActions = setOf(
        TextAiAction.SUMMARIZE, TextAiAction.GENERATE_TITLE, TextAiAction.ORGANIZE,
        TextAiAction.EXTRACT_POINTS, TextAiAction.CREATE_CHECKLIST, TextAiAction.MAKE_SHORTER
    )

    fun isTextActionSupportedOffline(action: TextAiAction): Boolean = action in supportedTextActions

    fun runTextAction(action: TextAiAction, text: String): String {
        if (text.isBlank()) return "There's no text yet to work with."
        return when (action) {
            TextAiAction.SUMMARIZE -> NoteTextAnalyzer.summarize(text)
            TextAiAction.GENERATE_TITLE -> NoteTextAnalyzer.suggestTitle(text)
            TextAiAction.ORGANIZE -> NoteTextAnalyzer.organizeAsBullets(text)
            TextAiAction.EXTRACT_POINTS -> NoteTextAnalyzer.extractKeyPoints(text)
            TextAiAction.CREATE_CHECKLIST -> NoteTextAnalyzer.createChecklist(text)
            TextAiAction.MAKE_SHORTER -> NoteTextAnalyzer.summarize(text, maxSentences = 2)
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

    // ---- Chat / Q&A ----

    suspend fun answerQuestion(question: String): AnswerResult = questionEngine.answer(question)
}
