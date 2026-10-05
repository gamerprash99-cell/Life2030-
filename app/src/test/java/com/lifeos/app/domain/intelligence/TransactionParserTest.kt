package com.lifeos.app.domain.intelligence

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.PaymentMethod
import com.lifeos.app.domain.model.CaptureConfidence
import com.lifeos.app.domain.model.TransactionCandidate
import com.lifeos.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Behaviour of [TransactionParser], written as the rules a person can check by
 * reading the notification text back to themselves.
 *
 * Every case is a plain JVM test: the parser is deliberately free of Android
 * types so this file needs no Robolectric, no instrumentation and no device.
 */
class TransactionParserTest {

    // ---- the happy path ----------------------------------------------------

    @Test
    fun `marked upi payment is read as a high confidence expense`() {
        val parsed = requireNotNull(parse(body = "₹349 paid to Swiggy via UPI"))
        assertEquals(349.0, parsed.amount, TOLERANCE)
        assertEquals("Swiggy", parsed.merchant)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
        assertEquals(PaymentMethod.UPI, parsed.paymentMethod)
        assertEquals(CaptureConfidence.HIGH, parsed.confidence)
        assertEquals(POST_TIME, parsed.occurredAtEpochMillis)
        assertNull(parsed.reference)
    }

    @Test
    fun `rs prefix is read as a rupee amount`() {
        val parsed = requireNotNull(parse(body = "Rs. 1,299.50 paid to Croma Store"))
        assertEquals(1299.50, parsed.amount, TOLERANCE)
        assertEquals("Croma Store", parsed.merchant)
        assertEquals(CaptureConfidence.HIGH, parsed.confidence)
    }

    @Test
    fun `trailing currency marker is read`() {
        val parsed = requireNotNull(parse(body = "Paid to Uber India 850 INR"))
        assertEquals(850.0, parsed.amount, TOLERANCE)
        assertEquals(PaymentMethod.OTHER, parsed.paymentMethod)
    }

    @Test
    fun `decomposed rupee sign is folded onto the composed one`() {
        // Notifications still arrive with the two-codepoint form of the rupee
        // sign (U+20B9 U+093F) rather than the composed U+20B9.
        val parsed = requireNotNull(parse(body = "\u20B9\u093F275 paid to Metro Recharge"))
        assertEquals(275.0, parsed.amount, TOLERANCE)
        assertEquals(CaptureConfidence.HIGH, parsed.confidence)
    }

    @Test
    fun `bank reference is lifted out for duplicate detection`() {
        val parsed = requireNotNull(parse(body = "₹349 paid to Swiggy via UPI. UTR: 5123456789"))
        assertEquals("5123456789", parsed.reference)
        assertEquals(349.0, parsed.amount, TOLERANCE)
    }

    // ---- the amounts must be right ----------------------------------------

    @Test
    fun `payment is chosen over the trailing balance`() {
        // Balance last, as several banks write it.
        val parsed = requireNotNull(parse(body = "₹1,299 debited for Netflix. Available balance ₹18,430"))
        assertEquals(1299.0, parsed.amount, TOLERANCE)
        assertEquals("Netflix", parsed.merchant)
    }

    @Test
    fun `payment is chosen over a leading balance`() {
        // Balance first, as other banks write it.
        val parsed = requireNotNull(parse(body = "Balance ₹18,430. ₹1,299 debited for Netflix"))
        assertEquals(1299.0, parsed.amount, TOLERANCE)
        assertEquals(CaptureConfidence.MEDIUM, parsed.confidence)
    }

    @Test
    fun `two different marked amounts are never high confidence`() {
        val parsed = requireNotNull(parse(body = "₹349 paid to Swiggy. Avl Bal ₹12,430"))
        assertEquals(CaptureConfidence.MEDIUM, parsed.confidence)
        assertEquals(349.0, parsed.amount, TOLERANCE)
    }

    @Test
    fun `two identical marked amounts are not treated as ambiguous`() {
        val parsed = requireNotNull(parse(body = "₹349 paid to Swiggy. Balance ₹349"))
        assertEquals(349.0, parsed.amount, TOLERANCE)
        assertEquals(CaptureConfidence.HIGH, parsed.confidence)
    }

    @Test
    fun `indian grouping is read`() {
        val parsed = requireNotNull(parse(body = "₹29,999 paid to Croma"))
        assertEquals(29999.0, parsed.amount, TOLERANCE)
    }

    @Test
    fun `malformed grouping produces no expense`() {
        // Stripping the separators here would turn 1,2,3,4 into 1234 — a
        // ten-fold error is worse than no expense.
        assertNull(parse(body = "₹1,2,3,4 debited from your account"))
    }

    @Test
    fun `implausibly large amount is refused`() {
        assertNull(parse(body = "₹1,29,999 debited from your account"))
    }

    @Test
    fun `bare amount without a currency marker is low confidence`() {
        val parsed = requireNotNull(parse(body = "Debited 349 from your account"))
        assertEquals(349.0, parsed.amount, TOLERANCE)
        assertEquals(CaptureConfidence.LOW, parsed.confidence)
        assertNull(parsed.merchant)
        assertEquals(PaymentMethod.OTHER, parsed.paymentMethod)
    }

    // ---- meaning, before money --------------------------------------------

    @Test
    fun `a credit is income and not an expense`() {
        val parsed = requireNotNull(parse(body = "₹5,000 credited to your HDFC card ending 4321"))
        assertEquals(TransactionType.INCOME, parsed.transactionType)
        assertEquals(5000.0, parsed.amount, TOLERANCE)
    }

    @Test
    fun `a refund is not an expense`() {
        val parsed = requireNotNull(parse(body = "₹500 refunded to your account"))
        assertEquals(TransactionType.REFUND, parsed.transactionType)
        assertEquals(500.0, parsed.amount, TOLERANCE)
    }

    @Test
    fun `a refund word beats a credit word`() {
        val parsed = requireNotNull(parse(body = "₹500 refunded and credited back to your account"))
        assertEquals(TransactionType.REFUND, parsed.transactionType)
    }

    @Test
    fun `a self directed transfer is not an expense`() {
        val parsed = requireNotNull(parse(body = "₹5,000 transferred to your own account"))
        assertEquals(TransactionType.TRANSFER, parsed.transactionType)
    }

    @Test
    fun `a transfer to another person is spending`() {
        val parsed = requireNotNull(parse(body = "₹500 transferred to Rahul"))
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
        assertEquals("Rahul", parsed.merchant)
    }

    @Test
    fun `atm withdrawal is ignored rather than filed as spending`() {
        // Money leaving the account without anything being bought. Phase 1
        // declines it rather than inventing a purchase.
        assertNull(parse(body = "₹2,000 withdrawn from ATM"))
        assertNull(parse(body = "₹2,000 ATM withdrawal at SBI ATM"))
        assertNull(parse(body = "₹2,000 cash withdrawal"))
    }

    // ---- things that are not transactions ---------------------------------

    @Test
    fun `promotional notification is ignored`() {
        assertNull(parse(body = "Your order has shipped and arrives tomorrow"))
    }

    @Test
    fun `balance alert mentioning an amount is ignored`() {
        assertNull(parse(body = "Your balance is ₹120. Add money to avoid a penalty"))
    }

    @Test
    fun `empty notification is ignored`() {
        assertNull(parse(body = ""))
        assertNull(parse(body = "   "))
        assertNull(parse(title = null, body = null))
    }

    @Test
    fun `noise without a movement word is ignored`() {
        // A currency amount is present, but nothing happened.
        assertNull(parse(body = "Flat ₹500 off your next order this weekend"))
    }

    // ---- merchant hygiene --------------------------------------------------

    @Test
    fun `a upi handle is not a merchant`() {
        val parsed = requireNotNull(parse(body = "₹349 paid to swiggy@paytm via UPI"))
        assertNull(parsed.merchant)
        assertEquals(CaptureConfidence.HIGH, parsed.confidence)
    }

    @Test
    fun `a masked account fragment is not a merchant`() {
        val parsed = requireNotNull(parse(body = "₹1,299 debited to XXXX4321"))
        assertNull(parsed.merchant)
    }

    @Test
    fun `merchant is cut at the first trailing keyword`() {
        val parsed = requireNotNull(parse(body = "₹349 paid to Amazon for order #A12 successful"))
        assertEquals("Amazon", parsed.merchant)
    }

    @Test
    fun `merchant keeps the banks own casing`() {
        val parsed = requireNotNull(parse(body = "₹349 paid to Blue Tokai Coffee House"))
        assertEquals("Blue Tokai Coffee House", parsed.merchant)
    }

    @Test
    fun `merchant never leaks the rest of the sentence`() {
        val parsed = requireNotNull(parse(body = "₹349 paid to Swiggy via UPI ref 5123456789"))
        assertEquals("Swiggy", parsed.merchant)
    }

    // ---- time --------------------------------------------------------------

    @Test
    fun `a clock time in the text beats a late post time`() {
        val posted = epochMillis(2026, 3, 14, 23, 30)
        val parsed = requireNotNull(parse(body = "₹349 paid to Swiggy at 21:40", postedAt = posted))
        assertEquals(epochMillis(2026, 3, 14, 21, 40), parsed.occurredAtEpochMillis)
    }

    @Test
    fun `meridiem time is read`() {
        val posted = epochMillis(2026, 3, 14, 21, 45)
        val parsed = requireNotNull(parse(body = "₹349 paid to Swiggy at 9:40 pm", postedAt = posted))
        assertEquals(epochMillis(2026, 3, 14, 21, 40), parsed.occurredAtEpochMillis)
    }

    @Test
    fun `an implausible in text time falls back to the post time`() {
        // 21:40 read off a 23:58 post would be two hours and eighteen minutes
        // away — more than the parser will believe, and more likely a reference
        // fragment than a time of day.
        val posted = epochMillis(2026, 3, 14, 23, 58)
        val parsed = requireNotNull(parse(body = "₹349 paid to Swiggy at 21:40", postedAt = posted))
        assertEquals(posted, parsed.occurredAtEpochMillis)
    }

    @Test
    fun `post time is used when the text carries no time`() {
        val posted = epochMillis(2026, 3, 14, 8, 5)
        val parsed = requireNotNull(parse(body = "₹349 paid to Swiggy", postedAt = posted))
        assertEquals(posted, parsed.occurredAtEpochMillis)
    }

    // ---- the cheap pre-filter the service relies on -------------------------

    @Test
    fun `financial prefilter needs both a currency marker and a movement word`() {
        assertTrue(TransactionParser.looksFinancial("₹349 paid to Swiggy via UPI"))
        assertTrue(TransactionParser.looksFinancial("Your bill of Rs 1200 is due"))

        assertFalse(TransactionParser.looksFinancial("Flat ₹500 off your next order"))
        assertFalse(TransactionParser.looksFinancial("Your order has shipped"))
        assertFalse(TransactionParser.looksFinancial(""))
    }

    // ---- helpers -----------------------------------------------------------

    private fun parse(
        title: String? = "Bank",
        body: String?,
        postedAt: Long = POST_TIME
    ): com.lifeos.app.domain.model.ParsedTransaction? =
        TransactionParser.parse(TransactionCandidate(title = title, body = body, postedAtEpochMillis = postedAt))

    private fun epochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute))
            .atZone(DateTimeUtils.zoneId())
            .toInstant()
            .toEpochMilli()

    private companion object {
        const val TOLERANCE = 0.0001
        val POST_TIME: Long =
            LocalDateTime.of(LocalDate.of(2026, 3, 14), LocalTime.of(21, 40))
                .atZone(DateTimeUtils.zoneId()).toInstant().toEpochMilli()
    }
}