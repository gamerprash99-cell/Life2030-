package com.lifeos.app.core.ai

/**
 * AI capability levels (spec: "permission-scoped LifeOS AI").
 *
 * Level 0 — Off: no AI feature is reachable; nothing is computed.
 * Level 1 — On-device summaries: extractive summaries of the user's own text.
 * Level 2 — + Tagging: automatic tag suggestions from the text itself.
 * Level 3 — + Gentle suggestions: review prompts derived from real data
 *           (e.g. "3 tasks overdue", "no diary entry for 4 days").
 *
 * Contract for every implementation:
 * - Pure function of data already on this device. Nothing is transmitted.
 * - No fabricated facts: output can always be traced to input rows/text.
 * - Deterministic by default so the UI never flickers between answers.
 * - Returning null means "no useful answer" — the UI then shows nothing
 *   rather than inventing content.
 */
interface AiProvider {
    val id: String
    val requiresNetwork: Boolean get() = false

    /** Short, honest name shown in Settings. */
    val displayName: String

    /** Extractive summary: picks the most informative sentences of [text]. */
    fun summarize(text: String, maxSentences: Int = 2): String?

    /** Tag candidates derived only from [text] (lowercased, normalized). */
    fun suggestTags(text: String, existingTags: List<String> = emptyList()): List<String>

    /** Insight built from real statistics — never invented numbers. */
    fun insight(request: InsightRequest): String?

    /** Suggestion derived only from the counters in [request]; null if nothing. */
    fun suggestion(request: SuggestionRequest): String?
}

data class InsightRequest(
    val expensesByCategory: List<Pair<String, Long>> = emptyList(),
    val currencyCode: String = "USD",
    val openTasks: Int = 0,
    val overdueTasks: Int = 0,
    val habitsActive: Int = 0,
    val habitsCompletedToday: Int = 0,
    val diaryEntriesLast7Days: Int = 0,
    val eventsLast7Days: Int = 0,
    val streakBest: Pair<String, Int>? = null,
)

data class SuggestionRequest(
    val overdueTasks: Int = 0,
    val stalledHabits: List<String> = emptyList(),
    val daysSinceDiary: Int = 0,
    val weekOverBudget: Boolean = false,
    val topExpenseCategory: String? = null,
)

/** Consent levels mirror [com.lifeos.app.core.data.AppSettings.aiConsentLevel]. */
object AiConsent {
    const val OFF = 0
    const val SUMMARIES = 1
    const val TAGGING = 2
    const val SUGGESTIONS = 3

    fun levelName(level: Int): String = when (level.coerceIn(0, 3)) {
        OFF -> "Off"
        SUMMARIES -> "Summaries"
        TAGGING -> "Summaries + tags"
        else -> "Summaries + tags + suggestions"
    }

    /** Enforces the level contract around any provider call. */
    fun allows(level: Int, capability: Int): Boolean =
        level.coerceIn(0, 3) >= capability.coerceAtLeast(1)
}
