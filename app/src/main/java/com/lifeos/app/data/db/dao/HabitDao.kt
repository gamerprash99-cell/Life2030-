package com.lifeos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeos.app.data.db.entities.HabitChecklistCheckEntity
import com.lifeos.app.data.db.entities.HabitChecklistItemEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isArchived = 1 ORDER BY createdAt ASC")
    fun observeArchived(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HabitEntity?

    @Query("UPDATE habits SET isArchived = 1, updatedAt = :now WHERE id = :id")
    suspend fun archive(id: String, now: Long)

    @Query("UPDATE habits SET isArchived = 0, updatedAt = :now WHERE id = :id")
    suspend fun unarchive(id: String, now: Long)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM habits")
    suspend fun getAllForBackup(): List<HabitEntity>
}

@Dao
interface HabitCompletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :epochDay")
    suspend fun clear(habitId: String, epochDay: Long)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteForHabit(habitId: String)

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :epochDay LIMIT 1")
    suspend fun get(habitId: String, epochDay: Long): HabitCompletionEntity?

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :epochDay LIMIT 1")
    fun observe(habitId: String, epochDay: Long): Flow<HabitCompletionEntity?>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay ASC")
    suspend fun getForHabitInRange(habitId: String, startEpochDay: Long, endEpochDay: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE dateEpochDay = :epochDay")
    fun observeAllForDay(epochDay: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE dateEpochDay = :epochDay")
    suspend fun getAllForDay(epochDay: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay ASC")
    fun observeAllInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<HabitCompletionEntity>>

    /** One reactive source for schedule-aware streaks across every card. */
    @Query("SELECT * FROM habit_completions ORDER BY dateEpochDay ASC")
    fun observeAllCompletions(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions")
    suspend fun getAllForBackup(): List<HabitCompletionEntity>
}

@Dao
interface HabitChecklistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: HabitChecklistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<HabitChecklistItemEntity>)

    @Query("SELECT * FROM habit_checklist_items WHERE habitId = :habitId AND isArchived = 0 ORDER BY position ASC, createdAt ASC")
    fun observeActiveItems(habitId: String): Flow<List<HabitChecklistItemEntity>>

    @Query("SELECT * FROM habit_checklist_items WHERE isArchived = 0 ORDER BY habitId ASC, position ASC")
    fun observeAllActiveItems(): Flow<List<HabitChecklistItemEntity>>

    @Query("SELECT * FROM habit_checklist_items WHERE habitId = :habitId AND isArchived = 0 ORDER BY position ASC, createdAt ASC")
    suspend fun getActiveItems(habitId: String): List<HabitChecklistItemEntity>

    @Query("SELECT * FROM habit_checklist_items WHERE habitId = :habitId ORDER BY position ASC, createdAt ASC")
    suspend fun getAllItems(habitId: String): List<HabitChecklistItemEntity>

    @Query("SELECT * FROM habit_checklist_items WHERE id = :id LIMIT 1")
    suspend fun getItem(id: String): HabitChecklistItemEntity?

    @Query("UPDATE habit_checklist_items SET isArchived = 1 WHERE id = :id")
    suspend fun archiveItem(id: String)

    @Query("DELETE FROM habit_checklist_items WHERE habitId = :habitId")
    suspend fun deleteItemsForHabit(habitId: String)

    @Query("SELECT * FROM habit_checklist_items")
    suspend fun getAllItemsForBackup(): List<HabitChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheck(check: HabitChecklistCheckEntity)

    @Query("SELECT * FROM habit_checklist_checks WHERE habitId = :habitId AND dateEpochDay = :epochDay")
    fun observeChecks(habitId: String, epochDay: Long): Flow<List<HabitChecklistCheckEntity>>

    @Query("SELECT * FROM habit_checklist_checks WHERE habitId = :habitId AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay ASC")
    suspend fun getChecksInRange(habitId: String, startEpochDay: Long, endEpochDay: Long): List<HabitChecklistCheckEntity>

    @Query("DELETE FROM habit_checklist_checks WHERE habitId = :habitId")
    suspend fun deleteChecksForHabit(habitId: String)

    @Query("SELECT * FROM habit_checklist_checks")
    suspend fun getAllChecksForBackup(): List<HabitChecklistCheckEntity>
}
