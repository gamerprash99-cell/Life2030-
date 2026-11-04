package com.lifeos.app.ui.diary.new

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHandwriting
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import java.time.LocalDate

/**
 * Diary Date Picker — new visual design.
 *
 * Month calendar grid, selected day, Cancel and Done.
 */
@Composable
fun DiaryNewDatePicker(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var selected by remember(initial) { mutableStateOf(initial) }
    var displayMonth by remember(initial) { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select date",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = DiaryHandwriting,
                    fontWeight = FontWeight.Bold,
                    color = DiaryNewInk
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Month navigation
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { displayMonth = displayMonth.minusMonths(1) }) {
                        Icon(
                            ArrowBack,
                            contentDescription = "Previous month",
                            tint = DiaryNewSave
                        )
                    }
                    Text(
                        formatMonthYear(displayMonth),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = DiaryHandwriting,
                            fontWeight = FontWeight.SemiBold,
                            color = DiaryNewInk
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    IconButton(onClick = { displayMonth = displayMonth.plusMonths(1) }) {
                        Icon(
                            ArrowBack,
                            contentDescription = "Next month",
                            tint = DiaryNewSave
                        )
                    }
                }

                // Calendar grid
                DiaryNewCalendarGrid(
                    month = displayMonth,
                    selected = selected,
                    onDaySelected = { selected = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(
                    "Done",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DiaryNewSave
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DiaryNewInkSoft)
            }
        }
    )
}

@Composable
private fun DiaryNewCalendarGrid(
    month: LocalDate,
    selected: LocalDate,
    onDaySelected: (LocalDate) -> Unit
) {
    val firstOfMonth = month.withDayOfMonth(1)
    val lastOfMonth = month.withDayOfMonth(month.lengthOfMonth())
    val startDayOfWeek = firstOfMonth.dayOfWeek.value // 1=Monday, 7=Sunday
    val daysInMonth = lastOfMonth.dayOfMonth

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Weekday headers
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .size(32.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        day,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DiaryNewInkSoft,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        // Calendar days
        var day = 1
        while (day <= daysInMonth) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) { col ->
                    val dayOfWeek = ((firstOfMonth.dayOfWeek.value + col - 2 + 7) % 7) + 1
                    if (col < startDayOfWeek - 1 || day > daysInMonth) {
                        Box(Modifier.fillMaxWidth().size(32.dp))
                    } else {
                        val dayDate = LocalDate.of(month.year, month.month, day)
                        val isSelected = dayDate == selected
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) DiaryNewSave else Color.Transparent
                                )
                                .border(
                                    if (isSelected) 0.dp else 1.dp,
                                    DiaryNewBorder,
                                    CircleShape
                                )
                                .clickable(onClick = { onDaySelected(dayDate) }),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isSelected) Color.White else DiaryNewInk,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                        day++
                    }
                }
            }
        }
    }
}

/** Format month and year for display. */
private fun formatMonthYear(date: LocalDate): String {
    return java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")
        .withLocale(java.util.Locale.getDefault())
        .format(date)
}
