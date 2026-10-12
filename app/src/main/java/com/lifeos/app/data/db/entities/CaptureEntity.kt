package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** The six things a Life Capture can be. Stored as [CaptureEntity.type]. */
object CaptureType {
    const val PHOTO = "PHOTO"
    const val VIDEO = "VIDEO"
    const val AUDIO = "AUDIO"
    const val THOUGHT = "THOUGHT"
    const val MOOD = "MOOD"
    const val LOCATION = "LOCATION"

    val ALL = listOf(PHOTO, VIDEO, AUDIO, THOUGHT, MOOD, LOCATION)
}

/**
 * A Life Capture — a moment saved in the fewest possible taps (spec §21/22).
 * Photo/Video/Audio captures point at an app-private file via [filePath];
 * Thought/Mood/Location captures carry their payload in [text] and (for
 * Location) [latitude]/[longitude]. [moodKey] is set for Mood captures and is
 * a light tag on any capture.
 */
@Serializable
@Entity(
    tableName = "captures",
    indices = [
        Index(value = ["type"]),
        Index(value = ["createdAt"]),
        Index(value = ["isFavorite"])
    ]
)
data class CaptureEntity(
    @PrimaryKey val id: String,
    val type: String,
    val text: String? = null,
    val filePath: String? = null,
    val mimeType: String? = null,
    val durationMillis: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeLabel: String? = null,
    val moodKey: String? = null,
    val tagsCsv: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long
)
