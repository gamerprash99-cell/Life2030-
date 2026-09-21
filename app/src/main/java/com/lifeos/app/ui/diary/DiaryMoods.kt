package com.lifeos.app.ui.diary

import androidx.compose.ui.graphics.Color
import com.lifeos.app.core.intelligence.Mood
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryMoodCalm
import com.lifeos.app.ui.theme.DiaryMoodExcited
import com.lifeos.app.ui.theme.DiaryMoodHappy
import com.lifeos.app.ui.theme.DiaryMoodSad
import com.lifeos.app.ui.theme.DiaryMoodStressed

/** One selectable mood on the editor sheet, keyed by the stored string ("😊 Happy"). */
data class DiaryMood(val key: String, val label: String, val emoji: String)

/**
 * The five mood pills from the Stitch "New diary entry" composer. Keys are the
 * exact strings LifeOS has always persisted, so old entries keep their mood.
 */
object DiaryMoods {
    val OPTIONS = listOf(
        DiaryMood("😊 Happy", "Happy", "😊"),
        DiaryMood("😌 Calm", "Calm", "😌"),
        DiaryMood("😔 Sad", "Sad", "😔"),
        DiaryMood("😤 Stressed", "Stressed", "😤"),
        DiaryMood("🤩 Excited", "Excited", "🤩")
    )

    fun fromStored(value: String?): DiaryMood? = OPTIONS.firstOrNull { it.key.equals(value, ignoreCase = true) }

    fun displayLabel(value: String?): String = fromStored(value)?.label ?: value.orEmpty()

    fun colorOf(value: String?): Color = when (fromStored(value)?.label) {
        "Happy" -> DiaryMoodHappy
        "Calm" -> DiaryMoodCalm
        "Sad" -> DiaryMoodSad
        "Stressed" -> DiaryMoodStressed
        "Excited" -> DiaryMoodExcited
        else -> DiaryLavender
    }

    /** Maps the analyzer's [Mood] (used when no mood is stored) back to a pill. */
    fun fromAnalysis(mood: Mood): DiaryMood? = when (mood) {
        Mood.VERY_POSITIVE -> fromStored("🤩 Excited")
        Mood.POSITIVE -> fromStored("😊 Happy")
        Mood.NEUTRAL -> fromStored("😌 Calm")
        Mood.NEGATIVE -> fromStored("😔 Sad")
        Mood.VERY_NEGATIVE -> fromStored("😤 Stressed")
        Mood.UNKNOWN -> null
    }

    fun inkOn(value: String?): Color = DiaryInkViolet.copy(alpha = 0.92f)
}