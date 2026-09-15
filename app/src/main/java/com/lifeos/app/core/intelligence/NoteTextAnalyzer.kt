package com.lifeos.app.core.intelligence

/**
 * Text operations on note/diary content that are genuinely achievable with
 * deterministic, extractive techniques — no generative model involved.
 *
 * IMPORTANT HONESTY NOTE: some of the original AI note actions (rewrite,
 * improve grammar, make longer, generate ideas, explain content, study
 * questions) are fundamentally generative writing tasks. A rule-based/
 * extractive engine cannot do these well, and faking them with garbled
 * output would be worse than not offering them. Those actions return a
 * clear explanation instead of fake content — see
 * LifeOSIntelligenceEngine.runNoteAction() for exactly which actions are
 * genuinely supported offline vs. which honestly say so.
 */
object NoteTextAnalyzer {

    private fun sentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

    /** Extractive summary: picks the sentences with the highest keyword density, in original order. */
    fun summarize(text: String, maxSentences: Int = 3): String {
        val sents = sentences(text)
        if (sents.size <= maxSentences) return text.trim()

        val keywords = KeywordExtractor.extract(text, maxKeywords = 10).map { it.keyword }.toSet()
        val scored = sents.mapIndexed { index, sentence ->
            val score = keywords.count { sentence.lowercase().contains(it) }
            Triple(index, sentence, score)
        }
        val top = scored.sortedByDescending { it.third }.take(maxSentences).sortedBy { it.first }
        return top.joinToString(" ") { it.second }
    }

    /** A short title guess from the most frequent keyword(s) or the first sentence as a fallback. */
    fun suggestTitle(text: String): String {
        val keywords = KeywordExtractor.extract(text, maxKeywords = 3)
        if (keywords.isNotEmpty()) {
            return keywords.joinToString(" · ") { it.keyword.replaceFirstChar { c -> c.uppercase() } }
        }
        val first = sentences(text).firstOrNull() ?: return "Untitled note"
        return if (first.length <= 40) first else first.take(37) + "…"
    }

    /** Reorganizes free text into a bullet list, one sentence per bullet. */
    fun organizeAsBullets(text: String): String {
        val sents = sentences(text)
        if (sents.isEmpty()) return text
        return sents.joinToString("\n") { "• $it" }
    }

    /** Same idea as organizeAsBullets, framed as "important points" (top keyword-scored subset). */
    fun extractKeyPoints(text: String, maxPoints: Int = 5): String {
        val sents = sentences(text)
        if (sents.size <= maxPoints) return sents.joinToString("\n") { "• $it" }
        val keywords = KeywordExtractor.extract(text, maxKeywords = 10).map { it.keyword }.toSet()
        val ranked = sents.map { s -> s to keywords.count { s.lowercase().contains(it) } }
            .sortedByDescending { it.second }
            .take(maxPoints)
            .map { it.first }
        return ranked.joinToString("\n") { "• $it" }
    }

    private val actionCues = listOf(
        "need to", "needs to", "have to", "should", "must", "todo", "to-do",
        "remember to", "don't forget", "make sure", "plan to", "going to"
    )
    private val actionVerbStarts = listOf(
        "call", "email", "buy", "send", "finish", "submit", "book", "schedule",
        "pay", "clean", "fix", "review", "prepare", "write", "read", "pick up",
        "renew", "cancel", "confirm", "order", "return", "reply"
    )

    /**
     * Finds sentences/lines that look like action items — used both for
     * "Create checklist" and for "Extract tasks". Heuristic, not perfect,
     * but genuinely useful and fully explainable.
     */
    fun findActionItems(text: String): List<String> {
        val candidates = text.split(Regex("[\\n.!?]"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length in 4..140 }

        return candidates.filter { line ->
            val lower = line.lowercase()
            actionCues.any { lower.contains(it) } || actionVerbStarts.any { lower.startsWith(it) }
        }.distinct()
    }

    fun createChecklist(text: String): String {
        val items = findActionItems(text)
        if (items.isEmpty()) return "No clear action items were found in this text."
        return items.joinToString("\n") { "\u2610 $it" }
    }
}
