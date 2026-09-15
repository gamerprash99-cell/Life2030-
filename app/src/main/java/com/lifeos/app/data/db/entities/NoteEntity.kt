package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Smart Notes — Section 6/7/8 of the spec.
 * `contentJson` stores the rich-text body as a serialized block list (paragraphs,
 * headings, checklists, bullet/numbered lists, highlights) so the editor can
 * support real rich formatting without needing a second table per block type.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val contentJson: String,          // serialized List<NoteBlock>
    val plainTextForSearch: String,    // flattened text, used by global search (FTS-free, indexed manually)
    val folder: String?,               // College / Projects / Ideas / Personal / Finance / Other / null
    val tagsCsv: String,                // comma-separated tag names, e.g. "college,urgent"
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,     // soft delete → supports "Restore notes" (Section 7)
    val createdAt: Long,
    val updatedAt: Long,
    val attachmentsJson: String = "[]"  // serialized List<AttachmentRef>
)
