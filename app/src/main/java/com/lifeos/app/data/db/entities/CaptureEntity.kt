package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class CaptureType { PHOTO, VIDEO, AUDIO, THOUGHT }

/** Life Capture — local media metadata model. */
@Serializable
@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey val id: String,
    val type: CaptureType,
    val filePath: String?,
    val thumbnailPath: String? = null,
    val caption: String? = null,
    val transcript: String? = null,
    val mood: String? = null,
    val tagsCsv: String = "",
    val dateEpochDay: Long,
    val timeMinutes: Int,
    val createdAt: Long
)
