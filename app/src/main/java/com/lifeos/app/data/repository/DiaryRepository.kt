package com.lifeos.app.data.repository

import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.data.db.dao.DiaryDao
import com.lifeos.app.data.db.entities.DiaryEntity
import kotlinx.coroutines.flow.Flow

class DiaryRepository(private val dao: DiaryDao) {

    fun observeAll(): Flow<List<DiaryEntity>> = dao.observeAll()
    fun observeForDay(epochDay: Long): Flow<List<DiaryEntity>> = dao.observeForDay(epochDay)
    fun observeUnreviewedAiDrafts(): Flow<List<DiaryEntity>> = dao.observeUnreviewedAiDrafts()

    suspend fun getById(id: String): DiaryEntity? = dao.getById(id)

    suspend fun createEntry(
        title: String?,
        content: String,
        mood: String?,
        tags: List<String>,
        dateEpochDay: Long,
        timeMinutes: Int,
        aiGenerated: Boolean = false
    ): String {
        val id = IdGenerator.newId()
        val now = System.currentTimeMillis()
        dao.upsert(
            DiaryEntity(
                id = id, title = title, content = content, mood = mood, tagsCsv = tags.joinToString(","),
                dateEpochDay = dateEpochDay, timeMinutes = timeMinutes, aiGenerated = aiGenerated,
                isReviewed = !aiGenerated, // Rule #8: AI drafts start unreviewed until the user confirms
                createdAt = now, updatedAt = now
            )
        )
        return id
    }

    suspend fun updateEntry(id: String, title: String?, content: String, mood: String?, tags: List<String>) {
        val existing = dao.getById(id) ?: return
        dao.upsert(
            existing.copy(
                title = title, content = content, mood = mood, tagsCsv = tags.joinToString(","),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun approveAiDraft(id: String) = dao.markReviewed(id, System.currentTimeMillis())

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun daysSinceLastEntry(todayEpochDay: Long): Int? {
        val last = dao.getLastEntryEpochDay() ?: return null
        return (todayEpochDay - last).toInt()
    }

    suspend fun countInRange(startEpochDay: Long, endEpochDay: Long): Int = dao.countInRange(startEpochDay, endEpochDay)
    suspend fun getInRange(startEpochDay: Long, endEpochDay: Long): List<DiaryEntity> = dao.getInRange(startEpochDay, endEpochDay)

    suspend fun search(query: String): List<DiaryEntity> {
        if (query.isBlank()) return emptyList()
        return dao.search(query)
    }

    suspend fun getAllForBackup(): List<DiaryEntity> = dao.getAllForBackup()
    suspend fun restoreFromBackup(entries: List<DiaryEntity>) = entries.forEach { dao.upsert(it) }
}
