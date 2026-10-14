package com.lifeos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeos.app.data.db.entities.ReminderEntity

/**
 * Persistence for the reminders table (AppDatabase v4). Row counts are tiny
 * (one per task/habit that has a reminder), so these are deliberately
 * straightforward single-row CRUD plus the field updates the scheduler state
 * machine needs.
 */
@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun getEnabled(): List<ReminderEntity>

    @Query("UPDATE reminders SET snoozeReturnAtEpochMillis = NULL, updatedAt = :now WHERE id = :id")
    suspend fun clearSnooze(id: String, now: Long)

    @Query("UPDATE reminders SET nextTriggerAtEpochMillis = :nextTriggerAt, updatedAt = :now WHERE id = :id")
    suspend fun advanceTrigger(id: String, nextTriggerAt: Long, now: Long)

    @Query("UPDATE reminders SET snoozeReturnAtEpochMillis = :snoozeReturnAt, updatedAt = :now WHERE id = :id")
    suspend fun setSnoozeReturn(id: String, snoozeReturnAt: Long, now: Long)

    @Query("UPDATE reminders SET enabled = 0, updatedAt = :now WHERE id = :id")
    suspend fun disable(id: String, now: Long)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM reminders WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteForEntity(entityType: String, entityId: String)
}