package com.lifeos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lifeos.app.core.database.entity.DiaryEntryEntity
import com.lifeos.app.core.database.entity.ExpenseEntity
import com.lifeos.app.core.database.entity.HabitCompletionEntity
import com.lifeos.app.core.database.entity.HabitEntity
import com.lifeos.app.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/** Tasks: open list, completed history, trash. */
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deletedAt = 0 AND status = 'open' " +
        "ORDER BY CASE priority WHEN 'high' THEN 0 WHEN 'medium' THEN 1 WHEN 'low' THEN 2 ELSE 3 END, " +
        "dueDate IS NULL, dueDate, updatedAt DESC")
    fun observeOpen(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt = 0 AND status = 'done' ORDER BY completedAt DESC")
    fun observeDone(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt = 0 AND dueDate = :date ORDER BY status, priority DESC")
    fun observeForDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt " +
        "WHERE id = :id AND deletedAt = 0")
    suspend fun setStatus(id: String, status: String, completedAt: Long?, updatedAt: Long)

    @Query("UPDATE tasks SET deletedAt = :at WHERE id = :id AND deletedAt = 0")
    suspend fun softDelete(id: String, at: Long)

    @Query("UPDATE tasks SET deletedAt = 0, updatedAt = :updatedAt WHERE id = :id AND deletedAt != 0")
    suspend fun restore(id: String, updatedAt: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteForever(id: String)

    @Query("SELECT * FROM tasks WHERE deletedAt != 0 ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<TaskEntity>>
}

/** Habits + completions. Completion rows are keyed by local ISO date. */
@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE deletedAt = 0 AND archivedAt IS NULL ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE deletedAt = 0 ORDER BY archivedAt IS NULL, createdAt DESC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: HabitEntity)

    @Query("UPDATE habits SET archivedAt = :at, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, at: Long?, updatedAt: Long)

    @Query("UPDATE habits SET deletedAt = :at WHERE id = :id AND deletedAt = 0")
    suspend fun softDelete(id: String, at: Long)

    @Query("UPDATE habits SET deletedAt = 0, updatedAt = :updatedAt WHERE id = :id AND deletedAt != 0")
    suspend fun restore(id: String, updatedAt: Long)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteForever(id: String)

    @Query("SELECT * FROM habits WHERE deletedAt != 0 ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date")
    suspend fun completionsFor(habitId: String): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun completionOn(habitId: String, date: String): HabitCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun removeCompletion(habitId: String, date: String)

    @Query("SELECT * FROM habit_completions WHERE date = :date")
    fun observeCompletionsOn(date: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE date BETWEEN :from AND :to")
    suspend fun completionsBetween(from: String, to: String): List<HabitCompletionEntity>

    /** Completing a habit and stamping its timeline row in one transaction. */
    @Transaction
    suspend fun completeWith(
        completion: HabitCompletionEntity,
        event: com.lifeos.app.core.database.entity.LifeEventEntity,
        eventDao: LifeEventDao,
    ) {
        upsertCompletion(completion)
        eventDao.upsert(event)
    }
}

/** Expenses: exact minor units, listed newest-first, summed in SQL as Long. */
@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE deletedAt = 0 ORDER BY expenseDate DESC, createdAt DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE deletedAt = 0 AND expenseDate BETWEEN :from AND :to " +
        "ORDER BY expenseDate DESC")
    fun observeBetween(from: String, to: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: ExpenseEntity)

    @Query("UPDATE expenses SET deletedAt = :at WHERE id = :id AND deletedAt = 0")
    suspend fun softDelete(id: String, at: Long)

    @Query("UPDATE expenses SET deletedAt = 0, updatedAt = :updatedAt WHERE id = :id AND deletedAt != 0")
    suspend fun restore(id: String, updatedAt: Long)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteForever(id: String)

    @Query("SELECT * FROM expenses WHERE deletedAt != 0 ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<ExpenseEntity>>

    /** Sum over a period as exact integer minor units (never a float aggregate). */
    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM expenses " +
        "WHERE deletedAt = 0 AND currencyCode = :currency AND expenseDate BETWEEN :from AND :to")
    fun observeTotalBetween(from: String, to: String, currency: String): Flow<Long>

    @Query("SELECT category, COALESCE(SUM(amountMinor), 0) AS total FROM expenses " +
        "WHERE deletedAt = 0 AND currencyCode = :currency AND expenseDate BETWEEN :from AND :to " +
        "GROUP BY category ORDER BY total DESC")
    fun observeCategoryTotals(from: String, to: String, currency: String): Flow<List<CategoryTotal>>
}

data class EntityTagName(val entityId: String, val name: String)

data class CategoryTotal(val category: String, val total: Long)

/** Diary: exactly one entry per local date. */
@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE deletedAt = 0 ORDER BY diaryDate DESC")
    fun observeAll(): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries WHERE deletedAt = 0 AND diaryDate = :date LIMIT 1")
    fun observeOn(date: String): Flow<DiaryEntryEntity?>

    @Query("SELECT * FROM diary_entries WHERE deletedAt = 0 AND diaryDate = :date LIMIT 1")
    suspend fun getOn(date: String): DiaryEntryEntity?

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: String): DiaryEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DiaryEntryEntity)

    @Query("UPDATE diary_entries SET deletedAt = :at WHERE id = :id AND deletedAt = 0")
    suspend fun softDelete(id: String, at: Long)

    @Query("UPDATE diary_entries SET deletedAt = 0, updatedAt = :updatedAt WHERE id = :id AND deletedAt != 0")
    suspend fun restore(id: String, updatedAt: Long)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteForever(id: String)

    @Query("SELECT * FROM diary_entries WHERE deletedAt != 0 ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<DiaryEntryEntity>>

    @Query("SELECT mood, diaryDate FROM diary_entries WHERE deletedAt = 0 " +
        "AND mood IS NOT NULL AND diaryDate BETWEEN :from AND :to ORDER BY diaryDate")
    suspend fun moodSeries(from: String, to: String): List<MoodPoint>
}

data class MoodPoint(val mood: String, val diaryDate: String)
