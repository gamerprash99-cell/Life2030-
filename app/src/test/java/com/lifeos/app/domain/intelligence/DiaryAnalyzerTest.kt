package com.lifeos.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryAnalyzerTest {

    private val today = 20_000L

    private fun row(
        id: String,
        content: String = "content",
        mood: String? = null,
        day: Long = today,
        tags: String = "",
        attachments: Int = 0,
        favorite: Boolean = false
    ) = DiaryAnalyzer.SnapshotRow(
        id = id,
        title = null,
        content = content,
        mood = mood,
        tagsCsv = tags,
        dateEpochDay = day,
        timeMinutes = 600,
        isFavorite = favorite,
        attachmentCount = attachments
    )

    // ---- the facade -----------------------------------------------------

    @Test
    fun `an empty journal produces empty insights and never throws`() {
        val insights = DiaryAnalyzer.analyze(DiarySnapshot.EMPTY)
        assertEquals(0, insights.statistics.entryCount)
        assertFalse(insights.mood.hasData)
        assertTrue(insights.patterns.isEmpty)
        assertNotNull(insights.question.text)
    }

    /**
     * One snapshot in, one consistent bundle out. If the screen read statistics
     * and moods from two separate reads, a save landing between them would
     * produce figures that disagree — and the user could not tell which was
     * stale.
     */
    @Test
    fun `the same snapshot always yields the same bundle`() {
        val snapshot = DiaryAnalyzer.snapshotOf(
            listOf(
                row("1", content = "coffee and a walk", mood = "😊 Happy", tags = "health,work"),
                row("2", content = "coffee in the morning", mood = "😌 Calm", day = today - 1, tags = "health"),
                row("3", content = "more coffee thoughts", mood = "😊 Happy", day = today - 2, tags = "health")
            ),
            todayEpochDay = today
        )
        val first = DiaryAnalyzer.analyze(snapshot)
        val second = DiaryAnalyzer.analyze(snapshot)
        assertEquals(first.statistics, second.statistics)
        assertEquals(first.mood, second.mood)
        assertEquals(first.patterns, second.patterns)
        assertEquals(first.question, second.question)
    }

    @Test
    fun `sparse journals are flagged so the screen can show its own state`() {
        val sparse = DiaryAnalyzer.analyze(DiaryAnalyzer.snapshotOf(listOf(row("1")), today))
        assertTrue(sparse.isSparse)
        assertEquals(DiaryAnalyzer.MIN_ENTRIES_FOR_INSIGHTS, 3)
    }

    @Test
    fun `a journal at the threshold is no longer sparse`() {
        val enough = DiaryAnalyzer.analyze(
            DiaryAnalyzer.snapshotOf(listOf(row("1"), row("2"), row("3")), today)
        )
        assertFalse(enough.isSparse)
    }

    // ---- row mapping ----------------------------------------------------

    @Test
    fun `tags are split, trimmed and blanks dropped`() {
        val snapshot = DiaryAnalyzer.snapshotOf(listOf(row("1", tags = " work , , health ")), today)
        assertEquals(listOf("work", "health"), snapshot.entries.single().tags)
    }

    @Test
    fun `an empty tag column yields no tags`() {
        assertTrue(DiaryAnalyzer.snapshotOf(listOf(row("1", tags = "")), today).entries.single().tags.isEmpty())
    }

    @Test
    fun `attachment count carries through from the decoded row`() {
        val snapshot = DiaryAnalyzer.snapshotOf(listOf(row("1", attachments = 3)), today)
        assertEquals(3, snapshot.entries.single().attachmentCount)
        assertEquals(3, DiaryAnalyzer.analyze(snapshot).statistics.attachmentCount)
    }

    // ---- display heading ------------------------------------------------

    @Test
    fun `display heading prefers the title`() {
        val entry = DiarySnapshotEntry("1", "A title", "body text", null, emptyList(), today, 600)
        assertEquals("A title", entry.displayHeading)
    }

    @Test
    fun `display heading falls back to the first non-blank line`() {
        val entry = DiarySnapshotEntry("1", null, "\n\n  first real line\nsecond", null, emptyList(), today, 600)
        assertEquals("first real line", entry.displayHeading)
    }

    @Test
    fun `display heading is empty when there is neither title nor content`() {
        assertEquals("", DiarySnapshotEntry("1", null, "   ", null, emptyList(), today, 600).displayHeading)
    }

    @Test
    fun `a blank title is treated as absent rather than rendered`() {
        val entry = DiarySnapshotEntry("1", "   ", "the body", null, emptyList(), today, 600)
        assertEquals("the body", entry.displayHeading)
    }

    // ---- streak ---------------------------------------------------------

    @Test
    fun `streak comes from the snapshot, not from any analyzer output`() {
        val snapshot = DiaryAnalyzer.snapshotOf(
            listOf(row("1", day = today), row("2", day = today - 1), row("3", day = today - 2)),
            today
        )
        assertEquals(3, DiaryAnalyzer.streak(snapshot).currentLength)
    }

    // ---- question -------------------------------------------------------

    @Test
    fun `an empty journal gets an onboarding question`() {
        val question = DiaryAnalyzer.analyze(DiarySnapshot.EMPTY).question
        assertEquals(DiaryQuestion.Kind.ONBOARDING, question.kind)
    }

    @Test
    fun `a single entry gets a present-tense question, not a reference to itself`() {
        val question = DiaryAnalyzer.analyze(
            DiaryAnalyzer.snapshotOf(listOf(row("1")), today)
        ).question
        assertEquals(DiaryQuestion.Kind.PRESENT, question.kind)
        assertTrue(question.isForToday)
    }

    @Test
    fun `a journal with history gets a reference question`() {
        val question = DiaryAnalyzer.analyze(
            DiaryAnalyzer.snapshotOf(listOf(row("1"), row("2", day = today - 1)), today)
        ).question
        assertEquals(DiaryQuestion.Kind.PAST_ENTRY, question.kind)
    }

    /** Nothing about the user is analysed to produce question text — only to choose a known one. */
    @Test
    fun `questions never mention a mood as a finding`() {
        val snapshot = DiaryAnalyzer.snapshotOf(
            listOf(
                row("1", mood = "😰 Anxious"), row("2", mood = "😰 Anxious", day = today - 1),
                row("3", mood = "😰 Anxious", day = today - 2)
            ),
            today
        )
        val insights = DiaryAnalyzer.analyze(snapshot)
        val text = insights.question.text.lowercase()
        listOf("anxious", "anxiety", "stressed", "diagnosis", "you seem", "diagnos").forEach { banned ->
            assertFalse("question leaked '$banned': $text", text.contains(banned))
        }
    }

    @Test
    fun `the question is stable for a given day`() {
        val snapshot = DiaryAnalyzer.snapshotOf(listOf(row("1"), row("2", day = today - 1)), today)
        assertEquals(LocalQuestionEngine.questionFor(snapshot), LocalQuestionEngine.questionFor(snapshot))
    }

    @Test
    fun `the question varies across days`() {
        val questions = (0..30).map { offset ->
            LocalQuestionEngine.questionFor(
                DiaryAnalyzer.snapshotOf(listOf(row("1", day = today)), todayEpochDay = today - offset)
            ).text
        }
        assertTrue("questions should not all be identical", questions.distinct().size > 1)
    }

    @Test
    fun `a theme question names the theme when one exists`() {
        val rows = (0..5).map { day ->
            row("id$day", content = "coffee and conversation about coffee", day = today - day)
        }
        val question = DiaryAnalyzer.analyze(DiaryAnalyzer.snapshotOf(rows, today)).question
        assertEquals(DiaryQuestion.Kind.THEME, question.kind)
        assertTrue(question.text.contains("coffee"))
    }

    @Test
    fun `the fallback question always has text`() {
        assertTrue(DiaryQuestion.forDay(today).text.isNotBlank())
        assertTrue(DiaryQuestion.forDay(today).isForToday)
    }
}