package com.lifeos.app.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Local-date helpers: the ISO string is the app's canonical day representation. */
class DatesTest {

    @Test
    fun `iso formats a date as yyyy-MM-dd`() {
        assertEquals("2026-03-02", Dates.iso(LocalDate.of(2026, 3, 2)))
    }

    @Test
    fun `parse is the inverse of iso`() {
        val date = LocalDate.of(2026, 12, 31)
        assertEquals(date, Dates.parse(Dates.iso(date)))
    }

    @Test
    fun `parseOrNull returns null for blank or malformed input`() {
        assertEquals(null, Dates.parseOrNull(null))
        assertEquals(null, Dates.parseOrNull(""))
        assertEquals(null, Dates.parseOrNull("not-a-date"))
        assertEquals(LocalDate.of(2026, 1, 5), Dates.parseOrNull("2026-01-05"))
    }

    @Test
    fun `daysBetweenInclusive counts both endpoints`() {
        val friday = LocalDate.of(2026, 3, 6)
        val sunday = LocalDate.of(2026, 3, 8)
        assertEquals(3L, Dates.daysBetweenInclusive(friday, sunday))
    }

    @Test
    fun `round-trip through epoch millis preserves the local day`() {
        val date = LocalDate.of(2026, 7, 4)
        val millis = Dates.toEpochMillis(date)
        assertEquals(date, Dates.toLocalDate(millis))
    }

    @Test
    fun `isToday is true only for the current local day`() {
        val now = LocalDate.now()
        assertTrue(Dates.isToday(Dates.iso(now)))
        assertFalse(Dates.isToday(Dates.iso(now.minusDays(1))))
        assertFalse(Dates.isToday(null))
    }

    @Test
    fun `relative labels today and yesterday and tomorrow`() {
        val now = LocalDate.now()
        assertEquals("Today", Dates.relative(Dates.iso(now)))
        assertEquals("Yesterday", Dates.relative(Dates.iso(now.minusDays(1))))
        assertEquals("Tomorrow", Dates.relative(Dates.iso(now.plusDays(1))))
    }

    @Test
    fun `relative of a blank value is an empty string`() {
        assertEquals("", Dates.relative(null))
        assertEquals("", Dates.relative(""))
    }
}
