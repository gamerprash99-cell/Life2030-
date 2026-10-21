package com.lifeos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeos.app.core.database.entity.AttachmentEntity
import com.lifeos.app.core.database.entity.CaptureEntity
import com.lifeos.app.core.database.entity.LifeEventEntity
import kotlinx.coroutines.flow.Flow

/** Captures (photo/video/audio/thought/mood/location) and their attachments. */
@Dao
interface CaptureDao {
    @Query("SELECT * FROM captures WHERE deletedAt = 0 ORDER BY occurredOn DESC, createdAt DESC")
    fun observeAll(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE deletedAt = 0 AND type = :type " +
        "ORDER BY occurredOn DESC, createdAt DESC")
    fun observeByType(type: String): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun getById(id: String): CaptureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(capture: CaptureEntity)

    @Query("UPDATE captures SET deletedAt = :at WHERE id = :id AND deletedAt = 0")
    suspend fun softDelete(id: String, at: Long)

    @Query("UPDATE captures SET deletedAt = 0, updatedAt = :updatedAt WHERE id = :id AND deletedAt != 0")
    suspend fun restore(id: String, updatedAt: Long)

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun deleteForever(id: String)

    @Query("SELECT * FROM captures WHERE deletedAt != 0 ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId AND deletedAt = 0")
    fun observeAttachments(ownerType: String, ownerId: String): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachment(attachment: AttachmentEntity)

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getAttachment(id: String): AttachmentEntity?

    @Query("SELECT relativePath FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun attachmentPaths(ownerType: String, ownerId: String): List<String>

    @Query("UPDATE attachments SET deletedAt = :at WHERE id = :id")
    suspend fun softDeleteAttachment(id: String, at: Long)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachmentForever(id: String)
}

/** Timeline. Newest first; grouped by `occurredOn` in the UI. */
@Dao
interface LifeEventDao {
    @Query("SELECT * FROM life_events WHERE deletedAt = 0 ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<LifeEventEntity>>

    @Query("SELECT * FROM life_events WHERE deletedAt = 0 AND occurredOn = :date ORDER BY occurredAt DESC")
    fun observeOn(date: String): Flow<List<LifeEventEntity>>

    @Query("SELECT * FROM life_events WHERE deletedAt = 0 " +
        "AND occurredOn BETWEEN :from AND :to ORDER BY occurredAt DESC")
    fun observeBetween(from: String, to: String): Flow<List<LifeEventEntity>>

    @Query("SELECT * FROM life_events WHERE sourceType = :sourceType AND sourceId = :sourceId " +
        "AND deletedAt = 0 ORDER BY occurredAt DESC")
    fun observeForSource(sourceType: String, sourceId: String): Flow<List<LifeEventEntity>>

    @Query("SELECT * FROM life_events WHERE id = :id")
    suspend fun getById(id: String): LifeEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: LifeEventEntity)

    @Query("UPDATE life_events SET deletedAt = :at WHERE id = :id")
    suspend fun softDelete(id: String, at: Long)

    @Query("DELETE FROM life_events WHERE id = :id")
    suspend fun deleteForever(id: String)

    @Query("DELETE FROM life_events WHERE sourceType = :sourceType AND sourceId = :sourceId")
    suspend fun deleteForSource(sourceType: String, sourceId: String)

    @Query("SELECT type, COUNT(*) AS count FROM life_events " +
        "WHERE deletedAt = 0 AND occurredOn BETWEEN :from AND :to GROUP BY type")
    suspend fun countsBetween(from: String, to: String): List<TypeCount>
}

data class TypeCount(val type: String, val count: Int)
