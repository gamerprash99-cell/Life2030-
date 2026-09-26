package com.lifeos.app.domain.model

/**
 * Text measurements for the Diary composer and the entry detail metadata.
 *
 * Counted from the user's actual text so the "126 / 1000" counter and the
 * "126 words" detail row can never disagree with each other or drift from
 * what is stored in the database.
 */
object DiaryTextStats {

    /**
     * Whitespace-delimited word count.
     *
     * Runs of whitespace (including the newlines a paragraph break produces)
     * count as a single separator, so an empty or whitespace-only body is 0
     * rather than 1. The character cap counts real characters, ignoring nothing,
     * which is what the composer's limit is enforced against.
     */
    fun wordCount(text: String): Int =
        text.split(WHITESPACE)
            .count { it.isNotBlank() }

    fun characterCount(text: String): Int = text.length

    fun remainingCharacters(text: String, limit: Int): Int = (limit - text.length).coerceAtLeast(0)

    fun isOverLimit(text: String, limit: Int): Boolean = text.length > limit

    private val WHITESPACE = Regex("\\s+")
}
