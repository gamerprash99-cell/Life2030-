package com.lifeos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeos.app.data.db.entities.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: String)

    /** Active (not trashed) notes, pinned first then newest-edited first. */
    @Query(
        "SELECT * FROM notes WHERE isTrashed = 0 " +
            "ORDER BY isPinned DESC, updatedAt DESC"
    )
    fun observeActive(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun observeTrashed(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun observeForFolder(folderId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isPinned = 1 AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun observePinned(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes")
    suspend fun getAllForBackup(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isTrashed = 0")
    suspend fun getAllActive(): List<NoteEntity>

    @Query(
        "SELECT * FROM notes WHERE isTrashed = 0 " +
            "AND (title LIKE :pattern ESCAPE '\\' " +
            "  OR content LIKE :pattern ESCAPE '\\' " +
            "  OR tagsCsv LIKE :pattern ESCAPE '\\') " +
            "ORDER BY updatedAt DESC"
    )
    suspend fun search(pattern: String): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE folderId = :folderId")
    suspend fun countInFolder(folderId: String): Int
}
