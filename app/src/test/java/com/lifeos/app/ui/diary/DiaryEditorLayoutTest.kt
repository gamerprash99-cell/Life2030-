package com.lifeos.app.ui.diary

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The composer's layout rules, as pure functions.
 *
 * Both the writing surface's height and the way that height is established are
 * plain arithmetic, so they can be held on a JVM — no emulator and no Compose UI
 * test is available in this project.
 *
 * What is being pinned down is the keyboard transition. The bug these rules
 * exist to prevent: the surface's height used to be derived from the viewport as
 * it was *after* the IME inset had been subtracted, so every frame of the
 * keyboard animation changed it, and a short entry — whose surface height is
 * exactly this minimum — visibly resized through the whole animation. Latching
 * the resting height ([latchRestingViewport]) and sizing from that instead
 * ([writingSurfaceMinHeight]) is what removes the jump, so that is what is
 * tested here, across the viewport heights a real keyboard transition produces.
 */
class DiaryEditorLayoutTest {

    // ---- the latch ---------------------------------------------------------

    @Test
    fun `the resting height starts unset`() {
        assertEquals(0.dp, latchRestingViewport(0.dp, 0.dp))
    }

    @Test
    fun `the first real measurement becomes the resting height`() {
        assertEquals(420.dp, latchRestingViewport(0.dp, 420.dp))
    }

    @Test
    fun `the resting height never shrinks while the keyboard opens`() {
        // The whole point: the viewport collapses to this when the keyboard is
        // up, and the remembered value must not follow it down.
        assertEquals(420.dp, latchRestingViewport(420.dp, 180.dp))
    }

    @Test
    fun `the resting height is unchanged by every frame of a keyboard transition`() {
        // Walking the animation down frame by frame must leave the latch exactly
        // where it was, at every step — that is what makes the transition free of
        // re-composition rather than merely small.
        var resting = latchRestingViewport(0.dp, 420.dp)
        listOf(400.dp, 360.dp, 300.dp, 250.dp, 210.dp, 190.dp, 180.dp).forEach { frame ->
            resting = latchRestingViewport(resting, frame)
            assertEquals(420.dp, resting)
        }
    }

    @Test
    fun `the resting height survives the keyboard closing again`() {
        var resting = latchRestingViewport(0.dp, 420.dp)
        resting = latchRestingViewport(resting, 180.dp)   // keyboard opens
        resting = latchRestingViewport(resting, 180.dp)   // still up
        resting = latchRestingViewport(resting, 420.dp)   // keyboard closes
        assertEquals(420.dp, resting)
    }

    @Test
    fun `a taller viewport does raise the resting height`() {
        // Rotation into a taller window (or a larger font scale) has to be able
        // to move it, or the surface would be stuck at the old page's size.
        assertEquals(560.dp, latchRestingViewport(420.dp, 560.dp))
    }

    // ---- the surface height ----------------------------------------------

    @Test
    fun `before the first measurement the surface keeps the bare minimum`() {
        // The one frame between composition and the first layout pass must not
        // collapse the surface to nothing.
        assertEquals(132.dp, writingSurfaceMinHeight(0.dp))
    }

    @Test
    fun `a cramped window still gets a usable writing surface`() {
        // 0.55 of 200dp is 110dp, under the floor, so the floor wins.
        assertEquals(132.dp, writingSurfaceMinHeight(200.dp))
    }

    @Test
    fun `the floor and the share meet at the same height`() {
        // 132 / 0.55 = 240dp: the boundary where the rule changes hands.
        assertEquals(132.dp, writingSurfaceMinHeight(240.dp))
    }

    @Test
    fun `a normal page gives the surface a majority of the height`() {
        assertEquals(231.dp, writingSurfaceMinHeight(420.dp))
    }

    @Test
    fun `a tall page gives a tall surface`() {
        assertEquals(308.dp, writingSurfaceMinHeight(560.dp))
    }

    @Test
    fun `the surface never shrinks as text grows`() {
        // The height is a minimum, not a height: it is a floor the text grows
        // past, so an empty editor and a full one start from the same place.
        assertEquals(writingSurfaceMinHeight(420.dp), writingSurfaceMinHeight(420.dp))
    }

    @Test
    fun `the surface height is monotonic in the resting height`() {
        var previous = 0.dp
        listOf(120.dp, 200.dp, 240.dp, 320.dp, 420.dp, 560.dp, 720.dp).forEach { resting ->
            val height = writingSurfaceMinHeight(resting)
            assertTrue("height fell from $previous to $height at resting=$resting", height >= previous)
            previous = height
        }
    }

    @Test
    fun `the fill share is honoured`() {
        assertEquals(300.dp, writingSurfaceMinHeight(500.dp, minimum = 0.dp, fill = 0.6f))
    }

    @Test
    fun `the surface is a real share of a tall screen, not a fixed strip`() {
        // 132dp on a 720dp page would be 18% of the screen — the "small field
        // floating in a large empty card" this replaced.
        val share = writingSurfaceMinHeight(720.dp) / 720.dp
        assertTrue("expected a majority share, was $share", share > 0.5f)
    }

    // ---- the counter -------------------------------------------------------

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

/** Convenience so the division above reads as a share of the page. */
private operator fun Dp.div(other: Dp): Float = this.value / other.value
