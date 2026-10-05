package com.lifeos.app.domain.intelligence

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.PaymentMethod
import com.lifeos.app.domain.model.CaptureConfidence
import com.lifeos.app.domain.model.ParsedTransaction
import com.lifeos.app.domain.model.TransactionCandidate
import com.lifeos.app.domain.model.TransactionType
import java.text.Normalizer
import java.time.Instant
import java.time.LocalTime

/**
 * Reads an Indian payment/bank notification and returns the transaction inside
 * it — or null, when there isn't one.
 *
 * Framework-free on purpose: no Android, no Room, no Compose, no I/O. The
 * capture service hands it a [TransactionCandidate] and this object decides
 * what the text means, which means the whole thing runs as an ordinary JVM unit
 * test and can be reasoned about without a device.
 *
 * The constraint that shaped every rule below is that a wrong automatic expense
 * is worse than a missed one — the user never sees what LifeOS thought the
 * notification said, so a confident mistake is invisible and permanent. The
 * parser is therefore allowed to answer "not a transaction", and answers it
 * often. Work is done in three independent passes — amount, meaning, merchant —
 * and a result is only reported when the *meaning* pass agrees there is one.
 *
 * Nothing here is a per-bank template. Formats differ between apps, between an
 * app's versions and between users' own custom templates, so the amount is
 * located by currency marker plus proximity to the movement word rather than by
 * position, and the meaning comes from keyword sets that overlap across banks.
 */
object TransactionParser {

    /** Below this, a "payment" is a rounding artefact or a mis-parsed fragment. */
    const val MIN_PLAUSIBLE_AMOUNT = 1.0

    /**
     * Above this, an automatic expense is not trustworthy enough to file. Either
     * the number is an account balance or limit that got read as a payment, or
     * the payment is genuinely large — which is exactly the kind of entry a
     * person should type themselves.
     */
    const val MAX_PLAUSIBLE_AMOUNT = 100_000.0

    /** A merchant longer than this is a sentence that leaked into the field. */
    const val MAX_MERCHANT_LENGTH = 48

    /** Shorter than this and there is not enough text to judge. */
    private const val MIN_TEXT_LENGTH = 8

    /** A clock time written in the text is only believed this close to the post time. */
    private const val IN_TEXT_TIME_TOLERANCE_MILLIS = 2 * 60 * 60 * 1000L

    /**
     * Currency-marked amounts. The rupee sign is matched in both its single-
     * codepoint and decomposed forms, because notifications still arrive with
     * the decomposed sequence. The `Rs` alternative is anchored at the start of
     * the token — so it can never match inside a word — but has no trailing word
     * boundary, because "Rs349" is a real spelling.
     */
    private val MARKED_AMOUNT = Regex(
        "(?:₹|रु|\\bINR|\\bRs)\\.?\\s*(\\d[\\d,]*(?:\\.\\d{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    /** The same idea with the marker *after* the number ("1,299 INR"). */
    private val TRAILING_MARKED_AMOUNT = Regex(
        "(\\d[\\d,]*(?:\\.\\d{1,2})?)\\s*(?:₹|\\bINR\\b|\\bRs\\b)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Last-resort amounts, for banks that write "Debited 349" with no symbol at
     * all. Only consulted when no marked amount exists, and never sufficient on
     * its own to make a notification count as a transaction.
     */
    private val BARE_AMOUNT = Regex("(?<![\\d.,])(\\d[\\d,]*(?:\\.\\d{1,2})?)(?![\\d])")

    /** Digits with consistent grouping: `349`, `1,299`, `1,29,999`, `12.50`. */
    private val PLAIN_AMOUNT = Regex("^\\d+(\\.\\d{1,2})?$")
    private val GROUPED_AMOUNT = Regex("^\\d{1,3}(,\\d{2,3})+(\\.\\d{1,2})?$")

    /** Words that mean "money moved". Locates an amount and justifies one. */
    private val TRANSACTION_VERBS = listOf(
        "paid", "pay", "debit", "debited", "spent", "spend", "charged", "charge",
        "purchased", "purchase", "payment", "payments", "txn", "transaction",
        "transferred", "transfer", "received", "receive", "credited", "credit",
        "refund", "refunded", "reversed", "reversal", "withdrawn", "withdrawal",
        "bill", "autopay", "mandate", "emi",
        "भुगतान", "प्राप्त", "रिफंड", "भेजा", "भेजे", "वापस"
    )

    /** Refund is checked first: textually a refund is also a credit. */
    private val REFUND_WORDS = listOf(
        "refund", "refunded", "reversed", "reversal", "credited back",
        "money returned", "returned to your", "chargeback",
        "रिफंड", "वापस"
    )

    private val INCOME_WORDS = listOf(
        "received", "receive", "credited", "incoming", "deposited",
        "added to your", "salary", "remittance", "cashback", "प्राप्त"
    )

    /**
     * A transfer is only "internal movement" when the text also says it is
     * self-directed. "Transferred to Rahul" is spending; "transferred to my own
     * account" is not. Without the second half, treating every "transferred" as
     * a non-expense would silently drop real payments.
     */
    private val TRANSFER_WORDS = listOf("transferred", "transfer to", "self transfer")
    private val SELF_ACCOUNT_WORDS = listOf(
        "own account", "own a/c", "own ac", "my account", "your account", "your a/c",
        "your ac", "self", "same account", "savings account", "अपने खाते"
    )

    private val EXPENSE_WORDS = listOf(
        "paid to", "debit", "debited", "spent", "charged", "charge",
        "purchased", "purchase", "payment of", "payment to", "bill payment",
        "bill paid", "autopay", "auto debit", "sent to", "emi paid",
        "भुगतान", "भेजा"
    )

    /**
     * A cash or ATM withdrawal is money leaving the account without anything
     * being bought, so filing it as an expense would invent spending. It is
     * classified UNKNOWN — ignored — rather than guessed at.
     */
    private val WITHDRAWAL_WORDS = listOf("withdrawal", "withdrawn", "atm withdrawal", "cash withdrawal", "निकासी")

    private val UPI_WORDS = listOf("upi", "gpay", "google pay", "phonepe", "phone pe", "paytm", "bhim", "whatsapp pay", "amazon pay")
    private val CARD_WORDS = listOf("card", "visa", "mastercard", "rupay", "maestro", "amex", "rupay card")
    private val BANK_WORDS = listOf("net banking", "netbanking", "netbank", "neft", "imps", "rtgs", "bank transfer", "wire transfer", "nefc")
    private val CASH_WORDS = listOf("cash")

    /** UTR / RRN / reference, when the bank includes one. */
    private val REFERENCE = Regex(
        "\\b(?:utr|rrn|utrno|txn\\s*id|txnid|ref(?:erence)?(?:\\s*(?:no|number|id))?)\\s*[:#.\\-]?\\s*([A-Za-z0-9]{6,24})\\b",
        RegexOption.IGNORE_CASE
    )

    /**
     * The rupee sign as notifications spell it. NFKC cannot help here: U+20B9
     * decomposes *canonically* to U+20A8 U+093F, and U+093F sits in the
     * composition exclusion list, so normalisation reliably turns a clean ₹
     * into a two-character sequence and never puts it back. Folding it by hand
     * is what keeps "₹275 paid to" parseable on the keyboards that produce it.
     */
    private val RUPEE_SPELLINGS = Regex("[\u20A8\u20B9]\u093F")

    /** A UPI handle, e.g. "swiggy@paytm". An account identifier, not a shop. */
    private val UPI_HANDLE = Regex("^[A-Za-z0-9._\\-]{1,64}@[A-Za-z0-9._\\-]{1,32}$")

    /** Masked or plain account fragments that must not become a "merchant". */
    private val ACCOUNT_FRAGMENT = Regex("^[Xx*•\\s\\d]{1,24}$")

    /**
     * A phrase that names an account rather than a shop. "Debited from your
     * account" is the commonest shape a merchant capture takes in bank
     * notifications, and without this every one of them would file an expense
     * with the merchant "your account".
     */
    private val ACCOUNT_PHRASE = Regex(
        "^(?:your|my|our|the|this)?\\s*(?:a/?c|ac|account|accounts|cards?|banking|bank|wallet|balance)" +
            "(?:\\s*(?:ending|in|no|number)?\\s*[Xx*]*\\s*\\d{0,8})?$",
        RegexOption.IGNORE_CASE
    )

    /** Keywords that end a merchant capture when they follow the name. */
    private val MERCHANT_TRAILERS = listOf(
        "via", "using", "through", "thru", "from", "with", "success", "successfully",
        "is", "was", "has", "been", "ref", "utr", "rrn", "txn", "transaction",
        "order", "upi", "inr", "rs", "card", "netbanking", "neft", "imps", "a/c",
        "ac", "no", "number", "date", "on", "at", "for", "by", "and", "your", "my",
        "account", "balance", "avl", "avlbl", "avl bal", "ifsc", "upi id", "upiid",
        "available", "total"
    )

    private val PUNCTUATION_EDGE = charArrayOf(
        '.', ',', ':', ';', '-', '–', '—', '*', '#', '/', '\\', '(', ')', '[', ']',
        '"', '\'', '!', '?', '_'
    )

    /** "7 pm" / "at 21:40" — the colon form only counts when prefixed by "at". */
    private val IN_TEXT_TIME_24H = Regex("\\bat\\s+(\\d{1,2}):(\\d{2})\\b", RegexOption.IGNORE_CASE)
    private val IN_TEXT_TIME_MERIDIEM = Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b", RegexOption.IGNORE_CASE)

    /**
     * Cheap pre-filter for the capture service, so a notification that cannot
     * possibly be a payment is rejected before any regex work or coroutine.
     * Requires *both* a currency marker and a movement word — a message can
     * mention a rupee amount ("your ₹500 cashback expires today") or a movement
     * word ("your order has shipped") without being a transaction.
     */
    fun looksFinancial(text: String): Boolean {
        if (text.length < MIN_TEXT_LENGTH) return false
        val flat = flatten(text)
        return hasCurrencyMarker(flat) && TRANSACTION_VERBS.any { flat.contains(it) }
    }

    /**
     * Parses [candidate], or returns null when the text is not a transaction
     * this parser is willing to act on.
     */
    fun parse(candidate: TransactionCandidate): ParsedTransaction? {
        if (candidate.isEmpty) return null
        val original = normalize(candidate.combinedText)
        if (original.length < MIN_TEXT_LENGTH) return null
        val flat = flatten(original)

        // A withdrawal is never filed; say so before any amount work.
        if (WITHDRAWAL_WORDS.any { flat.contains(it) }) return null

        val type = classify(flat)
        if (type == TransactionType.UNKNOWN) return null

        val verbRanges = verbRanges(flat)
        val amount = extractAmount(original, verbRanges) ?: return null
        if (amount.value < MIN_PLAUSIBLE_AMOUNT || amount.value > MAX_PLAUSIBLE_AMOUNT) return null

        return ParsedTransaction(
            amount = amount.value,
            merchant = extractMerchant(original),
            transactionType = type,
            paymentMethod = classifyPaymentMethod(flat),
            occurredAtEpochMillis = resolveTimestamp(candidate.postedAtEpochMillis, flat),
            reference = extractReference(flat),
            confidence = confidenceFor(amount, type)
        )
    }

    // ---- amount ----------------------------------------------------------

    private data class AmountMatch(val value: Double, val marked: Boolean, val ambiguous: Boolean)

    /** One readable amount, with where in the text it was found. */
    private data class AmountCandidate(val digits: String, val value: Double, val at: Int)

    /**
     * Picks the amount a person would read as *the* amount.
     *
     * Bank templates routinely carry two currency-marked numbers - the payment
     * and the resulting balance - in either order, so "the first one" is wrong
     * about half the time. Every marked amount is instead scored by its distance
     * to the nearest movement word, and the closest wins. With no movement word
     * there is nothing to attribute an amount to, so the result is marked
     * ambiguous and never reaches HIGH confidence.
     */
    private fun extractAmount(original: String, verbRanges: List<IntRange>): AmountMatch? {
        val marked = amountsIn(original, MARKED_AMOUNT) {
            amountsIn(original, TRAILING_MARKED_AMOUNT) { emptyMap() }
        }

        if (marked.isNotEmpty()) {
            val best = marked.values.minByOrNull { candidate ->
                if (verbRanges.isEmpty()) candidate.at else verbRanges.minOf { distanceTo(candidate.at, it) }
            } ?: return null
            // Two *different* currency-marked numbers is the payment-plus-balance
            // shape, where one of the two is not the payment. Two identical
            // marked numbers are not ambiguous, which is why the candidates are
            // keyed by digits above.
            val ambiguous = marked.values.map { it.value }.distinct().size > 1 || verbRanges.isEmpty()
            return AmountMatch(value = best.value, marked = true, ambiguous = ambiguous)
        }

        // No currency marker anywhere. Only usable with a movement word to
        // measure against, and always LOW confidence afterwards.
        if (verbRanges.isEmpty()) return null
        val bare = amountsIn(original, BARE_AMOUNT) { emptyMap() }
        val best = bare.values.minByOrNull { candidate ->
            verbRanges.minOf { distanceTo(candidate.at, it) }
        } ?: return null
        return AmountMatch(value = best.value, marked = false, ambiguous = true)
    }

    /**
     * Every readable amount in [original] matching [pattern], keyed by its digits
     * so the same number written twice collapses to a single candidate.
     */
    private fun amountsIn(
        original: String,
        pattern: Regex,
        alsoRead: () -> Map<String, AmountCandidate>
    ): Map<String, AmountCandidate> {
        val found = LinkedHashMap<String, AmountCandidate>()
        for (match in pattern.findAll(original)) {
            val hit = occurrence(match, 1) ?: continue
            val value = parseAmount(hit.first) ?: continue
            found.putIfAbsent(hit.first, AmountCandidate(hit.first, value, hit.second))
        }
        return found + alsoRead()
    }

    private fun occurrence(match: MatchResult, group: Int): Pair<String, Int>? {
        val digits = match.groups[group]?.value ?: return null
        return digits to match.range.first
    }

    /**
     * Digits as written, honouring Indian (`1,29,999`) and western (`12,345`)
     * grouping and up to two decimal places.
     *
     * A malformed grouping such as `1,2,3,4` returns null rather than having its
     * separators stripped: a number read wrong by 10x is far worse than a
     * notification that produces no expense.
     */
    private fun parseAmount(raw: String): Double? {
        val trimmed = raw.trim()
        val normalizedDigits = when {
            PLAIN_AMOUNT.matches(trimmed) -> trimmed
            GROUPED_AMOUNT.matches(trimmed) -> trimmed.replace(",", "")
            else -> return null
        }
        val value = normalizedDigits.toDoubleOrNull() ?: return null
        return if (value.isFinite()) value else null
    }

    private fun distanceTo(at: Int, range: IntRange): Int = when {
        at < range.first -> range.first - at
        at > range.last -> at - range.last
        else -> 0
    }

    // ---- meaning ---------------------------------------------------------

    /**
     * Refund, then income, then a self-directed transfer, then spending. The
     * order is the design: several of these markers co-occur ("refund received",
     * "₹500 credited back") and only one of them states what the money did.
     */
    private fun classify(flat: String): TransactionType = when {
        REFUND_WORDS.any { flat.contains(it) } -> TransactionType.REFUND
        INCOME_WORDS.any { flat.contains(it) } -> TransactionType.INCOME
        TRANSFER_WORDS.any { flat.contains(it) } && SELF_ACCOUNT_WORDS.any { flat.contains(it) } ->
            TransactionType.TRANSFER
        EXPENSE_WORDS.any { flat.contains(it) } -> TransactionType.EXPENSE
        TRANSACTION_VERBS.any { flat.contains(it) } -> TransactionType.EXPENSE
        else -> TransactionType.UNKNOWN
    }

    private fun classifyPaymentMethod(flat: String): PaymentMethod = when {
        UPI_WORDS.any { flat.contains(it) } -> PaymentMethod.UPI
        CARD_WORDS.any { flat.contains(it) } -> PaymentMethod.CARD
        BANK_WORDS.any { flat.contains(it) } -> PaymentMethod.BANK
        CASH_WORDS.any { flat.contains(it) } -> PaymentMethod.CASH
        else -> PaymentMethod.OTHER
    }

    private fun verbRanges(flat: String): List<IntRange> =
        TRANSACTION_VERBS.mapNotNull { verb ->
            flat.indexOf(verb).takeIf { it >= 0 }?.let { it..(it + verb.length) }
        }

    // ---- merchant --------------------------------------------------------

    /**
     * Ordered most-specific first: the first pattern that fires wins, so
     * "paid to" beats the generic "at"/"to" fallbacks that would otherwise
     * capture the rest of the sentence.
     */
    private val MERCHANT_PATTERNS = listOf(
        Regex("\\bpaid\\s+to\\s+(.+)", RegexOption.IGNORE_CASE),
        Regex("\\bspent\\s+at\\s+(.+)", RegexOption.IGNORE_CASE),
        Regex("\\breceived\\s+from\\s+(.+)", RegexOption.IGNORE_CASE),
        Regex("\\brefunded\\s+by\\s+(.+)", RegexOption.IGNORE_CASE),
        Regex("\\btransferred\\s+to\\s+(.+)", RegexOption.IGNORE_CASE),
        Regex("\\bpurchased\\s+(?:at|from)\\s+(.+)", RegexOption.IGNORE_CASE),
        Regex("\\bcharged\\s+(?:for|by|to)\\s+(.+)", RegexOption.IGNORE_CASE),
        Regex("\\bdebit(?:ed)?\\s+(?:for|to|from)\\s+(.+)", RegexOption.IGNORE_CASE),
        Regex("\\bdeduct(?:ed)?\\s+(?:for|to|from)\\s+(.+)", RegexOption.IGNORE_CASE),
        Regex("\\bpaid\\s+for\\s+(.+)", RegexOption.IGNORE_CASE)
    )

    /**
     * Last resort, only consulted when no specific pattern matched at all.
     * Every one of these captures the rest of the sentence, so the cleaning in
     * [cleanMerchant] is doing most of the work here.
     */
    private val GENERIC_MERCHANT_PATTERNS = listOf(
        Regex("\\bpaid\\s+(.+)", RegexOption.IGNORE_CASE),
        Regex("\\bat\\s+(.+)", RegexOption.IGNORE_CASE),
        Regex("\\bfrom\\s+(.+)", RegexOption.IGNORE_CASE),
        Regex("\\bto\\s+(.+)", RegexOption.IGNORE_CASE),
        Regex("\\bfor\\s+(.+)", RegexOption.IGNORE_CASE)
    )

    private val TRAILER_MATCHERS = MERCHANT_TRAILERS.map { Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE) }

    /**
     * The merchant, or null.
     *
     * Two kinds of capture are discarded rather than cleaned: a UPI handle and
     * anything that is only digits, masking characters or mostly digits. Those
     * are account identifiers rather than shops, and a notification's copy of one
     * has no business in an expense row.
     */
    private fun extractMerchant(original: String): String? {
        // Specific patterns only. Once one of them has matched this text, the
        // generic "at"/"to" fallbacks are not tried: they would re-capture the
        // same span *including the connector*, and cleaning that copy can
        // succeed where the specific reading was correctly rejected as an
        // account identifier ("to swiggy@paytm" instead of null).
        val specific = MERCHANT_PATTERNS.mapNotNull { pattern -> pattern.find(original)?.groups?.get(1)?.value }
        if (specific.isNotEmpty()) {
            for (captured in specific) {
                val cleaned = cleanMerchant(captured)
                if (!cleaned.isNullOrBlank()) return cleaned
            }
            return null
        }
        for (pattern in GENERIC_MERCHANT_PATTERNS) {
            val cleaned = cleanMerchant(pattern.find(original)?.groups?.get(1)?.value ?: continue)
            if (!cleaned.isNullOrBlank()) return cleaned
        }
        return null
    }

    private fun cleanMerchant(raw: String): String? {
        // Before any cutting: "your account" is an account phrase, but it also
        // contains a trailer keyword, so cutting first would leave the one word
        // that means nothing.
        val head = raw.trim()
        if (looksLikeIdentifier(head)) return null

        // Cut at the *earliest* trailing keyword, not the first one in the list:
        // "Amazon for Rs 349" must become "Amazon", and "for" comes before "Rs".
        val cutAt = TRAILER_MATCHERS
            .mapNotNull { matcher -> matcher.find(raw)?.range?.first }
            .filter { it > 0 }
            .minOrNull()
        var text = if (cutAt != null) raw.substring(0, cutAt) else raw
        text = text.trim().trim(*PUNCTUATION_EDGE).replace(Regex("\\s+"), " ").trim()
        if (text.isEmpty()) return null
        if (text.length > MAX_MERCHANT_LENGTH) {
            val head48 = text.take(MAX_MERCHANT_LENGTH)
            val lastSpace = head48.lastIndexOf(' ')
            text = (if (lastSpace > MAX_MERCHANT_LENGTH / 2) head48.take(lastSpace) else head48).trim()
        }
        if (text.isEmpty()) return null
        if (looksLikeIdentifier(text)) return null
        return text
    }

    /**
     * Whether [text] is an account identifier rather than a merchant name: a UPI
     * handle, a masked or plain account fragment, an account phrase, or a string
     * with more digits than letters.
     */
    private fun looksLikeIdentifier(text: String): Boolean =
        UPI_HANDLE.matches(text) ||
            ACCOUNT_PHRASE.matches(text) ||
            ACCOUNT_FRAGMENT.matches(text) ||
            text.all { it.isDigit() } ||
            text.count { it.isDigit() } > text.count { it.isLetter() }

    // ---- time, reference, confidence -------------------------------------

    /**
     * Prefers a clock time written in the notification, because a bank that posts
     * at 23:58 for a 21:40 purchase should land the expense on the evening.
     *
     * The in-text time is only believed when it falls within
     * [IN_TEXT_TIME_TOLERANCE_MILLIS] of the post time, and that check is what
     * keeps "Txn 98765432" or a reference fragment from being read as a time of
     * day. Post time is the fallback, and it is always the right timezone — it is
     * resolved through [DateTimeUtils.zoneId], the app's single zone convention.
     */
    private fun resolveTimestamp(postedAtEpochMillis: Long, flat: String): Long {
        val parsed = readInTextTime(flat) ?: return postedAtEpochMillis
        val posted = Instant.ofEpochMilli(postedAtEpochMillis).atZone(DateTimeUtils.zoneId())
        val local = runCatching { LocalTime.of(parsed.hour, parsed.minute) }.getOrNull() ?: return postedAtEpochMillis
        val adjusted = when {
            parsed.meridiem == "pm" && local.hour < 12 -> local.plusHours(12)
            parsed.meridiem == "am" && local.hour == 12 -> local.minusHours(12)
            else -> local
        }
        val millis = runCatching { posted.with(adjusted).toInstant().toEpochMilli() }.getOrNull() ?: return postedAtEpochMillis
        return if (kotlin.math.abs(millis - postedAtEpochMillis) <= IN_TEXT_TIME_TOLERANCE_MILLIS) millis else postedAtEpochMillis
    }

    private data class InTextTime(val hour: Int, val minute: Int, val meridiem: String?)

    private fun readInTextTime(flat: String): InTextTime? {
        // Meridiem first: "at 9:40 pm" also satisfies the 24-hour pattern, and
        // reading it as 09:40 would then be rejected as implausible.
        IN_TEXT_TIME_MERIDIEM.find(flat)?.let { match ->
            val hour = match.groups[1]?.value?.toIntOrNull()
            val minute = match.groups[2]?.value?.toIntOrNull() ?: 0
            val meridiem = match.groups[3]?.value?.lowercase()
            if (hour != null && hour in 1..12 && minute in 0..59 && meridiem != null) {
                return InTextTime(hour, minute, meridiem)
            }
        }
        IN_TEXT_TIME_24H.find(flat)?.let { match ->
            val hour = match.groups[1]?.value?.toIntOrNull()
            val minute = match.groups[2]?.value?.toIntOrNull() ?: 0
            if (hour != null && hour in 0..23 && minute in 0..59) {
                return InTextTime(hour, minute, meridiem = null)
            }
        }
        return null
    }

    private fun extractReference(flat: String): String? =
        REFERENCE.find(flat)?.groups?.get(1)?.value

    /**
     * Confidence is about *the transaction reading*, never about the merchant:
     * a debit with no merchant named is still a debit worth recording (it just
     * lands in "Other"), whereas an amount LifeOS cannot pin to the payment is
     * a guess.
     *
     *  - HIGH — a currency-marked amount, a recognised meaning, and no
     *    competition from a second amount.
     *  - MEDIUM — as above but two different marked amounts were present, so one
     *    of them is a balance or a limit.
     *  - LOW — the amount had to come from an unmarked bare number.
     */
    private fun confidenceFor(amount: AmountMatch, type: TransactionType): CaptureConfidence = when {
        !amount.marked -> CaptureConfidence.LOW
        amount.ambiguous -> CaptureConfidence.MEDIUM
        type == TransactionType.UNKNOWN -> CaptureConfidence.LOW
        else -> CaptureConfidence.HIGH
    }

    // ---- text normalisation ------------------------------------------------

    /**
     * Unicode-normalises and collapses whitespace without lowercasing, so the
     * merchant keeps the casing the bank wrote it in. NFKC is what folds the
     * decomposed rupee sign onto the single-codepoint one the amount patterns
     * match on.
     */
    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKC)
            .replace(RUPEE_SPELLINGS, "\u20B9")
            .replace('\u200B', ' ')
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

    /** Lowercase, marker-light form used only for keyword containment. */
    /**
     * Lowercase, marker-light form used only for keyword containment.
     *
     * Normalisation runs here too so [looksFinancial] — which is handed raw
     * notification text by the capture service — sees the same folded rupee sign
     * as [parse] does. Normalisation is idempotent, so the second pass on
     * already-normalised text is free of effect.
     */
    private fun flatten(text: String): String = normalize(text).lowercase()

    private fun hasCurrencyMarker(flat: String): Boolean =
        flat.contains("inr") ||
            Regex("\\brs\\b\\.?").containsMatchIn(flat) ||
            Regex("(?:₹|रु)\\s*\\d").containsMatchIn(flat) ||
            Regex("\\d\\s*(?:₹|\\binr\\b|\\brs\\b)").containsMatchIn(flat)
}
