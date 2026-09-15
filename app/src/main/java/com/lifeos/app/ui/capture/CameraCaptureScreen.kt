package com.lifeos.app.ui.capture

import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
 * Real CameraX photo capture (Section 3: Photo Memories). Permission is
 * requested only when this screen opens — never at app launch (Rule #22).
 * On success, the saved file path is handed back to the caller (CaptureSheet),
 * which writes the CaptureEntity row.
 */
@Composable
fun CameraCaptureScreen(onCaptured: (filePath: String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(LifeOSPermissions.CAMERA)

    LaunchedEffect(Unit) {
        if (cameraPermission.status == com.lifeos.app.core.util.PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE) {
            cameraPermission.request()
        }
    }

    if (!cameraPermission.isGranted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (cameraPermission.status == com.lifeos.app.core.util.PermissionStatus.PERMANENTLY_DENIED) {
                Text(
                    "Camera access was denied. Enable it in Settings to capture a photo.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = cameraPermission.openSettings, modifier = Modifier.padding(top = 12.dp)) { Text("Open Settings") }
            } else {
                Text("Camera permission is needed to capture a photo.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = cameraPermission.request, modifier = Modifier.padding(top = 12.dp)) { Text("Grant permission") }
            }
            Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) { Text("Cancel") }
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

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
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
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
            IconButton(
                onClick = {
                    val outputFile = MediaStorage.newPhotoFile(context)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onCaptured(outputFile.absolutePath)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            ) {
                Icon(
                    Icons.Filled.Camera,
                    contentDescription = "Take photo",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
            Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) { Text("Cancel") }
        }
    }
}
