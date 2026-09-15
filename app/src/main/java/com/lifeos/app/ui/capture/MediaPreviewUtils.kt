package com.lifeos.app.ui.capture

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Small, self-contained helpers for showing real previews of captured
 * media (Section 7/8 of the UI polish pass: "the user does not get a
 * clear next step / visible result" after capturing).
 *
 * These read directly from the actual files already written by
 * CameraCaptureScreen / VideoCaptureScreen / AudioCaptureScreen via
 * MediaStorage — nothing here is faked or mocked.
 */

/** Extracts a single frame from a video file to use as a thumbnail/poster. */
private fun extractVideoFrame(filePath: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(filePath)
        retriever.getFrameAtTime(0)
    } catch (e: Exception) {
        null
    } finally {
        try { retriever.release() } catch (e: Exception) { /* no-op */ }
    }
}

/** Reads a media file's duration in milliseconds (works for both video and audio). */
private fun extractDurationMs(filePath: String): Long? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(filePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    } catch (e: Exception) {
        null
    } finally {
        try { retriever.release() } catch (e: Exception) { /* no-op */ }
    }
}

/** Remembers a decoded video thumbnail for [filePath], loaded off the main thread. Null until ready or on failure. */
@Composable
fun rememberVideoThumbnail(filePath: String?): ImageBitmap? {
    val state by produceState<ImageBitmap?>(initialValue = null, key1 = filePath) {
        value = if (filePath == null) null else withContext(Dispatchers.IO) {
            extractVideoFrame(filePath)?.asImageBitmap()
        }
    }
    return state
}

/** Remembers a media file's duration in milliseconds, loaded off the main thread. Null until ready or unavailable. */
@Composable
fun rememberMediaDurationMs(filePath: String?): Long? {
    val state by produceState<Long?>(initialValue = null, key1 = filePath) {
        value = if (filePath == null) null else withContext(Dispatchers.IO) {
            extractDurationMs(filePath)
        }
    }
    return state
}

/** Formats milliseconds as "m:ss", e.g. 75_000L -> "1:15". Returns "0:00" for null/negative input. */
fun formatDurationMs(ms: Long?): String {
    if (ms == null || ms < 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
