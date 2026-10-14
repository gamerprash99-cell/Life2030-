package com.lifeos.app.ui.diary

import androidx.compose.ui.graphics.Color
import com.lifeos.app.domain.model.DiaryMood
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.ui.theme.DiaryMoodAngry
import com.lifeos.app.ui.theme.DiaryMoodAngryPastel
import com.lifeos.app.ui.theme.DiaryMoodAnxious
import com.lifeos.app.ui.theme.DiaryMoodAnxiousPastel
import com.lifeos.app.ui.theme.DiaryMoodCalm
import com.lifeos.app.ui.theme.DiaryMoodCalmPastel
import com.lifeos.app.ui.theme.DiaryMoodExcited
import com.lifeos.app.ui.theme.DiaryMoodExcitedPastel
import com.lifeos.app.ui.theme.DiaryMoodHappy
import com.lifeos.app.ui.theme.DiaryMoodHappyPastel
import com.lifeos.app.ui.theme.DiaryMoodNeutral
import com.lifeos.app.ui.theme.DiaryMoodSad
import com.lifeos.app.ui.theme.DiaryMoodSadPastel
import com.lifeos.app.ui.theme.DiaryMoodStressed
import com.lifeos.app.ui.theme.DiaryMoodStressedPastel
import com.lifeos.app.ui.theme.DiaryMoodTired
import com.lifeos.app.ui.theme.DiaryMoodTiredPastel

/**
 * Mood colour, kept out of [DiaryMood] on purpose.
 *
 * `DiaryMood` is now a domain model, because the intelligence layer counts and
 * compares moods and must not drag Compose `Color` in with it. That leaves the
 * palette as the one genuinely presentational part of a mood, so it lives here,
 * keyed by the same persisted string. An unrecognised mood falls back to the
 * neutral wash rather than being dropped, so a row written by a newer build
 * still renders.
 *
 * Note that colour is never the only carrier of meaning: every mood disc and
 * marker pairs its colour with the emoji and the label (see [MoodDot]).
 */
object DiaryMoodVisuals {

    private val accents: Map<String, Color> = mapOf(
        "😊 Happy" to DiaryMoodHappy,
        "😌 Calm" to DiaryMoodCalm,
        "🥱 Tired" to DiaryMoodTired,
        "😔 Sad" to DiaryMoodSad,
        "😰 Anxious" to DiaryMoodAnxious,
        "😤 Stressed" to DiaryMoodStressed,
        "😠 Angry" to DiaryMoodAngry,
        "🤩 Excited" to DiaryMoodExcited
    )

    private val halos: Map<String, Color> = mapOf(
        "😊 Happy" to DiaryMoodHappyPastel,
        "😌 Calm" to DiaryMoodCalmPastel,
        "🥱 Tired" to DiaryMoodTiredPastel,
        "😔 Sad" to DiaryMoodSadPastel,
        "😰 Anxious" to DiaryMoodAnxiousPastel,
        "😤 Stressed" to DiaryMoodStressedPastel,
        "😠 Angry" to DiaryMoodAngryPastel,
        "🤩 Excited" to DiaryMoodExcitedPastel
    )

    /** Saturated accent for the mood marker ring and label. */
    fun accentOf(value: String?): Color =
        accents[DiaryMoods.fromStored(value)?.key] ?: DiaryMoodNeutral

    /** Pastel wash behind the mood marker. */
    fun haloOf(value: String?): Color =
        halos[DiaryMoods.fromStored(value)?.key] ?: DiaryMoodNeutral

    fun accentOf(mood: DiaryMood?): Color = accentOf(mood?.key)

    fun haloOf(mood: DiaryMood?): Color = haloOf(mood?.key)
}