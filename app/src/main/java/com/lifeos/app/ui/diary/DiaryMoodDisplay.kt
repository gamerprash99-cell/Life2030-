package com.lifeos.app.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Read-only display of a stored mood.
 *
 * The composer no longer has a mood picker — [DiaryEditor] takes `mood` and
 * renders it, and never writes it. A memory written before the picker was
 * removed still shows the mood it was saved with, so removing the control does
 * not silently erase or hide what is already in the database, and new memories
 * simply have none. Nothing in this file can change a stored value: the data
 * model, the column and every historical mood string are untouched.
 *
 * The name always travels with the glyph, never colour alone.
 */
@Composable
fun StoredMoodChip(moodKey: String, modifier: Modifier = Modifier) {
    val mood = DiaryMoods.fromStored(moodKey) ?: return
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(mood.halo)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(mood.emoji, fontSize = 13.sp, color = mood.accent)
        Text(
            mood.label,
            style = MaterialTheme.typography.labelSmall,
            color = mood.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Small non-interactive mood marker used on the timeline spine and the entry detail. */
@Composable
fun MoodDot(moodKey: String?, modifier: Modifier = Modifier, diameter: Int = 12) {
    val accent = DiaryMoods.colorOf(moodKey)
    val halo = DiaryMoods.backgroundOf(moodKey)
    Box(
        modifier = modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .background(halo)
            .border(1.5.dp, accent, CircleShape)
    )
}
