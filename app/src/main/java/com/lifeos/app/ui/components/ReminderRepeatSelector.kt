package com.lifeos.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lifeos.app.data.db.entities.ReminderRepeatType

/**
 * Three-way cadence chooser for a reminder (once / daily / Mon–Fri). Shared by
 * the new-task dialog, the new-habit dialog, and the habit detail screen so the
 * semantics and labels are identical everywhere.
 */
@Composable
fun ReminderRepeatSelector(
    selected: ReminderRepeatType,
    onSelect: (ReminderRepeatType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            ReminderRepeatType.ONCE to "Once",
            ReminderRepeatType.DAILY to "Daily",
            ReminderRepeatType.WEEKDAYS to "Mon–Fri"
        ).forEach { (type, label) ->
            val isSelected = type == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(type) },
                label = { Text(label) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.primary,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    selectedBorderColor = Color.Transparent,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 0.dp
                ),
                modifier = Modifier.weight(1f).padding(vertical = 2.dp)
            )
        }
    }
}