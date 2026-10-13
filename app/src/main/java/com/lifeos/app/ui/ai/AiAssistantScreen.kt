package com.lifeos.app.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.ui.components.LifeOSBanner
import com.lifeos.app.ui.components.LifeOSIconButton
import com.lifeos.app.ui.components.prismBrush
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** A destination an answer can offer to open, so a reply is actionable. */
enum class AiAction(val label: String) {
    LIFE("Open Life"),
    DIARY("Open Diary"),
    TASKS("Open Tasks"),
    HABITS("Open Habits"),
    EXPENSES("Open Spending"),
    SEARCH("Search everything")
}

data class AiMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val action: AiAction? = null
)

data class AiUiState(
    val messages: List<AiMessage> = emptyList(),
    val thinking: Boolean = false
)

/**
 * LifeOS AI — a *local*, deterministic question answerer.
 *
 * There is no model and no network. The assistant classifies the question by
 * keyword, then answers using the same numbers the Life hub and every module
 * compute from the local database. That constraint is the feature: every reply
 * is traceable to stored rows and can never hallucinate a fact about the user's
 * life.
 */
class AiAssistantViewModel(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val expenseRepository: ExpenseRepository,
    private val diaryRepository: DiaryRepository,
    private val captureRepository: CaptureRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        AiUiState(
            messages = listOf(
                AiMessage(
                    id = 0,
                    fromUser = false,
                    text = "Ask me about your month — spending, tasks, habits or your diary. I read only your own records, on this phone."
                )
            )
        )
    )
    val state: StateFlow<AiUiState> = _state.asStateFlow()

    private var nextId = 1L

    fun ask(question: String) {
        val q = question.trim()
        if (q.isEmpty()) return
        val userMessage = AiMessage(nextId++, fromUser = true, text = q)
        _state.update { it.copy(messages = it.messages + userMessage, thinking = true) }
        viewModelScope.launch {
            val (answer, action) = answer(q)
            _state.update { it.copy(messages = it.messages + AiMessage(nextId++, fromUser = false, text = answer, action = action), thinking = false) }
        }
    }

    private suspend fun answer(question: String): Pair<String, AiAction?> {
        val text = question.lowercase(Locale.getDefault())
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val monthStart = today.withDayOfMonth(1).toEpochDay()
        val todayEpoch = today.toEpochDay()
        val weekStart = today.minusDays(6).toEpochDay()
        val monthStartMillis = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val tomorrowMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val weekStartMillis = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()

        return when {
            text.containsAny("spend", "spent", "money", "cost", "expense", "budget") -> {
                val expenses = runCatching { expenseRepository.observeInRange(monthStart, todayEpoch).first() }.getOrDefault(emptyList())
                val total = expenses.sumOf { it.amount }
                val top = expenses.groupBy { it.category }.maxByOrNull { it.value.sumOf { e -> e.amount } }
                val reply = if (expenses.isEmpty()) {
                    "You haven't logged any spending this month yet. Add one from Spending and I'll break it down."
                } else {
                    buildString {
                        append("This month you've spent ${money(total)} across ${expenses.size} expense${if (expenses.size == 1) "" else "s"}.")
                        top?.let { append(" The biggest category is ${it.key} at ${money(it.value.sumOf { e -> e.amount })}.") }
                    }
                }
                reply to AiAction.EXPENSES
            }

            text.containsAny("task", "todo", "to-do", "done", "finished") -> {
                val week = runCatching { taskRepository.getCompletedBetween(weekStartMillis, tomorrowMillis).size }.getOrDefault(0)
                val month = runCatching { taskRepository.getCompletedBetween(monthStartMillis, tomorrowMillis).size }.getOrDefault(0)
                val reply = if (month == 0) {
                    "No tasks logged as complete this month. Tick one off and I'll start counting."
                } else {
                    "You've completed $month task${if (month == 1) "" else "s"} this month — $week in the last 7 days."
                }
                reply to AiAction.TASKS
            }

            text.containsAny("habit", "streak", "routine") -> {
                val tracked = runCatching { habitRepository.observeAll().first().size }.getOrDefault(0)
                val done = runCatching { habitRepository.observeAllInRange(monthStart, todayEpoch).first().size }.getOrDefault(0)
                val reply = if (tracked == 0) {
                    "You're not tracking any habits yet. Add one and I'll keep the streak."
                } else {
                    "You have $tracked habit${if (tracked == 1) "" else "s"} tracked, with $done check-in${if (done == 1) "" else "s"} this month."
                }
                reply to AiAction.HABITS
            }

            text.containsAny("diary", "journal", "wrote", "writing", "mood", "feel") -> {
                val entries = runCatching { diaryRepository.getAllInRange(weekStart, todayEpoch).size }.getOrDefault(0)
                val reply = if (entries == 0) {
                    "Nothing in your diary this week. Even one line keeps the thread."
                } else {
                    "You wrote $entries diary entr${if (entries == 1) "y" else "ies"} in the last 7 days."
                }
                reply to AiAction.DIARY
            }

            text.containsAny("capture", "memory", "memories", "photo", "picture", "moment") -> {
                val captures = runCatching { captureRepository.getAllInRange(monthStartMillis, tomorrowMillis).size }.getOrDefault(0)
                val reply = if (captures == 0) {
                    "No captures this month yet. Use the ＋ button to save a moment."
                } else {
                    "You saved $captures moment${if (captures == 1) "" else "s"} this month."
                }
                reply to AiAction.SEARCH
            }

            text.containsAny("week", "month", "overview", "summary", "how am i", "progress") -> {
                val tasks = runCatching { taskRepository.getCompletedBetween(monthStartMillis, tomorrowMillis).size }.getOrDefault(0)
                val spend = runCatching { expenseRepository.observeInRange(monthStart, todayEpoch).first() }.getOrDefault(emptyList()).sumOf { it.amount }
                val diary = runCatching { diaryRepository.getAllInRange(monthStart, todayEpoch) }.getOrDefault(emptyList()).size
                "This month: $tasks task${if (tasks == 1) "" else "s"} done, ${money(spend)} spent, and $diary diary entr${if (diary == 1) "y" else "ies"} written." to AiAction.LIFE
            }

            else -> "I can answer questions about your spending, tasks, habits, diary or captured moments. Try “What did I spend this month?”" to AiAction.LIFE
        }
    }

    private fun String.containsAny(vararg needles: String): Boolean = needles.any { contains(it) }

    private fun money(amount: Double): String {
        val digits = if (amount % 1.0 == 0.0) 0 else 2
        return "₹" + String.format(Locale.getDefault(), "%,.${digits}f", amount)
    }
}

/**
 * The assistant surface: a scrolling transcript, a privacy line, tappable
 * starter questions, and a single input. Answers carry an action chip so a reply
 * always leads somewhere real.
 */
@Composable
fun AiAssistantScreen(
    onBack: () -> Unit,
    onAction: (AiAction) -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: AiAssistantViewModel = viewModel(
        factory = LambdaViewModelFactory {
            AiAssistantViewModel(
                locator.taskRepository,
                locator.habitRepository,
                locator.expenseRepository,
                locator.diaryRepository,
                locator.captureRepository
            )
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    val starters = remember {
        listOf(
            "What did I spend this month?",
            "How many tasks did I finish?",
            "How are my habits going?",
            "Did I journal this week?"
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding, vertical = LifeOSSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
        ) {
            LifeOSIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(prismBrush()),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("LifeOS AI", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("On-device · private", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = LifeOSSpacing.screenPadding, vertical = LifeOSSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
        ) {
            item {
                LifeOSBanner(
                    icon = Icons.Filled.Lock,
                    text = "Answers are computed on this phone from your own records. Nothing is sent anywhere."
                )
            }
            items(state.messages, key = { it.id }) { message -> MessageBubble(message, onAction = onAction) }
            if (state.thinking) {
                item {
                    Text(
                        "Thinking…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = LifeOSSpacing.sm)
                    )
                }
            }
        }

        LazyRow(
            Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm),
            contentPadding = PaddingValues(vertical = LifeOSSpacing.sm)
        ) {
            items(starters) { starter ->
                Text(
                    starter,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(LifeOSRadius.pill))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .clickable { viewModel.ask(starter) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LifeOSSpacing.screenPadding, vertical = LifeOSSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(LifeOSRadius.pill))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = LifeOSSpacing.lg, vertical = LifeOSSpacing.md)
            ) {
                if (input.isEmpty()) {
                    Text("Ask about your life…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (input.isBlank()) SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                        else prismBrush()
                    )
                    .clickable(enabled = input.isNotBlank()) {
                        viewModel.ask(input)
                        input = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (input.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: AiMessage, onAction: (AiAction) -> Unit) {
    val isUser = message.fromUser
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.86f)
                .clip(
                    RoundedCornerShape(
                        topStart = LifeOSRadius.large,
                        topEnd = LifeOSRadius.large,
                        bottomStart = if (isUser) LifeOSRadius.large else LifeOSRadius.small,
                        bottomEnd = if (isUser) LifeOSRadius.small else LifeOSRadius.large
                    )
                )
                .background(
                    if (isUser) prismBrush()
                    else SolidColor(MaterialTheme.colorScheme.surface)
                )
                .padding(LifeOSSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
        ) {
            Text(
                message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
            )
            message.action?.let { action ->
                Text(
                    action.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(LifeOSRadius.pill))
                        .background(if (isUser) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .clickable { onAction(action) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
