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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.time.LocalDate

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
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LifeOSSpacing.screenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                AnimatedContent(
                    targetState = date,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val offset = if (forward) 1 else -1
                        (
                            slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { width -> offset * width / 6 } +
                                fadeIn(tween(220))
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
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            HeaderIconButton(icon = Icons.Filled.CalendarMonth, description = "Create memory", onClick = onCreate)
            Spacer(Modifier.size(8.dp))
            HeaderIconButton(icon = Icons.Filled.MoreVert, description = "Diary options", onClick = { showMenu = true })
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Write a memory") }, onClick = { showMenu = false; onCreate() })
                DropdownMenuItem(text = { Text("Today") }, onClick = { showMenu = false; onSelectDay(DateTimeUtils.today().toEpochDay()) })
                DropdownMenuItem(text = { Text("Back") }, onClick = { showMenu = false; onBack() })
            }
        }

        Spacer(Modifier.height(12.dp))

        DiaryDateStrip(
            selectedDay = selectedDay,
            daysWithMemories = daysWithMemories,
            onSelectDay = onSelectDay
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(DiaryLavender.copy(alpha = 0.34f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = DiaryActionViolet, modifier = Modifier.size(22.dp))
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

@Composable
fun CreateMemoryAction(onClick: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    Text(
        text = if (compact) "Memory" else "+ Memory",
        style = MaterialTheme.typography.labelLarge,
        color = DiaryActionViolet,
        modifier = modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp)
    )
}