package com.lifeos.app.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingChronometerTest {

    private var now = 1_000_000L

    /** The injectable clock, passed as a function reference as the class expects. */
    private fun clock(): Long = now

    @Test
    fun `started chronometer is running and counts real elapsed time`() {
        val c = RecordingChronometer(::clock)
        c.start()
        assertTrue(c.isRunning)
        assertEquals(0L, c.elapsedMillis())

        now += 5_000 // 5 seconds pass
        assertEquals(5_000L, c.elapsedMillis())
    }

    @Test
    fun `pause freezes elapsed time resume continues`() {
        val c = RecordingChronometer(::clock)
        c.start()
        now += 3_000
        c.pause()
        assertFalse(c.isRunning)
        assertEquals(3_000L, c.elapsedMillis())

        now += 7_000 // paused time must not accumulate
        assertEquals(3_000L, c.elapsedMillis())

        c.resume()
        assertTrue(c.isRunning)
        now += 2_000
        assertEquals(5_000L, c.elapsedMillis())
    }

    @Test
    fun `multiple pause resume segments sum only running stretches`() {
        val c = RecordingChronometer(::clock)
        c.start()
        now += 1_000
        c.pause()
        now += 9_000 // ignored while paused
        c.resume()
        now += 2_000
        c.pause()
        assertEquals(3_000L, c.elapsedMillis())
    }

    @Test
    fun `reset clears everything and stops running`() {
        val c = RecordingChronometer(::clock)
        c.start()
        now += 4_000
        c.reset()
        assertFalse(c.isRunning)
        assertEquals(0L, c.elapsedMillis())
    }
}
