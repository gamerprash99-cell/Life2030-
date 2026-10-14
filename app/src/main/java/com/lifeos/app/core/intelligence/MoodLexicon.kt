package com.lifeos.app.core.intelligence

/**
 * A small, hand-curated English sentiment lexicon. This is the "deterministic
 * NLP" approach the product spec calls for instead of a generative model:
 * each word carries a fixed weight, and a mood score is just the sum of
 * matched weights in a piece of text. Deliberately compact (a few hundred
 * words) to keep the app's footprint small — this whole approach is
 * plain-text Kotlin data, not a downloaded model file.
 */
object MoodLexicon {

    // Weight +2 = strong positive, +1 = mild positive, -1 = mild negative, -2 = strong negative.
    val WEIGHTS: Map<String, Int> = buildMap {
        listOf(
            "amazing", "wonderful", "excellent", "fantastic", "thrilled", "ecstatic",
            "love", "loved", "loving", "grateful", "blessed", "proud", "accomplished",
            "excited", "brilliant", "perfect", "delighted", "joy", "joyful", "elated"
        ).forEach { put(it, 2) }

        listOf(
            "good", "happy", "glad", "nice", "pleased", "content", "calm", "relaxed",
            "fine", "okay", "productive", "motivated", "hopeful", "peaceful", "confident",
            "satisfied", "fun", "great", "better", "improving", "energized", "focused"
        ).forEach { put(it, 1) }

        listOf(
            "tired", "bored", "meh", "annoyed", "irritated", "worried", "nervous",
            "confused", "uncertain", "restless", "distracted", "off", "low", "flat",
            "disappointed", "uneasy"
        ).forEach { put(it, -1) }

        listOf(
            "sad", "angry", "stressed", "anxious", "depressed", "exhausted", "overwhelmed",
            "hopeless", "miserable", "furious", "terrible", "awful", "horrible", "hate",
            "hated", "hating", "lonely", "hurt", "crying", "cried", "panic", "panicked",
            "burnout", "devastated", "heartbroken", "afraid", "scared"
        ).forEach { put(it, -2) }
    }

    /** Very common English words excluded from keyword extraction so real topics surface. */
    val STOPWORDS = setOf(
        "the", "a", "an", "and", "or", "but", "if", "then", "so", "because", "as",
        "of", "at", "by", "for", "with", "about", "against", "between", "into",
        "through", "during", "before", "after", "above", "below", "to", "from",
        "up", "down", "in", "out", "on", "off", "over", "under", "again", "further",
        "once", "here", "there", "when", "where", "why", "how", "all", "any", "both",
        "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not",
        "only", "own", "same", "than", "too", "very", "s", "t", "can", "will", "just",
        "don", "should", "now", "i", "me", "my", "myself", "we", "our", "ours",
        "you", "your", "yours", "he", "him", "his", "she", "her", "hers", "it",
        "its", "they", "them", "their", "was", "were", "be", "been", "being",
        "have", "has", "had", "having", "do", "does", "did", "doing", "am", "is",
        "are", "this", "that", "these", "those", "today", "yesterday", "tomorrow",
        "im", "ive", "dont", "didnt", "youre", "thats", "get", "got", "would",
        "could", "really", "also", "still", "went", "going", "go"
    )
}
