package com.lifeos.app.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordExtractorTest {

    @Test
    fun `blank text yields no tokens`() {
        assertEquals(emptyList<String>(), KeywordExtractor.tokenize(""))
        assertEquals(emptyList<String>(), KeywordExtractor.tokenize("   \n\t "))
    }

    @Test
    fun `punctuation is stripped from the edges of a word`() {
        // The failure this guards: splitting on whitespace alone leaves "tired,"
        // as a token, so "tired" and "tired," become two different keywords.
        assertEquals(listOf("tired"), KeywordExtractor.tokenize("tired,"))
        assertEquals(listOf("tired"), KeywordExtractor.tokenize("\"tired.\""))
    }

    @Test
    fun `intra-word apostrophes and hyphens are kept`() {
        // Both are real words a diary contains; splitting on non-letters would
        // shatter them into "don" / "t" and "long" / "haul".
        assertTrue(KeywordExtractor.tokenize("I don't regret it").contains("don't"))
        assertTrue(KeywordExtractor.tokenize("a long-haul flight").contains("long-haul"))
    }

    @Test
    fun `function words are dropped`() {
        // "were", "there" and "with" are all on the stop list; "the" and "and"
        // are too. Only the two content words survive.
        assertEquals(
            listOf("cat", "dog"),
            KeywordExtractor.tokenize("The cat and the dog were there with us").sorted()
        )
    }

    @Test
    fun `short tokens are dropped`() {
        val tokens = KeywordExtractor.tokenize("it is ok to go now")
        assertTrue(tokens.none { it.length < 3 })
    }

    @Test
    fun `uppercase input is lowercased`() {
        assertEquals(listOf("coffee", "morning"), KeywordExtractor.tokenize("Coffee MORNING"))
    }

    @Test
    fun `counts accumulate across texts`() {
        val counts = KeywordExtractor.countAll(listOf("work and rest", "more work today"))
        assertEquals(2, counts["work"])
        assertEquals(1, counts["rest"])
    }

    /**
     * Equal counts must not reorder between runs. The Insights screen renders
     * this list, and a list that reshuffles on recomposition reads as a glitch.
     */
    @Test
    fun `ties break alphabetically so the order is stable`() {
        val first = KeywordExtractor.topKeywords(listOf("zebra apple mango"), limit = 3)
        val second = KeywordExtractor.topKeywords(listOf("mango apple zebra"), limit = 3)
        assertEquals(listOf("apple", "mango", "zebra"), first.map { it.first })
        assertEquals(first, second)
    }

    @Test
    fun `most frequent word sorts first`() {
        val top = KeywordExtractor.topKeywords(listOf("cat cat cat dog dog bird"), limit = 3)
        assertEquals("cat", top.first().first)
        assertEquals(3, top.first().second)
    }

    @Test
    fun `non-positive limit returns nothing`() {
        assertEquals(emptyList<Pair<String, Int>>(), KeywordExtractor.topKeywords(listOf("cat"), limit = 0))
        assertEquals(emptyList<Pair<String, Int>>(), KeywordExtractor.topKeywords(listOf("cat"), limit = -1))
    }

    @Test
    fun `a single emoji-only entry yields no keywords`() {
        assertEquals(emptyList<String>(), KeywordExtractor.tokenize("😊 😊 😊"))
    }
}