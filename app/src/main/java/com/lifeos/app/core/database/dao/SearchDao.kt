package com.lifeos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.lifeos.app.core.database.entity.CaptureEntity
import com.lifeos.app.core.database.entity.DiaryEntryEntity
import com.lifeos.app.core.database.entity.ExpenseEntity
import com.lifeos.app.core.database.entity.HabitEntity
import com.lifeos.app.core.database.entity.LifeEventEntity
import com.lifeos.app.core.database.entity.NoteEntity
import com.lifeos.app.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Global search across the five record types + timeline (spec: Search tab shows
 * results grouped by type, 25 per group in the UI).
 *
 * v1 uses case-insensitive `LIKE` matching over title/body columns. That is exact,
 * dependency-free and private (no data leaves the device). FTS4 is a documented
 * follow-up if the corpus grows (see NEXT_SESSION.md) — no index is claimed today.
 */
@Dao
interface SearchDao {
    @Query("SELECT * FROM notes WHERE deletedAt = 0 AND " +
        "(title LIKE :pattern ESCAPE '\\' OR body LIKE :pattern ESCAPE '\\') " +
        "ORDER BY updatedAt DESC LIMIT :limit")
    fun notes(pattern: String, limit: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt = 0 AND " +
        "(title LIKE :pattern ESCAPE '\\' OR notes LIKE :pattern ESCAPE '\\') " +
        "ORDER BY updatedAt DESC LIMIT :limit")
    fun tasks(pattern: String, limit: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM habits WHERE deletedAt = 0 AND " +
        "(name LIKE :pattern ESCAPE '\\' OR emoji LIKE :pattern ESCAPE '\\') " +
        "ORDER BY createdAt DESC LIMIT :limit")
    fun habits(pattern: String, limit: Int): Flow<List<HabitEntity>>

    @Query("SELECT * FROM expenses WHERE deletedAt = 0 AND " +
        "(note LIKE :pattern ESCAPE '\\' OR category LIKE :pattern ESCAPE '\\') " +
        "ORDER BY expenseDate DESC LIMIT :limit")
    fun expenses(pattern: String, limit: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM diary_entries WHERE deletedAt = 0 AND " +
        "(title LIKE :pattern ESCAPE '\\' OR body LIKE :pattern ESCAPE '\\') " +
        "ORDER BY diaryDate DESC LIMIT :limit")
    fun diary(pattern: String, limit: Int): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM captures WHERE deletedAt = 0 AND " +
        "(text LIKE :pattern ESCAPE '\\' OR placeLabel LIKE :pattern ESCAPE '\\') " +
        "ORDER BY occurredOn DESC LIMIT :limit")
    fun captures(pattern: String, limit: Int): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM life_events WHERE deletedAt = 0 AND " +
        "(title LIKE :pattern ESCAPE '\\' OR detail LIKE :pattern ESCAPE '\\') " +
        "ORDER BY occurredAt DESC LIMIT :limit")
    fun events(pattern: String, limit: Int): Flow<List<LifeEventEntity>>

    companion object {
        /** `%term%` with LIKE wildcards escaped, safe for user input. */
        fun patternFor(query: String): String {
            val escaped = query.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
            return "%$escaped%"
        }
    }
}
