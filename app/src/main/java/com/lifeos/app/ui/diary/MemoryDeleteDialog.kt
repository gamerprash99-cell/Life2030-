package com.lifeos.app.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.util.DateTimeUtils

/**
 * One confirmation for deleting a memory, shared by the timeline and the detail
 * screen so the two paths can never drift apart. Restyled to the Diary's voice
 * and it names the day and time being removed, because "this entry" is not
 * enough to confirm an irreversible action against a list of lookalikes.
 */
@Composable
fun MemoryDeleteDialog(
    dayEpochDay: Long,
    timeMinutes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val whenLabel = DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(dayEpochDay))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this memory?", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "$whenLabel at ${DateTimeUtils.formatMinutes(timeMinutes)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "It will be removed from this device and cannot be recovered.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Text("Keep", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
