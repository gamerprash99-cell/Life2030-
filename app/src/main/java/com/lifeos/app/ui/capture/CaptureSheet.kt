package com.lifeos.app.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.CaptureType
import kotlinx.coroutines.launch

private enum class CaptureMode { MENU, PHOTO, VIDEO, AUDIO, CONFIRM }
private data class JustCaptured(val type: CaptureType, val filePath: String?)

/**
 * Life Capture is intentionally a full-screen studio rather than a generic
 * Material bottom sheet. This keeps the camera viewport large, moves controls
 * above gesture/navigation areas, and gives all four capture actions one
 * consistent visual language.
 */
@Composable
fun CaptureSheet(onDismiss: () -> Unit) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(CaptureMode.MENU) }
    var thought by remember { mutableStateOf("") }
    var justCaptured by remember { mutableStateOf<JustCaptured?>(null) }

    fun saveCapture(type: CaptureType, filePath: String?, caption: String?, showConfirmation: Boolean) {
        scope.launch {
            val now = DateTimeUtils.today()
            val minutes = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
            locator.captureRepository.addCapture(type, filePath, caption, now.toEpochDay(), minutes)
            if (showConfirmation) {
                justCaptured = JustCaptured(type, filePath)
                mode = CaptureMode.CONFIRM
            } else onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(30.dp)),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 4.dp
        ) {
            when (mode) {
                CaptureMode.MENU -> CaptureMenu(
                    thought = thought,
                    onThoughtChange = { thought = it },
                    onSaveThought = { saveCapture(CaptureType.THOUGHT, null, thought, false) },
                    onPhoto = { mode = CaptureMode.PHOTO },
                    onVideo = { mode = CaptureMode.VIDEO },
                    onAudio = { mode = CaptureMode.AUDIO },
                    onDismiss = onDismiss
                )
                CaptureMode.PHOTO -> CameraCaptureScreen(
                    onCaptured = { saveCapture(CaptureType.PHOTO, it, null, true) },
                    onCancel = { mode = CaptureMode.MENU }
                )
                CaptureMode.VIDEO -> VideoCaptureScreen(
                    onCaptured = { saveCapture(CaptureType.VIDEO, it, null, true) },
                    onCancel = { mode = CaptureMode.MENU }
                )
                CaptureMode.AUDIO -> AudioCaptureScreen(
                    onCaptured = { saveCapture(CaptureType.AUDIO, it, null, true) },
                    onCancel = { mode = CaptureMode.MENU }
                )
                CaptureMode.CONFIRM -> CaptureConfirmation(justCaptured, onDismiss)
            }
        }
    }
}

@Composable
private fun CaptureMenu(
    thought: String,
    onThoughtChange: (String) -> Unit,
    onSaveThought: () -> Unit,
    onPhoto: () -> Unit,
    onVideo: () -> Unit,
    onAudio: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Capture a moment", style = MaterialTheme.typography.headlineMedium)
                Text("Thoughts, photos, video and audio — stored locally.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
        }

        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Quick thought", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedTextField(value = thought, onValueChange = onThoughtChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("What's on your mind?") }, minLines = 3)
                Button(onClick = onSaveThought, enabled = thought.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save thought") }
            }
        }

        Text("Life Capture", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CaptureTile("Photo", "Take a photo", Icons.Filled.CameraAlt, onPhoto, Modifier.weight(1f))
            CaptureTile("Video", "Record a moment", Icons.Filled.Videocam, onVideo, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CaptureTile("Audio", "Record your voice", Icons.Filled.Mic, onAudio, Modifier.weight(1f))
            CaptureTile("Close", "Return to LifeOS", Icons.Filled.Close, onDismiss, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CaptureTile(label: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(15.dp)) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
            }
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CaptureConfirmation(captured: JustCaptured?, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text("Saved to Timeline", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 10.dp))
        }
        captured?.filePath?.let { path ->
            when (captured.type) {
                CaptureType.PHOTO -> PhotoPreview(path, Modifier.fillMaxWidth())
                CaptureType.VIDEO -> VideoPreview(path, Modifier.fillMaxWidth())
                CaptureType.AUDIO -> AudioPreview(path, Modifier.fillMaxWidth())
                else -> Unit
            }
        }
        Text("Your capture is now part of the local LifeOS timeline.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}
