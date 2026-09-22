package com.lifeos.app.core.life

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.repository.*
import com.lifeos.app.domain.model.NoteBlock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

sealed class LifeDestination {
    data object Notes : LifeDestination()
    data object Diary : LifeDestination()
    data object Tasks : LifeDestination()
    data object Habits : LifeDestination()
    data object Expenses : LifeDestination()
    data object Home : LifeDestination()
    data class Note(val id: String) : LifeDestination()
}

data class LifeActionFeedback(val label: String, val destination: LifeDestination)
data class LifeAppContext(val section: LifeSection = LifeSection.HOME)
enum class LifeSection { HOME, NOTES, DIARY, TASKS, HABITS, EXPENSES, OTHER }

data class LifeResult(
    val message: String,
    val requiresConfirmation: Boolean = false,
    val destination: LifeDestination? = null,
    val feedback: LifeActionFeedback? = null
)

class LifeConversation(private val maxEntries: Int = 12) {
    private val user = ArrayDeque<String>()
    fun addUser(text: String) { user.addLast(text); while (user.size > maxEntries) user.removeFirst() }
    fun addAssistant(@Suppress("UNUSED_PARAMETER") text: String) = Unit
    fun previousUserText(): String? = user.dropLast(1).lastOrNull()
    fun clear() { user.clear() }
}

class LifeSessionMemory(private val maxEntries: Int = 12) {
    private val results = ArrayDeque<LifeResult>()
    fun remember(result: LifeResult) { results.addLast(result); while (results.size > maxEntries) results.removeFirst() }
    fun last(): LifeResult? = results.lastOrNull()
    fun clear() = results.clear()
}

/** Local, deterministic LIFE command controller. It is deliberately allow-listed and repository-only. */
class LifeController(
    private val notes: NoteRepository,
    private val tasks: TaskRepository,
    private val habits: HabitRepository,
    private val diary: DiaryRepository,
    private val expenses: ExpenseRepository
) {
    private var pending: (suspend () -> LifeResult)? = null

    suspend fun handle(text: String, context: LifeAppContext = LifeAppContext(), previousUserText: String? = null): LifeResult {
        val raw = text.trim()
        if (raw.equals("confirm", true)) return pending?.let { action -> pending = null; action() } ?: LifeResult("There is nothing waiting for confirmation.")
        if (raw.equals("cancel", true)) { pending = null; return LifeResult("Cancelled.") }
        val normalized = normalize(raw)
        if (normalized == "open it" || normalized == "open that") {
            return when (context.section) { LifeSection.NOTES -> LifeResult("Open your Notes.", destination = LifeDestination.Notes); LifeSection.DIARY -> LifeResult("Open your Diary.", destination = LifeDestination.Diary); else -> LifeResult("Tell me what you want me to open.") }
        }
        if (normalized.contains("delete") || normalized.contains("permanently") || normalized.contains("remove all")) {
            pending = { LifeResult("Destructive changes are intentionally not available through LIFE.") }
            return LifeResult("I won't delete data through a broad command. Please use the specific LifeOS screen.", requiresConfirmation = true)
        }
        return when {
            isCreateNote(normalized) -> createNote(raw)
            isCreateTask(normalized) -> createTask(raw)
            isCreateDiary(normalized) -> createDiary(raw)
            isCreateExpense(normalized) -> createExpense(raw)
            isCreateHabit(normalized) -> createHabit(raw)
            isSearch(normalized) -> search(raw, context)
            isCompleteTask(normalized) -> completeTask(raw)
            else -> LifeResult("I can work with your LifeOS data locally. Try: save a note, create a task, add an expense, or search your notes.")
        }
    }

    fun cancelPendingAction() { pending = null }

    private suspend fun createNote(raw: String): LifeResult {
        val content = raw.substringAfterAny(listOf("save a note", "save this note", "note:", "save note"), raw).trim().trim(':',' ')
        if (content.isBlank()) return LifeResult("What should I save in Notes?")
        val title = content.take(48).replaceFirstChar { it.uppercase() }
        val id = notes.createNote(title, listOf(NoteBlock.Paragraph(UUID.randomUUID().toString(), content)))
        return LifeResult("Saved to Notes: $title", destination = LifeDestination.Note(id), feedback = LifeActionFeedback("Open Note", LifeDestination.Note(id)))
    }

    private suspend fun createTask(raw: String): LifeResult {
        val content = raw.substringAfterAny(listOf("create a task", "create task", "make a task", "task:"), raw).trim().trim(':',' ')
        val (date, time) = parseDateTime(raw)
        val id = tasks.createTask(content, dueDateEpochDay = date, dueTimeMinutes = time)
        return LifeResult("Task created: $content", destination = LifeDestination.Tasks, feedback = LifeActionFeedback("Open Tasks", LifeDestination.Tasks))
    }

    private suspend fun createDiary(raw: String): LifeResult {
        val content = raw.substringAfterAny(listOf("save in diary", "save to diary", "diary:", "diary"), raw).trim().trim(':',' ')
        val now = LocalDateTime.now()
        diary.createEntry(null, content, null, emptyList(), now.toLocalDate().toEpochDay(), DateTimeUtils.nowMinutesOfDay(), false)
        return LifeResult("Saved to Diary.", destination = LifeDestination.Diary, feedback = LifeActionFeedback("Open Diary", LifeDestination.Diary))
    }

    private suspend fun createExpense(raw: String): LifeResult {
        val amount = Regex("(?:₹|rs\\.?|inr\\s*)\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE).find(raw)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: Regex("\\b(\\d+(?:\\.\\d+)?)\\b").find(raw)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: return LifeResult("Tell me the expense amount, for example ₹250.")
        val category = when { raw.contains("food", true) -> "Food"; raw.contains("travel", true) -> "Travel"; raw.contains("shopping", true) -> "Shopping"; raw.contains("bill", true) -> "Bills"; else -> "Other" }
        val now = LocalDateTime.now()
        expenses.addExpense(amount, category, now.toLocalDate().toEpochDay(), DateTimeUtils.nowMinutesOfDay())
        return LifeResult("Expense added: ₹${amount.toInt()} · $category", destination = LifeDestination.Expenses, feedback = LifeActionFeedback("Open Expenses", LifeDestination.Expenses))
    }

    private suspend fun createHabit(raw: String): LifeResult {
        val name = raw.substringAfterAny(listOf("create a habit", "create habit", "make a habit", "habit:"), raw).trim().trim(':',' ')
        habits.createHabit(name, "✓")
        return LifeResult("Habit created: $name", destination = LifeDestination.Habits, feedback = LifeActionFeedback("Open Habits", LifeDestination.Habits))
    }

    private suspend fun search(raw: String, context: LifeAppContext): LifeResult {
        val q = raw.substringAfterAny(listOf("search", "find", "look for"), "").trim()
        if (q.isBlank()) return LifeResult("What should I search for?")
        return when (context.section) {
            LifeSection.NOTES -> LifeResult("Found ${notes.search(q).size} matching note(s).", destination = LifeDestination.Notes)
            LifeSection.TASKS -> LifeResult("Found ${tasks.search(q).size} matching task(s).", destination = LifeDestination.Tasks)
            LifeSection.EXPENSES -> LifeResult("Found ${expenses.search(q).size} matching expense(s).", destination = LifeDestination.Expenses)
            else -> LifeResult("Found ${notes.search(q).size} matching note(s).", destination = LifeDestination.Notes)
        }
    }

    private suspend fun completeTask(raw: String): LifeResult {
        val q = raw.substringAfterAny(listOf("complete task", "complete", "finish task"), "").trim()
        val match = tasks.search(q).firstOrNull() ?: return LifeResult("I couldn't find that task.")
        tasks.setCompleted(match.id, true)
        return LifeResult("Completed: ${match.title}", destination = LifeDestination.Tasks, feedback = LifeActionFeedback("Open Tasks", LifeDestination.Tasks))
    }

    private fun isCreateNote(s: String) = listOf("save a note", "save this note", "save note", "note:").any { s.contains(it) }
    private fun isCreateTask(s: String) = listOf("create a task", "create task", "make a task", "task:").any { s.contains(it) }
    private fun isCreateDiary(s: String) = listOf("save in diary", "save to diary", "diary:").any { s.contains(it) }
    private fun isCreateExpense(s: String) = listOf("expense", "spent", "kharch", "खर्च", "₹", "rs ").any { s.contains(it) }
    private fun isCreateHabit(s: String) = listOf("create a habit", "create habit", "make a habit", "habit:").any { s.contains(it) }
    private fun isSearch(s: String) = s.startsWith("search ") || s.startsWith("find ") || s.startsWith("look for ") || s.contains("search notes")
    private fun isCompleteTask(s: String) = s.contains("complete task") || s.contains("finish task")

    private fun normalize(value: String): String = value.lowercase(Locale.ROOT)
        .replace("कृपया", " ").replace("कर दो", "").replace("कर de", "").replace("कर dena", "")
        .replace("bana do", "create").replace("bana", "create").replace("save kar do", "save").replace("save karna", "save")
        .replace("mein", "in").replace("me", "in").replace("में", "in").replace("में", "in")
        .replace(Regex("\\s+"), " ").trim()

    private fun parseDateTime(text: String): Pair<Long?, Int?> {
        val today = LocalDate.now()
        val date = when { text.contains("tomorrow", true) || text.contains("kal", true) || text.contains("કાલે", true) -> today.plusDays(1); else -> today }
        val match = Regex("(?:at|@|baje| વાગે)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE).find(text)
        val time = match?.let { m ->
            var h = m.groupValues[1].toInt(); val min = m.groupValues[2].ifBlank { "0" }.toInt(); val ap = m.groupValues[3]
            if (ap.equals("pm", true) && h < 12) h += 12; if (ap.equals("am", true) && h == 12) h = 0; if (ap.isBlank() && h in 1..7) h += 12
            h * 60 + min
        }
        return date.toEpochDay() to time
    }

    private fun String.substringAfterAny(tokens: List<String>, fallback: String): String {
        val index = tokens.mapNotNull { t -> indexOf(t, ignoreCase = true).takeIf { it >= 0 } }.minOrNull() ?: return fallback
        val token = tokens.first { indexOf(it, ignoreCase = true) == index }
        return substring(index + token.length)
    }
}
