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

    @Query("SELECT * FROM captures ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE type = :type ORDER BY createdAt DESC")
    fun observeForType(type: String): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CaptureEntity?

    @Query("SELECT * FROM captures WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<CaptureEntity?>

    @Query("SELECT * FROM captures")
    suspend fun getAllForBackup(): List<CaptureEntity>

    @Query("SELECT * FROM captures")
    suspend fun getAll(): List<CaptureEntity>

    @Query("SELECT * FROM captures WHERE createdAt BETWEEN :fromMillis AND :toMillis ORDER BY createdAt DESC")
    suspend fun getAllInRange(fromMillis: Long, toMillis: Long): List<CaptureEntity>

    @Query(
        "SELECT * FROM captures WHERE " +
            "(text LIKE :pattern ESCAPE '\\' " +
            " OR placeLabel LIKE :pattern ESCAPE '\\' " +
            " OR tagsCsv LIKE :pattern ESCAPE '\\') " +
            "ORDER BY createdAt DESC"
    )
    suspend fun search(pattern: String): List<CaptureEntity>

    @Query("SELECT COUNT(*) FROM captures WHERE type = :type")
    suspend fun countForType(type: String): Int
}
