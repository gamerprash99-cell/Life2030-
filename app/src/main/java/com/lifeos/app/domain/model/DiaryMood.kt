package com.lifeos.app.domain.model

/**
 * How a mood sits on the positive/negative axis, independent of *which* mood
 * it is.
 *
 * This is the one piece of mood data the intelligence layer needs and the
 * composer does not: counting "Calm" separately from "Excited" is a UI concern,
 * but deciding that both pull the same way is an analytical one. Keeping it
 * here is what lets [com.lifeos.app.domain.intelligence.MoodAnalyzer] reason
 * about mood without importing Compose.
 */
enum class MoodValence {
    POSITIVE,
    NEUTRAL,

    /** Low-arousal negative (Tired) and high-arousal negative are both here. */
    NEGATIVE
}

/**
 * One selectable mood. [key] is the exact string persisted in
 * `diary_entries.mood`, which for every mood LifeOS has ever shipped already
 * carries its own emoji ("😊 Happy"), so it round-trips through the database
 * and through the user's eyes unchanged.
 */
data class DiaryMood(
    val key: String,
    val label: String,
    val emoji: String,
    val valence: MoodValence
)

/**
 * The mood vocabulary.
 *
 * [OPTIONS] deliberately carries the *exact* keys LifeOS has always persisted
 * for the original five, so entries written before the Daily Memory redesign
 * keep their mood. The three newer moods are additive. Adding or reordering
 * this list therefore needs no schema change and no data migration — and the
 * keys must not be "cleaned up", because that would silently drop the mood off
 * every historical entry.
 *
 * Colour is *not* modelled here. The accent and pastel wash for a mood are a
 * rendering decision that belongs to the theme layer; see
 * `ui/diary/DiaryMoodVisuals`. Keeping `DiaryMood` free of Compose is what
 * lets the analyzers and the DAO-side code depend on it.
 */
object DiaryMoods {

    val OPTIONS: List<DiaryMood> = listOf(
        DiaryMood("😊 Happy", "Happy", "😊", MoodValence.POSITIVE),
        DiaryMood("😌 Calm", "Calm", "😌", MoodValence.POSITIVE),
        DiaryMood("🥱 Tired", "Tired", "🥱", MoodValence.NEGATIVE),
        DiaryMood("😔 Sad", "Sad", "😔", MoodValence.NEGATIVE),
        DiaryMood("😰 Anxious", "Anxious", "😰", MoodValence.NEGATIVE),
        DiaryMood("😤 Stressed", "Stressed", "😤", MoodValence.NEGATIVE),
        DiaryMood("😠 Angry", "Angry", "😠", MoodValence.NEGATIVE),
        DiaryMood("🤩 Excited", "Excited", "🤩", MoodValence.POSITIVE)
    )

    /** Lookup by persisted key, case-insensitively. `null` when unrecognised. */
    fun fromStored(value: String?): DiaryMood? =
        OPTIONS.firstOrNull { it.key.equals(value?.trim(), ignoreCase = true) }

    /**
     * Human label for a stored value. An unrecognised value is returned as
     * itself rather than dropped: a row written by a future build must still
     * read as *something* here instead of rendering blank.
     */
    fun displayLabel(value: String?): String = fromStored(value)?.label ?: value.orEmpty()

    /**
     * Valence for a stored value, or `null` when the value is missing or
     * unrecognised. Callers must treat `null` as "not counted", never as
     * `NEUTRAL` — folding an unknown mood into the neutral bucket would let a
     * single unrecognised row dilute a real average.
     */
    fun valenceOf(value: String?): MoodValence? = fromStored(value)?.valence

    /** The mood pre-selected in the composer, so a new entry starts annotated. */
    val DEFAULT: DiaryMood = OPTIONS.first()
}