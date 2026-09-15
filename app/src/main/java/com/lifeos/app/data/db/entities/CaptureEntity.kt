package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CaptureType { PHOTO, VIDEO, AUDIO, THOUGHT }

/**
 * Life Capture — Photo/Video/Audio Memories + quick "Thought" capture
 * (Sections 3/31-40 area of the spec, and the Capture button on Home).
 * `filePath` points into app-private local storage — never a cloud URI —
 * consistent with "local-data-first" (Section 2).
 */
@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey val id: String,
    val type: CaptureType,
    val filePath: String?,           // null for THOUGHT captures (text only)
    val thumbnailPath: String? = null,
    val caption: String? = null,
    val transcript: String? = null,   // speech-to-text result for AUDIO captures
    val mood: String? = null,
    val tagsCsv: String = "",
    val dateEpochDay: Long,
    val timeMinutes: Int,
    val createdAt: Long
)
