package com.lifeos.app.ui.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.domain.model.ExpenseCategories
import com.lifeos.app.ui.components.GlassCard
import com.lifeos.app.ui.components.GlassChip
import com.lifeos.app.ui.components.LifeOSTopBar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpensesViewModel(private val expenseRepository: ExpenseRepository) : ViewModel() {
    private val monthStart = DateTimeUtils.startOfMonthEpochDay()
    private val monthEnd = DateTimeUtils.endOfMonthEpochDay()

    val expensesThisMonth: StateFlow<List<ExpenseEntity>> = expenseRepository.observeInRange(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalThisMonth: StateFlow<Double> = expenseRepository.observeTotalInRange(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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
        factory = LambdaViewModelFactory { ExpensesViewModel(locator.expenseRepository) }
    )

    val expenses by viewModel.expensesThisMonth.collectAsState()
    val total by viewModel.totalThisMonth.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    val dailyAverage = if (expenses.isEmpty()) 0.0 else total / java.time.LocalDate.now().lengthOfMonth()
    val budget = 15_000.0

    Scaffold(
        topBar = { LifeOSTopBar("Expenses", "See where your day is going", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Filled.Add, contentDescription = "Add expense") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 10.dp,
                bottom = com.lifeos.app.ui.theme.LifeOSSpacing.fabContentClearance
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("This Month", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${"%,.2f".format(total)}", style = MaterialTheme.typography.headlineLarge)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ExpenseStat("Daily avg", "₹${"%.0f".format(dailyAverage)}")
                            ExpenseStat("Budget", "₹${"%,.0f".format(budget)}")
                            ExpenseStat("Left", "₹${"%,.0f".format((budget - total).coerceAtLeast(0.0))}")
                        }
                    }
                }
            }

            item {
                Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 4.dp))
            }

            if (expenses.isEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 18.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Text("No expenses logged yet.", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tap + to log your coffee, grocery run, or bill payment.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(expenses, key = { it.id }) { expense ->
                GlassCard(Modifier.fillMaxWidth()) {
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

@Composable
private fun ExpenseStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
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

    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
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
                    GlassChip(
                        modifier = Modifier.clickable { selectedCategory = cat.name }
                    ) {
                        Text("${cat.emoji} ${cat.name}", style = MaterialTheme.typography.labelSmall)
                    }
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
