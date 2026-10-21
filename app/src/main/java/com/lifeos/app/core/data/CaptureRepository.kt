package com.lifeos.app.core.data

import android.content.Context
import android.net.Uri
import com.lifeos.app.core.database.LifeOsDatabase
import com.lifeos.app.core.database.entity.AttachmentEntity
import com.lifeos.app.core.database.entity.CaptureEntity
import com.lifeos.app.core.database.entity.LifeEventEntity
import com.lifeos.app.core.domain.CaptureType
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.Ids
import com.lifeos.app.core.domain.LifeEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Capture repository.
 *
 * Media files are copied from their content-URI origin into the app-private
 * `filesDir/captures/` directory, so a capture keeps working after the temp file
 * behind a URI is gone and no storage permission is ever needed for imports.
 * Only [relativePath] is persisted — absolute paths never enter the database.
 */
class CaptureRepository(
    private val context: Context,
    private val db: LifeOsDatabase,
) {

    private val dao get() = db.captureDao()
    private val events get() = db.lifeEventDao()

    private val captureDir: File
        get() = File(context.filesDir, "captures").apply { mkdirs() }

    fun observeCaptures(): Flow<List<CaptureEntity>> = dao.observeAll()
    fun observeCapturesByType(type: CaptureType): Flow<List<CaptureEntity>> = dao.observeByType(type.code)
    fun observeTrash(): Flow<List<CaptureEntity>> = dao.observeTrash()

    suspend fun get(id: String): CaptureEntity? = dao.getById(id)

    /** Text-only capture (thought / mood / location label). */
    suspend fun saveText(
        type: CaptureType,
        text: String = "",
        moodScore: Int? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        placeLabel: String? = null,
        lifeEventId: String? = null,
    ): String {
        val now = Dates.nowMillis()
        val id = Ids.new()
        val on = Dates.iso(Dates.today())
        dao.upsert(
            CaptureEntity(
                id = id,
                type = type.code,
                text = text.trim(),
                occurredOn = on,
                latitude = latitude,
                longitude = longitude,
                placeLabel = placeLabel,
                moodScore = moodScore,
                lifeEventId = lifeEventId,
                createdAt = now,
                updatedAt = now,
            ),
        )
        events.upsert(
            LifeEventEntity(
                id = Ids.new(),
                type = eventFor(type).code,
                title = titleFor(type, text, placeLabel),
                detail = text.take(140),
                occurredOn = on,
                occurredAt = now,
                sourceType = "captures",
                sourceId = id,
            ),
        )
        return id
    }

    /**
     * Imports a picked media file. Returns the capture id, or null when the
     * source could not be read.
     */
    suspend fun saveMedia(
        type: CaptureType,
        source: Uri,
        mimeType: String,
        displayName: String,
    ): String? {
        val bytes = runCatching {
            context.contentResolver.openInputStream(source)?.use { it.readBytes() }
        }.getOrNull() ?: return null

        val now = Dates.nowMillis()
        val id = Ids.new()
        val on = Dates.iso(Dates.today())
        val ext = extensionFor(mimeType, displayName)
        val fileName = "$id.$ext"
        val target = File(captureDir, fileName)
        val written = runCatching { target.writeBytes(bytes) }.isSuccess
        if (!written) return null

        dao.upsert(
            CaptureEntity(
                id = id,
                type = type.code,
                text = displayName,
                occurredOn = on,
                createdAt = now,
                updatedAt = now,
            ),
        )
        dao.upsertAttachment(
            AttachmentEntity(
                id = Ids.new(),
                ownerType = "captures",
                ownerId = id,
                mimeType = mimeType.ifBlank { "application/octet-stream" },
                relativePath = "captures/$fileName",
                displayName = displayName.ifBlank { fileName },
                sizeBytes = target.length(),
                createdAt = now,
            ),
        )
        events.upsert(
            LifeEventEntity(
                id = Ids.new(),
                type = eventFor(type).code,
                title = titleFor(type, displayName, null),
                detail = displayName,
                occurredOn = on,
                occurredAt = now,
                sourceType = "captures",
                sourceId = id,
            ),
        )
        return id
    }

    /** Resolves a stored relative path to a readable file, or null. */
    fun fileFor(relativePath: String): File? {
        val file = File(context.filesDir, relativePath)
        return if (file.isFile && file.path.startsWith(context.filesDir.path)) file else null
    }

    /**
     * Trash → restore → permanent delete. Permanent delete also removes the
     * backing media files (they are referenced only by this capture).
     */
    suspend fun moveToTrash(id: String) {
        dao.softDelete(id, Dates.nowMillis())
        events.deleteForSource("captures", id)
    }

    suspend fun restore(id: String) = dao.restore(id, Dates.nowMillis())

    suspend fun deleteForever(id: String) {
        val attachmentPaths = dao.attachmentPaths("captures", id)
        events.deleteForSource("captures", id)
        db.noteDao().clearTags("captures", id)
        attachmentPaths.forEach { relative ->
            runCatching { fileFor(relative)?.delete() }
        }
        dao.deleteForever(id)
    }

    private fun eventFor(type: CaptureType): LifeEventType = when (type) {
        CaptureType.PHOTO -> LifeEventType.PHOTO_TAKEN
        CaptureType.VIDEO -> LifeEventType.VIDEO_RECORDED
        CaptureType.AUDIO -> LifeEventType.AUDIO_RECORDED
        CaptureType.LOCATION -> LifeEventType.LOCATION_SAVED
        CaptureType.MOOD -> LifeEventType.MOOD_LOGGED
        CaptureType.THOUGHT -> LifeEventType.CAPTURE_ADDED
    }

    private fun titleFor(type: CaptureType, text: String, placeLabel: String?): String = when (type) {
        CaptureType.PHOTO -> "Photo"
        CaptureType.VIDEO -> "Video"
        CaptureType.AUDIO -> "Audio note"
        CaptureType.MOOD -> "Mood logged"
        CaptureType.LOCATION -> placeLabel ?: text.ifBlank { "Location saved" }
        CaptureType.THOUGHT -> text.take(60).ifBlank { "Thought captured" }
    }

    private fun extensionFor(mimeType: String, displayName: String): String {
        val fromName = displayName.substringAfterLast('.', "").lowercase()
        if (fromName.length in 2..5 && fromName.all { it.isLetterOrDigit() }) return fromName
        return when (mimeType.substringBefore('/')) {
            "image" -> "jpg"
            "video" -> "mp4"
            "audio" -> "m4a"
            else -> "bin"
        }
    }
}
