package com.lifeos.app.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Money: the spec's hardest rule is "never a Float/Double". These tests pin the
 * exact minor-unit arithmetic and the display formatting.
 */
class MoneyTest {

    @Test
    fun `parse whole rupees into minor units`() {
        assertEquals(123400L, Money.parse("1234", "INR").minorUnits)
    }

    @Test
    fun `parse decimals into exact minor units`() {
        assertEquals(24950L, Money.parse("249.50", "INR").minorUnits)
    }

    @Test
    fun `parse single decimal digit pads correctly`() {
        assertEquals(24950L, Money.parse("249.5", "INR").minorUnits)
    }

    @Test
    fun `parse truncates extra fraction digits rather than rounding up`() {
        // 249.509 must never become 249.51
        assertEquals(24950L, Money.parse("249.509", "INR").minorUnits)
    }

    @Test
    fun `parse handles negative amounts`() {
        assertEquals(-499L, Money.parse("-4.99", "USD").minorUnits)
    }

    @Test
    fun `parse ignores thousands separators`() {
        assertEquals(123456L, Money.parse("1,234.56", "USD").minorUnits)
    }

    @Test
    fun `parse of blank input is zero`() {
        assertEquals(0L, Money.parse("", "INR").minorUnits)
    }

    @Test
    fun `zero-decimal currency stores whole units`() {
        // JPY has no minor unit; 1000 yen is 1000 minor units.
        assertEquals(1000L, Money.parse("1000", "JPY").minorUnits)
    }

    @Test
    fun `addition uses exact integer math`() {
        val a = Money.parse("0.10", "USD")
        val b = Money.parse("0.20", "USD")
        // Float logic would yield 0.30000000000000004; minor units cannot.
        assertEquals(30L, a.plus(b).minorUnits)
    }

    @Test
    fun `subtraction and unary minus are exact`() {
        val a = Money.parse("10.00", "USD")
        val b = Money.parse("3.25", "USD")
        assertEquals(675L, a.minus(b).minorUnits)
        assertEquals(-1000L, (-a).minorUnits)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `mismatched currencies are rejected`() {
        Money.parse("1", "USD").plus(Money.parse("1", "INR"))
    }

    @Test
    fun `percent truncates toward zero`() {
        assertEquals(1500L, Money.parse("100.00", "USD").percent(15).minorUnits)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `currency code must be three letters`() {
        Money(100L, "US")
    }

    @Test
    fun `formatMoney places the decimal point without a double`() {
        val formatted = formatMoney(24950L, "INR", java.util.Locale("en", "IN"), withSymbol = false)
        assertEquals("249.50", formatted)
    }

    @Test
    fun `formatMoney groups thousands`() {
        val formatted = formatMoney(123456789L, "USD", java.util.Locale.US, withSymbol = false)
        assertEquals("1,234,567.89", formatted)
    }

    @Test
    fun `formatMoney renders zero-decimal currencies without a point`() {
        val formatted = formatMoney(1000L, "JPY", java.util.Locale.JAPAN, withSymbol = false)
        assertEquals("1,000", formatted)
    }

    @Test
    fun `formatMoney marks negatives`() {
        val formatted = formatMoney(-499L, "USD", java.util.Locale.US, withSymbol = false)
        assertTrue(formatted.startsWith("-"))
    }

    @Test
    fun `formatMoney pads amounts smaller than one unit`() {
        assertEquals("0.05", formatMoney(5L, "USD", java.util.Locale.US, withSymbol = false))
    }
}
