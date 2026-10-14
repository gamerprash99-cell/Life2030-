package com.lifeos.app.ui.capture

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.media.AudioRecorder
import com.lifeos.app.core.media.RecordingFailure
import com.lifeos.app.core.media.RecordingState
import com.lifeos.app.core.media.formatAudioDuration
import com.lifeos.app.core.util.LifeOSPermissions
import com.lifeos.app.core.util.PermissionStatus
import com.lifeos.app.core.util.rememberPermissionState
import kotlinx.coroutines.delay

/**
 * The real audio recorder for Life Capture.
 *
 * ROOT CAUSE of the "audio has only UI" defect this replaces: the live Capture
 * hub (`CaptureHubScreen`) wired its Audio tile to a `notice = "Voice notes are
 * coming next"` stub while the only working recorder lived behind the
 * unreachable `CaptureSheet`. This screen is now owned directly by the hub, so
 * tapping **Audio** records through the platform `MediaRecorder` (via the same
 * tested [AudioRecorder] the diary composer uses) and stores a real row.
 *
 * States are explicit and never faked: permission → recording → saving →
 * success, plus TOO_SHORT / START_FAILED / STOP_FAILED failures that keep the
 * user's take or explain precisely why it could not be kept.
 */
@Composable
fun AudioCaptureScreen(
    recorder: AudioRecorder,
    onCaptured: (filePath: String, durationMillis: Long) -> Unit,
    onCancel: () -> Unit
) {
    val permission = rememberPermissionState(LifeOSPermissions.RECORD_AUDIO)
    LaunchedEffect(Unit) {
        if (permission.status == PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE) permission.request()
    }
    if (!permission.isGranted) {
        CapturePermissionState(
            message = "Microphone access is needed to record a voice note.",
            permanentlyDenied = permission.status == PermissionStatus.PERMANENTLY_DENIED,
            request = permission.request,
            openSettings = permission.openSettings,
            onCancel = onCancel
        )
        return
    }

    val state by recorder.state.collectAsState()
    val latestCancel by rememberUpdatedState(onCancel)
    val latestOnCaptured by rememberUpdatedState(onCaptured)

    // Begin the take as soon as the mic is genuinely available. If the platform
    // refuses (mic busy, permission revoked mid-flight) the failure state below
    // reports it rather than showing a dead "recording" screen.
    LaunchedEffect(Unit) {
        if (recorder.state.value is RecordingState.Idle) recorder.start()
    }

    // One loop drives the measured clock, the live level and completion, so the
    // effect is never restarted on every tick (every 90 ms) while recording.
    LaunchedEffect(Unit) {
        while (true) {
            if (recorder.isRecording()) recorder.tick()
            val current = recorder.state.value
            if (current is RecordingState.Finished) {
                latestOnCaptured(current.filePath, current.durationMillis)
                break
            }
            delay(90)
        }
    }

    BackHandler {
        recorder.cancel()
        latestCancel()
    }
    DisposableEffect(Unit) {
        onDispose { if (recorder.isRecording()) recorder.cancel() }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "AUDIO",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val recording = state as? RecordingState.Recording
                    Text(
                        if (recording != null) "Recording" else "Voice Capture",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                    Text(
                        when (state) {
                            is RecordingState.Recording -> "Stored locally · tap to keep"
                            is RecordingState.Failure -> failureCopy((state as RecordingState.Failure).reason)
                            else -> "Record a thought without leaving LifeOS."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            RecordingDial(
                recording = state is RecordingState.Recording,
                amplitude = (state as? RecordingState.Recording)?.amplitude ?: 0,
                onClick = {
                    when (state) {
                        is RecordingState.Recording -> recorder.stop()
                        is RecordingState.Failure -> {
                            // A failed take can be retried in place.
                            recorder.cancel()
                            recorder.start()
                        }
                        else -> recorder.start()
                    }
                }
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val elapsed = (state as? RecordingState.Recording)?.elapsedMillis ?: 0L
                Text(
                    if (state is RecordingState.Recording) formatAudioDuration(elapsed) else "Tap to start",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Tap to stop and save",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Surface(
                    onClick = {
                        recorder.cancel()
                        onCancel()
                    },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

/** The mic/stop dial, with a live inner ring that breathes with real amplitude. */
@Composable
private fun RecordingDial(recording: Boolean, amplitude: Int, onClick: () -> Unit) {
    // Real level, normalised. MediaRecorder reports 0..32767; anything quieter
    // than the floor is treated as silence rather than a fake animation.
    val level = (amplitude.coerceIn(0, 32_767) / 32_767f)
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (recording) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseLevel"
    )
    val scale = if (recording) 1f + (0.06f * pulse) + (0.10f * level) else 1f

    Box(contentAlignment = Alignment.Center) {
        if (recording) {
            Box(
                Modifier
                    .size(150.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.30f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(112.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(
                    if (recording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (recording) "Stop recording" else "Start recording",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

/** Human what→why→how copy for every recorder failure. */
private fun failureCopy(reason: RecordingFailure): String = when (reason) {
    RecordingFailure.START_FAILED ->
        "Couldn't start the microphone. Another app may be using it — close it and tap the mic to retry."
    RecordingFailure.TOO_SHORT ->
        "That take was too short to keep. Hold the mic a moment longer and stop again."
    RecordingFailure.STOP_FAILED ->
        "The recording couldn't be finished, so it wasn't saved. Tap the mic to try once more."
}
