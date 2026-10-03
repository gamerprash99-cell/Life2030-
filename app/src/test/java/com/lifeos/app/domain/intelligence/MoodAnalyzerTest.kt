package com.lifeos.app.domain.intelligence

import com.lifeos.app.domain.model.MoodValence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MoodAnalyzerTest {

    private val today = 20_000L

    private fun entry(
        day: Long,
        mood: String? = null,
        minutes: Int = 600,
        id: String = "$day-$mood"
    ) = DiarySnapshotEntry(
        id = id,
        title = null,
        content = "some words here",
        moodKey = mood,
        tags = emptyList(),
        dateEpochDay = day,
        timeMinutes = minutes
    )

    private fun snapshot(vararg entries: DiarySnapshotEntry) =
        DiarySnapshot(entries.toList(), todayEpochDay = today)

    // ---- distribution ---------------------------------------------------

    @Test
    fun `no mood on any entry yields the empty analysis`() {
        val analysis = MoodAnalyzer.analyze(snapshot(entry(today, mood = null)))
        assertFalse(analysis.hasData)
        assertTrue(analysis.distribution.isEmpty())
        assertNull(analysis.dominantMoodKey)
    }

    @Test
    fun `distribution counts each mood and shares sum to one`() {
        val analysis = MoodAnalyzer.analyze(
            snapshot(
                entry(1, "😊 Happy"), entry(2, "😊 Happy"), entry(3, "😌 Calm"), entry(4, "😔 Sad")
            )
        )
        val happy = analysis.distribution.first { it.moodKey == "😊 Happy" }
        assertEquals(2, happy.count)
        assertEquals(0.5f, happy.share, 0.0001f)
        assertEquals(1f, analysis.distribution.sumOf { it.share.toDouble() }.toFloat(), 0.0001f)
    }

    @Test
    fun `distribution is ordered by count then label`() {
        val analysis = MoodAnalyzer.analyze(
            snapshot(
                entry(1, "😌 Calm"), entry(2, "😌 Calm"), entry(3, "😊 Happy"),
                entry(4, "😔 Sad"), entry(5, "😔 Sad")
            )
        )
        assertEquals(listOf("😌 Calm", "😔 Sad", "😊 Happy"), analysis.distribution.map { it.moodKey })
    }

    /**
     * A tie has no dominant mood, so none is reported. Picking one arbitrarily
     * would make the Insights headline flip between otherwise identical reads.
     */
    @Test
    fun `a tie at the top has no dominant mood`() {
        val analysis = MoodAnalyzer.analyze(snapshot(entry(1, "😊 Happy"), entry(2, "😌 Calm")))
        assertNull(analysis.dominantMoodKey)
    }

    @Test
    fun `an outright majority is the dominant mood`() {
        val analysis = MoodAnalyzer.analyze(
            snapshot(entry(1, "😊 Happy"), entry(2, "😊 Happy"), entry(3, "😌 Calm"))
        )
        assertEquals("😊 Happy", analysis.dominantMoodKey)
    }

    /**
     * An unrecognised mood must not be folded into the neutral bucket: that
     * would let one bad row dilute a real average.
     */
    @Test
    fun `an unresolvable mood is excluded from the valence shares`() {
        val analysis = MoodAnalyzer.analyze(
            snapshot(entry(1, "😊 Happy"), entry(2, "😔 Sad"), entry(3, "not-a-mood"))
        )
        assertEquals(3, analysis.taggedEntryCount)
        assertEquals(2, analysis.resolvedEntryCount)
        assertEquals(0.5f, analysis.positiveShare, 0.0001f)
        assertEquals(0.5f, analysis.negativeShare, 0.0001f)
        assertEquals(0f, analysis.neutralShare, 0.0001f)
    }

    @Test
    fun `valence of an unrecognised mood is null rather than neutral`() {
        assertEquals(MoodValence.POSITIVE, com.lifeos.app.domain.model.DiaryMoods.valenceOf("😊 Happy"))
        assertNull(com.lifeos.app.domain.model.DiaryMoods.valenceOf("not-a-mood"))
        assertNull(com.lifeos.app.domain.model.DiaryMoods.valenceOf(null))
    }

    // ---- dominant mood per day ------------------------------------------

    @Test
    fun `a day with one mood maps to that mood`() {
        val analysis = MoodAnalyzer.analyze(snapshot(entry(5, "🤩 Excited")))
        assertEquals("🤩 Excited", analysis.dominantMoodByDay[5L])
    }

    @Test
    fun `a day with several entries maps to its most frequent mood`() {
        val analysis = MoodAnalyzer.analyze(
            snapshot(
                entry(5, "😔 Sad", id = "a"), entry(5, "😔 Sad", id = "b"), entry(5, "😊 Happy", id = "c")
            )
        )
        assertEquals("😔 Sad", analysis.dominantMoodByDay[5L])
    }

    /** Same inputs must always paint the same dot, whichever way iteration happens to run. */
    @Test
    fun `a tie within one day resolves to the same mood every time`() {
        val first = MoodAnalyzer.analyze(snapshot(entry(5, "😌 Calm", id = "a"), entry(5, "😊 Happy", id = "b")))
        val second = MoodAnalyzer.analyze(snapshot(entry(5, "😊 Happy", id = "b"), entry(5, "😌 Calm", id = "a")))
        assertEquals(first.dominantMoodByDay[5L], second.dominantMoodByDay[5L])
    }

    // ---- trend ----------------------------------------------------------

    @Test
    fun `too few entries for a trend reports none`() {
        val analysis = MoodAnalyzer.analyze(
            snapshot(entry(today, "😊 Happy"), entry(today - 10, "😔 Sad"))
        )
        assertNull(analysis.trend.change)
        assertFalse(analysis.trend.isRising)
        assertFalse(analysis.trend.isFalling)
    }

    @Test
    fun `enough entries on both sides produce a rising trend`() {
        val entries = buildList {
            repeat(4) { add(entry(today - 1 - it, "😊 Happy")) }
            repeat(4) { add(entry(today - 10 - it, "😔 Sad")) }
        }
        val analysis = MoodAnalyzer.analyze(snapshot(*entries.toTypedArray()))
        assertTrue(analysis.trend.isRising)
        assertTrue(analysis.trend.change!! > 0.05f)
    }

    @Test
    fun `enough entries on both sides produce a falling trend`() {
        val entries = buildList {
            repeat(4) { add(entry(today - 1 - it, "😔 Sad")) }
            repeat(4) { add(entry(today - 10 - it, "😊 Happy")) }
        }
        val analysis = MoodAnalyzer.analyze(snapshot(*entries.toTypedArray()))
        assertTrue(analysis.trend.isFalling)
    }

    /** A day with no mood is a real gap, so the chart keeps the day with a zero score. */
    @Test
    fun `recent points include empty days as zero`() {
        val analysis = MoodAnalyzer.analyze(snapshot(entry(today, "😊 Happy")))
        assertEquals(MoodAnalyzer.TREND_WINDOW_DAYS, analysis.recentPoints.size)
        val todayPoint = analysis.recentPoints.last()
        assertEquals(today, todayPoint.epochDay)
        assertEquals(1f, todayPoint.valence, 0.0001f)
        val gap = analysis.recentPoints.first()
        assertEquals(0f, gap.valence, 0.0001f)
        assertEquals(0, gap.entryCount)
    }

    @Test
    fun `recent points are in ascending day order`() {
        val analysis = MoodAnalyzer.analyze(snapshot(entry(today, "😊 Happy")))
        val days = analysis.recentPoints.map { it.epochDay }
        assertEquals(days.sorted(), days)
        assertEquals(today, days.last())
    }

    // ---- streaks --------------------------------------------------------

    @Test
    fun `an empty journal has no streak`() {
        val streak = MoodAnalyzer.streaks(DiarySnapshot.EMPTY)
        assertEquals(0, streak.currentLength)
        assertEquals(0, streak.longestLength)
        assertFalse(streak.isAlive)
    }

    @Test
    fun `consecutive days ending today are a live streak`() {
        val entries = (0..4).map { entry(today - it, "😊 Happy", id = "e$it") }
        val streak = MoodAnalyzer.streaks(snapshot(*entries.toTypedArray()))
        assertEquals(5, streak.currentLength)
        assertTrue(streak.isAlive)
        assertEquals(5, streak.longestLength)
    }

    /**
     * The bug this guards: someone on a 30-day streak who has simply not
     * written *yet today* still has a 30-day streak. Reporting zero because
     * Tuesday is not over is the most demotivating bug a streak can have.
     */
    @Test
    fun `today being empty does not break a streak that ran to yesterday`() {
        val entries = (1..4).map { entry(today - it, "😊 Happy", id = "e$it") }
        val streak = MoodAnalyzer.streaks(snapshot(*entries.toTypedArray()))
        assertEquals(4, streak.currentLength)
        assertFalse(streak.isAlive)
    }

    @Test
    fun `a gap two days back ends the current streak`() {
        val entries = listOf(entry(today, "😊 Happy"), entry(today - 4, "😊 Happy"), entry(today - 5, "😊 Happy"))
        val streak = MoodAnalyzer.streaks(snapshot(*entries.toTypedArray()))
        assertEquals(1, streak.currentLength)
        assertEquals(2, streak.longestLength)
    }

    @Test
    fun `longest streak is the maximum run anywhere in history`() {
        val entries = listOf(
            entry(today, "😊 Happy"),
            entry(today - 1, "😊 Happy"),
            // gap
            entry(today - 3, "😊 Happy"), entry(today - 4, "😊 Happy"),
            entry(today - 5, "😊 Happy"), entry(today - 6, "😊 Happy")
        )
        val streak = MoodAnalyzer.streaks(snapshot(*entries.toTypedArray()))
        assertEquals(2, streak.currentLength)
        assertEquals(4, streak.longestLength)
    }

    /**
     * Streaks count entries, not moods, because mood is optional by design. A
     * user who never sets a mood must still get a streak.
     */
    @Test
    fun `streaks count days with entries regardless of mood`() {
        val entries = (0..2).map { entry(today - it, mood = null, id = "e$it") }
        assertEquals(3, MoodAnalyzer.streaks(snapshot(*entries.toTypedArray())).currentLength)
    }

    @Test
    fun `a future-dated entry does not break the streak arithmetic`() {
        // The editor clamps dates to the past, but a restored backup is not
        // guaranteed to. A future day must not create a phantom run.
        val entries = listOf(entry(today, "😊 Happy"), entry(today + 1, "😊 Happy"), entry(today + 2, "😊 Happy"))
        assertEquals(1, MoodAnalyzer.streaks(snapshot(*entries.toTypedArray())).currentLength)
    }

    @Test
    fun `epoch day of the snapshot is used, not the system clock`() {
        val realToday = LocalDate.now().toEpochDay()
        val entries = (0..2).map { entry(realToday - it, "😊 Happy", id = "e$it") }
        val streak = MoodAnalyzer.streaks(DiarySnapshot(entries, todayEpochDay = realToday))
        assertEquals(3, streak.currentLength)
        assertTrue(streak.isAlive)
    }
}