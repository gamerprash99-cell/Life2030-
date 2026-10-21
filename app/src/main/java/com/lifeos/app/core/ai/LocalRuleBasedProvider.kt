package com.lifeos.app.core.ai

import com.lifeos.app.core.domain.formatMoney
import java.util.Locale

/**
 * The default, on-device, rule-based provider.
 *
 * Deliberately NOT a language model: it performs extractive summarization,
 * keyword-based tagging and arithmetic over real counters. That keeps every
 * output verifiable against input, works fully offline, and lets the app claim
 * "AI" features without shipping a fake one (spec: "no fake AI").
 *
 * Each function is a pure function of its inputs and returns null when it has
 * nothing truthful to say.
 */
class LocalRuleBasedProvider : AiProvider {
    override val id: String = "local-rules-v1"
    override val displayName: String = "On-device (built-in)"
    override val requiresNetwork: Boolean = false

    override fun summarize(text: String, maxSentences: Int): String? {
        val sentences = splitSentences(text)
        if (sentences.size <= maxSentences) {
            return sentences.takeIf { it.isNotEmpty() }?.joinToString(" ")
        }
        // Score by information signals: length, presence of digits/named topics,
        // and position (first sentences usually frame the topic).
        val stopWords = STOP_WORDS
        val scored = sentences.mapIndexed { index, sentence ->
            val words = sentence.lowercase(Locale.ROOT)
                .split(Regex("""[^\p{L}\p{N}']+"""))
                .filter { it.isNotBlank() }
            val informativeWords = words.count { it !in stopWords && it.length > 3 }
            val hasNumbers = if (sentence.any { it.isDigit() }) 2 else 0
            val positionBonus = if (index == 0) 2 else if (index <= 2) 1 else 0
            val lengthBonus = (words.size / 8).coerceAtMost(2)
            Triple(index, sentence, informativeWords + hasNumbers + positionBonus + lengthBonus)
        }
        return scored
            .sortedWith(compareByDescending<Triple<Int, String, Int>> { it.third }
                .thenBy { it.first })
            .take(maxSentences.coerceIn(1, 5))
            .sortedBy { it.first }
            .joinToString(" ") { it.second }
            .trim()
            .takeIf { it.isNotBlank() }
    }

    override fun suggestTags(text: String, existingTags: List<String>): List<String> {
        if (text.isBlank()) return emptyList()
        val lower = text.lowercase(Locale.ROOT)
        val found = LinkedHashSet<String>()

        // 1. Explicit #hashtags win, normalized identically to manual tags.
        Regex("""#([\p{L}\p{N}_-]+)""").findAll(lower).forEach { found += it.groupValues[1] }

        // 2. Topic dictionary: only matches that genuinely appear in the text.
        TOPIC_KEYWORDS.forEach { (tag, keywords) ->
            if (keywords.any { lower.contains(it) }) found += tag
        }

        // 3. Never re-suggest tags the entity already has.
        val existing = existingTags.map { it.lowercase(Locale.ROOT) }.toSet()
        return found
            .filterNot { it in existing }
            .filter { it.length in 2..24 }
            .take(5)
    }

    override fun insight(request: InsightRequest): String? {
        val parts = mutableListOf<String>()

        if (request.expensesByCategory.isNotEmpty()) {
            val (category, total) = request.expensesByCategory.first()
            parts += "Top spending: $category (${formatMoney(total, request.currencyCode)})."
        }
        if (request.openTasks > 0) {
            parts += if (request.overdueTasks > 0) {
                "${request.overdueTasks} of ${request.openTasks} open tasks are overdue."
            } else {
                "${request.openTasks} open tasks, none overdue."
            }
        }
        if (request.habitsActive > 0) {
            parts += "Habits today: ${request.habitsCompletedToday}/${request.habitsActive} done."
        }
        if (request.diaryEntriesLast7Days == 0 && request.eventsLast7Days == 0) {
            return null // nothing to summarize yet — say nothing rather than invent
        }
        request.streakBest?.let { (name, days) ->
            if (days >= 2) parts += "Best active streak: $name ($days days)."
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    override fun suggestion(request: SuggestionRequest): String? {
        val parts = mutableListOf<String>()
        if (request.overdueTasks > 0) {
            parts += when (request.overdueTasks) {
                1 -> "1 task is overdue — worth 2 minutes now?"
                else -> "${request.overdueTasks} tasks are overdue."
            }
        }
        if (request.daysSinceDiary >= 4) {
            parts += "No diary entry for $request.daysSinceDiary days."
        }
        if (request.weekOverBudget) {
            parts += "This week is above your usual spend" +
                (request.topExpenseCategory?.let { " (driven by $it)." } ?: ".")
        }
        if (request.stalledHabits.isNotEmpty()) {
            parts += "Quiet habits: ${request.stalledHabits.take(2).joinToString(", ")}."
        }
        return parts.firstOrNull()
    }

    private fun splitSentences(text: String): List<String> =
        text.trim()
            .split(Regex("""(?<=[.!?])\s+|\n+"""))
            .map { it.trim() }
            .filter { it.length >= 12 }

    private companion object {
        val STOP_WORDS = setOf(
            "the", "and", "for", "that", "with", "this", "from", "was", "were",
            "have", "has", "had", "but", "not", "you", "your", "are", "its",
            "into", "than", "then", "them", "they", "would", "could", "about",
        )

        /** Curated keyword map — small, auditable, and offline. */
        val TOPIC_KEYWORDS: Map<String, List<String>> = linkedMapOf(
            "health" to listOf("gym", "workout", "doctor", "sleep", "health", "run", "running", "walk", "water", "diet"),
            "work" to listOf("meeting", "deadline", "office", "boss", "client", "project", "report", "presentation"),
            "finance" to listOf("salary", "rent", "bill", "invoice", "budget", "saving", "invest", "emi", "tax"),
            "learning" to listOf("study", "course", "book", "learn", "exam", "tutorial", "practice", "read"),
            "family" to listOf("family", "mom", "dad", "parents", "kids", "child", "wife", "husband", "sister", "brother"),
            "travel" to listOf("flight", "trip", "hotel", "train", "vacation", "passport", "luggage"),
            "food" to listOf("dinner", "lunch", "breakfast", "recipe", "cook", "restaurant", "coffee"),
            "home" to listOf("clean", "laundry", "repair", "kitchen", "grocery", "apartment"),
            "mood" to listOf("happy", "sad", "stressed", "anxious", "calm", "tired", "excited", "grateful"),
        )
    }
}
