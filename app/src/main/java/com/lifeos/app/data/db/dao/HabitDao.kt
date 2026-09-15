package com.lifeos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<HabitEntity?>

    @Query("UPDATE habits SET isArchived = 1, updatedAt = :now WHERE id = :id")
    suspend fun archive(id: String, now: Long)

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

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :epochDay LIMIT 1")
    suspend fun get(habitId: String, epochDay: Long): HabitCompletionEntity?

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :epochDay LIMIT 1")
    fun observe(habitId: String, epochDay: Long): Flow<HabitCompletionEntity?>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY dateEpochDay ASC")
    fun observeAllForHabit(habitId: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay ASC")
    suspend fun getForHabitInRange(habitId: String, startEpochDay: Long, endEpochDay: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE dateEpochDay = :epochDay")
    fun observeAllForDay(epochDay: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun getAllInRange(startEpochDay: Long, endEpochDay: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions")
    suspend fun getAllForBackup(): List<HabitCompletionEntity>
}
