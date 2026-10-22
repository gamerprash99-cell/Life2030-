package com.lifeos.app.core.intelligence

/**
 * Detects mood from free text using [MoodLexicon] — pure lexicon lookup and
 * summation, no ML, no network. This is intentionally simple and explainable:
 * every score can be traced back to the exact words that produced it.
 */
object MoodAnalyzer {

    private val negators = setOf("not", "no", "never", "hardly", "barely", "without", "n't")
    private val wordRegex = Regex("[a-zA-Z']+")

    fun analyze(text: String): MoodResult {
        if (text.isBlank()) return MoodResult(Mood.UNKNOWN, 0, emptyList(), emptyList())

        val tokens = wordRegex.findAll(text.lowercase()).map { it.value }.toList()
        var score = 0
        val positiveHits = mutableListOf<String>()
        val negativeHits = mutableListOf<String>()

        for (i in tokens.indices) {
            val word = tokens[i]
            val weight = MoodLexicon.WEIGHTS[word] ?: continue
            val precededByNegator = i > 0 && tokens[i - 1] in negators
            val effectiveWeight = if (precededByNegator) -weight else weight
            score += effectiveWeight
            if (effectiveWeight > 0) positiveHits += word else if (effectiveWeight < 0) negativeHits += word
        }

        val clamped = score.coerceIn(-5, 5)
        val mood = when {
            positiveHits.isEmpty() && negativeHits.isEmpty() -> Mood.UNKNOWN
            clamped >= 3 -> Mood.VERY_POSITIVE
            clamped >= 1 -> Mood.POSITIVE
            clamped == 0 -> Mood.NEUTRAL
            clamped >= -2 -> Mood.NEGATIVE
            else -> Mood.VERY_NEGATIVE
        }

        return MoodResult(mood, clamped, positiveHits, negativeHits)
    }

    fun label(mood: Mood): String = when (mood) {
        Mood.VERY_POSITIVE -> "Very positive \uD83D\uDE04"
        Mood.POSITIVE -> "Positive \uD83D\uDE42"
        Mood.NEUTRAL -> "Neutral \uD83D\uDE10"
        Mood.NEGATIVE -> "Low \uD83D\uDE15"
        Mood.VERY_NEGATIVE -> "Very low \uD83D\uDE1E"
        Mood.UNKNOWN -> "Not enough text to tell"
    }

    /** A rough numeric mood value (-2..+2) used by TrendAnalyzer/CorrelationAnalyzer for averaging. */
    fun numericValue(mood: Mood): Double = when (mood) {
        Mood.VERY_POSITIVE -> 2.0
        Mood.POSITIVE -> 1.0
        Mood.NEUTRAL -> 0.0
        Mood.NEGATIVE -> -1.0
        Mood.VERY_NEGATIVE -> -2.0
        Mood.UNKNOWN -> 0.0
    }
}

/**
 * Simple frequency-based keyword extraction: tokenize, drop stopwords and
 * very short words, count occurrences, return the most frequent. This is
 * the same well-established technique behind many lightweight "tag cloud"
 * features — no embeddings, no model file.
 */
object KeywordExtractor {
    private val wordRegex = Regex("[a-zA-Z']+")

    fun extract(text: String, maxKeywords: Int = 6): List<KeywordResult> {
        if (text.isBlank()) return emptyList()
        val counts = LinkedHashMap<String, Int>()
        wordRegex.findAll(text.lowercase())
            .map { it.value.trim('\'') }
            .filter { it.length > 3 && it !in MoodLexicon.STOPWORDS }
            .forEach { word -> counts[word] = (counts[word] ?: 0) + 1 }

        return counts.entries
            .sortedByDescending { it.value }
            .take(maxKeywords)
            .map { KeywordResult(it.key, it.value) }
    }

    /** Extracts keywords across many texts at once (e.g. a week of diary entries), merging counts. */
    fun extractAcross(texts: List<String>, maxKeywords: Int = 8): List<KeywordResult> {
        val counts = LinkedHashMap<String, Int>()
        for (text in texts) {
            wordRegex.findAll(text.lowercase())
                .map { it.value.trim('\'') }
                .filter { it.length > 3 && it !in MoodLexicon.STOPWORDS }
                .forEach { word -> counts[word] = (counts[word] ?: 0) + 1 }
        }
        return counts.entries.sortedByDescending { it.value }.take(maxKeywords).map { KeywordResult(it.key, it.value) }
    }
}
