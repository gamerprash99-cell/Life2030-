package com.lifeos.app.feature.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.Priority
import com.lifeos.app.core.domain.Recurrence
import com.lifeos.app.core.domain.RecurrenceRules
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.ConfirmDialog
import com.lifeos.app.ui.components.EmptyState
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import com.lifeos.app.ui.components.Pill
import com.lifeos.app.ui.components.SectionHeader

/** Tasks: open list with priority/due ordering, optional completed view, add dialog. */
@Composable
fun TasksScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val state by viewModel.tasksState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(
            title = "Tasks",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add task")
                }
            },
        )

        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Pill(
                text = "Open (${state.open.size})",
                selected = !state.showDone,
                onClick = { if (state.showDone) viewModel.toggleShowDone() },
            )
            Pill(
                text = "Done (${state.done.size})",
                selected = state.showDone,
                onClick = { if (!state.showDone) viewModel.toggleShowDone() },
            )
        }

        if (state.visible.isEmpty()) {
            EmptyState(
                title = if (state.showDone) "Nothing completed yet" else "No open tasks",
                subtitle = if (state.showDone) {
                    "Completed tasks stay here so you can see what you finished."
                } else {
                    "Add the first task — due dates and repeats are optional."
                },
                modifier = Modifier.fillMaxSize(),
                actionLabel = if (state.showDone) null else "Add task",
                onAction = if (state.showDone) null else ({ showAdd = true }),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.visible, key = { it.id }) { task ->
                    val overdue = !state.showDone &&
                        task.dueDate != null && task.dueDate < Dates.iso(Dates.today())
                    LifeOsCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = task.status == "done",
                                onCheckedChange = { checked -> viewModel.setTaskDone(task.id, checked) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = LifeOs.tokens.border,
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (task.status == "done") {
                                        TextDecoration.LineThrough
                                    } else {
                                        TextDecoration.None
                                    },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val meta = buildString {
                                    task.dueDate?.let {
                                        append(Dates.relative(it))
                                        append(" · ")
                                    }
                                    val priority = Priority.fromCode(task.priority)
                                    if (priority != Priority.NONE) {
                                        append(priority.label)
                                        append(" · ")
                                    }
                                    val recurrence = Recurrence.fromId(task.recurrence)
                                    if (recurrence != Recurrence.NONE) {
                                        append(recurrence.label)
                                    }
                                }.trim().trimEnd('·').trim()
                                if (meta.isNotEmpty()) {
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (overdue) {
                                            LifeOs.tokens.warning
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                            IconButton(onClick = { deleteTarget = task.id }) {
                                Text(
                                    text = "✕",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddTaskDialog(
            onDismiss = { showAdd = false },
            onConfirm = { title, due, priority, recurrence, parameter ->
                viewModel.addTask(title, due, priority, recurrence, parameter)
                showAdd = false
            },
        )
    }

    deleteTarget?.let { id ->
        ConfirmDialog(
            title = "Move task to Trash?",
            message = "You can restore it from Life → Trash until you delete it permanently.",
            confirmLabel = "Move to Trash",
            onConfirm = {
                viewModel.trashTask(id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Priority, Recurrence, String?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.NONE) }
    var recurrence by remember { mutableStateOf(Recurrence.NONE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What needs doing?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = trackingFieldColors(),
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due date (yyyy-MM-dd, optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = trackingFieldColors(),
                )
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(Priority.entries.toList()) { option ->
                        Pill(
                            text = option.label,
                            selected = priority == option,
                            onClick = { priority = option },
                        )
                    }
                }
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(
                        listOf(
                            Recurrence.NONE,
                            Recurrence.DAILY,
                            Recurrence.WEEKLY,
                            Recurrence.MONTHLY,
                            Recurrence.YEARLY,
                        ),
                    ) { option ->
                        Pill(
                            text = option.label,
                            selected = recurrence == option,
                            onClick = { recurrence = option },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = Dates.parseOrNull(dueDate)
                    val iso = parsed?.let { Dates.iso(it) }
                    onConfirm(title, iso, priority, recurrence, null)
                },
                enabled = title.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
