package com.lifeos.app.ui.diary

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.time.LocalDate

/**
 * The masthead of a day: a compact eyebrow and date line, with the date strip
 * directly beneath as the single day-navigation control.
 *
 * The old layout gave a `displayLarge` numeral the most visual weight on the
 * page — more than the memories themselves — and then hid navigation in a row of
 * seven ambiguous ticks. The day is now *chosen* from the strip, so the header's
 * only job is to name the day currently being read; the strip owns where you can
 * go, and the day's own words stay the loudest thing on screen.
 */
@Composable
fun DiaryDayHeader(
    selectedDay: Long,
    daysWithMemories: Set<Long>,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onSelectDay: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val date = remember(selectedDay) { DateTimeUtils.epochDayToLocalDate(selectedDay) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = LifeOSSpacing.screenPadding, end = LifeOSSpacing.screenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DiaryInkViolet
                )
            }
            CreateMemoryAction(onClick = onCreate)
        }

        Spacer(Modifier.height(2.dp))

        // Direction is taken from the previously shown day so stepping back
        // slides content left and stepping forward slides it right.
        AnimatedContent(
            targetState = date,
            transitionSpec = {
                val forward = targetState > initialState
                val offset = if (forward) 1 else -1
                (
                    slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { width -> offset * width / 6 } +
                        fadeIn(tween(280))
                    ) togetherWith (
                    slideOutHorizontally(tween(220)) { width -> -offset * width / 6 } +
                        fadeOut(tween(180))
                    )
            },
            label = "diaryDayHeader"
        ) { day ->
            Column(
                modifier = Modifier.padding(horizontal = LifeOSSpacing.screenPadding)
            ) {
                Text(
                    dayEyebrow(day).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = DiaryInkViolet.copy(alpha = 0.45f),
                    letterSpacing = 1.8.sp
                )
                Text(
                    dayHeaderDateLine(day),
                    style = MaterialTheme.typography.titleMedium,
                    color = DiaryInkViolet,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Full-bleed so a centred cell is genuinely centred on the screen; the
        // label above keeps its own screen padding.
        DiaryDateStrip(
            selectedDay = selectedDay,
            daysWithMemories = daysWithMemories,
            onSelectDay = onSelectDay
        )
    }
}

/** "Today" / "Yesterday" / weekday name, so the user never has to do date maths. */
private fun dayEyebrow(day: LocalDate): String {
    val today = DateTimeUtils.today()
    return when (java.time.temporal.ChronoUnit.DAYS.between(day, today)) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> DateTimeUtils.formatDayOfWeek(day)
    }
}

/** "26 September · Saturday" — the day named once, plainly, under the eyebrow. */
private fun dayHeaderDateLine(day: LocalDate): String =
    "${day.dayOfMonth} ${day.month.name.lowercase().replaceFirstChar { it.uppercase() }} · " +
        DateTimeUtils.formatDayOfWeek(day).lowercase().replaceFirstChar { it.uppercase() }

/** The Diary's create affordance: an inline text + glyph action, not a floating button. */
@Composable
fun CreateMemoryAction(onClick: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    Text(
        text = if (compact) "Memory" else "+ Memory",
        style = MaterialTheme.typography.labelLarge,
        color = DiaryInkViolet,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}
