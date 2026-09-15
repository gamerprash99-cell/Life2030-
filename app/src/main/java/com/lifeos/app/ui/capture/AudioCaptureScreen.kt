package com.lifeos.app.ui.capture

import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.util.LifeOSPermissions
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.core.util.rememberPermissionState
import java.io.File

/**
 * Real MediaRecorder audio capture (Section 3: Audio Memories). Permission
 * is requested only when this screen opens (Rule #22). Recording is a simple
 * start/stop; the resulting .m4a file path is handed back to CaptureSheet.
 */
@Composable
fun AudioCaptureScreen(onCaptured: (filePath: String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val micPermission = rememberPermissionState(LifeOSPermissions.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (micPermission.status == com.lifeos.app.core.util.PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE) {
            micPermission.request()
        }
    }

    if (!micPermission.isGranted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (micPermission.status == com.lifeos.app.core.util.PermissionStatus.PERMANENTLY_DENIED) {
                Text("Microphone access was denied. Enable it in Settings to record audio.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = micPermission.openSettings, modifier = Modifier.padding(top = 12.dp)) { Text("Open Settings") }
            } else {
                Text("Microphone permission is needed to record audio.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = micPermission.request, modifier = Modifier.padding(top = 12.dp)) { Text("Grant permission") }
            }
            Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) { Text("Cancel") }
        }
        return
    }

    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    fun startRecording() {
        val file = MediaStorage.newAudioFile(context)
        outputFile = file
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        mr.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mr
        isRecording = true
    }

    fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (_: Exception) {
            // Recording too short or already stopped — the output file (if any) is still handed back.
        }
        recorder = null
        isRecording = false
        outputFile?.let { onCaptured(it.absolutePath) }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { recorder?.release() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isRecording) {
        elapsedSeconds = 0
        while (isRecording) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds++
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (isRecording) "Recording… ${elapsedSeconds}s" else "Ready to record", style = MaterialTheme.typography.titleLarge)

        Button(
            onClick = { if (isRecording) stopRecording() else startRecording() },
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(if (isRecording) "Stop & Save" else "Start Recording")
        }

        Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) { Text("Cancel") }
    }
}
