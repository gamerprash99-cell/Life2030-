package com.lifeos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifeos.app.data.db.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * `TaskPriority` is persisted by name ("HIGH"/"MEDIUM"/"LOW"), so a plain
 * `ORDER BY priority` sorts alphabetically (HIGH, LOW, MEDIUM). This CASE
 * expression restores the intended numeric ordering.
 */
private const val PRIORITY_ORDER =
    "CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 WHEN 'LOW' THEN 2 ELSE 3 END"

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("""
        SELECT * FROM tasks WHERE isDeleted = 0 AND dueDateEpochDay = :epochDay
        ORDER BY isCompleted ASC, $PRIORITY_ORDER ASC, dueTimeMinutes ASC
    """)
    fun observeForDay(epochDay: Long): Flow<List<TaskEntity>>

    /**
     * A task counts as overdue when it is unfinished and either its due date is
     * in the past, or it is due today with a due time that has already passed.
     * Uses numeric priority ordering, since `priority` is persisted as a string.
     */
    @Query("""
        SELECT * FROM tasks
        WHERE isDeleted = 0 AND isCompleted = 0 AND (
            dueDateEpochDay < :todayEpochDay
            OR (dueDateEpochDay = :todayEpochDay AND dueTimeMinutes IS NOT NULL AND dueTimeMinutes < :nowMinutes)
        )
        ORDER BY dueDateEpochDay ASC, $PRIORITY_ORDER ASC, dueTimeMinutes ASC
    """)
    fun observeOverdue(todayEpochDay: Long, nowMinutes: Int): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks WHERE isDeleted = 0
        ORDER BY isCompleted ASC, $PRIORITY_ORDER ASC, dueDateEpochDay ASC
    """)
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    /**
     * Completed tasks whose completion timestamp fell inside [startMillis, endMillis] —
     * the retained Timeline feature's source of data.
     */
    @Query("""
        SELECT * FROM tasks
        WHERE isDeleted = 0 AND isCompleted = 1
          AND completedAtEpochMillis >= :startMillis AND completedAtEpochMillis <= :endMillis
        ORDER BY completedAtEpochMillis ASC
    """)
    suspend fun getCompletedBetween(startMillis: Long, endMillis: Long): List<TaskEntity>

    @Query("UPDATE tasks SET isCompleted = :completed, completedAtEpochMillis = :completedAt, updatedAt = :now WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean, completedAt: Long?, now: Long)

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE tasks SET dueDateEpochDay = :newEpochDay, updatedAt = :now WHERE id = :id")
    suspend fun reschedule(id: String, newEpochDay: Long, now: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0 AND dueDateEpochDay = :epochDay")
    fun observeCountForDay(epochDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0 AND dueDateEpochDay = :epochDay AND isCompleted = 1")
    fun observeCompletedCountForDay(epochDay: Long): Flow<Int>

    @Query("SELECT * FROM tasks")
    suspend fun getAllForBackup(): List<TaskEntity>
}
