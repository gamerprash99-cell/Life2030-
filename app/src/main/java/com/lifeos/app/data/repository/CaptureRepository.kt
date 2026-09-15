package com.lifeos.app.data.repository

import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.data.db.dao.CaptureDao
import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.CaptureType
import kotlinx.coroutines.flow.Flow

class CaptureRepository(private val dao: CaptureDao) {

    fun observeLatest(): Flow<CaptureEntity?> = dao.observeLatest()
    fun observeAll(): Flow<List<CaptureEntity>> = dao.observeAll()
    fun observeForDay(epochDay: Long): Flow<List<CaptureEntity>> = dao.observeForDay(epochDay)
    suspend fun getById(id: String): CaptureEntity? = dao.getById(id)

    suspend fun addCapture(
        type: CaptureType,
        filePath: String?,
        thumbnailPath: String? = null,
        caption: String? = null,
        transcript: String? = null,
        mood: String? = null,
        tags: List<String> = emptyList(),
        dateEpochDay: Long,
        timeMinutes: Int
    ): String {
        val id = IdGenerator.newId()
        dao.upsert(
            CaptureEntity(
                id = id, type = type, filePath = filePath, thumbnailPath = thumbnailPath,
                caption = caption, transcript = transcript, mood = mood, tagsCsv = tags.joinToString(","),
                dateEpochDay = dateEpochDay, timeMinutes = timeMinutes, createdAt = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun countInRange(startEpochDay: Long, endEpochDay: Long): Int = dao.countInRange(startEpochDay, endEpochDay)

    suspend fun search(query: String): List<CaptureEntity> {
        if (query.isBlank()) return emptyList()
        return dao.search(query)
    }

    suspend fun getAllForBackup(): List<CaptureEntity> = dao.getAllForBackup()
    suspend fun restoreFromBackup(captures: List<CaptureEntity>) = captures.forEach { dao.upsert(it) }
}
