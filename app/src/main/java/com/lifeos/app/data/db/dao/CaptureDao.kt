package com.lifeos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeos.app.data.db.entities.CaptureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(capture: CaptureEntity)

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM captures ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(): Flow<CaptureEntity?>

    @Query("SELECT * FROM captures ORDER BY dateEpochDay DESC, timeMinutes DESC")
    fun observeAll(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE dateEpochDay = :epochDay ORDER BY timeMinutes DESC")
    fun observeForDay(epochDay: Long): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CaptureEntity?

    @Query("SELECT COUNT(*) FROM captures WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun countInRange(startEpochDay: Long, endEpochDay: Long): Int

    @Query("SELECT * FROM captures WHERE caption LIKE '%' || :query || '%' OR transcript LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<CaptureEntity>

    @Query("SELECT * FROM captures")
    suspend fun getAllForBackup(): List<CaptureEntity>
}
