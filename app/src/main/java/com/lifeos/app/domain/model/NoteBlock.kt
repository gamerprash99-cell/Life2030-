package com.lifeos.app.domain.model

import kotlinx.serialization.Serializable

/**
 * A single block of rich content inside a Note (Section 6 editor requirements:
 * paragraphs, headings, bullet/numbered lists, checklists, highlight).
 * Serialized to/from NoteEntity.contentJson.
 */
@Serializable
sealed class NoteBlock {
    abstract val id: String

    @Serializable
    data class Paragraph(override val id: String, val text: String, val bold: Boolean = false, val italic: Boolean = false, val underline: Boolean = false, val highlighted: Boolean = false) : NoteBlock()

    @Serializable
    data class Heading(override val id: String, val text: String, val level: Int = 1) : NoteBlock()

    @Serializable
    data class BulletItem(override val id: String, val text: String) : NoteBlock()

    @Serializable
    data class NumberedItem(override val id: String, val text: String, val index: Int) : NoteBlock()

    @Serializable
    data class ChecklistItem(override val id: String, val text: String, val checked: Boolean = false) : NoteBlock()
}

@Serializable
data class AttachmentRef(
    val id: String,
    val type: String,       // "photo" | "video" | "audio" | "document"
    val filePath: String,
    val fileName: String,
    val sizeBytes: Long = 0L
)
