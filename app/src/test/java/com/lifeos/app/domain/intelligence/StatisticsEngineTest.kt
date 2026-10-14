package com.lifeos.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StatisticsEngineTest {

    private val today = 20_000L

    private fun entry(
        day: Long,
        content: String = "one two three",
        minutes: Int = 600,
        favorite: Boolean = false,
        attachments: Int = 0
    ) = DiarySnapshotEntry(
        id = "$day-$minutes-$content",
        title = null,
        content = content,
        moodKey = null,
        tags = emptyList(),
        dateEpochDay = day,
        timeMinutes = minutes,
        isFavorite = favorite,
        attachmentCount = attachments
    )

    private fun snapshot(vararg entries: DiarySnapshotEntry) = DiarySnapshot(entries.toList(), todayEpochDay = today)

    @Test
    fun `an empty journal yields the empty statistics`() {
        val stats = StatisticsEngine.analyze(DiarySnapshot.EMPTY)
        assertEquals(0, stats.entryCount)
        assertNull(stats.averageWordsPerEntry)
        assertNull(stats.firstEntryEpochDay)
        assertNull(stats.latestEntryEpochDay)
    }

    @Test
    fun `counts entries, words and active days`() {
        val stats = StatisticsEngine.analyze(
            snapshot(entry(1, "one two three"), entry(1, "four five"), entry(2, "six"))
        )
        assertEquals(3, stats.entryCount)
        assertEquals(6, stats.wordCount)
        assertEquals(2, stats.activeDayCount)
    }

    /**
     * The rule this layer holds everywhere: below its minimum sample an average
     * is `null`, not a number produced by dividing by one.
     */
    @Test
    fun `word average is withheld below the minimum sample`() {
        val two = StatisticsEngine.analyze(snapshot(entry(1, "one two three"), entry(2, "four")))
        assertNull(two.averageWordsPerEntry)

        // 7 words over 3 entries, truncated: the field is an Int because the
        // UI shows it as a whole number and a mean of 2.33 words is not a
        // figure anyone acts on.
        val three = StatisticsEngine.analyze(
            snapshot(entry(1, "one two three"), entry(2, "four five"), entry(3, "six seven"))
        )
        assertEquals(7, three.wordCount)
        assertEquals(2, three.averageWordsPerEntry)
    }

    @Test
    fun `entries-per-day rate is withheld below the minimum active days`() {
        val stats = StatisticsEngine.analyze(snapshot(entry(1), entry(2)))
        assertNull(stats.averageEntriesPerActiveDay)
    }

    @Test
    fun `favorites and attachments are summed`() {
        val stats = StatisticsEngine.analyze(
            snapshot(
                entry(1, favorite = true, attachments = 2),
                entry(2, attachments = 1)
            )
        )
        assertEquals(1, stats.favoriteCount)
        assertEquals(3, stats.attachmentCount)
    }

    @Test
    fun `first and latest entry days bound the journal`() {
        val stats = StatisticsEngine.analyze(snapshot(entry(9), entry(3), entry(6)))
        assertEquals(3L, stats.firstEntryEpochDay)
        assertEquals(9L, stats.latestEntryEpochDay)
    }

    /** All seven days are always present so the chart's axis is stable. */
    @Test
    fun `weekday histogram always has seven days`() {
        val stats = StatisticsEngine.analyze(snapshot(entry(today)))
        assertEquals(7, stats.entriesByWeekday.size)
        assertEquals(DayOfWeek.values().toList(), stats.entriesByWeekday.map { it.dayOfWeek })
        assertEquals(1, stats.entriesByWeekday.sumOf { it.count })
    }

    @Test
    fun `entries land on the weekday their epoch day implies`() {
        val day = LocalDate.of(2026, 3, 14).toEpochDay() // a Saturday
        val stats = StatisticsEngine.analyze(snapshot(entry(day)))
        assertEquals(1, stats.entriesByWeekday.first { it.dayOfWeek == DayOfWeek.SATURDAY }.count)
    }

    @Test
    fun `time bands always have six slots`() {
        val stats = StatisticsEngine.analyze(snapshot(entry(today, minutes = 0), entry(today, minutes = 1439)))
        assertEquals(StatisticsEngine.TIME_BAND_COUNT, stats.entriesByTimeBand.size)
        assertEquals(1, stats.entriesByTimeBand.first().count) // 00:00 -> band 0
        assertEquals(1, stats.entriesByTimeBand.last().count) // 23:59 -> band 5
    }

    /**
     * A restored backup is not guaranteed to hold a sane `timeMinutes`, and the
     * editor's clamp only protects entries written in this build. An
     * out-of-range value must not be an index error on the Insights screen.
     */
    @Test
    fun `an out-of-range time is coerced instead of crashing`() {
        val stats = StatisticsEngine.analyze(
            snapshot(entry(today, minutes = -500), entry(today, minutes = 99_999))
        )
        assertEquals(2, stats.entriesByTimeBand.sumOf { it.count })
    }

    @Test
    fun `busiest day and band are withheld below the minimum`() {
        val stats = StatisticsEngine.analyze(snapshot(entry(today)))
        assertNull(StatisticsEngine.busiestWeekday(stats))
        assertNull(StatisticsEngine.busiestTimeBand(stats))
    }

    @Test
    fun `busiest day and band are reported above the minimum`() {
        val entries = (0..4).map { entry(today - it, minutes = 600, content = "w$it") }
        val stats = StatisticsEngine.analyze(snapshot(*entries.toTypedArray()))
        assertEquals(1, StatisticsEngine.busiestWeekday(stats)!!.count)
        assertEquals(5, StatisticsEngine.busiestTimeBand(stats)!!.count)
    }

    @Test
    fun `word count reuses the composer's own measure`() {
        // Must agree with DiaryTextStats exactly, or the Insights figure and the
        // entry's own "N words" row would disagree.
        val content = "  spaced   out \n\n words  "
        val stats = StatisticsEngine.analyze(snapshot(entry(1, content = content)))
        assertEquals(com.lifeos.app.domain.model.DiaryTextStats.wordCount(content), stats.wordCount)
    }
}