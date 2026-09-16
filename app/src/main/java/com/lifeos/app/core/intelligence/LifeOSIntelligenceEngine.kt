package com.lifeos.app.core.intelligence

import com.lifeos.app.core.ai.NoteAiAction
import com.lifeos.app.core.intelligence.generative.GenerationRequest
import com.lifeos.app.core.intelligence.generative.GenerationResult
import com.lifeos.app.core.intelligence.generative.LocalGenerativeGateway
import com.lifeos.app.core.intelligence.generative.LocalModelState
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.NoteRepository
import com.lifeos.app.data.repository.TaskRepository

/**
 * LifeOS Intelligence Engine — the single facade for deterministic personal
 * intelligence plus the optional on-device generative model path.
 *
 * Phase 19 adds only the model seam. No model is bundled yet, so generation
 * fails closed through LocalGenerativeGateway rather than using a network API
 * or pretending deterministic rules are generative AI.
 */
class LifeOSIntelligenceEngine(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val diaryRepository: DiaryRepository,
    private val expenseRepository: ExpenseRepository,
    private val captureRepository: CaptureRepository,
    private val generativeGateway: LocalGenerativeGateway = LocalGenerativeGateway()
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

    /** Current local generative-model availability without exposing runtime details. */
    val localGenerativeModelState: LocalModelState
        get() = generativeGateway.state

    /**
     * Generation entry point reserved for genuinely generative features.
     * The caller receives an explicit unavailable result until Phase 20 adds
     * a verified bundled on-device model implementation.
     */
    suspend fun generateLocally(
        systemInstruction: String,
        prompt: String,
        maxTokens: Int = 256,
        temperature: Float = 0.2f
    ): GenerationResult = generativeGateway.generate(
        GenerationRequest(systemInstruction, prompt, maxTokens, temperature)
    )

    // ---- Note actions (deterministic/extractive path) ----
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
            else -> "\"${action.label}\" needs a local generative model, which is not bundled in this build yet."
        }
    }

    fun extractTasksFromText(text: String): List<String> = NoteTextAnalyzer.findActionItems(text)

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

    suspend fun weeklyReport(): PeriodReport = reportGenerator.weekly()
    suspend fun monthlyReport(): PeriodReport = reportGenerator.monthly()
    suspend fun personalStatistics() = statisticsEngine.computeAllTime()
    suspend fun answerQuestion(question: String): AnswerResult = questionEngine.answer(question)
}
