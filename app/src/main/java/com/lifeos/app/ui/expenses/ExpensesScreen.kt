package com.lifeos.app.ui.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.lifeos.app.ui.components.GlassCard
import com.lifeos.app.ui.components.GlassChip
import com.lifeos.app.ui.components.HeaderActionCircle
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSEmptyState
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.LifeOSSectionHeader
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.theme.FigmaAccent
import com.lifeos.app.ui.theme.FigmaAccentStroke
import com.lifeos.app.ui.theme.FigmaCard
import com.lifeos.app.ui.theme.FigmaExpenseBlue
import com.lifeos.app.ui.theme.FigmaExpensePlum
import com.lifeos.app.ui.theme.FigmaExpenseSteel
import com.lifeos.app.ui.theme.FigmaHeatmap
import com.lifeos.app.ui.theme.FigmaStroke
import com.lifeos.app.ui.theme.FigmaText
import com.lifeos.app.ui.theme.FigmaTertiary
import com.lifeos.app.ui.theme.FigmaTrack
import com.lifeos.app.ui.theme.FigmaWash
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

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
    val monthTotal by viewModel.totalThisMonth.collectAsState()
    val budget by viewModel.monthlyBudget.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var showBudgetSheet by remember { mutableStateOf(false) }
    var period by rememberSaveable { mutableStateOf(ExpensePeriod.MONTH) }

    val today = remember { LocalDate.now() }
    val todayEpoch = today.toEpochDay()
    val weekStart = todayEpoch - 6

    // The month query already returns every expense in the month, so Today and
    // Week are exact client-side slices of it. A period filter therefore never
    // costs another Room subscription, and the tag's number is the same list the
    // transactions below it are drawn from.
    val periodExpenses = remember(expenses, period, todayEpoch, weekStart) {
        when (period) {
            ExpensePeriod.MONTH -> expenses
            ExpensePeriod.WEEK -> expenses.filter { it.dateEpochDay in weekStart..todayEpoch }
            ExpensePeriod.TODAY -> expenses.filter { it.dateEpochDay == todayEpoch }
        }
    }
    val periodTotal = remember(periodExpenses) { periodExpenses.sumOf { it.amount } }
    val todayTotal = remember(expenses, todayEpoch) { expenses.filter { it.dateEpochDay == todayEpoch }.sumOf { it.amount } }
    val weekTotal = remember(expenses, weekStart, todayEpoch) { expenses.filter { it.dateEpochDay in weekStart..todayEpoch }.sumOf { it.amount } }
    val dailyAverage = if (expenses.isEmpty()) 0.0 else monthTotal / today.lengthOfMonth()
    val progress = BudgetProgress.forMonth(budget = budget, spent = monthTotal)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            LifeOSTopBar(
                title = "Room for what matters",
                subtitle = "Expenses / ${today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))}",
                onBack = onBack,
                actions = { HeaderActionCircle(Icons.Filled.Add, "Add expense", onClick = { showAddSheet = true }) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = LifeOSSpacing.screenPadding, end = LifeOSSpacing.screenPadding,
                top = 0.dp, bottom = LifeOSSpacing.screenPadding
            ),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            item { ExpensePeriodRow(selected = period, onSelect = { period = it }) }
            item {
                SpendingHeroCard(
                    total = periodTotal,
                    period = period,
                    todayTotal = todayTotal,
                    weekTotal = weekTotal,
                    dailyAverage = dailyAverage
                )
            }
            item {
                if (progress == null) {
                    FirstBudgetCard(onSetBudget = { showBudgetSheet = true })
                } else {
                    BudgetCard(progress = progress, dailyAverage = dailyAverage, onEdit = { showBudgetSheet = true })
                }
            }
            if (periodExpenses.isNotEmpty()) {
                item { LifeOSSectionHeader("Where it went", "All categories") }
                item { CategoryBreakdownCard(slices = categorySlices(periodExpenses), total = periodTotal) }
            }
            item {
                LifeOSSectionHeader(
                    title = when (period) {
                        ExpensePeriod.TODAY -> "Today's transactions"
                        ExpensePeriod.WEEK -> "This week's transactions"
                        ExpensePeriod.MONTH -> "This month's transactions"
                    },
                    action = "₹${"%,.0f".format(periodTotal)} total"
                )
            }
            if (periodExpenses.isEmpty()) {
                item {
                    LifeOSEmptyState(
                        icon = Icons.Filled.Payments,
                        title = "No expenses yet",
                        body = "Tap Add expense to log your coffee, a grocery run, or a bill payment.",
                        actionLabel = "Add expense",
                        onAction = { showAddSheet = true }
                    )
                }
            } else {
                items(
                    periodExpenses.sortedWith(compareByDescending<ExpenseEntity> { it.dateEpochDay }.thenByDescending { it.timeMinutes }),
                    key = { it.id }
                ) { expense ->
                    TransactionRow(expense)
                }
            }
            item {
                LifeOSPrimaryButton(
                    text = "Add expense",
                    icon = Icons.Filled.Add,
                    onClick = { showAddSheet = true },
                    modifier = Modifier.fillMaxWidth()
                )
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

/** The Figma period tags. Each is a real slice of the month list, never decorative. */
private enum class ExpensePeriod(val label: String) { MONTH("This month"), WEEK("Week"), TODAY("Today") }

@Composable
private fun ExpensePeriodRow(selected: ExpensePeriod, onSelect: (ExpensePeriod) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpensePeriod.entries.forEach { period ->
            val isSelected = period == selected
            val shape = RoundedCornerShape(LifeOSRadius.pill)
            Box(
                Modifier
                    .clip(shape)
                    .background(if (isSelected) FigmaAccent.copy(alpha = 0.16f) else FigmaWash)
                    .border(BorderStroke(1.dp, if (isSelected) FigmaAccentStroke else FigmaStroke), shape)
                    .clickable(role = Role.Button) { onSelect(period) }
                    .semantics { contentDescription = period.label; role = Role.Button }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    period.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) FigmaAccent else FigmaTertiary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * SPENT SO FAR — the Figma hero: a muted kicker, the period total at 43sp, and a
 * three-up snapshot (Today · This week · Daily average). Every figure is summed
 * from the persisted month list, so the card cannot invent a rupee.
 */
@Composable
private fun SpendingHeroCard(total: Double, period: ExpensePeriod, todayTotal: Double, weekTotal: Double, dailyAverage: Double) {
    LifeOSCard(Modifier.fillMaxWidth(), tint = FigmaCard) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "SPENT SO FAR · ${period.label.uppercase()}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
                color = FigmaTertiary
            )
            Text(
                "₹${"%,.0f".format(total)}",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 43.sp, fontWeight = FontWeight.Medium),
                color = FigmaText
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SnapshotStat("Today", "₹${"%,.0f".format(todayTotal)}", Modifier.weight(1f))
                SnapshotStat("This week", "₹${"%,.0f".format(weekTotal)}", Modifier.weight(1f))
                SnapshotStat("Daily average", "₹${"%,.0f".format(dailyAverage)}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SnapshotStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = FigmaTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = FigmaAccent, maxLines = 1)
    }
}

/** One grouped category: its name, summed total and share of the period. */
private data class CategorySlice(val name: String, val emoji: String, val total: Double, val color: Color)

/** Groups the period's transactions by category, largest first. Pure arithmetic. */
private fun categorySlices(expenses: List<ExpenseEntity>): List<CategorySlice> {
    val palette = listOf(FigmaAccent, FigmaHeatmap, FigmaExpenseBlue, FigmaExpensePlum, FigmaExpenseSteel)
    return expenses.groupBy { it.category }
        .map { (category, list) -> category to list.sumOf { it.amount } }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .take(5)
        .mapIndexed { index, (category, sum) ->
            CategorySlice(
                name = category,
                emoji = ExpenseCategories.emojiFor(category),
                total = sum,
                color = palette[index % palette.size]
            )
        }
}

@Composable
private fun CategoryBreakdownCard(slices: List<CategorySlice>, total: Double) {
    LifeOSCard(Modifier.fillMaxWidth(), tint = FigmaCard) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            slices.forEach { slice ->
                val share = if (total > 0) (slice.total / total).toFloat() else 0f
                val percent = (share * 100).roundToInt()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${slice.emoji}  ${slice.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FigmaText,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            "₹${"%,.0f".format(slice.total)} · $percent%",
                            style = MaterialTheme.typography.bodySmall,
                            color = FigmaTertiary,
                            maxLines = 1
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(FigmaTrack)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(share.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(slice.color)
                        )
                    }
                }
            }
        }
    }
}

/** One transaction: category emoji, merchant/note, time · category · method, amount. */
@Composable
private fun TransactionRow(expense: ExpenseEntity) {
    LifeOSCard(Modifier.fillMaxWidth(), tint = FigmaCard) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FigmaTrack),
                contentAlignment = Alignment.Center
            ) {
                Text(ExpenseCategories.emojiFor(expense.category), style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(expense.merchant ?: expense.category, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = FigmaText, maxLines = 1)
                Text(
                    buildString {
                        append(DateTimeUtils.formatMinutes(expense.timeMinutes))
                        append(" · ")
                        append(expense.category)
                        append(" · ")
                        append(methodLabel(expense))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FigmaTertiary,
                    maxLines = 1
                )
                if (expense.isAutomaticallyCaptured()) AutoDetectedLabel()
            }
            Text(
                "₹${"%,.0f".format(expense.amount)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = FigmaText
            )
        }
    }
}

/** "UPI" stays uppercase; longer method names read as a word ("Card", "Other"). */
private fun methodLabel(expense: ExpenseEntity): String {
    val name = expense.paymentMethod.name
    return if (name.length <= 3) name.uppercase() else name.lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * Shown until a budget is set. A bar drawn against a number the user never chose
 * is the worst option here, so the first run gets a single honest action instead
 * of a progress indicator that is always full.
 */
@Composable
private fun FirstBudgetCard(onSetBudget: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("This Month", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("₹${"%,.2f".format(0.0)}", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Set a monthly budget to see what is left as the month goes on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onSetBudget,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("Set monthly budget") }
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

    GlassCard(Modifier.fillMaxWidth()) {
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
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .height(10.dp)
                    .semantics { contentDescription = "$percent% of monthly budget used, $status" },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                strokeCap = StrokeCap.Round
            )
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
                    GlassChip(
                        selected = selectedCategory == cat.name,
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
