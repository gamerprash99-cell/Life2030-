package com.lifeos.app.ui.insights

import org.junit.Assert.assertEquals
import org.junit.Test

class NarrativeScoreTest {

    @Test
    fun `inline score notation is parsed`() {
        assertEquals(62, narrativeScore("Overall: steady progress (score 62/100)."))
    }

    @Test
    fun `dash notation used by question answer is parsed`() {
        assertEquals(48, narrativeScore("Your productivity this week: getting started — 48/100 (tasks 30%, habits 50%)."))
    }

    @Test
    fun `first score wins when several are present`() {
        assertEquals(70, narrativeScore("score 70/100 and again score 40/100"))
    }

    @Test
    fun `missing score falls back to zero`() {
        assertEquals(0, narrativeScore("Overall: strong momentum."))
    }

    @Test
    fun `out of range numbers are clamped by parser or reported as is`() {
        assertEquals(120, narrativeScore("score 120/100"))
    }

    @Test
    fun `empty text falls back to zero`() {
        assertEquals(0, narrativeScore(""))
    }
}