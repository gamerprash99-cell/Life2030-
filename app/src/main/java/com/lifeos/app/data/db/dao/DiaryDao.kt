package com.lifeos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeos.app.data.db.entities.DiaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DiaryEntity)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM diary_entries ORDER BY dateEpochDay DESC, timeMinutes DESC")
    fun observeAll(): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diary_entries WHERE dateEpochDay = :epochDay ORDER BY timeMinutes DESC")
    fun observeForDay(epochDay: Long): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diary_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DiaryEntity?

    @Query("SELECT * FROM diary_entries WHERE aiGenerated = 1 AND isReviewed = 0 ORDER BY createdAt DESC")
    fun observeUnreviewedAiDrafts(): Flow<List<DiaryEntity>>

    @Query("UPDATE diary_entries SET isReviewed = 1, updatedAt = :now WHERE id = :id")
    suspend fun markReviewed(id: String, now: Long)

    @Query("SELECT COUNT(*) FROM diary_entries WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun countInRange(startEpochDay: Long, endEpochDay: Long): Int

    @Query("SELECT * FROM diary_entries WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay ASC, timeMinutes ASC")
    suspend fun getInRange(startEpochDay: Long, endEpochDay: Long): List<DiaryEntity>

    @Query("SELECT * FROM diary_entries WHERE content LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<DiaryEntity>

    @Query("SELECT MAX(dateEpochDay) FROM diary_entries")
    suspend fun getLastEntryEpochDay(): Long?

    @Query("SELECT * FROM diary_entries")
    suspend fun getAllForBackup(): List<DiaryEntity>
}
