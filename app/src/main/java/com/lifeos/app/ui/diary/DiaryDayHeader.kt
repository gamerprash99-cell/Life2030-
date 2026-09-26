package com.lifeos.app.ui.diary

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Icon
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
    var showDatePicker by remember { mutableStateOf(false) }

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
            CalendarAction(onClick = { showDatePicker = true })
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
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        letterSpacing = 2.2.sp
                    ),
                    color = DiaryInkViolet.copy(alpha = 0.55f)
                )
                Text(
                    dayHeaderDateLine(day),
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        letterSpacing = (-0.15).sp
                    ),
                    color = DiaryInkViolet
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

/** Reference-style calendar affordance; it opens the existing Room-backed editor. */
@Composable
private fun CalendarAction(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(DiaryLavender)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(27.dp)) {
            val stroke = 2.4.dp.toPx()
            val left = size.width * 0.16f
            val top = size.height * 0.20f
            val right = size.width * 0.84f
            val bottom = size.height * 0.84f

            drawRoundRect(
                color = DiaryInkViolet,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = CornerRadius(2.dp.toPx()),
                style = Stroke(width = stroke)
            )
            drawLine(
                color = DiaryInkViolet,
                start = Offset(left, size.height * 0.38f),
                end = Offset(right, size.height * 0.38f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = DiaryInkViolet,
                start = Offset(size.width * 0.32f, size.height * 0.08f),
                end = Offset(size.width * 0.32f, size.height * 0.28f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = DiaryInkViolet,
                start = Offset(size.width * 0.68f, size.height * 0.08f),
                end = Offset(size.width * 0.68f, size.height * 0.28f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
