package com.lifeos.app.ui.diary

import androidx.compose.ui.graphics.Color
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
 * One selectable mood. [key] is the exact string persisted in
 * `diary_entries.mood`, [accent] is the saturated colour used for the marker
 * ring and label, and [halo] is the pastel wash behind the marker.
 *
 * Emoji and label travel together on purpose: mood is never communicated by
 * colour alone (see the accessibility notes on [MoodSelector]).
 */
data class DiaryMood(
    val key: String,
    val label: String,
    val emoji: String,
    val accent: Color,
    val halo: Color
)

/**
 * The mood vocabulary. Keys are the exact strings LifeOS has always persisted
 * for the original five, so entries written before the Daily Memory redesign
 * keep their mood; the three newer moods are additive and require no schema
 * change.
 */
object DiaryMoods {
    val OPTIONS = listOf(
        DiaryMood("😊 Happy", "Happy", "😊", DiaryMoodHappy, DiaryMoodHappyPastel),
        DiaryMood("😌 Calm", "Calm", "😌", DiaryMoodCalm, DiaryMoodCalmPastel),
        DiaryMood("🥱 Tired", "Tired", "🥱", DiaryMoodTired, DiaryMoodTiredPastel),
        DiaryMood("😔 Sad", "Sad", "😔", DiaryMoodSad, DiaryMoodSadPastel),
        DiaryMood("😰 Anxious", "Anxious", "😰", DiaryMoodAnxious, DiaryMoodAnxiousPastel),
        DiaryMood("😤 Stressed", "Stressed", "😤", DiaryMoodStressed, DiaryMoodStressedPastel),
        DiaryMood("😠 Angry", "Angry", "😠", DiaryMoodAngry, DiaryMoodAngryPastel),
        DiaryMood("🤩 Excited", "Excited", "🤩", DiaryMoodExcited, DiaryMoodExcitedPastel)
    )

    fun fromStored(value: String?): DiaryMood? =
        OPTIONS.firstOrNull { it.key.equals(value, ignoreCase = true) }

    fun displayLabel(value: String?): String = fromStored(value)?.label ?: value.orEmpty()

    /** Saturated accent for the mood marker ring and label. */
    fun colorOf(value: String?): Color = fromStored(value)?.accent ?: DiaryMoodNeutral

    /** Pastel wash behind the mood marker. */
    fun backgroundOf(value: String?): Color = fromStored(value)?.halo ?: DiaryMoodNeutral
}
