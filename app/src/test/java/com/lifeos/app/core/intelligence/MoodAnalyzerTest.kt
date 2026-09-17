package com.lifeos.app.core.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodAnalyzerTest {

    @Test
    fun `blank text is unknown`() {
        val result = MoodAnalyzer.analyze("   ")
        assertEquals(Mood.UNKNOWN, result.mood)
        assertTrue(result.positiveHits.isEmpty())
        assertTrue(result.negativeHits.isEmpty())
    }

    @Test
    fun `positive words are detected`() {
        val result = MoodAnalyzer.analyze("I am so happy and excited about this")
        assertTrue(result.mood in setOf(Mood.POSITIVE, Mood.VERY_POSITIVE))
        assertTrue(result.positiveHits.isNotEmpty())
    }

    @Test
    fun `negative words are detected`() {
        val result = MoodAnalyzer.analyze("I feel very sad and lonely today")
        assertTrue(result.mood in setOf(Mood.NEGATIVE, Mood.VERY_NEGATIVE))
        assertTrue(result.negativeHits.isNotEmpty())
    }

    @Test
    fun `negation flips sentiment`() {
        val result = MoodAnalyzer.analyze("I am not happy about this")
        assertTrue(result.mood in setOf(Mood.NEGATIVE, Mood.VERY_NEGATIVE))
    }

    @Test
    fun `uppercase positive words are detected`() {
        val result = MoodAnalyzer.analyze("I am very HAPPY today")
        assertTrue(result.mood in setOf(Mood.POSITIVE, Mood.VERY_POSITIVE))
    }

    @Test
    fun `no lexicon hits is unknown`() {
        val result = MoodAnalyzer.analyze("the of and")
        assertEquals(Mood.UNKNOWN, result.mood)
    }

    @Test
    fun `labels exist for every mood`() {
        for (mood in Mood.entries) {
            assertTrue(MoodAnalyzer.label(mood).isNotBlank())
        }
    }

    @Test
    fun `numeric value aggregation maps moods to range`() {
        assertEquals(2.0, MoodAnalyzer.numericValue(Mood.VERY_POSITIVE), 0.0)
        assertEquals(-2.0, MoodAnalyzer.numericValue(Mood.VERY_NEGATIVE), 0.0)
        assertEquals(0.0, MoodAnalyzer.numericValue(Mood.UNKNOWN), 0.0)
    }

    @Test
    fun `keyword extraction counts and caps results`() {
        val keywords = KeywordExtractor.extract("productivity and focus, focus on study")
        assertTrue(keywords.any { it.keyword == "focus" && it.count >= 2 })
        assertTrue(keywords.size <= 6)
    }

    @Test
    fun `keyword extraction across texts merges counts`() {
        val keywords = KeywordExtractor.extractAcross(listOf("study hard", "study again"))
        assertTrue(keywords.any { it.keyword == "study" && it.count >= 2 })
    }
}