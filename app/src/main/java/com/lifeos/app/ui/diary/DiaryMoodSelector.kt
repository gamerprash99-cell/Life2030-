package com.lifeos.app.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeos.app.domain.model.DiaryMood
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline

/**
 * The mood picker: eight discs in a row.
 *
 * This exists because `DiaryEditorViewModel.onMoodChange` had never been called
 * by anything. `DiaryEntity.mood` was persisted, displayed on the timeline and
 * coloured the detail header — but there was no way to *set* one. Every
 * mood-driven feature (the calendar's day dots, the insights chart, the
 * distribution) was therefore unreachable from the UI, and adding those
 * features without this would have produced a set of screens permanently
 * showing "no mood recorded" for a user who had no way to record one.
 *
 * Horizontal scroll rather than a wrap, because 8 discs at 48dp need 384dp and
 * a 360dp phone does not have it. A wrapping row would push the writing surface
 * down by two rows on the smallest supported screen and lift the save controls
 * off the keyboard.
 *
 * Tapping the selected disc clears the mood. Mood is optional by design — the
 * entity column is nullable and most existing entries have none — so the
 * selector has to be able to return to that state without a separate "clear"
 * button competing for space.
 *
 * Colour is never the only signal: each disc carries its emoji, and the label
 * is exposed to accessibility services, so the picker is usable without colour
 * vision and with a screen reader.
 */
@Composable
fun DiaryMoodSelector(
    selectedMoodKey: String?,
    onMoodChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "How are you feeling?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = DiaryActionViolet
            )
            Text(
                "Optional",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            // No content padding: the row is already inset by the caller's card.
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            itemsIndexed(DiaryMoods.OPTIONS, key = { _, mood -> mood.key }) { _, mood ->
                MoodDisc(
                    mood = mood,
                    isSelected = mood.key.equals(selectedMoodKey, ignoreCase = true),
                    onClick = { onMoodChange(if (mood.key == selectedMoodKey) null else mood.key) }
                )
            }
        }
    }
}

/** One selectable mood: pastel halo, emoji, and a ring only when it is the current mood. */
@Composable
private fun MoodDisc(
    mood: DiaryMood,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accent = DiaryMoodVisuals.accentOf(mood)
    val halo = DiaryMoodVisuals.haloOf(mood)

    Box(
        modifier = Modifier
            // 48dp: the minimum touch target. The visible disc is 44dp, so the
            // extra 4dp is invisible padding rather than a smaller target.
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isSelected) halo else halo.copy(alpha = 0.45f))
            .then(
                if (isSelected) Modifier.border(2.dp, accent, CircleShape)
                else Modifier.border(1.dp, DiaryHairline, CircleShape)
            )
            .clickable(onClick = onClick)
            // One node for the whole disc: the emoji glyph is decoration, and
            // announcing it separately would make the row read "smiling face,
            // smiling face, smiling face" to a screen reader.
            .semantics {
                contentDescription = mood.label
                this.selected = isSelected
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            mood.emoji,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}
