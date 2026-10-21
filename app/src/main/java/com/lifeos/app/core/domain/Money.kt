package com.lifeos.app.core.domain

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

/**
 * Money value type.
 *
 * Hard rule from the spec: amounts are integer **minor units** (paise, cents, yen).
 * Float/Double arithmetic is never used for money anywhere in this app — see
 * SECURITY.md / ARCHITECTURE.md. Display formatting is the only place that touches
 * decimal points, and it does so with string math on a Long.
 */
data class Money(
    val minorUnits: Long,
    val currencyCode: String,
) {
    init {
        require(currencyCode.length == 3) { "currencyCode must be ISO-4217, was $currencyCode" }
    }

    val isZero: Boolean get() = minorUnits == 0L
    val isNegative: Boolean get() = minorUnits < 0L
    val isPositive: Boolean get() = minorUnits > 0L

    operator fun unaryMinus(): Money = copy(minorUnits = -minorUnits)

    fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = Math.addExact(minorUnits, other.minorUnits))
    }

    fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = Math.subtractExact(minorUnits, other.minorUnits))
    }

    /** Truncated percentage of this amount: `percent(15)` of 10000 minor = 1500. */
    fun percent(percent: Int): Money = copy(minorUnits = minorUnits * percent / 100)

    private fun requireSameCurrency(other: Money) =
        require(other.currencyCode == currencyCode) {
            "Currency mismatch: $currencyCode vs ${other.currencyCode}"
        }

    companion object {
        fun zero(currencyCode: String): Money = Money(0, currencyCode.uppercase(Locale.ROOT))

        /**
         * Parses user input such as `1234`, `1234.5`, `-4.99` into minor units for the
         * given currency, using only string splitting — no Float/Double ever appears.
         * More fraction digits than the currency supports are truncated (spec: never
         * silently round money up).
         */
        fun parse(input: String, currencyCode: String): Money {
            val code = currencyCode.uppercase(Locale.ROOT)
            val digits = input.trim().replace(",", "")
            val negative = digits.startsWith("-")
            val unsigned = digits.trimStart('-', '+')
            val fractionDigits = safeCurrency(code).defaultFractionDigits.coerceAtLeast(0)
            val wholePart = unsigned.substringBefore('.').ifEmpty { "0" }
            val fractionPart =
                if ('.' in unsigned) unsigned.substringAfter('.').take(fractionDigits)
                else ""
            val combined = (wholePart + fractionPart.padEnd(fractionDigits, '0'))
                .ifEmpty { "0" }
                .toLongOrNull() ?: 0L
            return Money(if (negative) -combined else combined, code)
        }
    }
}

fun safeCurrency(code: String): Currency = try {
    Currency.getInstance(code.uppercase(Locale.ROOT))
} catch (_: Exception) {
    Currency.getInstance(Locale.getDefault())
}

/**
 * Formats minor units for display, e.g. `₹1,234.56` or `$12`.
 * Pure string math on a Long — no Double participates.
 */
fun formatMoney(
    minorUnits: Long,
    currencyCode: String,
    locale: Locale = Locale.getDefault(),
    withSymbol: Boolean = true,
): String {
    val currency = safeCurrency(currencyCode)
    val digits = currency.defaultFractionDigits.coerceAtLeast(0)
    val negative = minorUnits < 0
    val asString = abs(minorUnits).toString().padStart(digits + 1, '0')
    val whole = groupDigits(asString.dropLast(digits), locale)
    val fraction = if (digits > 0) asString.takeLast(digits) else ""
    val number = if (fraction.isEmpty()) whole else "$whole.$fraction"
    if (!withSymbol) return if (negative) "-$number" else number
    val symbol = currency.getSymbol(locale)
    val body = if (symbol.any { it.code > 127 } || !symbol.first().isLetter()) {
        "$symbol$number"
    } else {
        "$symbol $number"
    }
    return if (negative) "-$body" else body
}

/** Groups thousands with commas (Indian and Western groupings both use 3 here). */
private fun groupDigits(digits: String, locale: Locale): String {
    if (digits.length <= 3) return digits
    val builder = StringBuilder()
    var count = 0
    for (i in digits.indices.reversed()) {
        builder.append(digits[i])
        count++
        if (count % 3 == 0 && i > 0) builder.append(',')
    }
    return builder.reverse().toString()
}

/** Sum helper used by Insights (checked add via Money.plus). */
fun Iterable<Money>.sum(): Money {
    val iterator = iterator()
    if (!iterator.hasNext()) return Money.zero("INR")
    var total = iterator.next()
    while (iterator.hasNext()) total = total.plus(iterator.next())
    return total
}
