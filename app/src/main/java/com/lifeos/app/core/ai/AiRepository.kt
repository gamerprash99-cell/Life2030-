package com.lifeos.app.core.ai

import com.lifeos.app.core.intelligence.LifeOSIntelligenceEngine

/**
 * "Ask LifeOS AI" — now backed entirely by the on-device
 * LifeOSIntelligenceEngine (core/intelligence/), not a cloud API.
 *
 * OFFLINE-FIRST GUARANTEE: this class makes zero network calls. There is
 * no HTTP client, no API key, and no external endpoint anywhere in this
 * file or in core/intelligence/. Every method below reads local data
 * through the existing repositories (already passed into the engine) and
 * returns a result computed entirely on-device.
 *
 * This class is kept as a thin compatibility layer over the engine so the
 * UI call sites (NotesViewModel, DiaryScreen, InsightsScreen,
 * AiAssistantScreen) didn't need a full rewrite — only their `NoApiKey`
 * branches were removed, since that case no longer exists.
 */
class AiRepository(private val engine: LifeOSIntelligenceEngine) {

    /** Note-level actions (Section 8). Some are genuinely offline-feasible (extractive); others say so honestly. */
    fun runNoteAction(action: NoteAiAction, noteText: String): AiResult =
        runCatching { engine.runNoteAction(action, noteText) }
            .fold(onSuccess = { AiResult.Success(it) }, onFailure = { AiResult.Error(it.message ?: "Something went wrong analyzing this note.") })

    /** Task extraction from a note's text — offline, extractive (action-item detection). */
    fun extractTasks(sourceText: String): AiResult =
        runCatching {
            val items = engine.extractTasksFromText(sourceText)
            if (items.isEmpty()) "NONE" else items.joinToString("\n")
        }.fold(onSuccess = { AiResult.Success(it) }, onFailure = { AiResult.Error(it.message ?: "Couldn't scan this text for tasks.") })

    /** Diary drafting from rough thoughts — offline, light templating (see engine doc comment for the honesty note). */
    fun draftDiaryEntry(rawThoughts: String): AiResult =
        runCatching { engine.draftDiaryEntry(rawThoughts) }
            .fold(onSuccess = { AiResult.Success(it) }, onFailure = { AiResult.Error(it.message ?: "Couldn't draft this entry.") })

    /** Weekly/monthly review — now a real, data-grounded report instead of a network-generated summary. */
    suspend fun generateReviewSummary(periodLabel: String): AiResult =
        runCatching {
            val report = if (periodLabel.contains("month", ignoreCase = true)) engine.monthlyReport() else engine.weeklyReport()
            report.narrative
        }.fold(onSuccess = { AiResult.Success(it) }, onFailure = { AiResult.Error(it.message ?: "Couldn't build your $periodLabel review.") })

    /** AI Assistant chat — routed through the LocalQuestionEngine's intent matching. */
    suspend fun chat(history: List<ChatMessage>): AiResult {
        val lastUserMessage = history.lastOrNull { it.role == "user" }?.content.orEmpty()
        return runCatching { engine.answerQuestion(lastUserMessage) }
            .fold(
                onSuccess = { result ->
                    val suffix = if (result.followUpSuggestions.isNotEmpty()) {
                        "\n\n" + result.followUpSuggestions.joinToString("\n")
                    } else ""
                    AiResult.Success(result.answer + suffix)
                },
                onFailure = { AiResult.Error(it.message ?: "Something went wrong answering that.") }
            )
    }

    fun parseExtractedTasks(rawText: String): List<ExtractedTask> {
        if (rawText.trim().equals("NONE", ignoreCase = true)) return emptyList()
        return rawText.lines()
            .map { it.trim().removePrefix("-").removePrefix("*").trim() }
            .filter { it.isNotBlank() }
            .map { ExtractedTask(title = it) }
    }
}
