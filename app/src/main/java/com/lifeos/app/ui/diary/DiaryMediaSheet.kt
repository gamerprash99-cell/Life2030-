package com.lifeos.app.ui.diary

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lifeos.app.core.media.DevicePhotoImporter
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.text.font.FontWeight

data class PickedPhoto(
    val uri: Uri,
    val filePath: String
)

/**
 * The "Add media" sheet offered from the editor's tool palette: Camera and
 * Photos & Videos. Both paths copy the image into app-private diary storage
 * and hand the result back via [onPhotoPicked], so the entry keeps working
 * after the source image is deleted or a provider grant expires.
 *
 * Camera uses CameraX inline within the sheet (no navigation away from the
 * entry); Photos & Videos uses the Android Photo Picker, which needs no
 * storage permission. Permissions are requested only when the user taps the
 * matching option, never at launch.
 */
@Composable
fun DiaryMediaSheet(
    onPhotoPicked: (PickedPhoto) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { pickedUri ->
            scope.launch {
                val stored = withContext(Dispatchers.IO) { DevicePhotoImporter(context).import(pickedUri) }
                stored?.let { photo ->
                    onPhotoPicked(PickedPhoto(uri = pickedUri, filePath = photo.filePath))
                }
            }
        }
    }

    var cameraOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(onClick = onDismiss)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .background(DiaryLavender)
                .padding(top = 40.dp, bottom = 16.dp)
                .align(Alignment.BottomCenter)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Add media",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = DiaryInkViolet
                )
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = DiaryInkSoft)
                }
            }

            Spacer(Modifier.height(12.dp))

            MediaSheetRow(
                icon = Icons.Default.CameraAlt,
                label = "Camera",
                onClick = {
                    when {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                            cameraOpen = true
                        }
                        else -> {
                            activity?.let { act ->
                                ActivityCompat.requestPermissions(act, arrayOf(Manifest.permission.CAMERA), 0x1111)
                            }
                        }
                    }
                }
            )

            MediaSheetRow(
                icon = Icons.Default.PhotoLibrary,
                label = "Photos & Videos",
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )

            Spacer(Modifier.height(16.dp))
        }

        if (cameraOpen) {
            CameraCaptureOverlay(
                onCaptured = { filePath ->
                    cameraOpen = false
                    val uri = Uri.fromFile(File(filePath))
                    onPhotoPicked(PickedPhoto(uri = uri, filePath = filePath))
                },
                onCancel = { cameraOpen = false }
            )
        }
    }
}

@Composable
private fun MediaSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = LifeOSSpacing.screenPadding, vertical = 10.dp)
            .clickable(onClick = onClick)
            .background(DiaryLavender.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DiaryActionViolet,
            modifier = androidx.compose.ui.Modifier.size(24.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = DiaryInkViolet
        )
    }
}

private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Inline camera capture used by the media sheet, so the user never leaves the
 * entry. CameraX preview + capture; result returns a file path.
 */
@Composable
private fun CameraCaptureOverlay(
    onCaptured: (filePath: String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    previewView.scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        val provider = future.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        runCatching { provider.unbindAll(); provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture) }
                            .onFailure { Toast.makeText(ctx, "Camera failed", Toast.LENGTH_SHORT).show() }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier.padding(bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = {
                            val file = MediaStorage.newDiaryPhotoFile(context)
                            imageCapture.takePicture(
                                androidx.camera.core.ImageCapture.OutputFileOptions.Builder(file).build(),
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) = onCaptured(file.absolutePath)
                                    override fun onError(exception: ImageCaptureException) {
                                        Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Take photo",
                        tint = DiaryActionViolet,
                        modifier = androidx.compose.ui.Modifier.size(28.dp)
                    )
                }
                TextButton(onClick = onCancel) { Text("Cancel", color = DiaryInkSoft) }
            }
        }
    }
}
