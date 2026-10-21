package com.lifeos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lifeos.app.core.database.entity.EntityTagCrossRef
import com.lifeos.app.core.database.entity.FolderEntity
import com.lifeos.app.core.database.entity.NoteEntity
import com.lifeos.app.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * Notes, folders and tags. Trash semantics: every query that surfaces content to
 * the user filters `deletedAt = 0`; restore is `UPDATE ... SET deletedAt = 0`.
 */
@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE deletedAt = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE deletedAt = 0 AND id = :id")
    suspend fun getLiveById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Query(
        "UPDATE notes SET title = :title, body = :body, updatedAt = :updatedAt " +
            "WHERE id = :id AND deletedAt = 0",
    )
    suspend fun updateContent(id: String, title: String, body: String, updatedAt: Long)

    @Query("UPDATE notes SET pinned = :pinned, updatedAt = :updatedAt WHERE id = :id AND deletedAt = 0")
    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET folderId = :folderId, updatedAt = :updatedAt WHERE id = :id AND deletedAt = 0")
    suspend fun setFolder(id: String, folderId: String?, updatedAt: Long)

    @Query("UPDATE notes SET deletedAt = :at WHERE id = :id AND deletedAt = 0")
    suspend fun softDelete(id: String, at: Long)

    @Query("UPDATE notes SET deletedAt = 0, updatedAt = :updatedAt WHERE id = :id AND deletedAt != 0")
    suspend fun restore(id: String, updatedAt: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteForever(id: String)

    @Query("SELECT * FROM notes WHERE deletedAt != 0 ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM notes WHERE deletedAt = 0")
    fun observeLiveCount(): Flow<Int>

    // ---- folders ----

    @Query("SELECT * FROM folders WHERE deletedAt = 0 ORDER BY sortOrder, name")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: FolderEntity)

    @Query("UPDATE folders SET deletedAt = :at WHERE id = :id")
    suspend fun softDeleteFolder(id: String, at: Long)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderForever(id: String)

    // ---- tags ----

    @Query("SELECT * FROM tags ORDER BY name")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findTag(name: String): TagEntity?

    /** Row-per-assignment, used to group tag names by note in one emission. */
    @Query("SELECT et.entityId AS entityId, t.name AS name FROM entity_tags et " +
        "INNER JOIN tags t ON t.id = et.tagId WHERE et.entityType = :entityType")
    fun observeTagNamesForEntity(entityType: String): Flow<List<EntityTagName>>

    @Query("SELECT et.entityId AS entityId, t.name AS name FROM entity_tags et " +
        "INNER JOIN tags t ON t.id = et.tagId WHERE et.entityType = 'notes'")
    fun observeAllNoteTagNames(): Flow<List<EntityTagName>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT t.* FROM tags t INNER JOIN entity_tags et ON et.tagId = t.id " +
        "WHERE et.entityType = :entityType AND et.entityId = :entityId ORDER BY t.name")
    suspend fun tagsFor(entityType: String, entityId: String): List<TagEntity>

    @Query("SELECT t.* FROM tags t INNER JOIN entity_tags et ON et.tagId = t.id " +
        "WHERE et.entityType = :entityType AND et.entityId = :entityId ORDER BY t.name")
    fun observeTagsFor(entityType: String, entityId: String): Flow<List<TagEntity>>

    @Query("DELETE FROM entity_tags WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun clearTags(entityType: String, entityId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntityTags(refs: List<EntityTagCrossRef>)

    /** Replaces the full tag set of an entity atomically. */
    @Transaction
    suspend fun setEntityTags(entityType: String, entityId: String, tagIds: Collection<String>) {
        clearTags(entityType, entityId)
        if (tagIds.isNotEmpty()) {
            insertEntityTags(tagIds.map { EntityTagCrossRef(entityType, entityId, it) })
        }
    }

    /**
     * Finds or creates tag rows for normalized names and returns their ids.
     * Kept here (rather than in the repository) so tag upsert + assignment can
     * share one transaction.
     */
    @Transaction
    suspend fun resolveTagIds(names: Collection<String>): List<String> =
        names.map { name ->
            findTag(name)?.id ?: run {
                val id = com.lifeos.app.core.domain.Ids.new()
                insertTag(TagEntity(id = id, name = name, createdAt = System.currentTimeMillis()))
                findTag(name)?.id ?: id
            }
        }
}
