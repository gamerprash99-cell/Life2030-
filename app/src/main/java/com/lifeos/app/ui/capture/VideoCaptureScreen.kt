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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.lifeos.app.core.util.LifeOSPermissions
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.core.util.PermissionStatus
import com.lifeos.app.core.util.rememberPermissionState

@Composable
fun VideoCaptureScreen(onCaptured: (filePath: String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(LifeOSPermissions.CAMERA)
    val audioPermission = rememberPermissionState(LifeOSPermissions.RECORD_AUDIO)
    LaunchedEffect(Unit) {
        if (cameraPermission.status == PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE) cameraPermission.request()
        if (audioPermission.status == PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE) audioPermission.request()
    }
    if (!cameraPermission.isGranted || !audioPermission.isGranted) {
        val denied = cameraPermission.status == PermissionStatus.PERMANENTLY_DENIED || audioPermission.status == PermissionStatus.PERMANENTLY_DENIED
        CapturePermissionState("Camera and microphone access are needed for video.", denied, {
            if (!cameraPermission.isGranted) cameraPermission.request()
            if (!audioPermission.isGranted) audioPermission.request()
        }, cameraPermission.openSettings, onCancel)
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val recorder = remember { Recorder.Builder().build() }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var seconds by remember { mutableStateOf(0) }

    LaunchedEffect(isRecording) {
        seconds = 0
        while (isRecording) { kotlinx.coroutines.delay(1000); seconds++ }
    }
    DisposableEffect(Unit) { onDispose { recording?.stop() } }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    runCatching { provider.unbindAll(); provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture) }
                        .onFailure { Toast.makeText(ctx, "Camera failed: ${it.message}", Toast.LENGTH_SHORT).show() }
                }, ContextCompat.getMainExecutor(ctx))
            }
        }, modifier = Modifier.fillMaxSize())

        Surface(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(14.dp), color = Color.Black.copy(alpha = .42f), shape = CircleShape) {
            IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = "Close camera", tint = Color.White) }
        }
        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VIDEO", color = Color.White, style = MaterialTheme.typography.labelLarge)
            if (isRecording) Text("Recording · ${seconds}s", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 3.dp))
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                onClick = {
                    if (isRecording) {
                        recording?.stop(); recording = null
                    } else {
                        val file = MediaStorage.newVideoFile(context)
                        val output = FileOutputOptions.Builder(file).build()
                        recording = videoCapture.output.prepareRecording(context, output).withAudioEnabled()
                            .start(ContextCompat.getMainExecutor(context)) { event ->
                                when (event) {
                                    is VideoRecordEvent.Start -> isRecording = true
                                    is VideoRecordEvent.Finalize -> {
                                        isRecording = false
                                        if (!event.hasError()) onCaptured(file.absolutePath)
                                        else Toast.makeText(context, "Recording failed: ${event.cause?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                    }
                },
                shape = CircleShape,
                color = if (isRecording) MaterialTheme.colorScheme.error else Color.White
            ) {
                Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.Videocam, contentDescription = if (isRecording) "Stop recording" else "Start recording", tint = if (isRecording) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.padding(18.dp))
            }
            Text(if (isRecording) "Tap to stop" else "Tap to record", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}
