package com.lifeos.app.ui.diary

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The composer's character counter, as a pure function.
 *
 * The counter's label and the 1000-character business rule are unchanged. This
 * file used to also pin a *latched* viewport height — a remembered value that
 * only ever grew — because the writing surface was sized from it and the latch
 * was what stopped the surface resizing through the keyboard animation. That
 * latch was the keyboard bug rather than the cure for it: a surface sized from a
 * remembered keyboard-closed height outgrew the viewport showing it, and
 * scrolling a viewport shorter than its own content pushed the text off the top
 * of the screen while the counter below it kept reporting the real length.
 *
 * The surface is now sized from the viewport's live constraints, and
 * [DiaryLayoutClearanceTest] holds the arithmetic that depends on them. Nothing
 * is remembered between layout passes any more, so there is no latch left here
 * to pin.
 */
class DiaryEditorLayoutTest {

    @Test
    fun `the counter reads an empty draft`() {
        assertEquals("0 of 1000", characterCounterLabel(0))
    }

    @Test
    fun `the counter reads a partial draft`() {
        assertEquals("412 of 1000", characterCounterLabel(412))
    }

    @Test
    fun `the counter reads a full draft`() {
        assertEquals("1000 of 1000", characterCounterLabel(1000))
    }

    @Test
    fun `the counter uses the composer's 1000 character limit by default`() {
        // The limit is a business rule; the counter reports it and does not set it.
        assertEquals(1000, characterCounterLabel(1).substringAfter(" of ").toInt())
    }

    @Test
    fun `the counter counts every character of the draft`() {
        val draft = "x".repeat(250)
        assertEquals("250 of 1000", characterCounterLabel(draft.length))
    }
}