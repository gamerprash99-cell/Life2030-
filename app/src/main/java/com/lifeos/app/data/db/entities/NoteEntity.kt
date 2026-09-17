package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Smart Notes — Section 6/7/8 of the spec.
 * `contentJson` stores the rich-text body as a serialized block list (paragraphs,
 * headings, checklists, bullet/numbered lists, highlights) so the editor can
 * support real rich formatting without needing a second table per block type.
 */
@Serializable
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val contentJson: String,
    val plainTextForSearch: String,
    val folder: String?,
    val tagsCsv: String,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val attachmentsJson: String = "[]"
)
