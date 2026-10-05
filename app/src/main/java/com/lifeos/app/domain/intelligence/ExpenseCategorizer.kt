package com.lifeos.app.domain.intelligence

import com.lifeos.app.domain.model.CaptureConfidence
import com.lifeos.app.domain.model.ExpenseCategories

/**
 * Maps a merchant name from a parsed transaction onto one of the categories the
 * app already offers.
 *
 * This is a lookup table, not a model: no training, no weights, no network, no
 * inference. That is deliberate — a merchant string off a notification is either
 * a name in the table or it is not, and the two cases deserve different answers:
 *
 *  - exact match → [CaptureConfidence.HIGH]
 *  - the name merely *contains* a known brand ("Amazon Pay", "SWIGGY EATS") →
 *    [CaptureConfidence.MEDIUM]
 *  - nothing matches → null, and the caller files it under
 *    [ExpenseCategories.DEFAULT]
 *
 * Nothing here writes to the category list. [ExpenseCategories.ALL] is the single
 * definition of the categories, this file only ever *names* one of them, and an
 * unrecognised merchant never becomes a new category.
 */
object ExpenseCategorizer {

    /** One category's worth of merchant tokens. */
    private data class Rule(val category: String, val tokens: List<String>)

    /**
     * Order matters, and only in one place: subscriptions are tested before
     * shopping, so "Amazon Prime Video" files as a subscription rather than as a
     * shopping basket. Everywhere else the categories do not share tokens.
     *
     * Deliberately a short, recognisable list of brands a person would expect to
     * be auto-filed. A longer list of thousands would raise the hit rate and the
     * error rate together, and an expense that silently lands under the wrong
     * category is the failure mode this whole feature is designed to avoid.
     */
    private val RULES = listOf(
        Rule(
            "Subscriptions",
            listOf(
                "netflix", "spotify", "prime video", "amazon prime", "hotstar", "jiohotstar",
                "disney", "hotstar plus", "youtube premium", "google one", "icloud", "apple",
                "dropbox", "canva", "notion", "adobe", "microsoft 365", "prime", "gaana",
                "hbo", "paramount", "zee5", "sonyliv", "aha", "sunnxt"
            )
        ),
        Rule(
            "Food",
            listOf(
                "swiggy", "zomato", "mcdonalds", "mcdelivery", "dominos", "domino",
                "burger king", "subway", "papa johns", "kfc", "restaurant", "eats",
                "food", "biryani", "tandoor", "haldiram"
            )
        ),
        Rule(
            "Cafe",
            listOf("starbucks", "blue tokai", "third wave", "coffee", "cafe", "chai", "barista")
        ),
        Rule(
            "Travel",
            listOf(
                "uber", "ola", "rapido", "lyft", "indigo", "goindigo", "irctc", "makemytrip",
                "ixigo", "redbus", "metro", "indian oil", "indianoil", "bharatpetroleum",
                "hpcl", "bpcl", "nayara", "petronet", "toll", "parking"
            )
        ),
        Rule(
            "Shopping",
            listOf(
                "amazon", "flipkart", "myntra", "ajio", "meesho", "snapdeal", "dmart",
                "avenue", "reliance", "ikea", "decathlon", "croma", "bigbasket",
                "blinkit", "zepto", "swiggy instamart", "clovia", "westside", "zara"
            )
        ),
        Rule(
            "Bills",
            listOf(
                "airtel", "jio", "bsnl", "vodafone", "idea", "reliance jio", "tata power",
                "mahanagar gas", "bescom", "mahadiscom", "adani", "torrent power", "bses",
                "act fibernet", "hathway", "ex broadband", "tikona", "electricity", "gas bill"
            )
        ),
        Rule(
            "Health",
            listOf(
                "apollo", "netmeds", "1mg", "pharmeasy", "pharm easy", "practo", "healthkart",
                "medplus", "hospital", "clinic", "chemist", "pharmacy", "diagnostics"
            )
        ),
        Rule(
            "Education",
            listOf(
                "udemy", "coursera", "unacademy", "byju", "byjus", "upgrad", "greatlearning",
                "khan academy", "duolingo", "toppr", "physicswallah", "cuemath", "whitehat jr",
                "coursera plus", "school", "tuition", "academy"
            )
        ),
        Rule(
            "Entertainment",
            listOf("bookmyshow", "pvr", "inox", "cinepolis", "miraj", "sirius", "film city", "amusement", "gaming")
        )
    )

    /**
     * Best category for [merchant], or null when the merchant is unknown or
     * carries nothing recognisable. Never guesses a category outside
     * [ExpenseCategories.ALL].
     */
    fun categorize(merchant: String?): Suggestion? {
        val normalized = normalizeForMatch(merchant ?: return null)
        if (normalized.isEmpty()) return null
        val words = normalized.split(' ')

        for (rule in RULES) {
            if (!ExpenseCategories.isKnown(rule.category)) continue
            for (token in rule.tokens) {
                if (normalized == token) return Suggestion(rule.category, CaptureConfidence.HIGH)
                if (containsPhrase(words, token)) return Suggestion(rule.category, CaptureConfidence.MEDIUM)
            }
        }
        return null
    }

    /**
     * Whether [words] contains [phrase] as consecutive whole words.
     *
     * Substring matching would be shorter to write and wrong: "ola" sits inside
     * "Cholamandalam" and "card" inside "Saffron Card", and a three-letter token
     * matching a fragment is how a bank statement ends up filed under Transport.
     */
    private fun containsPhrase(words: List<String>, phrase: String): Boolean {
        val needle = phrase.split(' ').filter { it.isNotEmpty() }
        if (needle.isEmpty() || needle.size > words.size) return false
        for (start in 0..(words.size - needle.size)) {
            if (words.subList(start, start + needle.size) == needle) return true
        }
        return false
    }

    /**
     * The category to file under, which is always one the app offers: the
     * suggestion when there is one, [ExpenseCategories.DEFAULT] otherwise.
     */
    fun categoryOrDefault(merchant: String?): String =
        categorize(merchant)?.category ?: ExpenseCategories.DEFAULT

    /** A category and how much the lookup trusts itself. */
    data class Suggestion(val category: String, val confidence: CaptureConfidence)

    /**
     * Lowercase, word-per-token. Punctuation and accents become word separators,
     * so "Swiggy.in" and "SWIGGY" both reduce to the token "swiggy", while
     * "Domino's" reduces to "domino" + "s" — which still matches the "domino"
     * rule as a whole word.
     *
     * Also used by duplicate detection, so "SWIGGY", "Swiggy" and "swiggy " are
     * one merchant there and not three.
     */
    internal fun normalizeForMatch(merchant: String): String =
        merchant.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
}
