package com.lifeos.app.ui.capture

import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.lifeos.app.core.util.LifeOSPermissions
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.core.util.rememberPermissionState

/**
 * Real CameraX video capture (Section 3: Video Memories). This is the
 * VideoCapture-based sibling of CameraCaptureScreen.kt's ImageCapture
 * pipeline — same permission gate, same lifecycle-bound preview, with
 * Recorder/VideoCapture swapped in and start/stop controls added.
 */
@Composable
fun VideoCaptureScreen(onCaptured: (filePath: String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(LifeOSPermissions.CAMERA)
    val audioPermission = rememberPermissionState(LifeOSPermissions.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (cameraPermission.status == com.lifeos.app.core.util.PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE) cameraPermission.request()
        if (audioPermission.status == com.lifeos.app.core.util.PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE) audioPermission.request()
    }

    if (!cameraPermission.isGranted || !audioPermission.isGranted) {
        val anyPermanentlyDenied = cameraPermission.status == com.lifeos.app.core.util.PermissionStatus.PERMANENTLY_DENIED ||
            audioPermission.status == com.lifeos.app.core.util.PermissionStatus.PERMANENTLY_DENIED
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (anyPermanentlyDenied) {
                Text("Camera and/or microphone access was denied. Enable both in Settings to record video.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = cameraPermission.openSettings, modifier = Modifier.padding(top = 12.dp)) { Text("Open Settings") }
            } else {
                Text("Camera and microphone permission are needed to record video.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = {
                    if (!cameraPermission.isGranted) cameraPermission.request()
                    if (!audioPermission.isGranted) audioPermission.request()
                }, modifier = Modifier.padding(top = 12.dp)) { Text("Grant permissions") }
            }
            Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) { Text("Cancel") }
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val recorder = remember { Recorder.Builder().build() }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }

    var isRecording by remember { mutableStateOf(false) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(isRecording) {
        elapsedSeconds = 0
        while (isRecording) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds++
        }
    }

    DisposableEffect(Unit) {
        onDispose { activeRecording?.stop() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture
                        )
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRecording) {
                Text("Recording… ${elapsedSeconds}s", color = androidx.compose.ui.graphics.Color.White)
            }
            IconButton(
                onClick = {
                    if (!isRecording) {
                        val outputFile = MediaStorage.newVideoFile(context)
                        val outputOptions = FileOutputOptions.Builder(outputFile).build()
                        val pending = videoCapture.output.prepareRecording(context, outputOptions)
                            .withAudioEnabled()
                        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                            when (event) {
                                is VideoRecordEvent.Start -> isRecording = true
                                is VideoRecordEvent.Finalize -> {
                                    isRecording = false
                                    if (!event.hasError()) {
                                        onCaptured(outputFile.absolutePath)
                                    } else {
                                        Toast.makeText(context, "Recording error: ${event.cause?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                else -> Unit
                            }
                        }
                    } else {
                        activeRecording?.stop()
                        activeRecording = null
                    }
                }
            ) {
                Icon(
                    if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                    contentDescription = if (isRecording) "Stop recording" else "Start recording",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
            Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) { Text("Cancel") }
        }
    }
}
