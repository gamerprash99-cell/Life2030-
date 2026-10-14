package com.lifeos.app.ui.snap

import androidx.compose.ui.graphics.Color
import com.lifeos.app.ui.theme.DiaryMoodCalm
import com.lifeos.app.ui.theme.DiaryMoodExcited
import com.lifeos.app.ui.theme.DiaryMoodHappy
import com.lifeos.app.ui.theme.DiaryMoodSad
import com.lifeos.app.ui.theme.DiaryMoodTired

/**
 * The moods offered when posting a Snap. The [key] is the same token the diary
 * stores in its nullable `mood` column, so a snap's mood is understood by the
 * rest of the app without a second vocabulary. [emoji] is content (a face), never
 * a UI control glyph.
 */
enum class SnapMood(val key: String, val label: String, val emoji: String, val accent: Color) {
    HAPPY("happy", "Happy", "😄", DiaryMoodHappy),
    CALM("calm", "Calm", "😌", DiaryMoodCalm),
    EXCITED("excited", "Excited", "🤩", DiaryMoodExcited),
    SAD("sad", "Down", "😔", DiaryMoodSad),
    TIRED("tired", "Tired", "😴", DiaryMoodTired);

    companion object {
        fun fromKey(key: String?): SnapMood? = entries.firstOrNull { it.key == key }
    }
}

/** Which surface of the Snap flow is showing. */
enum class SnapStage { CAMERA, COMPOSE }

/** Preset zoom steps. 3x is the headline "close-up" step; all are clamped to
 *  whatever the active lens actually supports. */
val snapZoomPresets = listOf(1f, 2f, 3f)
