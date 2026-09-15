package com.lifeos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifeos.app.data.db.entities.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isPinned = 1 ORDER BY updatedAt DESC")
    fun observePinned(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun observeTrash(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND folder = :folder ORDER BY updatedAt DESC")
    fun observeByFolder(folder: String): Flow<List<NoteEntity>>

    @Query("SELECT DISTINCT folder FROM notes WHERE folder IS NOT NULL AND isDeleted = 0")
    fun observeFolders(): Flow<List<String>>

    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' OR plainTextForSearch LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    suspend fun search(query: String): List<NoteEntity>

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, now: Long)

    @Query("UPDATE notes SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, now: Long)

    @Query("UPDATE notes SET isArchived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long)

    @Query("UPDATE notes SET isDeleted = :deleted, updatedAt = :now WHERE id = :id")
    suspend fun setDeleted(id: String, deleted: Boolean, now: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 0")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND createdAt BETWEEN :startMillis AND :endMillis")
    suspend fun getCreatedBetween(startMillis: Long, endMillis: Long): List<NoteEntity>

    @Query("SELECT * FROM notes")
    suspend fun getAllForBackup(): List<NoteEntity>
}
