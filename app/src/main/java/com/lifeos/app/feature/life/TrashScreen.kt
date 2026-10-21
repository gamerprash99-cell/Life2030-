package com.lifeos.app.feature.life

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.data.TrashContent
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.SearchGroup
import com.lifeos.app.core.domain.formatMoney
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.ConfirmDialog
import com.lifeos.app.ui.components.EmptyState
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import com.lifeos.app.ui.components.SectionHeader

/**
 * Trash: soft-deleted rows across all six record types.
 *
 * Two outcomes only — Restore, or Delete forever (behind a confirmation and
 * described exactly, since it cannot be undone).
 */
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LifeViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val trash by viewModel.trashState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Pair<SearchGroup, String>?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(title = "Trash", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (trash.totalCount == 0) {
                item {
                    EmptyState(
                        title = "Trash is empty",
                        subtitle = "Items you delete wait here so you can restore them. " +
                            "Deleting them permanently is a separate, deliberate step.",
                    )
                }
            } else {
                trashSection(SearchGroup.NOTES, "Notes", trash.notes.size)
                items(trash.notes, key = { it.id }) { note ->
                    TrashCard(
                        title = note.title.ifBlank { "Untitled note" },
                        subtitle = Dates.relative(Dates.iso(Dates.toLocalDate(note.deletedAt))),
                        onRestore = { viewModel.restore(SearchGroup.NOTES, note.id) },
                        onDelete = { pendingDelete = SearchGroup.NOTES to note.id },
                    )
                }

                trashSection(SearchGroup.TASKS, "Tasks", trash.tasks.size)
                items(trash.tasks, key = { it.id }) { task ->
                    TrashCard(
                        title = task.title,
                        subtitle = Dates.relative(Dates.iso(Dates.toLocalDate(task.deletedAt))),
                        onRestore = { viewModel.restore(SearchGroup.TASKS, task.id) },
                        onDelete = { pendingDelete = SearchGroup.TASKS to task.id },
                    )
                }

                trashSection(SearchGroup.HABITS, "Habits", trash.habits.size)
                items(trash.habits, key = { it.id }) { habit ->
                    TrashCard(
                        title = habit.name,
                        subtitle = Dates.relative(Dates.iso(Dates.toLocalDate(habit.deletedAt))),
                        onRestore = { viewModel.restore(SearchGroup.HABITS, habit.id) },
                        onDelete = { pendingDelete = SearchGroup.HABITS to habit.id },
                    )
                }

                trashSection(SearchGroup.EXPENSES, "Expenses", trash.expenses.size)
                items(trash.expenses, key = { it.id }) { expense ->
                    TrashCard(
                        title = formatMoney(expense.amountMinor, expense.currencyCode),
                        subtitle = "${expense.category} · " +
                            Dates.relative(Dates.iso(Dates.toLocalDate(expense.deletedAt))),
                        onRestore = { viewModel.restore(SearchGroup.EXPENSES, expense.id) },
                        onDelete = { pendingDelete = SearchGroup.EXPENSES to expense.id },
                    )
                }

                trashSection(SearchGroup.DIARY, "Diary", trash.diary.size)
                items(trash.diary, key = { it.id }) { entry ->
                    TrashCard(
                        title = entry.title.ifBlank { entry.body.take(40) },
                        subtitle = "Entry for ${Dates.human(Dates.parse(entry.diaryDate))}",
                        onRestore = { viewModel.restore(SearchGroup.DIARY, entry.id) },
                        onDelete = { pendingDelete = SearchGroup.DIARY to entry.id },
                    )
                }

                trashSection(SearchGroup.CAPTURES, "Captures", trash.captures.size)
                items(trash.captures, key = { it.id }) { capture ->
                    TrashCard(
                        title = capture.text.ifBlank {
                            com.lifeos.app.core.domain.CaptureType.fromCode(capture.type).label
                        },
                        subtitle = Dates.relative(capture.occurredOn),
                        onRestore = { viewModel.restore(SearchGroup.CAPTURES, capture.id) },
                        onDelete = { pendingDelete = SearchGroup.CAPTURES to capture.id },
                    )
                }
            }
        }
    }

    pendingDelete?.let { (group, id) ->
        ConfirmDialog(
            title = "Delete permanently?",
            message = "This ${group.label.lowercase().dropLast(1)} is removed from the database " +
                "and cannot be restored. Any attached files are deleted too.",
            confirmLabel = "Delete forever",
            onConfirm = {
                viewModel.deleteForever(group, id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.trashSection(
    group: SearchGroup,
    title: String,
    count: Int,
) {
    if (count == 0) return
    item(key = "header-${group.code}") {
        SectionHeader(title = title, subtitle = "$count deleted")
    }
}

@Composable
private fun TrashCard(
    title: String,
    subtitle: String,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    LifeOsCard {
        Column {
            Text(
                text = title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.padding(top = Spacing.xs))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onRestore) { Text("Restore") }
                TextButton(onClick = onDelete) {
                    Text("Delete forever", color = LifeOs.tokens.error)
                }
            }
        }
    }
}

// Kept for exhaustiveness documentation; TrashContent.groupFor covers all groups.
private val ALL_TRASH_GROUPS = SearchGroup.entries.toList()
