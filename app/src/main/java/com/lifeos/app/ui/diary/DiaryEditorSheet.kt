package com.lifeos.app.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.ui.theme.DiaryMoodCalm
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * New/Edit diary entry — the composer from the Stitch "New diary entry" screen:
 * a bottom sheet with a tinted mood picker ("How did you feel?") and a
 * borderless "What happened today?" textarea. Saved only via the explicit Save
 * button through [onSave]; the sheet itself never writes to the database.
 */
@Composable
fun DiaryEditorSheet(
    editing: DiaryEntity?,
    onDismiss: () -> Unit,
    onSave: (content: String, mood: String?) -> Unit,
    onDelete: () -> Unit
) {
    var content by rememberSaveable(editing?.id) { mutableStateOf(editing?.content ?: "") }
    var mood by rememberSaveable(editing?.id) { mutableStateOf(editing?.mood) }
    val isEditing = editing != null

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().imePadding(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Grab handle
                Box(
                    Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        .height(5.dp)
                        .fillMaxWidth(0.16f)
                )

                Text(
                    if (isEditing) "Edit diary entry" else "New diary entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("How did you feel?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiaryMoods.OPTIONS.forEach { option ->
                            DiaryMoodChip(
                                option = option,
                                selected = mood == option.key,
                                onClick = { mood = if (mood == option.key) null else option.key }
                            )
                        }
                    }
                }

                TextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    placeholder = { Text("What happened today?", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (isEditing) {
                        TextButton(onClick = onDelete) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            com.lifeos.app.core.util.DateTimeUtils.formatFullDate(
                                com.lifeos.app.core.util.DateTimeUtils.epochDayToLocalDate(editing.dateEpochDay)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))

                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = LifeOSSpacing.compactPadding),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { onSave(content, mood) },
                        enabled = content.isNotBlank(),
                        modifier = Modifier.weight(1.4f)
                    ) { Text(if (isEditing) "Save changes" else "Save entry") }
                }
            }
        }
    }
}

@Composable
private fun DiaryMoodChip(option: DiaryMood, selected: Boolean, onClick: () -> Unit) {
    val color = DiaryMoods.colorOf(option.key)
    val container = if (selected) color.copy(alpha = 0.16f) else Color.Transparent
    val borderColor = if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    val labelColor = if (selected) DiaryMoods.inkOn(option.key) else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(option.emoji, style = MaterialTheme.typography.bodyMedium)
        Text(
            option.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor
        )
    }
}