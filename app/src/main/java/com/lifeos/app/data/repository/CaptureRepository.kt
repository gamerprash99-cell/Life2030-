package com.lifeos.app.data.repository

import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.data.db.dao.CaptureDao
import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.CaptureType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Persistence boundary for Life Captures.
 *
 * File-backed captures (photo/video/audio) own an app-private file; deleting
 * the capture deletes that file, on [io], so a removed memory does not leave
 * bytes orphaned on disk. Text-backed captures (thought/mood/location) own no
 * file.
 */
class CaptureRepository(
    private val dao: CaptureDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {

    fun observeAll(): Flow<List<CaptureEntity>> = dao.observeAll()
    fun observeForType(type: String): Flow<List<CaptureEntity>> = dao.observeForType(type)
    fun observeFavorites(): Flow<List<CaptureEntity>> = dao.observeFavorites()
    fun observeById(id: String): Flow<CaptureEntity?> = dao.observeById(id)

    suspend fun getById(id: String): CaptureEntity? = dao.getById(id)

    suspend fun create(
        type: String,
        text: String? = null,
        filePath: String? = null,
        mimeType: String? = null,
        durationMillis: Long? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        placeLabel: String? = null,
        moodKey: String? = null,
        tags: List<String> = emptyList()
    ): String {
        val id = IdGenerator.newId()
        withContext(io) {
            dao.upsert(
                CaptureEntity(
                    id = id,
                    type = type,
                    text = text?.trim()?.takeIf { it.isNotEmpty() },
                    filePath = filePath,
                    mimeType = mimeType,
                    durationMillis = durationMillis,
                    latitude = latitude,
                    longitude = longitude,
                    placeLabel = placeLabel,
                    moodKey = moodKey,
                    tagsCsv = tags.joinToString(","),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        return id
    }

    suspend fun setFavorite(id: String, favorite: Boolean) = withContext(io) {
        val existing = dao.getById(id) ?: return@withContext
        dao.upsert(existing.copy(isFavorite = favorite))
    }

    /** Deletes the capture and the file it owns, if any. */
    suspend fun delete(id: String) = withContext(io) {
        dao.getById(id)?.filePath?.let { MediaStorage.deleteIfExists(it) }
        dao.delete(id)
    }

    suspend fun getAll(): List<CaptureEntity> = withContext(io) { dao.getAll() }

    suspend fun getAllInRange(fromMillis: Long, toMillis: Long): List<CaptureEntity> = withContext(io) {
        val from = minOf(fromMillis, toMillis)
        val to = maxOf(fromMillis, toMillis)
        dao.getAllInRange(from, to)
    }

    suspend fun countForType(type: String): Int = withContext(io) { dao.countForType(type) }

    suspend fun search(query: String): List<CaptureEntity> = withContext(io) {
        if (query.isBlank()) return@withContext emptyList()
        dao.search(escapeLikePattern(query.trim()))
    }

    suspend fun getAllForBackup(): List<CaptureEntity> = dao.getAllForBackup()

    suspend fun restoreFromBackup(captures: List<CaptureEntity>) = withContext(io) {
        captures.forEach { dao.upsert(it) }
    }

    internal fun escapeLikePattern(raw: String): String = raw
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    companion object {
        val FILTER_TYPES = CaptureType.ALL
    }
}
