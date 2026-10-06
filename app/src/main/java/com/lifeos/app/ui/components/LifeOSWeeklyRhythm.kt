package com.lifeos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.domain.usecase.DayCheck
import com.lifeos.app.ui.theme.LifeOSPrimary
import java.time.LocalDate

/**
 * The seven-day rhythm strip. Shared by Home and Habits & Routines so both
 * screens render the same [DayCheck] rule from the same composable, rather than
 * each keeping a copy that can drift apart.
 *
 * [dotSize] scales the whole cell with it, so a narrow screen gets a
 * proportionally smaller dot instead of a clipped one, and the weekday letter
 * stays a fixed sibling of the dot rather than a background behind it.
 */
@Composable
fun LifeOSWeeklyRhythm(
    days: List<DayCheck>,
    modifier: Modifier = Modifier,
    dotSize: Dp = 30.dp
) {
    if (days.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Weekly rhythm: ${days.count { it.isDone }} of 7 days complete"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // The row states the week once for assistive tech; repeating it
                // seven times as "M, M, M…" is noise, not information.
                modifier = Modifier.clearAndSetSemantics { }
            ) {
                Text(
                    weekdayLetter(day.epochDay),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(
                            when {
                                day.isDone -> LifeOSPrimary
                                day.isToday -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        day.isDone -> Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(dotSize * 0.5f)
                        )
                        day.isToday -> Box(
                            Modifier
                                .size(dotSize * 0.2f)
                                .clip(CircleShape)
                                .background(LifeOSPrimary)
                        )
                        else -> Box(
                            Modifier
                                .size(dotSize * 0.17f)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
            }
        }
    }
}

/** "M", "T", … for the calendar day behind [epochDay]. */
fun weekdayLetter(epochDay: Long): String =
    "MTWTFSS"[LocalDate.ofEpochDay(epochDay).dayOfWeek.value - 1].toString()
