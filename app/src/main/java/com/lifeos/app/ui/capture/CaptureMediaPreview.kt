package com.lifeos.app.ui.capture

import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.lifeos.app.ui.components.GlassCard
import kotlinx.coroutines.delay
import java.io.File

/**
 * Large photo preview using the actual captured file — the real fix for
 * "user captures a photo but gets no visible result". No placeholder/fake
 * image is ever shown; if the file is missing, that's shown honestly too.
 */
@Composable
fun PhotoPreview(filePath: String, modifier: Modifier = Modifier) {
    val fileExists = remember(filePath) { File(filePath).exists() }
    if (fileExists) {
        AsyncImage(
            model = filePath,
            contentDescription = "Captured photo",
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(18.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        MissingFileNotice(modifier)
    }
}

/**
 * Video preview: shows a real extracted frame from the captured file first
 * (no autoplay), with a tap-to-play control. Once tapped, mounts a real
 * VideoView with Android's built-in MediaController (play/pause/seek).
 */
@Composable
fun VideoPreview(filePath: String, modifier: Modifier = Modifier) {
    val fileExists = remember(filePath) { File(filePath).exists() }
    if (!fileExists) {
        MissingFileNotice(modifier)
        return
    }

    var isPlaying by remember(filePath) { mutableStateOf(false) }
    val thumbnail = rememberVideoThumbnail(filePath)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isPlaying) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val controller = MediaController(ctx)
                        setMediaController(controller)
                        controller.setAnchorView(this)
                        setVideoURI(Uri.fromFile(File(filePath)))
                        setOnPreparedListener { start() }
                    }
                }
            )
        } else {
            thumbnail?.let {
                androidx.compose.foundation.Image(
                    bitmap = it,
                    contentDescription = "Video preview",
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            IconButton(
                onClick = { isPlaying = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play video", tint = Color.White)
            }
        }
    }
}

/**
 * Audio preview: a simple, real MediaPlayer-backed play/pause control with
 * elapsed/total duration — plays the actual recorded file.
 */
@Composable
fun AudioPreview(filePath: String, modifier: Modifier = Modifier) {
    val fileExists = remember(filePath) { File(filePath).exists() }
    if (!fileExists) {
        MissingFileNotice(modifier)
        return
    }

    var isPlaying by remember(filePath) { mutableStateOf(false) }
    var positionMs by remember(filePath) { mutableStateOf(0L) }
    val durationMs = rememberMediaDurationMs(filePath)
    var mediaPlayer by remember(filePath) { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(filePath) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = mediaPlayer?.let { if (it.isPlaying) it.currentPosition.toLong() else positionMs } ?: positionMs
            delay(250)
        }
    }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    val player = mediaPlayer ?: MediaPlayer().apply {
                        setDataSource(filePath)
                        prepare()
                        setOnCompletionListener {
                            isPlaying = false
                            positionMs = 0L
                            seekTo(0)
                        }
                    }.also { mediaPlayer = it }
                    player.start()
                    isPlaying = true
                }
            }) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play recording"
                )
            }
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                val total = durationMs ?: 0L
                LinearProgressIndicator(
                    progress = { if (total > 0) (positionMs.toFloat() / total).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${formatDurationMs(positionMs)} / ${formatDurationMs(durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MissingFileNotice(modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Text(
            "This file could not be found on the device. It may have been removed outside the app.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
