package com.lifeos.app.domain.usecase

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.domain.intelligence.ExpenseCategorizer
import com.lifeos.app.domain.intelligence.TransactionParser
import com.lifeos.app.domain.model.AUTO_CAPTURED_TAG
import com.lifeos.app.domain.model.CaptureConfidence
import com.lifeos.app.domain.model.ParsedTransaction
import com.lifeos.app.domain.model.TransactionCandidate
import com.lifeos.app.domain.model.TransactionType
import java.security.MessageDigest
import java.time.Instant

/** What [CaptureTransactionUseCase] did with one notification. */
sealed interface CaptureOutcome {

    /** An expense row was written. [expenseId] is its primary key. */
    data class Created(val expenseId: String) : CaptureOutcome

    /**
     * This payment is already recorded. Deliberately distinct from [Skipped]:
     * a duplicate means the feature worked, a skip means it declined.
     */
    data object Duplicate : CaptureOutcome

    /** Nothing was written. [reason] says which gate stopped it. */
    data class Skipped(val reason: SkipReason) : CaptureOutcome
}

/** Why a notification produced no expense. */
enum class SkipReason {
    /** Not text LifeOS recognises as a payment at all. */
    NOT_A_TRANSACTION,

    /** A payment, but not one that maps to a spending entry (income, refund, transfer). */
    NOT_AN_EXPENSE,

    /** A reading LifeOS is not confident enough to file without asking. */
    BELOW_AUTO_CONFIDENCE,

    /**
     * A transaction whose timestamp is implausible for a notification that just
     * arrived — a replayed notification, or a clock change mid-flight.
     */
    IMPLAUSIBLE_TIMESTAMP
}

/**
 * Turns a payment notification into an expense, through the ordinary expense
 * path.
 *
 * The whole point is that there is nothing automatic about *where* the row ends
 * up: this parses, decides, and then calls [ExpenseRepository.addExpense] — the
 * same call the add-expense sheet makes. There is no second table, no second DAO
 * path, and no service that writes to a DAO directly.
 *
 * Three gates stand in front of the write, in this order:
 *
 *  1. **Meaning.** Only a transaction LifeOS parsed as [TransactionType.EXPENSE]
 *     becomes an expense. Income, refunds and self-directed transfers are
 *     recognised precisely so they are *not* filed as spending — that mistake is
 *     the one that makes an expense tracker untrustworthy.
 *  2. **Confidence.** Only [CaptureConfidence.HIGH] is filed. MEDIUM means the
 *     text held two different marked amounts and one of them is probably a
 *     balance; LOW means the amount came from an unmarked number. There is no
 *     review queue in this app yet, so the honest Phase 1 answer is to leave the
 *     entry to the user rather than invent a subsystem for it.
 *  3. **Duplicate.** See [dedupe] below.
 *
 * [nowMillis] is injected rather than read from the clock so the plausibility
 * window is testable.
 */
class CaptureTransactionUseCase(
    private val expenseRepository: ExpenseRepository
) {

    suspend operator fun invoke(
        candidate: TransactionCandidate,
        nowMillis: Long = System.currentTimeMillis()
    ): CaptureOutcome {
        val parsed = TransactionParser.parse(candidate) ?: return CaptureOutcome.Skipped(SkipReason.NOT_A_TRANSACTION)

        if (parsed.transactionType != TransactionType.EXPENSE) {
            return CaptureOutcome.Skipped(SkipReason.NOT_AN_EXPENSE)
        }
        if (parsed.confidence != CaptureConfidence.HIGH) {
            return CaptureOutcome.Skipped(SkipReason.BELOW_AUTO_CONFIDENCE)
        }

        val zone = DateTimeUtils.zoneId()
        val local = Instant.ofEpochMilli(parsed.occurredAtEpochMillis).atZone(zone)
        val epochDay = local.toLocalDate().toEpochDay()
        val timeMinutes = DateTimeUtils.minutesOfDay(parsed.occurredAtEpochMillis)

        // A notification that just arrived describes something that just
        // happened. Anything outside this window is a replay or a clock jump,
        // and filing it would scatter expenses across days nobody spent anything.
        if (kotlin.math.abs(nowMillis - parsed.occurredAtEpochMillis) > MAX_PLAUSIBLE_AGE_MILLIS) {
            return CaptureOutcome.Skipped(SkipReason.IMPLAUSIBLE_TIMESTAMP)
        }

        val fingerprint = fingerprint(parsed, epochDay, timeMinutes)
        if (expenseRepository.existsById(fingerprint)) return CaptureOutcome.Duplicate
        if (isNearDuplicate(parsed, epochDay, timeMinutes)) return CaptureOutcome.Duplicate

        expenseRepository.addExpense(
            amount = parsed.amount,
            category = ExpenseCategorizer.categoryOrDefault(parsed.merchant),
            dateEpochDay = epochDay,
            timeMinutes = timeMinutes,
            merchant = parsed.merchant,
            paymentMethod = parsed.paymentMethod,
            // No note: the notification text is deliberately never stored. The
            // extracted fields above are everything an expense row legitimately
            // holds.
            note = null,
            tags = listOf(AUTO_CAPTURED_TAG),
            id = fingerprint
        )
        return CaptureOutcome.Created(fingerprint)
    }

    /**
     * Deterministic row id for a parsed transaction.
     *
     * Because the id is derived from the transaction itself rather than from a
     * random UUID, the *same* notification processed twice collapses onto one
     * row at the primary key — the existence check exists only to report that as
     * a duplicate instead of silently rewriting the row.
     *
     * A bank reference (UTR/RRN) is folded in as a **hash**, and only when
     * present, which is what separates two genuinely different payments of the
     * same amount to the same merchant in the same minute. Hashing is also what
     * keeps the reference itself out of the database: the id is one-way, so the
     * number cannot be recovered from it, and the raw value is never persisted,
     * logged or shown.
     */
    private fun fingerprint(parsed: ParsedTransaction, epochDay: Long, timeMinutes: Int): String {
        val parts = ArrayList<String>(6)
        parts += FINGERPRINT_VERSION
        parts += epochDay.toString()
        parts += timeMinutes.toString()
        parts += cents(parsed.amount).toString()
        parts += ExpenseCategorizer.normalizeForMatch(parsed.merchant.orEmpty())
        parts += parsed.transactionType.name
        parsed.reference?.let { parts += sha256(it).take(REFERENCE_FINGERPRINT_LENGTH) }
        return AUTO_CAPTURED_ID_PREFIX + sha256(parts.joinToString("|")).take(FINGERPRINT_LENGTH)
    }

    /**
     * The second dedupe gate, and the one that actually catches the common case:
     * banks that post a "payment successful" notification and then a separate
     * "debit" one, a minute or two apart, sometimes with different wording and
     * therefore a different fingerprint.
     *
     * A same-amount row on the same local day is a duplicate when the merchant
     * matches and the times are within [DEDUP_WINDOW_MINUTES]; when either row has
     * no merchant, only an *identical* minute counts. Requiring the merchant to
     * agree is what stops two equal-priced coffees two minutes apart from being
     * swallowed as one — and refusing to widen the window when the merchant is
     * missing is what stops a ₹10 taxi and a ₹10 snack in the same minute from
     * being swallowed at all.
     */
    private suspend fun isNearDuplicate(parsed: ParsedTransaction, epochDay: Long, timeMinutes: Int): Boolean {
        val merchant = ExpenseCategorizer.normalizeForMatch(parsed.merchant.orEmpty())
        return expenseRepository.findSameAmountOnDay(epochDay, parsed.amount).any { existing ->
            val existingMerchant = ExpenseCategorizer.normalizeForMatch(existing.merchant.orEmpty())
            val delta = kotlin.math.abs(existing.timeMinutes - timeMinutes)
            val bothMerchantsKnown = merchant.isNotEmpty() && existingMerchant.isNotEmpty()
            if (bothMerchantsKnown) {
                merchant == existingMerchant && delta <= DEDUP_WINDOW_MINUTES
            } else {
                delta == 0
            }
        }
    }

    private fun cents(amount: Double): Long = Math.round(amount * 100.0)

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    companion object {
        /**
         * Same merchant, same amount, same day, within this many minutes counts
         * as the same payment. Two minutes is what separates a bank's duplicate
         * notification pair from two real purchases — the gap between "posted
         * twice" and "bought twice".
         */
        const val DEDUP_WINDOW_MINUTES = 2

        /** Older (or newer) than this and the timestamp is not believable. */
        const val MAX_PLAUSIBLE_AGE_MILLIS = 24 * 60 * 60 * 1000L

        /**
         * Id prefix for automatically captured expenses. Fingerprints only —
         * "was this captured automatically" is answered by the
         * [AUTO_CAPTURED_TAG] tag, never by this prefix.
         */
        const val AUTO_CAPTURED_ID_PREFIX = "auto-"

        /** Bumped if the fingerprint recipe changes, so old ids cannot collide. */
        private const val FINGERPRINT_VERSION = "lifeos.auto-expense.v1"

        private const val FINGERPRINT_LENGTH = 24
        private const val REFERENCE_FINGERPRINT_LENGTH = 12
    }
}
