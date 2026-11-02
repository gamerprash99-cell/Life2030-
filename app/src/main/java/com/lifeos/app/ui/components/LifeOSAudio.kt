package com.lifeos.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.media.VoicePlaybackState
import com.lifeos.app.core.media.VoiceRecordingState
import com.lifeos.app.core.media.formatAudioDuration
import com.lifeos.app.ui.theme.LifeOSSpacing

// ===========================================================================
// Voice — the two controls a real recorder and a real player need.
//
// Both are driven entirely by measured state (see core/media/VoiceRecording.kt
// and VoiceAudioPlayer.kt). The waveform draws the microphone's own amplitude
// history when the app has it — i.e. for the take that was just recorded — and
// falls back to an honest progress track when it does not, rather than
// inventing bars that look like audio data.
// ===========================================================================

/**
 * The seek control for one clip.
 *
 * [waveform] may be null: that is the case for a clip recorded in an earlier
 * session, whose amplitude history was never stored. When it is present the
 * bars are the real recorded levels, filled up to the true playback position;
 * when it is absent the control is a plain progress track. Either way, dragging
 * or tapping reports a real seek target through [onSeek].
 */
@Composable
fun LifeOSWaveformSeek(
    positionMillis: Long,
    durationMillis: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    waveform: List<Int>? = null,
    enabled: Boolean = true,
    height: Dp = 46.dp
) {
    val progress = if (durationMillis > 0) {
        (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
    } else {
        0f
    }
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    val shown = dragProgress ?: progress

    val playedColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val unplayedColor = MaterialTheme.colorScheme.outline

    fun seekToFraction(fraction: Float) {
        if (!enabled || durationMillis <= 0L) return
        onSeek((fraction.coerceIn(0f, 1f) * durationMillis).toLong())
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(enabled, durationMillis) {
                if (!enabled || durationMillis <= 0L) return@pointerInput
                detectTapGestures { offset ->
                    val fraction = offset.x / size.width.toFloat()
                    dragProgress = null
                    seekToFraction(fraction)
                }
            }
            .pointerInput(enabled, durationMillis) {
                if (!enabled || durationMillis <= 0L) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragProgress = offset.x / size.width.toFloat() },
                    onDragEnd = {
                        dragProgress?.let { seekToFraction(it) }
                        dragProgress = null
                    },
                    onDragCancel = { dragProgress = null }
                ) { change, _ ->
                    dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                }
            }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
                contentDescription = "Playback position"
                setProgress { target ->
                    seekToFraction(target)
                    true
                }
            }
    ) {
        val centerY = size.height / 2f
        if (waveform.isNullOrEmpty()) {
            // No recorded level history for this clip: a clean track + thumb.
            val barHeight = 4.dp.toPx()
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, centerY - barHeight / 2f),
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(barHeight / 2f)
            )
            drawRoundRect(
                color = playedColor,
                topLeft = Offset(0f, centerY - barHeight / 2f),
                size = Size(size.width * shown, barHeight),
                cornerRadius = CornerRadius(barHeight / 2f)
            )
            drawCircle(color = playedColor, radius = 7.dp.toPx(), center = Offset(size.width * shown, centerY))
        } else {
            // Real amplitude history: one bar per measured level.
            val gap = 2.dp.toPx()
            val barWidth = ((size.width - gap * (waveform.size - 1)) / waveform.size).coerceAtLeast(1f)
            val playedUntil = size.width * shown
            waveform.forEachIndexed { index, level ->
                val x = index * (barWidth + gap)
                val fraction = (level.coerceIn(0, 100) / 100f).coerceAtLeast(0.06f)
                val barHeight = size.height * fraction
                drawRoundRect(
                    color = if (x + barWidth / 2f <= playedUntil) playedColor else unplayedColor,
                    topLeft = Offset(x, centerY - barHeight / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f)
                )
            }
        }
    }
}

/**
 * The recording transport: one primary button whose meaning is the current
 * state, plus pause/resume and stop while a take is live.
 *
 * Nothing here animates on a timer of its own — the pulse follows the real
 * input level, so a silent room produces a still disc and a loud one moves it.
 */
@Composable
fun LifeOSRecordingControls(
    state: VoiceRecordingState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = state is VoiceRecordingState.Recording
    val isPaused = state is VoiceRecordingState.Paused
    val isLive = isRecording || isPaused
    val elapsed = when (state) {
        is VoiceRecordingState.Recording -> state.elapsedMillis
        is VoiceRecordingState.Paused -> state.elapsedMillis
        else -> 0L
    }

    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
            Text(
                formatAudioDuration(elapsed),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isPaused) {
                LifeOSPill(text = "Paused", tint = MaterialTheme.colorScheme.secondary)
            }
        }

        Text(
            when {
                isRecording -> "Listening…"
                isPaused -> "Paused — your clip is still open"
                else -> "Tap the microphone to start"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val pulse by animateFloatAsState(
            targetValue = if (isRecording && state is VoiceRecordingState.Recording) {
                1f + (state.amplitude.coerceIn(0, 32_767) / 32_767f) * 0.12f
            } else {
                1f
            },
            animationSpec = spring(stiffness = 700f),
            label = "recordPulse"
        )

        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(96.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(
                        if (isLive) MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
            )
            VoiceRoundButton(
                icon = if (isLive) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (isLive) "Stop recording" else "Start recording",
                container = if (isLive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                size = 72.dp,
                onClick = { if (isLive) onStop() else onStart() }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
            if (isRecording) {
                LifeOSSecondaryButton(text = "Pause", onClick = onPause, icon = Icons.Filled.Pause)
            }
            if (isPaused) {
                LifeOSSecondaryButton(text = "Resume", onClick = onResume, icon = Icons.Filled.PlayArrow)
            }
            if (isLive) {
                LifeOSSecondaryButton(text = "Stop", onClick = onStop, icon = Icons.Filled.Stop)
            }
        }
    }
}

/** The live input level, drawn from the microphone's own recent readings. */
@Composable
fun LifeOSLiveWaveform(
    levels: List<Int>,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    active: Boolean = true
) {
    val playedColor = MaterialTheme.colorScheme.primary
    val idleColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = "Microphone input level" }
    ) {
        val buckets = 40
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (buckets - 1)) / buckets).coerceAtLeast(1f)
        val centerY = size.height / 2f
        val recent = levels.takeLast(buckets)
        // The newest sample sits at the right edge, the same way a live meter
        // reads; empty leading space is simply "nothing recorded yet".
        repeat(buckets) { index ->
            val level = recent.getOrNull(recent.size - buckets + index)
            val fraction = if (level == null) 0.04f else (level.coerceIn(0, 100) / 100f).coerceAtLeast(0.04f)
            val barHeight = size.height * fraction
            drawRoundRect(
                color = if (level == null || !active) idleColor else playedColor,
                topLeft = Offset(index * (barWidth + gap), centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

/**
 * The player card: transport, title, waveform seek and the real times. Used by
 * the voice player screen and anywhere a saved clip is played back.
 */
@Composable
fun LifeOSAudioPlayer(
    state: VoicePlaybackState,
    onToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    waveform: List<Int>? = null
) {
    val position: Long
    val duration: Long
    val isPlaying: Boolean
    when (state) {
        is VoicePlaybackState.Playing -> {
            position = state.positionMillis; duration = state.durationMillis; isPlaying = true
        }
        is VoicePlaybackState.Paused -> {
            position = state.positionMillis; duration = state.durationMillis; isPlaying = false
        }
        is VoicePlaybackState.Completed -> {
            position = state.durationMillis; duration = state.durationMillis; isPlaying = false
        }
        else -> {
            position = 0L; duration = 0L; isPlaying = false
        }
    }
    val enabled = state is VoicePlaybackState.Playing || state is VoicePlaybackState.Paused ||
        state is VoicePlaybackState.Completed || state is VoicePlaybackState.Idle

    LifeOSSurface(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
            title?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
            ) {
                VoiceRoundButton(
                    icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    container = MaterialTheme.colorScheme.primary,
                    size = 52.dp,
                    enabled = enabled,
                    onClick = onToggle
                )
                Column(Modifier.weight(1f)) {
                    LifeOSWaveformSeek(
                        positionMillis = position,
                        durationMillis = duration,
                        onSeek = onSeek,
                        waveform = waveform,
                        enabled = enabled && duration > 0
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.xs)) {
                        Text(
                            formatAudioDuration(position),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "/",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatAudioDuration(duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            when (state) {
                is VoicePlaybackState.MissingFile -> PlayerNotice(
                    "This recording's file is no longer on the device, so there is nothing to play.",
                    MaterialTheme.colorScheme.error
                )
                is VoicePlaybackState.Failed -> PlayerNotice(
                    "This recording could not be played on this device.",
                    MaterialTheme.colorScheme.error
                )
                is VoicePlaybackState.Preparing -> PlayerNotice(
                    "Opening the recording…",
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun PlayerNotice(text: String, tint: Color) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = tint,
        modifier = Modifier.padding(top = LifeOSSpacing.xs)
    )
}

/** A circular transport button with real press feedback. */
@Composable
private fun VoiceRoundButton(
    icon: ImageVector,
    contentDescription: String,
    container: Color,
    size: Dp,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(stiffness = 900f),
        label = "voiceButton"
    )
    Box(
        Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(if (enabled) container else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size * 0.44f)
        )
    }
}
