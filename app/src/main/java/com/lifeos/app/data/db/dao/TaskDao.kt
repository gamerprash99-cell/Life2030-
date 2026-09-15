package com.lifeos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifeos.app.data.db.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND dueDateEpochDay = :epochDay ORDER BY isCompleted ASC, priority ASC, dueTimeMinutes ASC")
    fun observeForDay(epochDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND isCompleted = 0 AND dueDateEpochDay < :todayEpochDay ORDER BY dueDateEpochDay ASC")
    fun observeOverdue(todayEpochDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY isCompleted ASC, dueDateEpochDay ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("UPDATE tasks SET isCompleted = :completed, completedAtEpochMillis = :completedAt, updatedAt = :now WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean, completedAt: Long?, now: Long)

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE tasks SET dueDateEpochDay = :newEpochDay, updatedAt = :now WHERE id = :id")
    suspend fun reschedule(id: String, newEpochDay: Long, now: Long)

    @Query("""
        SELECT * FROM tasks
        WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY dueDateEpochDay ASC
    """)
    suspend fun search(query: String): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0 AND isCompleted = 1 AND completedAtEpochMillis BETWEEN :startMillis AND :endMillis")
    suspend fun countCompletedBetween(startMillis: Long, endMillis: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0 AND dueDateEpochDay = :epochDay")
    fun observeCountForDay(epochDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0 AND dueDateEpochDay = :epochDay AND isCompleted = 1")
    fun observeCompletedCountForDay(epochDay: Long): Flow<Int>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND createdAt BETWEEN :startMillis AND :endMillis")
    suspend fun getCreatedBetween(startMillis: Long, endMillis: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun getAllForBackup(): List<TaskEntity>
}
