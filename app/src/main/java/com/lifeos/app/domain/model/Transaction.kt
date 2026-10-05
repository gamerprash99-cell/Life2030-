package com.lifeos.app.domain.model

import com.lifeos.app.data.db.entities.PaymentMethod

/**
 * What a payment notification turned out to *be*.
 *
 * Only [EXPENSE] becomes an expense in Phase 1 (see
 * [com.lifeos.app.domain.usecase.CaptureTransactionUseCase]). The other cases are
 * distinguished anyway, because collapsing them into "expense" is exactly the
 * failure that makes auto-capture untrustworthy: an inbound transfer or a refund
 * logged as spending is worse than not logging it at all.
 */
enum class TransactionType { EXPENSE, INCOME, REFUND, TRANSFER, UNKNOWN }

/**
 * How much the parser trusts its own reading of a notification.
 *
 * Declared low-to-high so [isAtLeast] can express "at least this sure" without
 * a comparison table. The Phase 1 policy lives in the use case, not here.
 */
enum class CaptureConfidence { LOW, MEDIUM, HIGH;

    fun isAtLeast(other: CaptureConfidence): Boolean = ordinal >= other.ordinal
}

/**
 * One financial transaction read out of a notification, as raw extracted
 * fields — never as the notification text it came from.
 *
 * Deliberately absent: the original title/body. Nothing downstream needs it,
 * and keeping the raw string on this type is how notification text (which
 * usually carries an account number, a UPI handle or a reference) ends up in
 * the database. [reference] is the one exception, and it exists only to sharpen
 * duplicate detection inside a single call — see the use case, which hashes it
 * and never persists it.
 */
data class ParsedTransaction(
    val amount: Double,
    val merchant: String?,
    val transactionType: TransactionType,
    val paymentMethod: PaymentMethod,
    /** When the payment happened, in epoch millis. Always local-zone resolved. */
    val occurredAtEpochMillis: Long,
    /** UTR/RRN/reference token if the notification carried one. Ephemeral. */
    val reference: String?,
    val confidence: CaptureConfidence
)

/**
 * The minimum a capture surface hands to the parser: the pieces of a
 * notification it is allowed to read.
 *
 * [title] and [body] are already length-capped by the caller. Keeping this a
 * plain framework-free type is what lets [com.lifeos.app.domain.intelligence.TransactionParser]
 * run as an ordinary JVM unit test, with no Robolectric and no device.
 */
data class TransactionCandidate(
    val title: String?,
    val body: String?,
    /** Notification post time (epoch millis). The fallback transaction time. */
    val postedAtEpochMillis: Long
) {
    /** Title and body joined for matching, skipping whichever is absent. */
    val combinedText: String
        get() = listOfNotNull(title?.takeIf { it.isNotBlank() }, body?.takeIf { it.isNotBlank() })
            .joinToString(". ")

    val isEmpty: Boolean get() = combinedText.isBlank()
}
