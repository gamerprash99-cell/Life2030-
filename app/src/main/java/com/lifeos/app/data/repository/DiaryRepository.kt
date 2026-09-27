package com.lifeos.app.data.repository

import com.lifeos.app.core.util.IdGenerator
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.data.db.dao.DiaryDao
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryAttachments
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * The Diary's persistence boundary: rows live in Room, their photos and voice
 * notes live as files in app-private storage.
 *
 * Every write here reads a row, decodes its attachment list, touches the
 * filesystem and writes the row back. Room moves *its own* suspend calls onto
 * its query executor, but the Kotlin in between — the JSON decode, the encode,
 * and especially `MediaStorage.deleteIfExists`, which is a blocking
 * `File.delete()` — would otherwise run on whatever thread called in. Every
 * caller is a `viewModelScope` (Main), so without the switch below a single
 * "remove this photo" tap would unlink a file on the UI thread. Doing it here
 * rather than at each call site fixes every caller at once and keeps the
 * threading contract with the layer that owns the storage.
 *
 * [io] is injectable so tests can drive the storage work on a test scheduler
 * instead of racing a real thread pool; production always uses [Dispatchers.IO].
 */
class DiaryRepository(
    private val dao: DiaryDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {

    fun observeAll(): Flow<List<DiaryEntity>> = dao.observeAll()
    fun observeForDay(epochDay: Long): Flow<List<DiaryEntity>> = dao.observeForDay(epochDay)

    suspend fun getById(id: String): DiaryEntity? = dao.getById(id)

    /**
     * Creates an entry from the full composer state.
     *
     * [attachments] are the real photos / voice note / place the user attached;
     * they are serialised into the existing `attachmentsJson` column, so no
     * schema change is needed for media. The entry keeps the timestamp the user
     * picked in the editor (not "now"), so back-dated entries stay back-dated.
     */
    suspend fun createEntry(
        title: String?,
        content: String,
        mood: String?,
        tags: List<String>,
        dateEpochDay: Long,
        timeMinutes: Int,
        attachments: List<DiaryAttachment> = emptyList(),
        aiGenerated: Boolean = false
    ): String {
        val id = IdGenerator.newId()
        val now = System.currentTimeMillis()
        withContext(io) {
            dao.upsert(
                DiaryEntity(
                    id = id, title = title, content = content, mood = mood, tagsCsv = tags.joinToString(","),
                    dateEpochDay = dateEpochDay, timeMinutes = timeMinutes, aiGenerated = aiGenerated,
                    isReviewed = !aiGenerated, // Rule #8: AI drafts start unreviewed until the user confirms
                    attachmentsJson = DiaryAttachments.encode(attachments),
                    isFavorite = false,
                    createdAt = now, updatedAt = now
                )
            )
        }
        return id
    }

    /**
     * Updates an entry, preserving its favourite flag and creation time.
     *
     * When [attachments] is supplied it replaces the stored set and deletes any
     * media file the user removed, so discarding a photo or a voice note also
     * reclaims its storage instead of orphaning it on disk.
     */
    suspend fun updateEntry(
        id: String,
        title: String?,
        content: String,
        mood: String?,
        tags: List<String>,
        dateEpochDay: Long? = null,
        timeMinutes: Int? = null,
        attachments: List<DiaryAttachment>? = null
    ) {
        withContext(io) {
            val existing = dao.getById(id) ?: return@withContext
            val previous = DiaryAttachments.decode(existing.attachmentsJson)
            val nextJson = attachments?.let { DiaryAttachments.encode(it) } ?: existing.attachmentsJson

            if (attachments != null) deleteOrphanedMedia(previous, attachments)

            dao.upsert(
                existing.copy(
                    title = title,
                    content = content,
                    mood = mood,
                    tagsCsv = tags.joinToString(","),
                    dateEpochDay = dateEpochDay ?: existing.dateEpochDay,
                    timeMinutes = timeMinutes ?: existing.timeMinutes,
                    attachmentsJson = nextJson,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** Flips the favourite flag, returning the value actually persisted. */
    suspend fun setFavorite(id: String, favorite: Boolean) {
        val existing = dao.getById(id) ?: return
        dao.upsert(existing.copy(isFavorite = favorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleFavorite(id: String): Boolean {
        val existing = dao.getById(id) ?: return false
        val next = !existing.isFavorite
        setFavorite(id, next)
        return next
    }

    /**
     * Deletes an entry and the media files it owns. Entry content, photos and
     * audio are user data the user explicitly asked to remove, so nothing is
     * left behind on disk.
     */
    suspend fun delete(id: String) = withContext(io) {
        dao.getById(id)?.let { existing ->
            DiaryAttachments.decode(existing.attachmentsJson).forEach { attachment ->
                when (attachment) {
                    is DiaryAttachment.Photo -> MediaStorage.deleteIfExists(attachment.filePath)
                    is DiaryAttachment.VoiceNote -> MediaStorage.deleteIfExists(attachment.filePath)
                    // A Place has no file of its own; its coordinates live only
                    // in the row being deleted.
                    is DiaryAttachment.Place -> Unit
                }
            }
        }
        dao.delete(id)
    }

    /** Removes media present in [previous] but absent from [next]. */
    private fun deleteOrphanedMedia(
        previous: List<DiaryAttachment>,
        next: List<DiaryAttachment>
    ) {
        val keptPaths = next.mapNotNull { attachment ->
            when (attachment) {
                is DiaryAttachment.Photo -> attachment.filePath
                is DiaryAttachment.VoiceNote -> attachment.filePath
                is DiaryAttachment.Place -> null
            }
        }.toSet()

        previous.forEach { attachment ->
            val path = when (attachment) {
                is DiaryAttachment.Photo -> attachment.filePath
                is DiaryAttachment.VoiceNote -> attachment.filePath
                is DiaryAttachment.Place -> null
            }
            if (path != null && path !in keptPaths) MediaStorage.deleteIfExists(path)
        }
    }

    /**
     * Strips a single attachment from an entry and deletes the backing file once
     * nothing references it. The detail screen uses this so the user can drop a
     * photo or voice note in place, without opening the composer.
     *
     * A no-op when the entry is gone or the path is not one of its attachments,
     * so a stale tap can never clobber the row.
     */
    suspend fun removeAttachment(id: String, filePath: String) = withContext(io) {
        val existing = dao.getById(id) ?: return@withContext
        val current = DiaryAttachments.decode(existing.attachmentsJson)
        val next = current.filterNot { attachment ->
            (attachment is DiaryAttachment.Photo && attachment.filePath == filePath) ||
                (attachment is DiaryAttachment.VoiceNote && attachment.filePath == filePath)
        }
        if (next.size == current.size) return@withContext
        deleteOrphanedMedia(current, next)
        dao.upsert(
            existing.copy(
                attachmentsJson = DiaryAttachments.encode(next),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getAllForBackup(): List<DiaryEntity> = dao.getAllForBackup()
    suspend fun restoreFromBackup(entries: List<DiaryEntity>) = withContext(io) {
        entries.forEach { dao.upsert(it) }
    }
}
