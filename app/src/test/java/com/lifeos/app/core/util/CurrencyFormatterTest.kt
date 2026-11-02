package com.lifeos.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun `whole amounts drop the decimals`() {
        assertEquals("₹250", CurrencyFormatter.format(250.0))
        assertEquals("₹1,000", CurrencyFormatter.format(1000.0))
        assertEquals("₹12,500", CurrencyFormatter.format(12500.0))
    }

    @Test
    fun `amounts with paise keep two decimals`() {
        assertEquals("₹249.50", CurrencyFormatter.format(249.50.toDouble()))
        assertEquals("₹0.99", CurrencyFormatter.format(0.99.toDouble()))
    }

    @Test
    fun `decimal form drops the symbol and trailing zeros`() {
        assertEquals("250", CurrencyFormatter.decimalFor(250.0))
        assertEquals("249.50", CurrencyFormatter.decimalFor(249.50))
        assertEquals("12,500", CurrencyFormatter.decimalFor(12500.0))
    }

    @Test
    fun `non-finite amounts do not crash`() {
        assertEquals("₹0", CurrencyFormatter.format(Double.NaN))
        assertEquals("₹0", CurrencyFormatter.format(Double.POSITIVE_INFINITY))
    }
}
