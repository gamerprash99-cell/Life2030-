package com.lifeos.app.core.backup

import com.lifeos.app.core.database.LifeOsDatabase
import com.lifeos.app.core.database.entity.CaptureEntity
import com.lifeos.app.core.database.entity.DiaryEntryEntity
import com.lifeos.app.core.database.entity.ExpenseEntity
import com.lifeos.app.core.database.entity.HabitCompletionEntity
import com.lifeos.app.core.database.entity.HabitEntity
import com.lifeos.app.core.database.entity.LifeEventEntity
import com.lifeos.app.core.database.entity.NoteEntity
import com.lifeos.app.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * JSON export of the user's own data.
 *
 * Deliberately dependency-free: the export format is written by hand with a tiny
 * escaper so the app does not pull a serialization library just for this, and so
 * the produced file shape is auditable in one file. The format is versioned
 * ([FORMAT_VERSION]) so a future importer can be written against it.
 *
 * Nothing here talks to a network: the exporter returns a String and the caller
 * decides where to write it (app-private storage, or a user-chosen destination).
 */
class DataExporter(private val db: LifeOsDatabase) {

    /** Full logical dump as pretty-printed JSON. */
    suspend fun exportJson(): String {
        val notes = firstOf(db.noteDao().observeAll())
        val tasks = firstOf(db.taskDao().observeOpen())
        val doneTasks = firstOf(db.taskDao().observeDone())
        val habits = firstOf(db.habitDao().observeActive())
        val expenses = firstOf(db.expenseDao().observeAll())
        val diary = firstOf(db.diaryDao().observeAll())
        val captures = firstOf(db.captureDao().observeAll())
        val events = firstOf(db.lifeEventDao().observeRecent(Int.MAX_VALUE))
        val completions = habits.flatMap { db.habitDao().completionsFor(it.id) }

        return buildString {
            appendLine("{")
            appendLine("  \"format\": \"lifeos-export\",")
            appendLine("  \"formatVersion\": $FORMAT_VERSION,")
            appendLine("  \"generatedAtMillis\": ${System.currentTimeMillis()},")
            appendLine("  \"counts\": {")
            appendLine("    \"notes\": ${notes.size},")
            appendLine("    \"tasks\": ${tasks.size + doneTasks.size},")
            appendLine("    \"habits\": ${habits.size},")
            appendLine("    \"habitCompletions\": ${completions.size},")
            appendLine("    \"expenses\": ${expenses.size},")
            appendLine("    \"diaryEntries\": ${diary.size},")
            appendLine("    \"captures\": ${captures.size},")
            appendLine("    \"lifeEvents\": ${events.size}")
            appendLine("  },")

            appendLine("  \"notes\": [")
            appendLines(notes) { note -> noteJson(note) }
            appendLine("  ],")

            appendLine("  \"tasks\": [")
            appendLines(tasks + doneTasks) { task -> taskJson(task) }
            appendLine("  ],")

            appendLine("  \"habits\": [")
            appendLines(habits) { habit -> habitJson(habit) }
            appendLine("  ],")

            appendLine("  \"habitCompletions\": [")
            appendLines(completions) { completion -> completionJson(completion) }
            appendLine("  ],")

            appendLine("  \"expenses\": [")
            appendLines(expenses) { expense -> expenseJson(expense) }
            appendLine("  ],")

            appendLine("  \"diaryEntries\": [")
            appendLines(diary) { entry -> diaryJson(entry) }
            appendLine("  ],")

            appendLine("  \"captures\": [")
            appendLines(captures) { capture -> captureJson(capture) }
            appendLine("  ],")

            appendLine("  \"lifeEvents\": [")
            appendLines(events) { event -> eventJson(event) }
            appendLine("  ]")

            appendLine("}")
        }
    }

    /** Human-readable plain-text summary, for sharing or printing. */
    suspend fun exportText(): String {
        val notes = firstOf(db.noteDao().observeAll())
        val tasks = firstOf(db.taskDao().observeOpen())
        val habits = firstOf(db.habitDao().observeActive())
        val expenses = firstOf(db.expenseDao().observeAll())
        val diary = firstOf(db.diaryDao().observeAll())
        val captures = firstOf(db.captureDao().observeAll())

        return buildString {
            appendLine("LifeOS export")
            appendLine("--------------")
            appendLine("Notes: ${notes.size}")
            appendLine("Open tasks: ${tasks.size}")
            appendLine("Habits: ${habits.size}")
            appendLine("Expenses: ${expenses.size}")
            appendLine("Diary entries: ${diary.size}")
            appendLine("Captures: ${captures.size}")
        }
    }

    private suspend fun <T> firstOf(flow: Flow<T>): T = flow.first()

    private inline fun StringBuilder.appendLines(
        items: List<*>,
        crossinline json: (Any?) -> String,
    ) {
        items.forEachIndexed { index, item ->
            append("    ")
            append(json(item))
            if (index != items.lastIndex) append(',')
            appendLine()
        }
    }

    // --------------------------------------------------------------- rows

    private fun noteJson(value: Any?): String {
        val note = value as NoteEntity
        return obj(
            "id" to quoted(note.id),
            "title" to quoted(note.title),
            "body" to quoted(note.body),
            "kind" to quoted(note.kind),
            "pinned" to note.pinned.toString(),
            "createdAt" to note.createdAt.toString(),
            "updatedAt" to note.updatedAt.toString(),
        )
    }

    private fun taskJson(value: Any?): String {
        val task = value as TaskEntity
        return obj(
            "id" to quoted(task.id),
            "title" to quoted(task.title),
            "notes" to quoted(task.notes),
            "dueDate" to (task.dueDate?.let(::quoted) ?: "null"),
            "priority" to quoted(task.priority),
            "status" to quoted(task.status),
            "recurrence" to quoted(task.recurrence),
            "completedAt" to (task.completedAt?.toString() ?: "null"),
        )
    }

    private fun habitJson(value: Any?): String {
        val habit = value as HabitEntity
        return obj(
            "id" to quoted(habit.id),
            "name" to quoted(habit.name),
            "area" to quoted(habit.area),
            "recurrence" to quoted(habit.recurrence),
            "mode" to quoted(habit.mode),
            "targetPerDay" to habit.targetPerDay.toString(),
        )
    }

    private fun completionJson(value: Any?): String {
        val completion = value as HabitCompletionEntity
        return obj(
            "habitId" to quoted(completion.habitId),
            "date" to quoted(completion.date),
            "amount" to completion.amount.toString(),
            "note" to quoted(completion.note),
        )
    }

    private fun expenseJson(value: Any?): String {
        val expense = value as ExpenseEntity
        // amountMinor is a Long on purpose: the export preserves exact minor units.
        return obj(
            "id" to quoted(expense.id),
            "amountMinor" to expense.amountMinor.toString(),
            "currencyCode" to quoted(expense.currencyCode),
            "category" to quoted(expense.category),
            "note" to quoted(expense.note),
            "expenseDate" to quoted(expense.expenseDate),
        )
    }

    private fun diaryJson(value: Any?): String {
        val entry = value as DiaryEntryEntity
        return obj(
            "id" to quoted(entry.id),
            "diaryDate" to quoted(entry.diaryDate),
            "title" to quoted(entry.title),
            "body" to quoted(entry.body),
            "mood" to (entry.mood?.let(::quoted) ?: "null"),
        )
    }

    private fun captureJson(value: Any?): String {
        val capture = value as CaptureEntity
        return obj(
            "id" to quoted(capture.id),
            "type" to quoted(capture.type),
            "text" to quoted(capture.text),
            "occurredOn" to quoted(capture.occurredOn),
            "latitude" to (capture.latitude?.toString() ?: "null"),
            "longitude" to (capture.longitude?.toString() ?: "null"),
            "placeLabel" to (capture.placeLabel?.let(::quoted) ?: "null"),
            "moodScore" to (capture.moodScore?.toString() ?: "null"),
        )
    }

    private fun eventJson(value: Any?): String {
        val event = value as LifeEventEntity
        return obj(
            "id" to quoted(event.id),
            "type" to quoted(event.type),
            "title" to quoted(event.title),
            "detail" to quoted(event.detail),
            "area" to quoted(event.area),
            "occurredOn" to quoted(event.occurredOn),
            "occurredAt" to event.occurredAt.toString(),
        )
    }

    // ------------------------------------------------------------ helpers

    private fun obj(vararg pairs: Pair<String, String>): String =
        pairs.joinToString(prefix = "{", postfix = "}") { (key, value) -> "\"$key\": $value" }

    /** Minimal JSON string escaping — enough for the text this app stores. */
    private fun quoted(value: String): String {
        val builder = StringBuilder(value.length + 2)
        builder.append('"')
        value.forEach { char ->
            when (char) {
                '"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> if (char < ' ') {
                    builder.append("\\u%04x".format(char.code))
                } else {
                    builder.append(char)
                }
            }
        }
        builder.append('"')
        return builder.toString()
    }

    companion object {
        /** Bump when the JSON shape changes; importers switch on this. */
        const val FORMAT_VERSION = 1
    }
}
