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

    /**
     * Per-entry background/theme selection, stored as presentation metadata.
     * [key] is a stable identifier for a bundled LifeOS theme/background
     * (never a remote URL). Missing or unknown keys fall back to default.
     */
    @Serializable
    @SerialName("theme")
    data class ThemeStyle(val key: String) : DiaryAttachment()

    /**
     * A locally bundled sticker or emoji placed independently on the diary
     * page. Coordinates are normalized (0..1) so the same placement survives
     * font-scale changes and different screen densities. [scale] and
     * [rotation] let the user resize and turn the sticker.
     */
    @Serializable
    @SerialName("sticker")
    data class Sticker(
        val assetKey: String,
        /** Horizontal position as a fraction of the page width (0..1). */
        val x: Float,
        /** Vertical position as a fraction of the page height (0..1). */
        val y: Float,
        /** Scale multiplier relative to the asset's natural size. */
        val scale: Float = 1f,
        /** Rotation in degrees. */
        val rotation: Float = 0f
    ) : DiaryAttachment()

    /**
     * A drawing produced in the editor's drawing canvas. Strokes are persisted
     * as a raster/vector artifact under app-private storage; [filePath] is the
     * committed artifact. [width]/[height] record the canvas aspect so the
     * drawing renders inline without distortion.
     */
    @Serializable
    @SerialName("drawing")
    data class Drawing(
        val filePath: String,
        /** Natural width of the drawing artifact in pixels. */
        val width: Float,
        /** Natural height of the drawing artifact in pixels. */
        val height: Float
    ) : DiaryAttachment()

    /**
     * An audio file the user imported through the document/audio picker.
     * [filePath] is an app-private copy; [mimeType], [displayName] and
     * [durationMillis] are metadata only, populated when reliably available.
     */
    @Serializable
    @SerialName("importedAudio")
    data class ImportedAudio(
        val filePath: String,
        val mimeType: String,
        val displayName: String,
        val durationMillis: Long
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

    /** The theme/background style applied to this entry, if any. */
    fun themeStyle(attachments: List<DiaryAttachment>): DiaryAttachment.ThemeStyle? =
        attachments.filterIsInstance<DiaryAttachment.ThemeStyle>().lastOrNull()

    /** All stickers placed on this entry's page, in insertion order. */
    fun stickers(attachments: List<DiaryAttachment>): List<DiaryAttachment.Sticker> =
        attachments.filterIsInstance<DiaryAttachment.Sticker>()

    /** The drawing artifact attached to this entry, if any. */
    fun drawing(attachments: List<DiaryAttachment>): DiaryAttachment.Drawing? =
        attachments.filterIsInstance<DiaryAttachment.Drawing>().lastOrNull()

    /** All audio files the user imported into this entry. */
    fun importedAudio(attachments: List<DiaryAttachment>): List<DiaryAttachment.ImportedAudio> =
        attachments.filterIsInstance<DiaryAttachment.ImportedAudio>()
}
