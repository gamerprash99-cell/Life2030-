package com.lifeos.app.ui.diary

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The masthead of a day: eyebrow, a large day numeral, the month and weekday
 * underneath, and a row of ticks showing where the selected day sits and which
 * neighbouring days already hold memories.
 *
 * The day is presented as the current page of the user's memory rather than a
 * selected button, so there is no "is this chip active?" state to read — the
 * numeral simply *is* the day.
 */
@Composable
fun DiaryDayHeader(
    selectedDay: Long,
    daysWithMemories: Set<Long>,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val date = remember(selectedDay) { DateTimeUtils.epochDayToLocalDate(selectedDay) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = LifeOSSpacing.screenPadding, end = LifeOSSpacing.screenPadding, top = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
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
            Column {
                Text(
                    dayEyebrow(day).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = DiaryInkViolet.copy(alpha = 0.45f),
                    letterSpacing = 1.8.sp
                )
                Text(
                    day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = DiaryInkViolet,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${day.month.name.lowercase().replaceFirstChar { it.uppercase() }} · " +
                        day.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        DayTicks(
            selectedDay = selectedDay,
            daysWithMemories = daysWithMemories,
            canGoForward = canGoForward,
            onPrevious = onPrevious,
            onNext = onNext
        )
    }
}

/** "Today" / "Yesterday" / "3 days ago" so the user never has to do date maths. */
private fun dayEyebrow(day: LocalDate): String {
    val today = DateTimeUtils.today()
    return when (java.time.temporal.ChronoUnit.DAYS.between(day, today)) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofPattern("EEEE"))
    }
}

/**
 * Seven ticks spanning the selected day ±3. Filled ticks are days that already
 * hold memories, so the control doubles as a map of where the user's diary
 * actually has content.
 */
@Composable
private fun DayTicks(
    selectedDay: Long,
    daysWithMemories: Set<Long>,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = "Previous day",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            (-3L..3L).forEach { offset ->
                val epochDay = selectedDay + offset
                val isSelected = offset == 0L
                val hasMemories = epochDay in daysWithMemories
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 9.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> DiaryInkViolet
                                hasMemories -> DiaryInkViolet.copy(alpha = 0.42f)
                                else -> DiaryHairline
                            }
                        )
                        .then(if (isSelected) Modifier.semantics { contentDescription = "Selected day" } else Modifier)
                )
            }
        }

        IconButton(onClick = onNext, enabled = canGoForward, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Next day",
                tint = if (canGoForward) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        }
    }
}

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
