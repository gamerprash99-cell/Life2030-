package com.lifeos.app.ui.diary.new

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHandwriting
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet

/**
 * Diary Delete Dialog — new visual design.
 *
 * Confirm deletion of an entry with date/time context.
 */
@Composable
fun DiaryNewDeleteDialog(
    dayEpochDay: Long,
    timeMinutes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Delete entry?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = DiaryHandwriting,
                    fontWeight = FontWeight.Bold,
                    color = DiaryNewInk
                )
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${DateTimeUtils.formatFullDate(java.time.LocalDate.ofEpochDay(dayEpochDay))} at ${DateTimeUtils.formatMinutes(timeMinutes)}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "This entry and its attachments will be removed from this device.",
                    style = MaterialTheme.typography.bodySmall.copy(color = DiaryNewInkMuted)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(
                    "Delete",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
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

/** Uses shared DiaryNewInkMuted from DiaryDesign. */
