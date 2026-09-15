package com.lifeos.app.core.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * All captured media lives under app-private storage (files/captures/) —
 * never MediaStore/public storage — consistent with "your data stays on
 * your device, under your control" (spec Section 2/59).
 */
object MediaStorage {
    private val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun capturesDir(context: Context): File =
        File(context.filesDir, "captures").apply { if (!exists()) mkdirs() }

    fun newPhotoFile(context: Context): File =
        File(capturesDir(context), "PHOTO_${timestampFormat.format(java.util.Date())}.jpg")

    fun newVideoFile(context: Context): File =
        File(capturesDir(context), "VIDEO_${timestampFormat.format(java.util.Date())}.mp4")

    fun newAudioFile(context: Context): File =
        File(capturesDir(context), "AUDIO_${timestampFormat.format(java.util.Date())}.m4a")
}
