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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.time.LocalDate

/**
 * Diary masthead matching the supplied day reference:
 * TODAY → large date → calendar / overflow, followed by the five-day strip.
 *
 * Navigation remains the existing day-selection callback; this component is
 * presentation only and does not introduce a second navigation stack.
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
            .padding(top = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LifeOSSpacing.screenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = date,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val offset = if (forward) 1 else -1
                        (
                            slideInHorizontally(
                                tween(260, easing = FastOutSlowInEasing)
                            ) { width -> offset * width / 6 } + fadeIn(tween(220))
                            ) togetherWith (
                            slideOutHorizontally(tween(200)) { width -> -offset * width / 6 } +
                                fadeOut(tween(160))
                            )
                    },
                    label = "diaryHeaderDate"
                ) { day ->
                    Column {
                        Text(
                            dayEyebrow(day).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = DiaryInkViolet.copy(alpha = 0.55f),
                            letterSpacing = 2.0.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                DateTimeUtils.formatFullDate(day),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.35).sp
                                ),
                                color = DiaryInkViolet,
                                maxLines = 1
                            )
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Choose date",
                                tint = DiaryActionViolet,
                                modifier = Modifier
                                    .padding(start = 2.dp)
                                    .size(24.dp)
                            )
                        }
                    }
                }
            }

            HeaderIconButton(
                icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                description = "Choose date",
                onClick = onCreate,
                filled = false
            )
            Spacer(Modifier.size(8.dp))
            HeaderIconButton(
                icon = { Icon(Icons.Filled.MoreVert, contentDescription = null) },
                description = "Diary options",
                onClick = onCreate,
                filled = false
            )
        }

        Spacer(Modifier.height(14.dp))

        DiaryDateStrip(
            selectedDay = selectedDay,
            daysWithMemories = daysWithMemories,
            onSelectDay = onSelectDay
        )
    }
}

/**
 * Small 48dp target with a soft lavender container, visually echoing the
 * reference's calendar and overflow buttons while keeping the tap target safe.
 */
@Composable
private fun HeaderIconButton(
    icon: @Composable () -> Unit,
    description: String,
    onClick: () -> Unit,
    filled: Boolean
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (filled) DiaryActionViolet else DiaryLavender.copy(alpha = 0.34f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = if (description == "Choose date") Icons.Filled.CalendarMonth else Icons.Filled.MoreVert,
            contentDescription = description,
            tint = if (filled) MaterialTheme.colorScheme.onPrimary else DiaryActionViolet,
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun dayEyebrow(day: LocalDate): String {
    val today = DateTimeUtils.today()
    return when (java.time.temporal.ChronoUnit.DAYS.between(day, today)) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> DateTimeUtils.formatDayOfWeek(day)
    }
}

/** Five compact dates remain the existing bounded, centred day navigation. */
@Composable
fun CreateMemoryAction(onClick: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    // Kept for existing callers; the new masthead uses the reference's icon
    // actions directly.
    Text(
        text = if (compact) "Memory" else "+ Memory",
        style = MaterialTheme.typography.labelLarge,
        color = DiaryActionViolet,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}
