package com.lifeos.app.core.data

import com.lifeos.app.core.database.LifeOsDatabase
import com.lifeos.app.core.database.entity.LifeEventEntity
import com.lifeos.app.core.database.entity.NoteEntity
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.Ids
import com.lifeos.app.core.domain.LifeEventType
import com.lifeos.app.core.domain.NoteKind
import com.lifeos.app.core.domain.Tags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Notes repository: the app's reference implementation of the standard content
 * lifecycle (create → edit → trash → restore → delete forever) that every other
 * feature follows. All writes also append a timeline event so the Timeline is a
 * truthful log of things that actually happened.
 */
class NoteRepository(private val db: LifeOsDatabase) {

    private val dao get() = db.noteDao()
    private val events get() = db.lifeEventDao()

    fun observeNotes(): Flow<List<NoteWithTags>> =
        combine(dao.observeAll(), dao.observeAllNoteTagNames()) { notes, tagRows ->
            val grouped = tagRows.groupBy({ it.entityId }, { it.name })
            notes.map { note -> NoteWithTags(note, grouped[note.id].orEmpty()) }
        }

    fun observeTrash(): Flow<List<NoteEntity>> = dao.observeTrash()

    fun observeTags(): Flow<List<String>> = dao.observeTags().map { list -> list.map { it.name } }

    fun observeNote(id: String): Flow<NoteWithTags?> =
        combine(dao.observeAll(), dao.observeTagsFor(ENTITY, id)) { notes, tags ->
            notes.firstOrNull { it.id == id }?.let { NoteWithTags(it, tags.map { t -> t.name }) }
        }

    suspend fun get(id: String): NoteEntity? = dao.getLiveById(id)

    /** Seed for the editor's tag field, e.g. `#health #work`. */
    suspend fun tagTextFor(id: String): String =
        dao.tagsFor(ENTITY, id).joinToString(" ") { "#${it.name}" }

    suspend fun create(
        title: String,
        body: String,
        kind: NoteKind = NoteKind.PLAIN,
        folderId: String? = null,
        tagText: String = "",
    ): String {
        val now = Dates.nowMillis()
        val id = Ids.new()
        val note = NoteEntity(
            id = id,
            title = title.trim(),
            body = body.trim(),
            kind = kind.code,
            folderId = folderId,
            createdAt = now,
            updatedAt = now,
        )
        dao.upsert(note)
        applyTags(id, tagText)
        events.upsert(
            LifeEventEntity(
                id = Ids.new(),
                type = LifeEventType.NOTE_CREATED.code,
                title = note.title.ifBlank { "Note" },
                detail = note.body.take(140),
                occurredOn = Dates.iso(Dates.today()),
                occurredAt = now,
                sourceType = ENTITY,
                sourceId = id,
            ),
        )
        return id
    }

    suspend fun update(id: String, title: String, body: String, tagText: String) {
        val now = Dates.nowMillis()
        dao.updateContent(id, title.trim(), body.trim(), now)
        applyTags(id, tagText)
    }

    suspend fun setPinned(id: String, pinned: Boolean) =
        dao.setPinned(id, pinned, Dates.nowMillis())

    suspend fun setFolder(id: String, folderId: String?) =
        dao.setFolder(id, folderId, Dates.nowMillis())

    suspend fun moveToTrash(id: String) {
        val now = Dates.nowMillis()
        dao.softDelete(id, now)
        events.deleteForSource(ENTITY, id)
    }

    suspend fun restore(id: String) {
        val now = Dates.nowMillis()
        dao.restore(id, now)
        get(id)?.let { note ->
            events.upsert(
                LifeEventEntity(
                    id = Ids.new(),
                    type = LifeEventType.NOTE_CREATED.code,
                    title = "Restored note: ${note.title.ifBlank { "Note" }}",
                    occurredOn = Dates.iso(Dates.today()),
                    occurredAt = now,
                    sourceType = ENTITY,
                    sourceId = id,
                ),
            )
        }
    }

    suspend fun deleteForever(id: String) {
        events.deleteForSource(ENTITY, id)
        dao.clearTags(ENTITY, id)
        dao.deleteForever(id)
    }

    /** Normalizes `#tags` from free text, dedupes, and replaces the entity's set. */
    private suspend fun applyTags(entityId: String, tagText: String) {
        val names = Tags.parseAll(tagText).map { Tags.normalize(it) }.filter { it.isNotEmpty() }
        if (names.isEmpty()) {
            dao.clearTags(ENTITY, entityId)
            return
        }
        val ids = dao.resolveTagIds(names.distinct())
        dao.setEntityTags(ENTITY, entityId, ids)
    }

    companion object {
        const val ENTITY = "notes"
    }
}

data class NoteWithTags(
    val note: NoteEntity,
    val tags: List<String>,
)
