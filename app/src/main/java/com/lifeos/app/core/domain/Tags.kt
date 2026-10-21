package com.lifeos.app.core.domain

import java.util.Locale
import java.util.UUID

/** Tag normalization rules (spec: `#health` and `#Health` are the same tag). */
object Tags {
    const val MAX_LENGTH = 32
    const val MAX_TAGS_PER_ENTITY = 12

    /**
     * Lowercases, trims, collapses whitespace/underscores to a single hyphen, and
     * strips characters that are awkward in search tokens. Never returns a blank.
     */
    fun normalize(raw: String): String =
        raw.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("""[\s_]+"""), "-")
            .replace(Regex("""[^\p{L}\p{N}-]"""), "")
            .trim('-')
            .take(MAX_LENGTH)
            .trim('-')

    /** True when the raw text normalizes into a usable tag. */
    fun isValid(raw: String): Boolean = normalize(raw).isNotEmpty()

    /**
     * Splits free text into normalized, de-duplicated tags in first-seen order,
     * capped at [limit].
     */
    fun parseAll(text: String, limit: Int = MAX_TAGS_PER_ENTITY): List<String> {
        val seen = LinkedHashSet<String>()
        Regex("""#([\p{L}\p{N}_-]+)""").findAll(text).forEach { match ->
            val normalized = normalize(match.groupValues[1])
            if (normalized.isNotEmpty()) seen.add(normalized)
            if (seen.size >= limit) return seen.toList()
        }
        return seen.toList()
    }

    /** Parses a stored comma list back into tags. */
    fun fromStored(stored: String?): List<String> =
        stored?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    fun toStored(tags: Collection<String>): String =
        tags.map { normalize(it) }.filter { it.isNotEmpty() }
            .distinct().take(MAX_TAGS_PER_ENTITY).joinToString(",")
}

/** Id generation: UUID v4 strings, generated once at creation and never mutated. */
object Ids {
    fun new(): String = UUID.randomUUID().toString()

    fun isPlausible(id: String): Boolean =
        id.length in 8..64 && id.all { it.isLetterOrDigit() || it == '-' }
}
