package com.lifeos.app.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifeos.app.BuildConfig
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.components.ProfileAvatar
import com.lifeos.app.ui.theme.LifeOSAccentLavender
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppLock: () -> Unit = onOpenSettings
) {
    val locator = LocalServiceLocator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val name by locator.settingsStore.profileName.collectAsState(initial = null)
    var showNameDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var pendingCrop by remember { mutableStateOf<Bitmap?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { loadRotatedBitmap(context, uri) }
            if (bitmap == null) {
                photoError = "Couldn't open that image. Try another photo."
            } else {
                photoError = null
                pendingCrop = bitmap
            }
        }
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxWidth().padding(padding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LifeOSTopBar("Profile", "Your LifeOS space", onBack = onBack)
            Column(Modifier.padding(horizontal = LifeOSSpacing.screenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LifeOSCard(Modifier.fillMaxWidth(), tint = LifeOSAccentLavender) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(
                            size = 72.dp,
                            onClick = { photoPicker.launch(arrayOf("image/*")) }
                        )
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(name?.takeIf { it.isNotBlank() } ?: "Your LifeOS Profile", style = MaterialTheme.typography.titleLarge)
                            Text("Personal space · stored on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tap your photo to change it", style = MaterialTheme.typography.labelSmall, color = LifeOSPrimary, modifier = Modifier.padding(top = 4.dp))
                        }
                        Surface(onClick = { showNameDialog = true }, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit display name", tint = LifeOSPrimary, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
                photoError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                ProfileRow(Icons.Filled.Lock, "App Lock", "Protect LifeOS with your 4-digit PIN", onOpenAppLock)
                ProfileRow(Icons.Filled.Backup, "Local Backup", "Your backup stays under your control", onOpenSettings)
                LifeOSCard(Modifier.fillMaxWidth(), onClick = onOpenSettings) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Settings, contentDescription = null, tint = LifeOSPrimary); Column(Modifier.padding(start = 14.dp)) { Text("Settings", style = MaterialTheme.typography.titleMedium); Text("Preferences, reminders and privacy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                ProfileRow(Icons.Filled.Info, "About LifeOS", "Version ${BuildConfig.VERSION_NAME}", { showAboutDialog = true })
            }
        }
    }

    pendingCrop?.let { bitmap ->
        ProfilePhotoCropDialog(
            bitmap = bitmap,
            onCancel = { pendingCrop = null },
            onDone = { cropped ->
                pendingCrop = null
                scope.launch { saveProfilePhoto(context, locator, cropped) }
            }
        )
    }

    if (showNameDialog) {
        var draft by remember { mutableStateOf(name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Display name") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 40) draft = it },
                    singleLine = true,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { locator.settingsStore.setProfileName(draft) }
                    showNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About LifeOS") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("LifeOS ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})")
                    Text(
                        "A private, offline-first life manager. Your data is stored only on this device, " +
                            "encrypted at rest, and never uploaded. No account, no internet permission, no telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Close") } }
        )
    }
}

/**
 * Persists the cropped photo as a JPEG in app-private storage (never a gallery
 * content URI), so the profile photo survives day changes, app restarts and
 * process death, and can never be invalidated by a temporary URI grant.
 */
private suspend fun saveProfilePhoto(
    context: Context,
    locator: com.lifeos.app.core.di.ServiceLocator,
    cropped: Bitmap
) {
    val file = withContext(Dispatchers.IO) {
        val out = MediaStorage.newProfilePhotoFile(context)
        FileOutputStream(out).use { fos -> cropped.compress(Bitmap.CompressFormat.JPEG, 92, fos) }
        MediaStorage.profilePhotosDir(context).listFiles()
            ?.filter { it != out }
            ?.forEach { old -> runCatching { old.delete() } }
        out
    }
    locator.settingsStore.setProfilePhotoUri(file.absolutePath)
}

/**
 * Decodes the picked image with its EXIF orientation applied (portrait photos
 * are not stretched or left sideways), downscaled so a large gallery shot
 * stays memory-friendly for the crop editor.
 */
private fun loadRotatedBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    val orientation = context.contentResolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

    val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        ?: return null

    val maxDim = 2048
    val largest = max(decoded.width, decoded.height)
    var bitmap = if (largest > maxDim) {
        val scale = maxDim.toFloat() / largest
        Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).roundToInt().coerceAtLeast(1),
            (decoded.height * scale).roundToInt().coerceAtLeast(1),
            true
        ).also { if (it != decoded) decoded.recycle() }
    } else {
        decoded
    }

    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees != 0f) {
        val rotated = Bitmap.createBitmap(
            bitmap,
            0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(degrees) },
            true
        )
        if (rotated != bitmap) bitmap.recycle()
        bitmap = rotated
    }
    bitmap
}.getOrNull()

/** Full-screen square crop editor. Drag inside the window to move it; drag the corner handle to resize. */
@Composable
private fun ProfilePhotoCropDialog(
    bitmap: Bitmap,
    onDone: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val density = LocalDensity.current
    var boardPx by remember { mutableStateOf(IntSize.Zero) }
    val fitRect = remember(bitmap, boardPx) {
        if (boardPx == IntSize.Zero) null
        else computeFitRect(
            bitmap.width.toFloat(), bitmap.height.toFloat(),
            boardPx.width.toFloat(), boardPx.height.toFloat()
        )
    }
    var cropRect by remember(bitmap, boardPx) { mutableStateOf(fitRect?.let { initialCropWindow(it) }) }
    val currentCropRect by rememberUpdatedState(cropRect)
    val currentFitRect by rememberUpdatedState(fitRect)

    val handleTouch = with(density) { 24.dp.toPx() }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.96f)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Crop profile photo", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Text("Square · never stretched", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel crop", tint = Color.White)
                    }
                }
                Spacer(Modifier.padding(top = 10.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onSizeChanged { boardPx = it }
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                val base = currentCropRect ?: return@detectDragGestures
                                val fit = currentFitRect ?: return@detectDragGestures
                                val pos = change.position
                                val nearHandle = (pos - Offset(base.right, base.bottom)).getDistance() <= handleTouch
                                if (nearHandle) {
                                    val rawRight = base.right + drag.x
                                    val rawBottom = base.bottom + drag.y
                                    val side = maxOf(rawRight - base.left, rawBottom - base.top)
                                    val minSide = 96.dp.toPx()
                                    val maxSide = min(fit.right - base.left, fit.bottom - base.top).coerceAtLeast(minSide)
                                    val clamped = side.coerceIn(minSide, maxSide)
                                    cropRect = Rect(base.left, base.top, base.left + clamped, base.top + clamped)
                                } else if (base.contains(pos)) {
                                    val left = (base.left + drag.x).coerceIn(fit.left, fit.right - base.width)
                                    val top = (base.top + drag.y).coerceIn(fit.top, fit.bottom - base.height)
                                    cropRect = Rect(left, top, left + base.width, top + base.height)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val fit = fitRect
                    val crop = cropRect
                    Canvas(Modifier.fillMaxSize()) {
                        val currentFit = fit ?: return@Canvas
                        val currentCrop = crop ?: return@Canvas
                        val img = bitmap.asImageBitmap()
                        drawImage(
                            img,
                            dstOffset = IntOffset(currentFit.left.roundToInt(), currentFit.top.roundToInt()),
                            dstSize = IntSize(currentFit.width.roundToInt(), currentFit.height.roundToInt())
                        )
                        val scrim = Color.Black.copy(alpha = 0.55f)
                        drawRect(scrim, topLeft = Offset(currentFit.left, currentFit.top), size = Size(currentFit.width, currentCrop.top - currentFit.top))
                        drawRect(scrim, topLeft = Offset(currentFit.left, currentCrop.bottom), size = Size(currentFit.width, currentFit.bottom - currentCrop.bottom))
                        drawRect(scrim, topLeft = Offset(currentFit.left, currentCrop.top), size = Size(currentCrop.left - currentFit.left, currentCrop.height))
                        drawRect(scrim, topLeft = Offset(currentCrop.right, currentCrop.top), size = Size(currentFit.right - currentCrop.right, currentCrop.height))
                        drawRoundRect(
                            color = Color.White,
                            topLeft = currentCrop.topLeft,
                            size = currentCrop.size,
                            cornerRadius = CornerRadius(8f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        val handle = 18.dp.toPx()
                        drawCircle(
                            color = LifeOSPrimary,
                            radius = handle / 2f,
                            center = currentCrop.bottomRight - Offset(handle / 4f, handle / 4f)
                        )
                    }
                }
                Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp)) {
                    Text("Drag the window to position · corner dot to resize", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    Button(
                        onClick = {
                            val f = fitRect ?: return@Button
                            val c = cropRect ?: return@Button
                            onDone(cropToBitmap(bitmap, f, c))
                        },
                        enabled = fitRect != null && cropRect != null,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) { Text("Save photo") }
                }
            }
        }
    }
}

private fun computeFitRect(imgW: Float, imgH: Float, boardW: Float, boardH: Float): Rect {
    val scale = minOf(boardW / imgW, boardH / imgH)
    val dw = imgW * scale
    val dh = imgH * scale
    val left = (boardW - dw) / 2f
    val top = (boardH - dh) / 2f
    return Rect(left, top, left + dw, top + dh)
}

private fun initialCropWindow(fit: Rect): Rect {
    val side = minOf(fit.width, fit.height) * 0.8f
    val left = fit.left + (fit.width - side) / 2f
    val top = fit.top + (fit.height - side) / 2f
    return Rect(left, top, left + side, top + side)
}

/** Maps the on-screen square crop window back to bitmap-pixel coordinates. */
private fun cropToBitmap(bitmap: Bitmap, fit: Rect, crop: Rect): Bitmap {
    val scale = bitmap.width.toFloat() / fit.width
    val x = ((crop.left - fit.left) * scale).roundToInt().coerceIn(0, bitmap.width - 1)
    val y = ((crop.top - fit.top) * scale).roundToInt().coerceIn(0, bitmap.height - 1)
    val w = (crop.width * scale).roundToInt().coerceIn(1, bitmap.width - x)
    val h = (crop.height * scale).roundToInt().coerceIn(1, bitmap.height - y)
    return Bitmap.createBitmap(bitmap, x, y, w, h)
}

@Composable
private fun ProfileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    LifeOSCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp)) {
                Icon(icon, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.padding(10.dp))
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}