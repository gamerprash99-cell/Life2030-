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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.lifeos.app.ui.components.HeaderActionCircle
import com.lifeos.app.ui.components.LifeOSBanner
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.LifeOSTopBar
import com.lifeos.app.ui.components.liquidGlass
import com.lifeos.app.ui.theme.FigmaAccent
import com.lifeos.app.ui.theme.FigmaCard
import com.lifeos.app.ui.theme.FigmaElevated
import com.lifeos.app.ui.theme.FigmaMuted
import com.lifeos.app.ui.theme.FigmaOnAccent
import com.lifeos.app.ui.theme.FigmaStroke
import com.lifeos.app.ui.theme.FigmaSurface
import com.lifeos.app.ui.theme.FigmaTertiary
import com.lifeos.app.ui.theme.FigmaText
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors

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

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LifeOSTopBar(
            title = "Keep this moment",
            subtitle = "Life Capture",
            actions = {
                HeaderActionCircle(Icons.Filled.Close, "Close capture", onClick = onClose)
            }
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
                Text(
                    "It doesn’t have to be extraordinary to be worth remembering.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FigmaMuted
                )
            }
            item {
                // Figma group "Visual capture" (#5:36792).
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                    CaptureTile(Icons.Filled.CameraAlt, "Photo", "Open camera", onOpenCamera, Modifier.weight(1f))
                    CaptureTile(Icons.Filled.Videocam, "Video", "A little slice of life", onOpenCamera, Modifier.weight(1f))
                }
            }
            item {
                // Figma group "Personal capture" (#5:36805).
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                    CaptureTile(
                        Icons.Filled.GraphicEq,
                        "Audio",
                        "Hold a thought",
                        { audioRecorder = DiaryAudioRecorder(locator.appContext, MediaStorage::newCaptureAudioFile) },
                        Modifier.weight(1f)
                    )
                    CaptureTile(Icons.Filled.Edit, "Thought", "Write it down", { thoughtFocus.requestFocus() }, Modifier.weight(1f))
                }
            }
            item {
                // Figma group "Context capture" (#5:36818).
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                    CaptureTile(Icons.Filled.Mood, "Mood", "How’s your day?", { showMoodPicker = !showMoodPicker }, Modifier.weight(1f))
                    CaptureTile(
                        Icons.Filled.LocationOn,
                        "Location",
                        "Choose to attach",
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
                // Figma "Card" (#5:36831): FigmaCard fill over the hairline stroke,
                // radius 22, 18dp pad — the quick thought sits inside it rather than
                // under a bare label.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LifeOSRadius.large))
                        .background(FigmaCard, RoundedCornerShape(LifeOSRadius.large))
                        .border(BorderStroke(1.dp, FigmaStroke), RoundedCornerShape(LifeOSRadius.large))
                        .padding(LifeOSSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = FigmaTertiary, modifier = Modifier.size(20.dp))
                        Box(Modifier.weight(1f)) {
                            if (quickThought.isEmpty()) {
                                Text(
                                    "What’s on your mind?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FigmaMuted
                                )
                            }
                            BasicTextField(
                                value = quickThought,
                                onValueChange = { quickThought = it },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = FigmaText),
                                cursorBrush = SolidColor(FigmaAccent),
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(thoughtFocus)
                            )
                        }
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(LifeOSRadius.small))
                                .background(FigmaAccent)
                                .clickable(enabled = quickThought.isNotBlank()) {
                                    viewModel.saveThought(quickThought)
                                    quickThought = ""
                                }
                                .semantics { contentDescription = "Save thought" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.ArrowUpward,
                                contentDescription = null,
                                tint = FigmaOnAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        "Saved to this device & linked to your timeline.",
                        style = MaterialTheme.typography.labelSmall,
                        color = FigmaMuted
                    )
                }
            }
            item {
                // Permission note (#5:36838) — true of this app: every permission is
                // requested when its own tile is used, never at launch.
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = FigmaTertiary, modifier = Modifier.size(17.dp))
                    Text(
                        "Camera, mic and location access are requested only when you choose to use them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FigmaMuted,
                        modifier = Modifier.weight(1f)
                    )
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Figma "Capture choice" (#5:36793): FigmaSurface fill, hairline stroke,
    // radius 20, 18dp pad; the 42x42 icon tile inside is FigmaElevated with the
    // same stroke at radius 14. Both radii round to the shared LifeOSRadius
    // scale rather than inventing one-off values.
    val cardShape = RoundedCornerShape(LifeOSRadius.xlarge - 2.dp)
    val iconShape = RoundedCornerShape(LifeOSRadius.medium - 2.dp)
    Column(
        modifier
            .clip(cardShape)
            .background(FigmaSurface, cardShape)
            .border(BorderStroke(1.dp, FigmaStroke), cardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(LifeOSSpacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(iconShape)
                .background(FigmaElevated, iconShape)
                .border(BorderStroke(1.dp, FigmaStroke), iconShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = FigmaText, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = FigmaText)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = FigmaMuted)
        }
    }
}

@Composable
private fun MoodChip(mood: DiaryMood, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(LifeOSRadius.pill))
            .liquidGlass(shape = RoundedCornerShape(LifeOSRadius.pill), tint = MaterialTheme.colorScheme.primary, interactive = true)
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
            .liquidGlass(shape = RoundedCornerShape(LifeOSRadius.xlarge), tint = LocalLifeOSSemanticColors.current.success)
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
