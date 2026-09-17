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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
fun CameraCaptureScreen(onCaptured: (filePath: String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val permission = rememberPermissionState(LifeOSPermissions.CAMERA)
    LaunchedEffect(Unit) { if (permission.status == PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE) permission.request() }
    if (!permission.isGranted) {
        CapturePermissionState("Camera access is needed for photos.", permission.status == PermissionStatus.PERMANENTLY_DENIED, permission.request, permission.openSettings, onCancel)
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    runCatching { provider.unbindAll(); provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture) }
                        .onFailure { Toast.makeText(ctx, "Camera failed: ${it.message}", Toast.LENGTH_SHORT).show() }
                }, ContextCompat.getMainExecutor(ctx))
            }
        }, modifier = Modifier.fillMaxSize())
        CaptureOverlay("PHOTO", onCancel) {
            val file = MediaStorage.newPhotoFile(context)
            imageCapture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) = onCaptured(file.absolutePath)
                    override fun onError(exception: ImageCaptureException) { Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show() }
                }
            )
        }
    }
}

@Composable
private fun CaptureOverlay(label: String, onCancel: () -> Unit, onCapture: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Surface(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(14.dp), color = Color.Black.copy(alpha = .42f), shape = CircleShape) {
            IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = "Close camera", tint = Color.White) }
        }
        Text(label, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 22.dp), color = Color.White, style = MaterialTheme.typography.labelLarge)
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(onClick = onCapture, shape = CircleShape, color = Color.White, modifier = Modifier.padding(4.dp)) {
                Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.padding(6.dp)) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Take photo", tint = Color.White, modifier = Modifier.padding(18.dp))
                }
            }
            Text("Tap to capture", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun CapturePermissionState(message: String, permanentlyDenied: Boolean, request: () -> Unit, openSettings: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = if (permanentlyDenied) openSettings else request, modifier = Modifier.padding(top = 16.dp)) { Text(if (permanentlyDenied) "Open Settings" else "Allow Access") }
        androidx.compose.material3.TextButton(onClick = onCancel) { Text("Cancel") }
    }
}
