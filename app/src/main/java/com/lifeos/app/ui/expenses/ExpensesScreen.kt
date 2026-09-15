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
import androidx.compose.material3.TopAppBar
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
    val viewModel: ExpensesViewModel = viewModel(factory = LambdaViewModelFactory { ExpensesViewModel(locator.expenseRepository) })

    val expenses by viewModel.expensesThisMonth.collectAsState()
    val total by viewModel.totalThisMonth.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategories.ALL.first().name) }
    var merchant by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, contentDescription = "Add expense") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp,
                bottom = com.lifeos.app.ui.theme.LifeOSSpacing.fabContentClearance
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("This month", style = MaterialTheme.typography.labelLarge)
                        Text("₹${"%.0f".format(total)}", style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }
            if (expenses.isEmpty()) {
                item { Text("No expenses logged yet.", style = MaterialTheme.typography.bodySmall) }
            }
            items(expenses, key = { it.id }) { expense ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${ExpenseCategories.emojiFor(expense.category)}  ${expense.merchant ?: expense.category}", style = MaterialTheme.typography.bodyLarge)
                            Text(expense.category, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("₹${"%.0f".format(expense.amount)}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it },
                        placeholder = { Text("Amount") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(value = merchant, onValueChange = { merchant = it }, placeholder = { Text("Merchant (optional)") })
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    LazyRow {
                        items(ExpenseCategories.ALL) { cat ->
                            GlassChip(
                                modifier = Modifier.padding(end = 6.dp).clickable { selectedCategory = cat.name }
                            ) {
                                Text("${cat.emoji} ${cat.name}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Text("Selected: $selectedCategory", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    viewModel.addExpense(amount, selectedCategory, merchant.ifBlank { null })
                    amountText = ""; merchant = ""; showAddDialog = false
                }) { Text("Save") }
            },
            dismissButton = { Button(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}
