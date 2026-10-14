package com.lifeos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeos.app.data.db.entities.NoteFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteFolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: NoteFolderEntity)

    @Query("DELETE FROM note_folders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM note_folders ORDER BY position ASC, name ASC")
    fun observeAll(): Flow<List<NoteFolderEntity>>

    @Query("SELECT * FROM note_folders ORDER BY position ASC, name ASC")
    suspend fun getAll(): List<NoteFolderEntity>

    @Query("SELECT * FROM note_folders WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): NoteFolderEntity?

    @Query("SELECT COUNT(*) FROM note_folders")
    suspend fun count(): Int
}
