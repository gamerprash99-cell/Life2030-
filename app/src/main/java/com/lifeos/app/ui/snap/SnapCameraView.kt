package com.lifeos.app.ui.snap

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ZoomState
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.lifeos.app.core.util.MediaStorage

/**
 * The in-app Snap camera. A single [LifecycleCameraController] owns the preview,
 * the zoom state and still capture, so flipping the lens, changing zoom and
 * taking the photo all share one camera session.
 *
 * Reading task: it deliberately renders dark regardless of the app theme — a
 * camera viewfinder is a dark surface, and a white control bar over a live feed
 * would both look wrong and fight the preview's contrast.
 */
@Composable
fun SnapCameraView(
    onCaptured: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    var isFront by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }
    var gridOn by remember { mutableStateOf(true) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    var isCapturing by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf<String?>(null) }
    var shutterFlash by remember { mutableFloatStateOf(0f) }

    DisposableEffect(lifecycleOwner, isFront) {
        controller.cameraSelector =
            if (isFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }

    DisposableEffect(controller, lifecycleOwner) {
        val observer = Observer<ZoomState> { state ->
            maxZoom = state.maxZoomRatio.coerceAtLeast(1f)
            zoomRatio = state.zoomRatio
        }
        controller.zoomState.observe(lifecycleOwner, observer)
        onDispose { controller.zoomState.removeObserver(observer) }
    }

    DisposableEffect(controller, flashOn, isFront) {
        controller.imageCaptureFlashMode =
            if (flashOn && !isFront) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        onDispose { }
    }

    val flashAlpha by animateFloatAsState(shutterFlash, tween(if (shutterFlash > 0f) 40 else 260), label = "shutterFlash")

    fun capture() {
        if (isCapturing) return
        isCapturing = true
        failed = null
        shutterFlash = 1f
        val file = MediaStorage.newCapturePhotoFile(context)
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        controller.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    shutterFlash = 0f
                    onCaptured(file.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    shutterFlash = 0f
                    failed = "Couldn't save the snap. Try again."
                }
            }
        )
    }

    LaunchedEffect(shutterFlash) {
        if (shutterFlash > 0f) {
            kotlinx.coroutines.delay(60)
            shutterFlash = 0f
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(maxZoom) {
                detectTransformGestures { _, _, gestureZoom, _ ->
                    val next = (zoomRatio * gestureZoom).coerceIn(1f, maxZoom)
                    zoomRatio = next
                    controller.setZoomRatio(next)
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (gridOn) {
            RuleOfThirdsGrid(Modifier.fillMaxSize())
        }

        // White shutter flash overlay.
        Box(
            Modifier
                .fillMaxSize()
                .alpha(flashAlpha)
                .background(Color.White)
        )

        // Top controls: close, flash, grid.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraRoundButton(Icons.Filled.Close, "Close camera", onClick = onClose)
            Box(Modifier.weight(1f))
            CameraRoundButton(
                if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                if (flashOn) "Turn flash off" else "Turn flash on",
                enabled = !isFront,
                onClick = { flashOn = !flashOn }
            )
            CameraRoundButton(
                Icons.Filled.GridOn,
                if (gridOn) "Hide grid" else "Show grid",
                active = gridOn,
                onClick = { gridOn = !gridOn }
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.32f))
                .padding(bottom = 26.dp, top = 14.dp)
        ) {
            failed?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFFB4AB),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                )
            }

            // Zoom presets.
            Row(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                snapZoomPresets.forEach { preset ->
                    val available = preset <= maxZoom + 0.05f
                    val selected = kotlin.math.abs(zoomRatio - preset) < 0.08f && available
                    ZoomChip(
                        label = "${preset.toInt()}x",
                        selected = selected,
                        enabled = available,
                        onClick = {
                            val target = preset.coerceIn(1f, maxZoom)
                            zoomRatio = target
                            controller.setZoomRatio(target)
                        }
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left slot keeps the shutter visually centred.
                Box(Modifier.size(52.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ShutterButton(isCapturing = isCapturing, onClick = ::capture)
                }
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    CameraRoundButton(
                        Icons.Filled.Cameraswitch,
                        if (isFront) "Switch to back camera" else "Switch to front camera",
                        onClick = { isFront = !isFront }
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleOfThirdsGrid(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.dp.toPx()
        val color = Color.White.copy(alpha = 0.28f)
        for (i in 1..2) {
            val x = size.width * i / 3f
            val y = size.height * i / 3f
            drawLine(color, Offset(x, 0f), Offset(x, size.height), stroke, StrokeCap.Round)
            drawLine(color, Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
        }
    }
}

@Composable
private fun ShutterButton(isCapturing: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isCapturing) 0.86f else 1f, tween(120), label = "shutter")
    Box(
        Modifier
            .size(78.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .clickable(enabled = !isCapturing, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(60.dp)
                .padding(2.dp)
                .clip(CircleShape)
                .background(if (isCapturing) MaterialTheme.colorScheme.secondary else Color.White)
        )
        Canvas(Modifier.size(78.dp)) {
            drawCircle(
                color = Color.Transparent,
                radius = size.minDimension / 2f,
                style = Stroke(width = scale * 4.dp.toPx())
            )
        }
    }
}

@Composable
private fun ZoomChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg = when {
        selected -> Color.White
        enabled -> Color.Black.copy(alpha = 0.42f)
        else -> Color.Black.copy(alpha = 0.22f)
    }
    val fg = when {
        selected -> Color.Black
        enabled -> Color.White
        else -> Color.White.copy(alpha = 0.38f)
    }
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

@Composable
private fun CameraRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false
) {
    Box(
        Modifier
            .padding(4.dp)
            .size(46.dp)
            .clip(CircleShape)
            .background(
                if (active) Color.White else Color.Black.copy(alpha = 0.38f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription,
            tint = when {
                !enabled -> Color.White.copy(alpha = 0.35f)
                active -> Color.Black
                else -> Color.White
            },
            modifier = Modifier.size(22.dp)
        )
    }
}
