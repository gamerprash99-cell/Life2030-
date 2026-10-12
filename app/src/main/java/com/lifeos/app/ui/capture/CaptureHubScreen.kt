package com.lifeos.app.ui.capture

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.domain.model.DiaryMood
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.ui.components.LifeOSBanner
import com.lifeos.app.ui.components.LifeOSIconButton
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.liquidGlass
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSecondary
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LifeOSSuccess
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
    var notice by remember { mutableStateOf<String?>(null) }
    val thoughtFocus = remember { androidx.compose.ui.focus.FocusRequester() }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) viewModel.saveLocation()
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
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LifeOSSpacing.screenPadding, vertical = LifeOSSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Capture", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "A moment, saved in seconds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LifeOSIconButton(Icons.Filled.Close, contentDescription = "Close capture", onClick = onClose)
        }

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
                notice?.let {
                    LifeOSBanner(
                        icon = Icons.Filled.GraphicEq,
                        text = it,
                        action = "Dismiss",
                        onAction = { notice = null }
                    )
                    Spacer(Modifier.height(LifeOSSpacing.md))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                    CaptureTile(Icons.Filled.CameraAlt, "Photo", "Take one now", LifeOSPrimary, onOpenCamera, Modifier.weight(1f))
                    CaptureTile(Icons.Filled.Videocam, "Video", "From the camera", LifeOSSecondary, onOpenCamera, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                    CaptureTile(Icons.Filled.GraphicEq, "Audio", "Clip a voice note", LifeOSSuccess, { notice = "Voice notes are coming next. Type a thought for now." }, Modifier.weight(1f))
                    CaptureTile(Icons.Filled.Mood, "Mood", "How are you?", LocalLifeOSSemanticColors.current.gold, { showMoodPicker = !showMoodPicker }, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                    CaptureTile(Icons.Filled.Lightbulb, "Thought", "Write it down", LifeOSSecondary, { thoughtFocus.requestFocus() }, Modifier.weight(1f))
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
                            .liquidGlass(shape = RoundedCornerShape(LifeOSRadius.xlarge), tint = LifeOSPrimary)
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
                                cursorBrush = SolidColor(LifeOSPrimary),
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
                                    else Brush.linearGradient(listOf(LifeOSPrimary, LifeOSSecondary))
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
                                tint = if (quickThought.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else androidx.compose.ui.graphics.Color.White,
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
            .clip(RoundedCornerShape(LifeOSRadius.xlarge))
            .liquidGlass(shape = RoundedCornerShape(LifeOSRadius.xlarge), tint = accent, interactive = true)
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
            .liquidGlass(shape = RoundedCornerShape(LifeOSRadius.pill), tint = LifeOSPrimary, interactive = true)
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
private fun SuccessPanel(message: String, onDone: () -> Unit, onAgain: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = LifeOSSpacing.screenPadding)
            .liquidGlass(shape = RoundedCornerShape(LifeOSRadius.xlarge), tint = LifeOSSuccess)
            .padding(LifeOSSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = LifeOSSuccess, modifier = Modifier.size(36.dp))
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
