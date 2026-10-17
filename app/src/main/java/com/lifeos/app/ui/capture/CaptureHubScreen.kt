package com.lifeos.app.ui.capture

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.media.AudioRecorder
import com.lifeos.app.core.media.DiaryAudioRecorder
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.domain.model.DiaryMood
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.ui.components.LifeOSBanner
import com.lifeos.app.ui.components.LifeOSPageHeader
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.lifeOSCardShape
import com.lifeos.app.ui.components.lifeOSIsDark
import com.lifeos.app.ui.theme.LifeOSMint
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors
import com.lifeos.app.ui.theme.lifeOSCanvasBrush

/**
 * The Capture hub — the app's centre action made real. A full-window glass
 * sheet offering the six capture types in a single glance, with an inline
 * quick-thought field for the "I just want to note this" case.
 *
 * Photo/Video hand off to the Snap camera; Thought/Mood/Location are written
 * straight to the Capture table by [CaptureViewModel]. After a capture the
 * sheet names exactly what was stored and where it went.
 */
@Composable
fun CaptureHubScreen(
    onClose: () -> Unit,
    onOpenCamera: () -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: CaptureViewModel = viewModel(
        factory = LambdaViewModelFactory {
            CaptureViewModel(locator.captureRepository, locator.deviceLocationProvider)
        }
    )
    val state by viewModel.state.collectAsState()

    var quickThought by rememberSaveable { mutableStateOf("") }
    var showMoodPicker by rememberSaveable { mutableStateOf(false) }
    // Non-null only while the audio recorder flow is open. Creating it on demand
    // (rather than remembering one instance) guarantees every take starts from
    // Idle, so a second recording is never blocked by the previous Finished state.
    var audioRecorder by remember { mutableStateOf<AudioRecorder?>(null) }
    val thoughtFocus = remember { androidx.compose.ui.focus.FocusRequester() }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) viewModel.saveLocation()
    }

    // Audio owns the whole window while it is open — a live recorder with its own
    // controls, exactly like the Snap camera. Recording goes through the same
    // tested AudioRecorder the diary composer uses, writing to captures/audio.
    val recorder = audioRecorder
    if (recorder != null) {
        AudioCaptureScreen(
            recorder = recorder,
            onCaptured = { filePath, durationMillis ->
                audioRecorder = null
                viewModel.saveAudio(filePath, durationMillis)
            },
            onCancel = {
                recorder.cancel()
                audioRecorder = null
            }
        )
        return
    }

    val dark = lifeOSIsDark()
    Column(
        Modifier
            .fillMaxSize()
            .background(lifeOSCanvasBrush(dark))
    ) {
        LifeOSPageHeader(
            eyebrow = "Quick capture",
            title = "Capture",
            actionIcon = Icons.Filled.Close,
            actionDescription = "Close capture",
            onAction = onClose
        )

        AnimatedVisibility(state.confirmation != null, enter = fadeIn(), exit = fadeOut()) {
            SuccessPanel(
                message = state.confirmation.orEmpty(),
                onDone = { viewModel.dismissConfirmation(); onClose() },
                onAgain = { viewModel.dismissConfirmation() }
            )
        }

        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = LifeOSSpacing.screenPadding,
                end = LifeOSSpacing.screenPadding,
                top = LifeOSSpacing.sm,
                bottom = LifeOSSpacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
        ) {
            item {
                state.error?.let {
                    LifeOSBanner(
                        icon = Icons.Filled.LocationOn,
                        text = it,
                        action = "Dismiss",
                        onAction = viewModel::dismissError
                    )
                    Spacer(Modifier.height(LifeOSSpacing.md))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                    CaptureTile(Icons.Filled.CameraAlt, "Photo", "Take one now", MaterialTheme.colorScheme.primary, onOpenCamera, Modifier.weight(1f))
                    CaptureTile(Icons.Filled.Videocam, "Video", "From the camera", MaterialTheme.colorScheme.secondary, onOpenCamera, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                    CaptureTile(
                        Icons.Filled.GraphicEq,
                        "Audio",
                        "Clip a voice note",
                        LocalLifeOSSemanticColors.current.success,
                        { audioRecorder = DiaryAudioRecorder(locator.appContext, MediaStorage::newCaptureAudioFile) },
                        Modifier.weight(1f)
                    )
                    CaptureTile(Icons.Filled.Mood, "Mood", "How are you?", LocalLifeOSSemanticColors.current.gold, { showMoodPicker = !showMoodPicker }, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                    CaptureTile(Icons.Filled.Lightbulb, "Thought", "Write it down", MaterialTheme.colorScheme.secondary, { thoughtFocus.requestFocus() }, Modifier.weight(1f))
                    CaptureTile(
                        Icons.Filled.LocationOn,
                        "Location",
                        "Where you are",
                        LocalLifeOSSemanticColors.current.info,
                        {
                            if (viewModel.hasLocationPermission()) viewModel.saveLocation()
                            else locationPermission.launch(
                                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                            )
                        },
                        Modifier.weight(1f)
                    )
                }
            }
            if (showMoodPicker) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
                        Text("How are you feeling?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
                            items(DiaryMoods.OPTIONS) { mood ->
                                MoodChip(mood) { viewModel.saveMood(mood, null) }
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
                    Text("QUICK THOUGHT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .lifeOSCardShape()
                            .padding(horizontal = LifeOSSpacing.lg, vertical = LifeOSSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
                    ) {
                        Box(Modifier.weight(1f)) {
                            if (quickThought.isEmpty()) {
                                Text("＋  quick thought…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            BasicTextField(
                                value = quickThought,
                                onValueChange = { quickThought = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth().focusRequester(thoughtFocus)
                            )
                        }
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (quickThought.isBlank()) SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                                    else SolidColor(LifeOSMint)
                                )
                                .clickable(enabled = quickThought.isNotBlank()) {
                                    viewModel.saveThought(quickThought)
                                    quickThought = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Save thought",
                                tint = if (quickThought.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureTile(
    icon: ImageVector,
    title: String,
    caption: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .lifeOSCardShape()
            .clickable(onClick = onClick)
            .padding(LifeOSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(LifeOSRadius.medium))
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.12f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MoodChip(mood: DiaryMood, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(LifeOSRadius.pill))
            .background(LifeOSMint.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = LifeOSSpacing.lg, vertical = LifeOSSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
    ) {
        Text(mood.emoji, style = MaterialTheme.typography.titleMedium)
        Text(mood.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AnimatedSuccessTick() {
    // A real "done": the disc springs in and the check draws itself, so a saved
    // capture is confirmed with motion rather than a static glyph.
    val pop = remember { Animatable(0f) }
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        draw.animateTo(1f, tween(durationMillis = 340, easing = FastOutSlowInEasing))
    }
    val success = LocalLifeOSSemanticColors.current.success
    Canvas(
        Modifier
            .size(44.dp)
            .graphicsLayer {
                val s = 0.6f + 0.4f * pop.value
                scaleX = s
                scaleY = s
                alpha = pop.value
            }
    ) {
        drawCircle(color = success, radius = size.minDimension / 2f)
        val w = size.width
        val h = size.height
        val tick = Path().apply {
            moveTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.68f)
            lineTo(w * 0.74f, h * 0.34f)
        }
        val measure = PathMeasure().apply { setPath(tick, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * draw.value, partial, true)
        drawPath(
            path = partial,
            color = Color.White,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun SuccessPanel(message: String, onDone: () -> Unit, onAgain: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = LifeOSSpacing.screenPadding)
            .lifeOSCardShape()
            .padding(LifeOSSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
    ) {
        AnimatedSuccessTick()
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Row(
            horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LifeOSPrimaryButton("Capture another", onAgain)
            Text(
                "Done",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(LifeOSRadius.pill))
                    .clickable(onClick = onDone)
                    .padding(horizontal = LifeOSSpacing.lg, vertical = LifeOSSpacing.sm)
            )
        }
    }
}
