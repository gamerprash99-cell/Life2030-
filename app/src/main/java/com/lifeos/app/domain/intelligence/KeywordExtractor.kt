package com.lifeos.app.domain.intelligence

/**
 * Turns entry text into ranked words.
 *
 * Pure and synchronous: tokenisation is a few string operations, and being
 * able to call it from a loop without thinking about dispatchers is what lets
 * [PatternDetector] stay a plain function.
 *
 * The ranking is raw frequency, which is the simplest honest measure and also
 * the one with a known bias: a word repeated ten times in one entry outranks a
 * word used once in ten entries. That is a real limitation and it is why
 * [PatternDetector] also reports `distinctDays` — a companion measure that
 * catches the opposite bias. Neither alone is sufficient.
 */
object KeywordExtractor {

    /**
     * Words carrying no topical signal, dropped before counting.
     *
     * English only, and deliberately small: a large stopword list is easy to
     * write and easy to get wrong, and an over-long list quietly deletes real
     * words a user actually writes about. These are the function words that
     * dominate any English text and would otherwise crowd out everything.
     */
    private val STOP_WORDS = setOf(
        "the", "and", "but", "for", "with", "from", "into", "onto", "over", "under",
        "this", "that", "these", "those", "there", "here", "then", "than", "when", "where",
        "what", "which", "who", "whom", "whose", "why", "how", "was", "were", "been",
        "being", "are", "am", "is", "be", "do", "does", "did", "doing", "done",
        "have", "has", "had", "having", "will", "would", "shall", "should", "can", "could",
        "may", "might", "must", "not", "no", "nor", "yes", "you", "your", "yours",
        "i", "me", "my", "mine", "we", "us", "our", "ours", "they", "them", "their",
        "theirs", "he", "him", "his", "she", "her", "hers", "it", "its", "as", "at", "by",
        "in", "of", "on", "to", "up", "out", "off", "so", "if", "or", "just", "very",
        "too", "also", "because", "about", "after", "before", "again", "all", "any",
        "each", "more", "most", "other", "some", "such", "only", "own", "same", "own",
        "get", "got", "went", "come", "came", "just", "like", "really", "much", "still"
    )

    /** Tokens shorter than this are never topical. "ok" and "no" carry nothing. */
    private const val MIN_WORD_LENGTH = 3

    /** Beyond this, emoji and punctuation runs are discarded rather than counted. */
    private const val MAX_TOKEN_LENGTH = 32

    /**
     * Letters, digits and intra-word apostrophes/hyphens. Splitting on
     * whitespace alone would keep the trailing comma off "tired," as part of the
     * word; splitting on non-letters instead keeps "don't" and "long-haul" whole,
     * which matters because both are real words a diary actually contains.
     */
    private val TOKEN = Regex("[\\p{L}\\p{N}]+(?:['’-][\\p{L}\\p{N}]+)*")

    /** Lowercase word tokens from [text], stopwords and short tokens removed. */
    fun tokenize(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return TOKEN.findAll(text.lowercase())
            .map { it.value }
            .filter { it.length >= MIN_WORD_LENGTH && it.length <= MAX_TOKEN_LENGTH }
            .filterNot { it in STOP_WORDS }
            .toList()
    }

    /** Counted occurrences of each topical word across [texts]. */
    fun countAll(texts: Iterable<String>): Map<String, Int> {
        val counts = HashMap<String, Int>()
        for (text in texts) {
            for (word in tokenize(text)) counts.merge(word, 1, Int::plus)
        }
        return counts
    }

    /**
     * The [limit] most frequent words across [texts], descending by count and
     * then alphabetically so equal counts never reorder between runs.
     *
     * The tiebreak is not cosmetic: the Insights screen renders this list, and
     * a list that reshuffles on every recomposition reads as a glitch.
     */
    fun topKeywords(texts: Iterable<String>, limit: Int = DEFAULT_LIMIT): List<Pair<String, Int>> {
        if (limit <= 0) return emptyList()
        return countAll(texts).entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { it.key to it.value }
    }

    const val DEFAULT_LIMIT = 8
}