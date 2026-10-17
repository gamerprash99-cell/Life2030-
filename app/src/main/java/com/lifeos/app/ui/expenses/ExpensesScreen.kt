package com.lifeos.app.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.SettingsStore
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.domain.model.BudgetProgress
import com.lifeos.app.domain.model.ExpenseCategories
import com.lifeos.app.domain.model.isAutomaticallyCaptured
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSEmptyState
import com.lifeos.app.ui.components.LifeOSFilterPill
import com.lifeos.app.ui.components.LifeOSPageHeader
import com.lifeos.app.ui.components.LifeOSProgressTrack
import com.lifeos.app.ui.components.LifeOSSectionHeader
import com.lifeos.app.ui.components.lifeOSIsDark
import com.lifeos.app.ui.theme.lifeOSCanvasBrush
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpensesViewModel(
    private val expenseRepository: ExpenseRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {
    private val monthStart = DateTimeUtils.startOfMonthEpochDay()
    private val monthEnd = DateTimeUtils.endOfMonthEpochDay()

    val expensesThisMonth: StateFlow<List<ExpenseEntity>> = expenseRepository.observeInRange(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalThisMonth: StateFlow<Double> = expenseRepository.observeTotalInRange(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * The monthly budget, or null when none is set. Read from the same settings
     * store as every other preference rather than a screen-local constant, so the
     * budget survives the screen and is not retyped on each visit.
     */
    val monthlyBudget: StateFlow<Double?> = settingsStore.monthlyBudget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Setting or clearing the budget; null removes it and restores the empty state. */
    fun setMonthlyBudget(amount: Double?) {
        viewModelScope.launch { settingsStore.setMonthlyBudget(amount) }
    }

    fun addExpense(amount: Double, category: String, merchant: String?) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val now = DateTimeUtils.today()
            val minutes = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
            expenseRepository.addExpense(
                amount = amount, category = category, dateEpochDay = now.toEpochDay(),
                timeMinutes = minutes, merchant = merchant
            )
        }
    }
}

@Composable
fun ExpensesScreen(onBack: () -> Unit = {}) {
    val locator = LocalServiceLocator.current
    val viewModel: ExpensesViewModel = viewModel(
        factory = LambdaViewModelFactory { ExpensesViewModel(locator.expenseRepository, locator.settingsStore) }
    )

    val expenses by viewModel.expensesThisMonth.collectAsState()
    val total by viewModel.totalThisMonth.collectAsState()
    val budget by viewModel.monthlyBudget.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var showBudgetSheet by remember { mutableStateOf(false) }

    val dailyAverage = if (expenses.isEmpty()) 0.0 else total / java.time.LocalDate.now().lengthOfMonth()
    val progress = BudgetProgress.forMonth(budget = budget, spent = total)

    val dark = lifeOSIsDark()
    Column(
        Modifier
            .fillMaxSize()
            .background(lifeOSCanvasBrush(dark))
    ) {
        LifeOSPageHeader(
            eyebrow = "This month",
            title = "Expenses",
            onBack = onBack,
            actionIcon = Icons.Filled.Add,
            actionDescription = "Add expense",
            onAction = { showAddSheet = true }
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 24.dp, end = 24.dp, top = 12.dp,
                bottom = com.lifeos.app.ui.theme.LifeOSSpacing.fabContentClearance
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                if (progress == null) {
                    FirstBudgetCard(onSetBudget = { showBudgetSheet = true })
                } else {
                    BudgetCard(progress = progress, dailyAverage = dailyAverage, onEdit = { showBudgetSheet = true })
                }
            }

            item {
                LifeOSSectionHeader("Recent transactions")
            }

            if (expenses.isEmpty()) {
                item {
                    LifeOSEmptyState(
                        icon = Icons.Filled.Payments,
                        title = "No expenses yet",
                        body = "Tap ＋ to log your coffee, a grocery run, or a bill payment.",
                        actionText = "Add expense",
                        onAction = { showAddSheet = true }
                    )
                }
            }

            items(expenses, key = { it.id }) { expense ->
                LifeOSCard(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                ExpenseCategories.emojiFor(expense.category),
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                expense.merchant ?: expense.category,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (expense.isAutomaticallyCaptured()) {
                                AutoDetectedLabel()
                            }
                            Text(
                                expense.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("₹${"%.0f".format(expense.amount)}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (showBudgetSheet) {
        SetBudgetSheet(
            current = budget,
            onDismiss = { showBudgetSheet = false },
            onSave = { amount ->
                viewModel.setMonthlyBudget(amount)
                showBudgetSheet = false
            }
        )
    }

    if (showAddSheet) {
        AddExpenseSheet(
            onDismiss = { showAddSheet = false },
            onSave = { amount, category, merchant ->
                viewModel.addExpense(amount, category, merchant)
                showAddSheet = false
            }
        )
    }
}

/**
 * Shown until a budget is set. A bar drawn against a number the user never chose
 * is the worst option here, so the first run gets a single honest action instead
 * of a progress indicator that is always full.
 */
@Composable
private fun FirstBudgetCard(onSetBudget: () -> Unit) {
    LifeOSCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("This Month", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("₹${"%,.2f".format(0.0)}", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Set a monthly budget to see what is left as the month goes on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            com.lifeos.app.ui.components.LifeOSPrimaryButton(
                text = "Set monthly budget",
                onClick = onSetBudget,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Budget progress for a budget the user set. The bar is clamped by
 * [BudgetProgress] and coloured with the primary colour, switching to the theme
 * error colour only once the spending is genuinely over — so the colour carries
 * meaning rather than decoration.
 */
@Composable
private fun BudgetCard(progress: BudgetProgress, dailyAverage: Double, onEdit: () -> Unit) {
    val over = progress.isOverBudget
    val barColor = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val percent = BudgetProgress.percentOfBudget(progress) ?: 0
    val status = if (over) {
        "₹${"%,.0f".format(progress.overage)} over budget"
    } else {
        "₹${"%,.0f".format(progress.remaining)} left"
    }

    LifeOSCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("This Month", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("₹${"%,.2f".format(progress.spent)}", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "$percent%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "$percent% of monthly budget used, $status" }
            ) {
                LifeOSProgressTrack(
                    progress = progress.fraction,
                    height = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ExpenseStat("Daily avg", "₹${"%.0f".format(dailyAverage)}")
                ExpenseStat("Budget", "₹${"%,.0f".format(progress.budget)}")
                ExpenseStat(
                    label = if (over) "Over" else "Left",
                    value = if (over) "₹${"%,.0f".format(progress.overage)}" else "₹${"%,.0f".format(progress.remaining)}",
                    valueColor = if (over) MaterialTheme.colorScheme.error else null
                )
            }
            androidx.compose.material3.TextButton(onClick = onEdit) { Text("Edit budget") }
        }
    }
}

/** Marks an expense LifeOS filed from a payment notification. */
@Composable
private fun AutoDetectedLabel() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics { contentDescription = "Automatically detected" }
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(13.dp)
        )
        Text(
            "Automatically detected",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Enter or change the monthly budget. Removing it is a first-class action, not a
 * side effect of clearing the field: "no budget" is a state the user chooses, and
 * it returns the screen to its first-run card.
 */
@Composable
private fun SetBudgetSheet(current: Double?, onDismiss: () -> Unit, onSave: (Double?) -> Unit) {
    var amountText by remember { mutableStateOf(current?.let { "%.0f".format(it) } ?: "") }
    val parsed = amountText.toDoubleOrNull()

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Monthly budget", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Applies to the current calendar month. LifeOS shows what is left as you spend.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Amount (INR)") },
                leadingIcon = { Text("₹", style = MaterialTheme.typography.titleMedium) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { onSave(parsed) },
                    enabled = parsed != null && parsed > 0,
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
            if (current != null) {
                androidx.compose.material3.TextButton(
                    onClick = { onSave(null) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Remove budget") }
            }
        }
    }
}

@Composable
private fun ExpenseStat(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color? = null) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor ?: MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AddExpenseSheet(
    onDismiss: () -> Unit,
    onSave: (Double, String, String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategories.ALL.first().name) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add expense", style = MaterialTheme.typography.headlineSmall)
            Text("Track your instant spending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Amount (INR)") },
                leadingIcon = { Text("₹", style = MaterialTheme.typography.titleMedium) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Merchant or note (optional)") },
                singleLine = true
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExpenseCategories.ALL) { cat ->
                    LifeOSFilterPill(
                        label = "${cat.emoji} ${cat.name}",
                        selected = selectedCategory == cat.name,
                        onClick = { selectedCategory = cat.name }
                    )
                }
            }
            Text("Selected: $selectedCategory", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) onSave(amount, selectedCategory, merchant.ifBlank { null })
                    },
                    enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true,
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }
}
