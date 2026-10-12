package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Smart Notes — the long-form thinking surface (distinct from Diary, which is
 * the dated "what happened today" memory). A note is not tied to a day; it is
 * pinned, favourited, archived, filed into a folder and tagged.
 *
 * [contentJson] holds the rich-text block list for future formatting; for now
 * the editor writes plain [content] and [contentJson] carries `[]`, so adding
 * rich formatting later needs no second migration — the column already exists.
 */
@Serializable
@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["updatedAt"]),
        Index(value = ["isArchived"])
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String = "",
    val content: String = "",
    val contentJson: String = "[]",
    val tagsCsv: String = "",
    val folderId: String? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)
