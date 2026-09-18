package com.lifeos.app.ui.capture

import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.util.LifeOSPermissions
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.core.util.PermissionStatus
import com.lifeos.app.core.util.rememberPermissionState
import java.io.File

/** Hard cap so a forgotten recording can never fill the device's storage. */
private const val MAX_AUDIO_SECONDS = 15 * 60

@Composable
fun AudioCaptureScreen(onCaptured: (filePath: String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val permission = rememberPermissionState(LifeOSPermissions.RECORD_AUDIO)
    LaunchedEffect(Unit) { if (permission.status == PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE) permission.request() }
    if (!permission.isGranted) {
        CapturePermissionState("Microphone access is needed for audio capture.", permission.status == PermissionStatus.PERMANENTLY_DENIED, permission.request, permission.openSettings, onCancel)
        return
    }

    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var file by remember { mutableStateOf<File?>(null) }
    var seconds by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    fun releaseRecorder() {
        runCatching { recorder?.release() }
        recorder = null
    }

    /** Discards the in-progress recording and cleans up its partial file. */
    fun discard() {
        val current = file
        releaseRecorder()
        isRecording = false
        current?.delete()
        file = null
        seconds = 0
    }

    fun start() {
        val output = MediaStorage.newAudioFile(context)
        val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        error = null
        try {
            current.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44_100)
                setAudioEncodingBitRate(128_000)
                setOutputFile(output.absolutePath)
                prepare()
                start()
            }
        } catch (t: Throwable) {
            runCatching { current.reset() }
            runCatching { current.release() }
            runCatching { output.delete() }
            error = "Couldn't start recording. Please try again."
            return
        }
        file = output
        recorder = current
        isRecording = true
    }

    fun stop() {
        val current = recorder
        val saved = runCatching { current?.stop() }.isSuccess
        runCatching { current?.release() }
        recorder = null
        isRecording = false
        val output = file
        when {
            !saved || output == null || !output.exists() || output.length() == 0L -> {
                output?.delete()
                error = "That recording was too short to save."
            }
            else -> onCaptured(output.absolutePath)
        }
        file = null
        seconds = 0
    }

    BackHandler { if (isRecording) discard(); onCancel() }
    LaunchedEffect(isRecording) {
        seconds = 0
        while (isRecording) {
            kotlinx.coroutines.delay(1000)
            seconds++
            if (seconds >= MAX_AUDIO_SECONDS) { stop(); break }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            releaseRecorder()
            file?.delete()
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxSize().weight(1f), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("AUDIO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(if (isRecording) "Recording" else "Voice Capture", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 5.dp))
                    Text(
                        when {
                            isRecording -> "${seconds}s · max ${MAX_AUDIO_SECONDS / 60} min · stored locally"
                            else -> "Record a thought without leaving LifeOS."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
                }
                IconButton(onClick = { if (isRecording) discard(); onCancel() }) { Icon(Icons.Filled.Close, contentDescription = "Close recorder") }
            }
            Surface(onClick = { if (isRecording) stop() else start() }, shape = CircleShape, color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) {
                Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = if (isRecording) "Stop recording" else "Start recording", tint = Color.White, modifier = Modifier.padding(26.dp).size(34.dp))
            }
            Text(if (isRecording) "Tap to save" else "Tap to start", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
            TextButton(onClick = { if (isRecording) discard(); onCancel() }, modifier = Modifier.padding(top = 20.dp).padding(bottom = 8.dp)) { Text("Cancel") }
        }
    }
}
