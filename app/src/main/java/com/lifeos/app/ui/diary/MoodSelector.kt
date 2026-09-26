package com.lifeos.app.ui.diary

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * Mood picker: a hairline rule with expressive glyphs resting on it, not a row
 * of pills. Selection is carried by a spring scale plus a coloured halo, and
 * the chosen mood's name fades in underneath — so the state is legible from
 * the label and the emoji as well as from colour.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodSelector(
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = DiaryMoods.fromStored(selectedKey)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 5
        ) {
            DiaryMoods.OPTIONS.forEach { mood ->
                MoodMark(
                    mood = mood,
                    selected = mood.key == selectedKey,
                    onClick = { onSelect(mood.key) }
                )
            }
        }

        // Reserve the row whether or not a mood is chosen so the writing
        // surface below never shifts as the user picks.
        Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.Center) {
            // Cross-faded rather than swapped: the label changes on every tap,
            // and an instant replacement reads as a flicker at the exact moment
            // the user is looking for confirmation.
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(110)) },
                label = "moodLabel"
            ) { chosen ->
                Text(
                    chosen?.label ?: "How did the day feel?",
                    style = MaterialTheme.typography.labelMedium,
                    color = chosen?.accent
                        ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** One mood glyph: 48dp touch target, a pastel halo and an accent ring when chosen. */
@Composable
private fun MoodMark(
    mood: DiaryMood,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.16f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "moodScale"
    )

    Box(
        modifier = Modifier
            // 48dp keeps the glyph inside the platform touch-target minimum
            // even though the visible disc is only 34dp.
            .defaultMinSize(LifeOSSpacing.minTouchTarget, LifeOSSpacing.minTouchTarget)
            .clip(CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                this.selected = selected
                this.role = Role.RadioButton
                // Emoji alone is not a dependable label; the name is.
                contentDescription = mood.label
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(if (selected) mood.halo else DiaryHairline.copy(alpha = 0.34f))
                .then(
                    if (selected) Modifier.border(1.5.dp, mood.accent, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                mood.emoji,
                fontSize = 17.sp,
                color = if (selected) mood.accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Small non-interactive mood marker used on the timeline spine and the editor header. */
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

/** Editorial caption above the editor, e.g. the day being written for. */
@Composable
fun EditorEyebrow(dayEpochDay: Long, isEditing: Boolean, modifier: Modifier = Modifier) {
    val date = DateTimeUtils.epochDayToLocalDate(dayEpochDay)
    val eyebrow = when {
        isEditing -> "Editing a memory"
        date == DateTimeUtils.today() -> "Today"
        else -> DateTimeUtils.formatFullDate(date)
    }
    Text(
        eyebrow.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = DiaryInkViolet.copy(alpha = 0.45f),
        letterSpacing = 1.6.sp,
        modifier = modifier
    )
}
