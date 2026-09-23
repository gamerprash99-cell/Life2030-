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
}
