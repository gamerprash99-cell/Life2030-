package com.lifeos.app.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tag normalization: `#Health` and `#health` must resolve to the same tag. */
class TagsTest {

    @Test
    fun `normalize lowercases`() {
        assertEquals("health", Tags.normalize("Health"))
    }

    @Test
    fun `normalize collapses spaces and underscores to hyphens`() {
        assertEquals("deep-work", Tags.normalize("Deep  Work"))
        assertEquals("deep-work", Tags.normalize("deep_work"))
    }

    @Test
    fun `normalize strips punctuation and trims hyphens`() {
        assertEquals("todo", Tags.normalize("--todo!!"))
    }

    @Test
    fun `normalize enforces the length cap`() {
        val long = "a".repeat(100)
        assertEquals(Tags.MAX_LENGTH, Tags.normalize(long).length)
    }

    @Test
    fun `isValid rejects text that normalizes to nothing`() {
        assertTrue(Tags.isValid("health"))
        assertTrue(!Tags.isValid("!!!"))
        assertTrue(!Tags.isValid("   "))
    }

    @Test
    fun `parseAll extracts hashed tags in first-seen order`() {
        val tags = Tags.parseAll("Went to #gym then #Reading and #gym again")
        assertEquals(listOf("gym", "reading"), tags)
    }

    @Test
    fun `parseAll caps the number of tags`() {
        val text = (1..20).joinToString(" ") { "#tag$it" }
        assertEquals(Tags.MAX_TAGS_PER_ENTITY, Tags.parseAll(text).size)
    }

    @Test
    fun `stored round-trips through normalization and dedupe`() {
        val stored = Tags.toStored(listOf("Health", "health", "Deep Work"))
        assertEquals("health,deep-work", stored)
        assertEquals(listOf("health", "deep-work"), Tags.fromStored(stored))
    }

    @Test
    fun `fromStored of null is empty`() {
        assertEquals(emptyList<String>(), Tags.fromStored(null))
    }
}
