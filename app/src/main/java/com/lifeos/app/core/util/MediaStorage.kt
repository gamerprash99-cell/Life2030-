package com.lifeos.app.core.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * App-private storage for user media. All media lives under app-private
 * storage — never MediaStore/public storage — consistent with "your data
 * stays on your device, under your control" (spec Section 2/59).
 */
object MediaStorage {
    private val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * App-private directory for the user's profile photo. The photo lives here
     * (not as a content URI) so it survives day changes, app restarts and
     * process death, and is never at the mercy of a provider's temporary read
     * grant. It still lives entirely on-device.
     */
    fun profilePhotosDir(context: Context): File =
        File(context.filesDir, "profile-photos").apply { if (!exists()) mkdirs() }

    fun newProfilePhotoFile(context: Context): File =
        File(profilePhotosDir(context), "profile_${timestampFormat.format(java.util.Date())}.jpg")

    /**
     * App-private directory for photos attached to diary entries. Same
     * guarantee as the profile photo: the bytes are copied in here rather than
     * referenced as a content URI, so a photo survives the user deleting the
     * source image, a provider revoking a read grant, or a reboot.
     */
    fun diaryPhotosDir(context: Context): File =
        File(context.filesDir, "diary-photos").apply { if (!exists()) mkdirs() }

    fun newDiaryPhotoFile(context: Context): File =
        File(diaryPhotosDir(context), "diary_photo_${uniqueSuffix()}.jpg")

    /** App-private directory for recorded diary voice notes. */
    fun diaryAudioDir(context: Context): File =
        File(context.filesDir, "diary-audio").apply { if (!exists()) mkdirs() }

    fun newDiaryAudioFile(context: Context): File =
        File(diaryAudioDir(context), "diary_voice_${uniqueSuffix()}.m4a")

    /**
     * Deletes diary media files that no entry references any more. Called
     * after an entry is deleted or its attachments are replaced, so removing
     * a photo/voice note does not leave orphaned recordings on disk.
     */
    fun deleteIfExists(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    /**
     * A collision-proof suffix. Two attachments created inside the same second
     * (a scripted test, or a user adding photos quickly) must not overwrite
     * each other, so the millisecond is folded in.
     */
    private fun uniqueSuffix(): String {
        val stamp = timestampFormat.format(java.util.Date())
        val millis = java.util.Date().time % 1000
        return "${stamp}_$millis"
    }
}
