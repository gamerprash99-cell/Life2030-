package com.lifeos.app.domain.intelligence

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Finds things that show up *repeatedly*, across entries and over time.
 *
 * This exists to cover the gap [KeywordExtractor] leaves. Raw frequency ranks
 * the word you repeated most in one long entry above the word you returned to
 * across a month, which is almost never what "a recurring theme" means. So
 * everything here is ranked by **distinct days** and reports the occurrence
 * count alongside, letting the UI show both.
 *
 * The bar is deliberately high — [MIN_DAYS_FOR_A_RECURRING_WORD] days — because
 * a word that appears on two days is a coincidence far more often than it is a
 * theme, and a "recurring themes" panel full of two-day coincidences would make
 * the whole feature untrustworthy.
 */
object PatternDetector {

    /** A word must appear on at least this many distinct days to be "recurring". */
    const val MIN_DAYS_FOR_A_RECURRING_WORD = 3

    /** A weekday or time band must hold this share of entries to be called a pattern. */
    const val MIN_SHARE_FOR_A_PATTERN = 0.4f

    /** Below this many entries, no pattern is reported at all. */
    const val MIN_ENTRIES_FOR_PATTERNS = 5

    private const val MIN_TAG_LENGTH = 2
    private const val MAX_TAG_LENGTH = 32
    const val MAX_KEYWORDS = 6
    const val MAX_TAGS = 6

    fun analyze(snapshot: DiarySnapshot): PatternAnalysis {
        if (snapshot.entries.size < MIN_ENTRIES_FOR_PATTERNS) return PatternAnalysis.EMPTY

        return PatternAnalysis(
            recurringKeywords = recurringKeywords(snapshot),
            topTags = topTags(snapshot),
            patterns = temporalPatterns(snapshot)
        )
    }

    /**
     * Topical words ranked by how many *distinct days* they appear on.
     *
     * A word must clear [MIN_DAYS_FOR_A_RECURRING_WORD] days to appear at all,
     * so the list can legitimately be empty and the UI must handle that.
     */
    private fun recurringKeywords(snapshot: DiarySnapshot): List<RecurringKeyword> {
        val daysPerWord = HashMap<String, MutableSet<Long>>()
        val occurrences = HashMap<String, Int>()

        for (entry in snapshot.entries) {
            // The title is searchable and part of what the user chose to write,
            // so it belongs in the same pool as the body. Mood keys are excluded
            // because they would make every entry containing the word "happy"
            // look like a theme about happiness.
            val words = KeywordExtractor.tokenize("${entry.displayHeading} ${entry.content}")
            for (word in words) {
                daysPerWord.getOrPut(word) { mutableSetOf() }.add(entry.dateEpochDay)
                occurrences.merge(word, 1, Int::plus)
            }
        }

        return daysPerWord.entries
            .filter { it.value.size >= MIN_DAYS_FOR_A_RECURRING_WORD }
            .sortedWith(
                compareByDescending<Map.Entry<String, Set<Long>>> { it.value.size }
                    .thenByDescending { occurrences[it.key] ?: 0 }
                    .thenBy { it.key }
            )
            .take(MAX_KEYWORDS)
            .map { (word, days) ->
                RecurringKeyword(word = word, occurrences = occurrences[word] ?: days.size, distinctDays = days.size)
            }
    }

    /** User-written tags, ranked by how many distinct entries carry each. */
    private fun topTags(snapshot: DiarySnapshot): List<RecurringKeyword> {
        val perTag = HashMap<String, MutableSet<String>>()
        for (entry in snapshot.entries) {
            for (raw in entry.tags) {
                val tag = raw.trim().lowercase()
                if (tag.length !in MIN_TAG_LENGTH..MAX_TAG_LENGTH) continue
                // Keyed by entry id, so two identical tags on one entry count once.
                perTag.getOrPut(tag) { mutableSetOf() }.add(entry.id)
            }
        }
        return perTag.entries
            .filter { it.value.size >= 2 }
            .sortedWith(
                compareByDescending<Map.Entry<String, Set<String>>> { it.value.size }.thenBy { it.key }
            )
            .take(MAX_TAGS)
            .map { (tag, ids) -> RecurringKeyword(tag, occurrences = ids.size, distinctDays = ids.size) }
    }

    /**
     * Weekday and time-of-day concentrations.
     *
     * A pattern is only reported when one bucket holds [MIN_SHARE_FOR_A_PATTERN]
     * of all entries *and* is meaningfully ahead of the field. Without the share
     * test, a journal spread evenly across seven days would always crown a
     * winner by a single entry and call it a habit.
     */
    private fun temporalPatterns(snapshot: DiarySnapshot): List<WritingPattern> {
        val total = snapshot.entries.size.toFloat()
        val patterns = mutableListOf<WritingPattern>()

        // Counted once and reused: both lookups read the same histograms, and
        // recomputing them here would re-walk every entry a second time for a
        // result that cannot differ.
        val statistics = StatisticsEngine.analyze(snapshot)

        val weekday = StatisticsEngine.busiestWeekday(statistics)
        if (weekday != null && weekday.count.toFloat() / total >= MIN_SHARE_FOR_A_PATTERN) {
            patterns += WritingPattern(
                kind = WritingPattern.Kind.WEEKDAY,
                label = weekday.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                count = weekday.count,
                share = weekday.count.toFloat() / total
            )
        }

        val band = StatisticsEngine.busiestTimeBand(statistics)
        if (band != null && band.count.toFloat() / total >= MIN_SHARE_FOR_A_PATTERN) {
            patterns += WritingPattern(
                kind = WritingPattern.Kind.TIME_OF_DAY,
                label = timeBandLabel(band.band),
                count = band.count,
                share = band.count.toFloat() / total
            )
        }

        return patterns
    }

    /** "Early morning", "Late morning", … for a four-hour band index. */
    internal fun timeBandLabel(band: Int): String = when (band) {
        0 -> "Late night"
        1 -> "Early morning"
        2 -> "Late morning"
        3 -> "Afternoon"
        4 -> "Early evening"
        5 -> "Late evening"
        else -> "During the day"
    }

    /** The month a snapshot's entries fall in, for "Looking back". */
    fun mostRecentMonth(snapshot: DiarySnapshot): YearMonth? =
        snapshot.entries.maxOfOrNull { it.dateEpochDay }?.let { YearMonth.from(LocalDate.ofEpochDay(it)) }
}