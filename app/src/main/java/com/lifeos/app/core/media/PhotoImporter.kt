package com.lifeos.app.core.media

import android.content.Context
import android.net.Uri
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.domain.model.DiaryAttachment
import java.io.File

/**
 * Copies a photo the user picked into app-private storage.
 *
 * The Android Photo Picker hands back a temporary content URI that the system
 * can revoke at any time, so the bytes are copied into our own directory the
 * moment they are chosen. That is what makes an attachment survive the source
 * photo being deleted — and it is why the diary needs no storage permission.
 */
interface PhotoImporter {
    /** Copies [sourceUri] into private storage, or returns null if it cannot be read. */
    fun import(sourceUri: Uri): DiaryAttachment.Photo?
}

class DevicePhotoImporter(private val context: Context) : PhotoImporter {

    override fun import(sourceUri: Uri): DiaryAttachment.Photo? = runCatching {
        val target: File = MediaStorage.newDiaryPhotoFile(context)
        // stream rather than decode, so a large photo never has to exist in
        // memory all at once.
        val copied = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
            target.length() > 0L
        } ?: false
        if (!copied) {
            // A failed or empty copy must not leave a zero-byte file behind for
            // the row to point at.
            MediaStorage.deleteIfExists(target.absolutePath)
            return null
        }
        DiaryAttachment.Photo(filePath = target.absolutePath)
    }.getOrNull()
}
