package com.lifeos.app.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** Small non-interactive mood marker used on the timeline spine and the editor header. */
@Composable
fun MoodDot(moodKey: String?, modifier: Modifier = Modifier, diameter: Int = 12) {
    val accent = DiaryMoodVisuals.accentOf(moodKey)
    val halo = DiaryMoodVisuals.haloOf(moodKey)
    Box(
        modifier = modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .background(halo)
            .border(1.5.dp, accent, CircleShape)
    )
}
