package com.lifeos.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Media and place metadata attached to a diary entry.
 *
 * These are persisted inside the *existing* `DiaryEntity.attachmentsJson`
 * column (a `String` defaulting to `"[]"`), so photos, voice notes and the
 * captured location need **no Room schema change** — the column has been part
 * of the table since v1 and was simply never written to.
 *
 * Every variant stores a real, on-device artifact:
 *  - [Photo]   → a file path under app-private storage (see `MediaStorage`)
 *  - [VoiceNote] → a recorded `.m4a` file path plus its measured duration
 *  - [Place]   → real coordinates from the Android location APIs plus the
 *                 human-readable name resolved by the platform `Geocoder`
 *
 * Nothing here is ever uploaded; the JSON never leaves the encrypted database.
 */
@Serializable
sealed class DiaryAttachment {

    /** A photo the user picked. [filePath] is app-private storage, not a content URI. */
    @Serializable
    @SerialName("photo")
    data class Photo(val filePath: String) : DiaryAttachment()

    /**
     * A recorded voice note. [durationMillis] is the value the platform
     * `MediaRecorder` actually measured — never a fixed or estimated value.
     */
    @Serializable
    @SerialName("voice")
    data class VoiceNote(val filePath: String, val durationMillis: Long) : DiaryAttachment()

    /**
     * The place the entry was written at.
     * [latitude]/[longitude] come from a real `Location` fix;
     * [placeName] is whatever the platform geocoder resolved (may be empty on
     * devices without a geocoder backend — we never invent one).
     */
    @Serializable
    @SerialName("place")
    data class Place(
        val latitude: Double,
        val longitude: Double,
        val placeName: String = "",
        val accuracyMeters: Float? = null
    ) : DiaryAttachment()
}

/** One decoded attachment plus whether its backing file still exists on disk. */
data class ResolvedAttachment(
    val attachment: DiaryAttachment,
    val isMissing: Boolean = false
)

/**
 * Codec for [DiaryAttachment] <-> the `attachmentsJson` string column.
 *
 * Decoding is deliberately total: a row written by an older build, a row
 * hand-edited by a restore, or a truncated string yields an empty list rather
 * than throwing, so one bad row can never crash the Diary list.
 */
object DiaryAttachments {

    private val json = Json {
        ignoreUnknownKeys = true      // tolerate fields added by a future build
        encodeDefaults = true
        classDiscriminator = "kind"   // stable on-disk tag, so the column stays readable
    }

    fun encode(attachments: List<DiaryAttachment>): String =
        if (attachments.isEmpty()) "[]" else json.encodeToString(attachments)

    fun decode(raw: String?): List<DiaryAttachment> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<DiaryAttachment>>(raw) }
            .getOrElse { emptyList() }
    }

    /** Attachments grouped for the UI, in the order the reference shows them. */
    fun photos(attachments: List<DiaryAttachment>): List<DiaryAttachment.Photo> =
        attachments.filterIsInstance<DiaryAttachment.Photo>()

    /** The entry supports at most one voice note, so this returns at most one. */
    fun voiceNote(attachments: List<DiaryAttachment>): DiaryAttachment.VoiceNote? =
        attachments.filterIsInstance<DiaryAttachment.VoiceNote>().lastOrNull()

    /** An entry is pinned to the place it was written at, so the last one wins. */
    fun place(attachments: List<DiaryAttachment>): DiaryAttachment.Place? =
        attachments.filterIsInstance<DiaryAttachment.Place>().lastOrNull()
}
