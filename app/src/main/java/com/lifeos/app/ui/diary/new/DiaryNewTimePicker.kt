package com.lifeos.app.ui.diary.new

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHandwriting
import com.lifeos.app.ui.theme.DiaryInkSoft
import java.time.LocalTime

/**
 * Diary Time Picker — new visual design.
 *
 * Hour/minute picker with Cancel and Done.
 */
@Composable
fun DiaryNewTimePicker(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var hour by remember(initial) { mutableStateOf(initial.hour) }
    var minute by remember(initial) { mutableStateOf(initial.minute - initial.minute % 5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select time",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = DiaryHandwriting,
                    fontWeight = FontWeight.Bold,
                    color = DiaryNewInk
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    DateTimeUtils.formatMinutes(hour * 60 + minute),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = DiaryHandwriting,
                        fontWeight = FontWeight.Bold,
                        color = DiaryNewInk
                    )
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { hour = (hour - 1 + 24) % 24 }) {
                        Text("<", style = MaterialTheme.typography.titleLarge, color = DiaryNewSave)
                    }
                    Text(
                        hour.toString(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = DiaryHandwriting,
                            fontWeight = FontWeight.SemiBold,
                            color = DiaryNewInk
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    IconButton(onClick = { hour = (hour + 1) % 24 }) {
                        Text(">", style = MaterialTheme.typography.titleLarge, color = DiaryNewSave)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { minute = (minute - 5 + 60) % 60 }) {
                        Text("<", style = MaterialTheme.typography.titleLarge, color = DiaryNewSave)
                    }
                    Text(
                        minute.toString(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = DiaryHandwriting,
                            fontWeight = FontWeight.SemiBold,
                            color = DiaryNewInk
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    IconButton(onClick = { minute = (minute + 5) % 60 }) {
                        Text(">", style = MaterialTheme.typography.titleLarge, color = DiaryNewSave)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(hour, minute)) }) {
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
