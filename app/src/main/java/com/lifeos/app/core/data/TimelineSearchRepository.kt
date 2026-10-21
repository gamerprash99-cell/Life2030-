package com.lifeos.app.core.data

import com.lifeos.app.core.database.LifeOsDatabase
import com.lifeos.app.core.database.dao.SearchDao
import com.lifeos.app.core.database.entity.CaptureEntity
import com.lifeos.app.core.database.entity.DiaryEntryEntity
import com.lifeos.app.core.database.entity.ExpenseEntity
import com.lifeos.app.core.database.entity.HabitEntity
import com.lifeos.app.core.database.entity.LifeEventEntity
import com.lifeos.app.core.database.entity.NoteEntity
import com.lifeos.app.core.database.entity.TaskEntity
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.SearchGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Timeline + global search reads.
 *
 * The Timeline is a pure projection of real rows (nothing is generated), and
 * search is grouped per record type with a per-group limit so no screen can be
 * flooded (spec: 25 results per group).
 */
class TimelineSearchRepository(private val db: LifeOsDatabase) {

    fun observeTimeline(limit: Int = 500): Flow<List<LifeEventEntity>> =
        db.lifeEventDao().observeRecent(limit)

    fun observeTimelineOn(date: String): Flow<List<LifeEventEntity>> =
        db.lifeEventDao().observeOn(date)

    fun observeTimelineBetween(from: String, to: String): Flow<List<LifeEventEntity>> =
        db.lifeEventDao().observeBetween(from, to)

    suspend fun typeCounts(from: String, to: String) =
        db.lifeEventDao().countsBetween(from, to)

    fun observeTrash(): Flow<TrashContent> {
        // Nested combine: the six-arg overload's type inference is fragile against
        // the vararg overload, so pairs are combined explicitly.
        val head = combine(
            db.noteDao().observeTrash(),
            db.taskDao().observeTrash(),
            db.habitDao().observeTrash(),
        ) { notes, tasks, habits -> Triple(notes, tasks, habits) }
        val tail = combine(
            db.expenseDao().observeTrash(),
            db.diaryDao().observeTrash(),
            db.captureDao().observeTrash(),
        ) { expenses, diary, captures -> Triple(expenses, diary, captures) }
        return combine(head, tail) { (notes, tasks, habits), (expenses, diary, captures) ->
            TrashContent(
                notes = notes,
                tasks = tasks,
                habits = habits,
                expenses = expenses,
                diary = diary,
                captures = captures,
                totalCount = notes.size + tasks.size + habits.size +
                    expenses.size + diary.size + captures.size,
            )
        }
    }

    /** Grouped search results; every group is independently limited. */
    fun observeSearch(query: String, perGroupLimit: Int = 25): Flow<SearchResults> {
        val pattern = SearchDao.patternFor(query)
        val dao = db.searchDao()
        val head = combine(
            dao.notes(pattern, perGroupLimit),
            dao.tasks(pattern, perGroupLimit),
            dao.habits(pattern, perGroupLimit),
            dao.expenses(pattern, perGroupLimit),
        ) { n, t, h, e -> listOf(n, t, h, e) }
        val tail = combine(
            dao.diary(pattern, perGroupLimit),
            dao.captures(pattern, perGroupLimit),
            dao.events(pattern, perGroupLimit),
        ) { d, c, ev -> listOf(d, c, ev) }
        return combine(head, tail) { headParts, tailParts ->
            @Suppress("UNCHECKED_CAST")
            val n = headParts[0] as List<NoteEntity>
            @Suppress("UNCHECKED_CAST")
            val t = headParts[1] as List<TaskEntity>
            @Suppress("UNCHECKED_CAST")
            val h = headParts[2] as List<HabitEntity>
            @Suppress("UNCHECKED_CAST")
            val e = headParts[3] as List<ExpenseEntity>
            @Suppress("UNCHECKED_CAST")
            val d = tailParts[0] as List<DiaryEntryEntity>
            @Suppress("UNCHECKED_CAST")
            val c = tailParts[1] as List<CaptureEntity>
            @Suppress("UNCHECKED_CAST")
            val ev = tailParts[2] as List<LifeEventEntity>
            SearchResults(
                notes = n,
                tasks = t,
                habits = h,
                expenses = e,
                diary = d,
                captures = c,
                events = ev,
                isEmpty = n.isEmpty() && t.isEmpty() && h.isEmpty() &&
                    e.isEmpty() && d.isEmpty() && c.isEmpty() && ev.isEmpty(),
            )
        }
    }

    /** Count for the empty-query hint on the Search screen. */
    suspend fun totalCount(): Int {
        val pattern = "%"
        val dao = db.searchDao()
        var total = 0
        // Values come from the flows' first emission via a one-shot read.
        total += dao.notes(pattern, Int.MAX_VALUE).first().size
        total += dao.tasks(pattern, Int.MAX_VALUE).first().size
        total += dao.habits(pattern, Int.MAX_VALUE).first().size
        total += dao.expenses(pattern, Int.MAX_VALUE).first().size
        total += dao.diary(pattern, Int.MAX_VALUE).first().size
        total += dao.captures(pattern, Int.MAX_VALUE).first().size
        return total
    }
}

data class TrashContent(
    val notes: List<NoteEntity>,
    val tasks: List<TaskEntity>,
    val habits: List<HabitEntity>,
    val expenses: List<ExpenseEntity>,
    val diary: List<DiaryEntryEntity>,
    val captures: List<CaptureEntity>,
    val totalCount: Int,
) {
    fun groupFor(group: SearchGroup): List<Any> = when (group) {
        SearchGroup.NOTES -> notes
        SearchGroup.TASKS -> tasks
        SearchGroup.HABITS -> habits
        SearchGroup.EXPENSES -> expenses
        SearchGroup.DIARY -> diary
        SearchGroup.CAPTURES -> captures
        SearchGroup.TIMELINE -> emptyList()
    }
}

data class SearchResults(
    val notes: List<NoteEntity>,
    val tasks: List<TaskEntity>,
    val habits: List<HabitEntity>,
    val expenses: List<ExpenseEntity>,
    val diary: List<DiaryEntryEntity>,
    val captures: List<CaptureEntity>,
    val events: List<LifeEventEntity>,
    val isEmpty: Boolean,
)
