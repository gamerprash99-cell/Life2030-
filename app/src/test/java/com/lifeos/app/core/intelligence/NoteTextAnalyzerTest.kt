package com.lifeos.app.core.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteTextAnalyzerTest {

    @Test
    fun `short text is summarized verbatim`() {
        val text = "Short note."
        assertEquals(text, NoteTextAnalyzer.summarize(text))
    }

    @Test
    fun `summarize reduces to at most the requested number of sentences`() {
        val text = ("One two three four. ".repeat(6)).trim()
        val summary = NoteTextAnalyzer.summarize(text, maxSentences = 3)
        val count = summary.split(Regex("(?<=[.!?])\\s+")).count()
        assertEquals(3, count)
    }

    @Test
    fun `suggest title capitalizes the top keywords`() {
        val title = NoteTextAnalyzer.suggestTitle("productivity and focus, focus on study")
        assertEquals("Focus · Productivity · Study", title)
    }

    @Test
    fun `suggest title falls back for empty text`() {
        assertEquals("Untitled note", NoteTextAnalyzer.suggestTitle("   "))
    }

    @Test
    fun `bullets keep one sentence per line`() {
        val organized = NoteTextAnalyzer.organizeAsBullets("Hello there. This is fun.")
        val lines = organized.lines()
        assertTrue(lines[0].startsWith("• "))
        assertTrue(lines[1].startsWith("• "))
    }

    @Test
    fun `action items are extracted by cues and verbs`() {
        val items = NoteTextAnalyzer.findActionItems(
            "I need to call the bank.\nBuy groceries tomorrow.\nIt was a nice walk."
        )
        assertTrue(items.any { it.startsWith("I need to call the bank") })
        assertTrue(items.any { it.startsWith("Buy groceries tomorrow") })
    }

    @Test
    fun `checklist renders unchecked boxes`() {
        val checklist = NoteTextAnalyzer.createChecklist("Remember to renew the pass and pay the rent.")
        assertTrue(checklist.startsWith("\u2610"))
        assertTrue(checklist.contains("renew the pass"))
    }

    @Test
    fun `checklist with no action items is honest`() {
        val checklist = NoteTextAnalyzer.createChecklist("The weather today is quite pleasant.")
        assertTrue(checklist.contains("No clear action items"))
    }
}