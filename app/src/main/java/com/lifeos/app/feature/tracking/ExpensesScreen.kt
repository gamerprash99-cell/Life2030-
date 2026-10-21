package com.lifeos.app.feature.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.database.entity.ExpenseEntity
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.formatMoney
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.EmptyState
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import com.lifeos.app.ui.components.Pill
import com.lifeos.app.ui.components.SectionHeader

/**
 * Expenses: integer-minor-unit amounts, this-month total, category breakdown and
 * the full history. Formatting happens only at the display edge.
 */
@Composable
fun ExpensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val state by viewModel.expensesState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(
            title = "Expenses",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add expense")
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                LifeOsCard(containerColor = LifeOs.tokens.softPinkSurface) {
                    Column {
                        Text(
                            text = "This month",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = state.totalDisplay(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${state.currencyCode} · amounts stored exactly, in minor units",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.categoryTotals.isNotEmpty()) {
                item {
                    SectionHeader(title = "By category", subtitle = "This month")
                }
                items(state.categoryTotals) { (category, total) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = formatMoney(total, state.currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "All expenses",
                    subtitle = if (state.expenses.isEmpty()) "Nothing recorded yet" else
                        "${state.expenses.size} recorded",
                )
            }

            if (state.expenses.isEmpty()) {
                item {
                    EmptyState(
                        title = "No expenses yet",
                        subtitle = "Add an amount and category. Nothing is estimated or " +
                            "auto-filled for you.",
                        actionLabel = "Add expense",
                        onAction = { showAdd = true },
                    )
                }
            } else {
                items(state.expenses, key = { it.id }) { expense ->
                    ExpenseRow(
                        expense = expense,
                        onDelete = { viewModel.trashExpense(expense.id) },
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddExpenseDialog(
            currencyCode = state.currencyCode,
            categories = viewModel.knownCategories(),
            onDismiss = { showAdd = false },
            onConfirm = { amount, category, note ->
                viewModel.addExpense(amount, category, note, "", Dates.today())
                showAdd = false
            },
        )
    }
}

@Composable
private fun ExpenseRow(expense: ExpenseEntity, onDelete: () -> Unit) {
    LifeOsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = formatMoney(expense.amountMinor, expense.currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = buildString {
                        append(expense.category)
                        append(" · ")
                        append(Dates.relative(expense.expenseDate))
                        if (expense.note.isNotBlank()) {
                            append(" · ")
                            append(expense.note)
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AddExpenseDialog(
    currencyCode: String,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.firstOrNull() ?: "Other") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Add expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        amount = input.filter { it.isDigit() || it == '.' }.take(12)
                    },
                    label = { Text("Amount ($currencyCode)") },
                    supportingText = { Text("E.g. 249.50 — stored as 24950 minor units.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = trackingFieldColors(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = trackingFieldColors(),
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(categories) { option ->
                        Pill(
                            text = option,
                            selected = category == option,
                            onClick = { category = option },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount, category, note) },
                enabled = amount.filter { it.isDigit() }.isNotEmpty(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
