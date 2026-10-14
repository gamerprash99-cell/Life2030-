package com.lifeos.app.data.repository

import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.data.db.dao.NoteDao
import com.lifeos.app.data.db.dao.NoteFolderDao
import com.lifeos.app.data.db.entities.NoteEntity
import com.lifeos.app.data.db.entities.NoteFolderEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Persistence boundary for Smart Notes and their folders.
 *
 * Folder deletion detaches its notes (sets folderId to null) rather than
 * deleting them — a note is the user's writing and must survive the folder
 * housekeeping that organised it. The detach happens inside one
 * [withContext] block so the UI never observes a folder gone while its notes
 * still claim it.
 */
class NoteRepository(
    private val dao: NoteDao,
    private val folderDao: NoteFolderDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {

    // ---- notes ------------------------------------------------------------

    fun observeActive(): Flow<List<NoteEntity>> = dao.observeActive()
    fun observeArchived(): Flow<List<NoteEntity>> = dao.observeArchived()
    fun observeTrashed(): Flow<List<NoteEntity>> = dao.observeTrashed()
    fun observePinned(): Flow<List<NoteEntity>> = dao.observePinned()
    fun observeFavorites(): Flow<List<NoteEntity>> = dao.observeFavorites()
    fun observeForFolder(folderId: String): Flow<List<NoteEntity>> = dao.observeForFolder(folderId)
    fun observeById(id: String): Flow<NoteEntity?> = dao.observeById(id)

    suspend fun getById(id: String): NoteEntity? = dao.getById(id)

    suspend fun create(
        title: String,
        content: String,
        tags: List<String>,
        folderId: String?
    ): String {
        val id = IdGenerator.newId()
        val now = System.currentTimeMillis()
        withContext(io) {
            dao.upsert(
                NoteEntity(
                    id = id,
                    title = title.trim(),
                    content = content,
                    tagsCsv = normalizeTags(tags),
                    folderId = folderId,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        return id
    }

    suspend fun update(
        id: String,
        title: String,
        content: String,
        tags: List<String>,
        folderId: String?
    ) = withContext(io) {
        val existing = dao.getById(id) ?: return@withContext
        dao.upsert(
            existing.copy(
                title = title.trim(),
                content = content,
                tagsCsv = normalizeTags(tags),
                folderId = folderId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** Moves a note to the recycle bin (soft delete). */
    suspend fun trash(id: String) = withContext(io) {
        val existing = dao.getById(id) ?: return@withContext
        dao.upsert(existing.copy(isTrashed = true, trashedAt = System.currentTimeMillis()))
    }

    suspend fun restore(id: String) = withContext(io) {
        val existing = dao.getById(id) ?: return@withContext
        dao.upsert(existing.copy(isTrashed = false, trashedAt = null))
    }

    suspend fun deleteForever(id: String) = withContext(io) { dao.delete(id) }

    suspend fun setPinned(id: String, pinned: Boolean) = setFlag(id) { it.copy(isPinned = pinned) }
    suspend fun setFavorite(id: String, favorite: Boolean) = setFlag(id) { it.copy(isFavorite = favorite) }
    suspend fun setArchived(id: String, archived: Boolean) = setFlag(id) { it.copy(isArchived = archived) }

    private suspend fun setFlag(id: String, transform: (NoteEntity) -> NoteEntity) = withContext(io) {
        val existing = dao.getById(id) ?: return@withContext
        dao.upsert(transform(existing).copy(updatedAt = System.currentTimeMillis()))
    }

    // ---- folders ----------------------------------------------------------

    fun observeFolders(): Flow<List<NoteFolderEntity>> = folderDao.observeAll()

    /** Creates a folder. Returns null when a folder with that name already exists. */
    suspend fun createFolder(name: String): String? = withContext(io) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext null
        if (folderDao.getByName(trimmed) != null) return@withContext null
        val id = IdGenerator.newId()
        folderDao.upsert(
            NoteFolderEntity(
                id = id,
                name = trimmed,
                position = folderDao.count(),
                createdAt = System.currentTimeMillis()
            )
        )
        id
    }

    /** Deletes a folder and detaches (not deletes) the notes inside it. */
    suspend fun deleteFolder(id: String) = withContext(io) {
        dao.getAllActive().filter { it.folderId == id }.forEach { note ->
            dao.upsert(note.copy(folderId = null, updatedAt = System.currentTimeMillis()))
        }
        folderDao.delete(id)
    }

    suspend fun renameFolder(id: String, name: String) = withContext(io) {
        val folders = folderDao.getAll()
        val existing = folders.firstOrNull { it.id == id } ?: return@withContext
        val trimmed = name.trim()
        if (trimmed.isEmpty() || folders.any { it.id != id && it.name == trimmed }) return@withContext
        folderDao.upsert(existing.copy(name = trimmed))
    }

    // ---- search / backup --------------------------------------------------

    suspend fun search(query: String): List<NoteEntity> = withContext(io) {
        if (query.isBlank()) return@withContext emptyList()
        dao.search(escapeLikePattern(query.trim()))
    }

    suspend fun getAllForBackup(): List<NoteEntity> = dao.getAllForBackup()
    suspend fun getAllFoldersForBackup(): List<NoteFolderEntity> = folderDao.getAll()

    suspend fun restoreFromBackup(notes: List<NoteEntity>, folders: List<NoteFolderEntity>) = withContext(io) {
        folders.forEach { folderDao.upsert(it) }
        notes.forEach { dao.upsert(it) }
    }

    private fun normalizeTags(tags: List<String>): String =
        tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")

    internal fun escapeLikePattern(raw: String): String = raw
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
}
