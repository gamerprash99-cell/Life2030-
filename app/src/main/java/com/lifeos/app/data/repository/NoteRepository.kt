package com.lifeos.app.data.repository

import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.data.db.dao.NoteDao
import com.lifeos.app.data.db.entities.NoteEntity
import com.lifeos.app.domain.model.NoteBlock
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NoteRepository(private val dao: NoteDao) {

    private val json = Json { ignoreUnknownKeys = true }

    fun observeAll(): Flow<List<NoteEntity>> = dao.observeAll()
    fun observePinned(): Flow<List<NoteEntity>> = dao.observePinned()
    fun observeFavorites(): Flow<List<NoteEntity>> = dao.observeFavorites()
    fun observeArchived(): Flow<List<NoteEntity>> = dao.observeArchived()
    fun observeTrash(): Flow<List<NoteEntity>> = dao.observeTrash()
    fun observeByFolder(folder: String): Flow<List<NoteEntity>> = dao.observeByFolder(folder)
    fun observeFolders(): Flow<List<String>> = dao.observeFolders()
    fun observeById(id: String): Flow<NoteEntity?> = dao.observeById(id)
    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun getById(id: String): NoteEntity? = dao.getById(id)

    suspend fun createNote(
        title: String,
        blocks: List<NoteBlock>,
        folder: String? = null,
        tags: List<String> = emptyList()
    ): String {
        val id = IdGenerator.newId()
        val now = System.currentTimeMillis()
        val entity = NoteEntity(
            id = id,
            title = title,
            contentJson = json.encodeToString(blocks),
            plainTextForSearch = flattenBlocks(blocks),
            folder = folder,
            tagsCsv = tags.joinToString(","),
            createdAt = now,
            updatedAt = now
        )
        dao.upsert(entity)
        return id
    }

    suspend fun updateNoteContent(id: String, title: String, blocks: List<NoteBlock>) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                title = title,
                contentJson = json.encodeToString(blocks),
                plainTextForSearch = flattenBlocks(blocks),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setFolder(id: String, folder: String?) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(folder = folder, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setTags(id: String, tags: List<String>) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(tagsCsv = tags.joinToString(","), updatedAt = System.currentTimeMillis()))
    }

    suspend fun togglePin(id: String, pinned: Boolean) = dao.setPinned(id, pinned, System.currentTimeMillis())
    suspend fun toggleFavorite(id: String, favorite: Boolean) = dao.setFavorite(id, favorite, System.currentTimeMillis())
    suspend fun archive(id: String, archived: Boolean) = dao.setArchived(id, archived, System.currentTimeMillis())

    /** Soft delete → moves to Trash, supports "Restore notes" (Section 7). */
    suspend fun moveToTrash(id: String) = dao.setDeleted(id, true, System.currentTimeMillis())
    suspend fun restoreFromTrash(id: String) = dao.setDeleted(id, false, System.currentTimeMillis())

    /** Permanent delete — caller (UI) is responsible for requiring confirmation (Rule #10). */
    suspend fun permanentlyDelete(id: String) = dao.hardDelete(id)

    suspend fun search(query: String): List<NoteEntity> {
        if (query.isBlank()) return emptyList()
        return dao.search(query)
    }

    fun decodeBlocks(note: NoteEntity): List<NoteBlock> = try {
        json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(NoteBlock.serializer()), note.contentJson)
    } catch (e: Exception) {
        emptyList()
    }

    private fun flattenBlocks(blocks: List<NoteBlock>): String = blocks.joinToString(" ") { block ->
        when (block) {
            is NoteBlock.Paragraph -> block.text
            is NoteBlock.Heading -> block.text
            is NoteBlock.BulletItem -> block.text
            is NoteBlock.NumberedItem -> block.text
            is NoteBlock.ChecklistItem -> block.text
        }
    }

    suspend fun getAllForBackup(): List<NoteEntity> = dao.getAllForBackup()
    suspend fun restoreFromBackup(notes: List<NoteEntity>) = notes.forEach { dao.upsert(it) }
}
