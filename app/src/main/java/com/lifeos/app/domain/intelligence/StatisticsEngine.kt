package com.lifeos.app.domain.intelligence

import com.lifeos.app.domain.model.DiaryTextStats
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Counts what is in the journal.
 *
 * Every average here is `null` below its own minimum sample size rather than a
 * number produced by dividing by one or two. "Average 1000 words per entry" is
 * true and useless when it comes from a single entry, and the Insights screen
 * would have no way to tell the difference once it is a `Float`.
 */
object StatisticsEngine {

    /** Below this many entries, an average is not reported. */
    const val MIN_ENTRIES_FOR_WORD_AVERAGE = 3

    /** Below this many active days, an entries-per-day average is not reported. */
    const val MIN_DAYS_FOR_RATE = 3

    /** Four-hour bands: 00–04, 04–08, … 20–24. */
    const val TIME_BAND_COUNT = 6

    private const val MINUTES_PER_BAND = 240

    fun analyze(snapshot: DiarySnapshot): WritingStatistics {
        val entries = snapshot.entries
        if (entries.isEmpty()) return WritingStatistics.EMPTY

        val wordCount = entries.sumOf { DiaryTextStats.wordCount(it.content) }
        val activeDays = snapshot.daysWithEntries.size

        return WritingStatistics(
            entryCount = entries.size,
            wordCount = wordCount,
            activeDayCount = activeDays,
            averageWordsPerEntry = if (entries.size >= MIN_ENTRIES_FOR_WORD_AVERAGE) {
                wordCount / entries.size
            } else {
                null
            },
            averageEntriesPerActiveDay = if (activeDays >= MIN_DAYS_FOR_RATE) {
                entries.size.toFloat() / activeDays
            } else {
                null
            },
            favoriteCount = entries.count { it.isFavorite },
            attachmentCount = entries.sumOf { it.attachmentCount },
            entriesByWeekday = byWeekday(entries),
            entriesByTimeBand = byTimeBand(entries),
            firstEntryEpochDay = entries.minOfOrNull { it.dateEpochDay },
            latestEntryEpochDay = entries.maxOfOrNull { it.dateEpochDay }
        )
    }

    /**
     * Entries per day of the week, always all seven days, Monday first.
     *
     * Every day is present even at zero so the chart's axis is stable: a bar
     * chart that silently omits the days nobody wrote on would imply those days
     * do not exist, rather than that they were empty.
     */
    private fun byWeekday(entries: List<DiarySnapshotEntry>): List<DayOfWeekCount> {
        val counts = IntArray(DayOfWeek.entries.size)
        for (entry in entries) {
            val index = LocalDate.ofEpochDay(entry.dateEpochDay).dayOfWeek.value - 1
            counts[index]++
        }
        return DayOfWeek.entries.mapIndexed { index, day -> DayOfWeekCount(day, counts[index]) }
    }

    /** Entries per four-hour band of the day, always all six bands. */
    private fun byTimeBand(entries: List<DiarySnapshotEntry>): List<TimeBandCount> {
        val counts = IntArray(TIME_BAND_COUNT)
        for (entry in entries) {
            // Coerce rather than trust the column: the editor clamps to 0..1439,
            // but a restored backup could carry anything, and an out-of-range
            // index here would be an IndexOutOfBounds on the Insights screen.
            val minutes = entry.timeMinutes.coerceIn(0, 1439)
            counts[minutes / MINUTES_PER_BAND]++
        }
        return counts.mapIndexed { band, count -> TimeBandCount(band, count) }
    }

    /** The band with the most entries, or `null` below [MIN_DAYS_FOR_RATE]. */
    fun busiestTimeBand(statistics: WritingStatistics): TimeBandCount? {
        val total = statistics.entriesByTimeBand.sumOf { it.count }
        if (total < MIN_DAYS_FOR_RATE) return null
        return statistics.entriesByTimeBand.maxByOrNull { it.count }
    }

    /** The weekday with the most entries, or `null` below [MIN_DAYS_FOR_RATE]. */
    fun busiestWeekday(statistics: WritingStatistics): DayOfWeekCount? {
        val total = statistics.entriesByWeekday.sumOf { it.count }
        if (total < MIN_DAYS_FOR_RATE) return null
        return statistics.entriesByWeekday.maxByOrNull { it.count }
    }
}