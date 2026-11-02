package com.lifeos.app.core.util

import java.text.DecimalFormat
import java.util.Locale

/**
 * One rule for writing an amount of money, used by every surface that shows
 * one — the Expenses tab, the Life timeline and the assistant.
 *
 * Expenses are entered in the app's single currency (the rupee, matching the
 * payment-notification parser and every existing screen), so this is a
 * *formatting* helper rather than a currency conversion layer: there is no
 * exchange rate, no locale lookup and no network anywhere near it.
 *
 * Whole amounts drop the decimals ("₹250", not "₹250.00") because a price
 * typed as 250 should read back as 250; anything with paise keeps two decimals
 * ("₹249.50"). Thousands are separated so a large total stays scannable.
 */
object CurrencyFormatter {

    const val SYMBOL = "₹"

    private val whole = DecimalFormat("#,##0", DecimalFormatSymbolsFactory.symbols())
    private val fractional = DecimalFormat("#,##0.00", DecimalFormatSymbolsFactory.symbols())

    /** `₹1,250` or `₹249.50` — the display form for a stored amount. */
    fun format(amount: Double): String = SYMBOL + decimalFor(amount)

    /** The same number without the symbol, for a field that already shows one. */
    fun decimalFor(amount: Double): String {
        val safe = if (amount.isFinite()) amount else 0.0
        val rounded = String.format(Locale.US, "%.2f", safe)
        return if (rounded.endsWith(".00")) {
            whole.format(rounded.toDouble())
        } else {
            fractional.format(rounded.toDouble())
        }
    }
}

/**
 * DecimalFormat's symbols are locale-derived by default; pinning them keeps the
 * separator a comma and the decimal point a dot regardless of the device
 * locale, so a number the user typed on a locale that uses "1.234,50" still
 * reads back the way the rest of the app writes money.
 */
private object DecimalFormatSymbolsFactory {
    fun symbols() = java.text.DecimalFormatSymbols(Locale.US)
}
