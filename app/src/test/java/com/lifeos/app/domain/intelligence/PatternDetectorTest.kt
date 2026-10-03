package com.lifeos.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternDetectorTest {

    private val today = 20_000L

    private fun entry(
        day: Long,
        content: String,
        title: String? = null,
        tags: List<String> = emptyList()
    ) = DiarySnapshotEntry(
        id = "$day-$content",
        title = title,
        content = content,
        moodKey = null,
        tags = tags,
        dateEpochDay = day,
        timeMinutes = 600
    )

    private fun snapshot(vararg entries: DiarySnapshotEntry) = DiarySnapshot(entries.toList(), todayEpochDay = today)

    /**
     * A word repeated many times in one long entry must not outrank a word
     * spread across several days. That is the whole reason this class ranks by
     * distinct days.
     */
    @Test
    fun `a word spread across days outranks one repeated in a single entry`() {
        val analysis = PatternDetector.analyze(
            snapshot(
                entry(1, "sleep sleep sleep sleep sleep sleep sleep"),
                entry(2, "morning walk and a good coffee"),
                entry(3, "morning swim and good coffee"),
                entry(4, "morning run and good coffee"),
                entry(5, "morning yoga and good coffee")
            )
        )
        val words = analysis.recurringKeywords.map { it.word }
        assertTrue("morning" in words)
        assertFalse("sleep" in words)
    }

    @Test
    fun `a word below the distinct-day bar is not reported`() {
        val analysis = PatternDetector.analyze(
            snapshot(
                entry(1, "meditation"),
                entry(2, "meditation"),
                entry(3, "unrelated words entirely"),
                entry(4, "different content here"),
                entry(5, "more separate text")
            )
        )
        assertFalse(analysis.recurringKeywords.any { it.word == "meditation" })
    }

    @Test
    fun `too few entries report no patterns at all`() {
        val analysis = PatternDetector.analyze(
            snapshot(entry(1, "coffee coffee"), entry(2, "coffee coffee"))
        )
        assertTrue(analysis.isEmpty)
    }

    @Test
    fun `recurring keywords are ordered by distinct days`() {
        val analysis = PatternDetector.analyze(
            snapshot(
                entry(1, "coffee garden"),
                entry(2, "coffee garden"),
                entry(3, "coffee garden"),
                entry(4, "coffee"),
                entry(5, "coffee"),
                entry(6, "coffee")
            )
        )
        // coffee spans all six days; garden only the first three. Ranking by
        // distinct days puts coffee first and keeps both in the list.
        assertEquals("coffee", analysis.recurringKeywords.first().word)
        assertEquals(6, analysis.recurringKeywords.first().distinctDays)
        val garden = analysis.recurringKeywords.first { it.word == "garden" }
        assertEquals(3, garden.distinctDays)
    }

    /** Mood keys must not count as themes, or every happy entry reads as one. */
    @Test
    fun `mood is excluded from the keyword pool`() {
        val analysis = PatternDetector.analyze(
            snapshot(
                entry(1, "quiet day"),
                entry(2, "quiet day"),
                entry(3, "quiet day"),
                entry(4, "quiet day"),
                entry(5, "quiet day")
            )
        )
        assertFalse(analysis.recurringKeywords.any { it.word.contains("happy") })
    }

    @Test
    fun `a title counts toward the keyword pool`() {
        val analysis = PatternDetector.analyze(
            snapshot(
                entry(1, "kneaded dough", title = "sourdough"),
                entry(2, "oven noise", title = "sourdough"),
                entry(3, "crumb structure", title = "sourdough"),
                entry(4, "unrelated errand", title = "unrelated"),
                entry(5, "another errand", title = "unrelated")
            )
        )
        assertEquals("sourdough", analysis.recurringKeywords.first().word)
    }

    @Test
    fun `a tag used twice is reported and a one-off tag is not`() {
        val analysis = PatternDetector.analyze(
            snapshot(
                entry(1, "a", tags = listOf("Work", "travel")),
                entry(2, "b", tags = listOf("work")),
                entry(3, "c", tags = listOf("once")),
                entry(4, "d"),
                entry(5, "e")
            )
        )
        assertEquals(listOf("work"), analysis.topTags.map { it.word })
    }

    @Test
    fun `tags are deduplicated within one entry`() {
        val analysis = PatternDetector.analyze(
            snapshot(
                entry(1, "a", tags = listOf("work", "work")),
                entry(2, "b", tags = listOf("work")),
                entry(3, "c"),
                entry(4, "d"),
                entry(5, "e")
            )
        )
        assertEquals(2, analysis.topTags.first().occurrences)
    }

    /**
     * Without a share test, a journal spread evenly across seven days would
     * always crown a winner by a single entry and call it a habit.
     */
    @Test
    fun `an even weekday spread reports no weekday pattern`() {
        // One entry per weekday, so no single day clears the share test.
        val entries = (0..6).map { entry(today - it, "text $it") }
        val weekday = PatternDetector.analyze(snapshot(*entries.toTypedArray())).patterns
            .filter { it.kind == WritingPattern.Kind.WEEKDAY }
        assertTrue(weekday.isEmpty())
    }

    /** The converse: a single four-hour band holding everything *is* a pattern. */
    @Test
    fun `a time-of-day concentration is still reported on an even weekday spread`() {
        val entries = (0..6).map { entry(today - it, "text $it").copy(timeMinutes = 600) }
        val band = PatternDetector.analyze(snapshot(*entries.toTypedArray())).patterns
            .first { it.kind == WritingPattern.Kind.TIME_OF_DAY }
        assertEquals(7, band.count)
    }

    @Test
    fun `a genuine weekday concentration is reported`() {
        // Six of eight entries on the same weekday.
        val weekdayEpochDay = java.time.LocalDate.of(2026, 3, 7).toEpochDay() // Saturday
        val entries = (0..5).map { entry(weekdayEpochDay + it * 7, "saturday entry $it") } +
            listOf(entry(today, "odd one"), entry(today - 1, "odd two"))
        val patterns = PatternDetector.analyze(snapshot(*entries.toTypedArray())).patterns
        val saturday = patterns.firstOrNull { it.kind == WritingPattern.Kind.WEEKDAY }
        assertNotNull("expected a Saturday pattern in $patterns", saturday)
        assertTrue(saturday!!.label.contains("Saturday", ignoreCase = true))
    }

    @Test
    fun `a time-of-day concentration is reported`() {
        val entries = (0..5).map { entry(today, "late entry $it", ) }
            .map { it.copy(timeMinutes = 1_380) } // 23:00 -> band 5
        val patterns = PatternDetector.analyze(snapshot(*entries.toTypedArray())).patterns
        assertEquals(
            "Late evening",
            patterns.first { it.kind == WritingPattern.Kind.TIME_OF_DAY }.label
        )
    }

    @Test
    fun `time band labels cover every band`() {
        val labels = (0 until StatisticsEngine.TIME_BAND_COUNT).map { PatternDetector.timeBandLabel(it) }
        assertEquals(labels.size, labels.distinct().size)
        assertTrue(labels.none { it.isBlank() })
        assertEquals("During the day", PatternDetector.timeBandLabel(99))
    }

    @Test
    fun `most recent month comes from the newest entry`() {
        val entries = listOf(entry(19_000, "old"), entry(20_000, "new"))
        val month = PatternDetector.mostRecentMonth(snapshot(*entries.toTypedArray()))
        assertEquals(java.time.YearMonth.from(java.time.LocalDate.ofEpochDay(20_000)), month)
    }

    @Test
    fun `most recent month is null for an empty journal`() {
        assertEquals(null, PatternDetector.mostRecentMonth(DiarySnapshot.EMPTY))
    }
}