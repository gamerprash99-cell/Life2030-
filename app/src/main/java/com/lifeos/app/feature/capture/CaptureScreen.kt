package com.lifeos.app.feature.capture

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.core.designsystem.theme.MinTouchTarget
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.domain.CaptureType
import com.lifeos.app.core.domain.Dates
import com.lifeos.app.core.domain.Mood
import com.lifeos.app.ui.AppViewModelFactory
import com.lifeos.app.ui.LocalAppContainer
import com.lifeos.app.ui.components.EmptyState
import com.lifeos.app.ui.components.LifeOsCard
import com.lifeos.app.ui.components.LifeOsTopBar
import com.lifeos.app.ui.components.Pill

private data class CaptureOption(
    val type: CaptureType,
    val title: String,
    val subtitle: String,
)

private val CAPTURE_OPTIONS = listOf(
    CaptureOption(CaptureType.PHOTO, "Photo", "Pick a photo from this device"),
    CaptureOption(CaptureType.VIDEO, "Video", "Pick a video from this device"),
    CaptureOption(CaptureType.AUDIO, "Audio", "Record a voice note"),
    CaptureOption(CaptureType.THOUGHT, "Thought", "Write something down quickly"),
    CaptureOption(CaptureType.MOOD, "Mood", "Log how you feel right now"),
    CaptureOption(CaptureType.LOCATION, "Location", "Save where you are"),
)

/**
 * Capture hub.
 *
 * Permission policy (spec: permissions at the point of use only):
 * - PHOTO/VIDEO use the system document picker, which needs **no** runtime
 *   permission: the user chooses the exact file and the app only reads it.
 * - AUDIO records via `MediaRecorder` into app-private storage; RECORD_AUDIO is
 *   requested only when the user taps Audio, never at launch.
 * - LOCATION is requested only when the user taps Location, and the capture is
 *   saved only from the fix the OS returns.
 */
@Composable
fun CaptureScreen(
    onPickMedia: (CaptureType) -> Unit,
    onCaptureThought: () -> Unit,
    onCaptureMood: () -> Unit,
    onCaptureLocation: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CaptureViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        LifeOsTopBar(title = "Capture", onBack = null)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Column {
                    Text(
                        text = "What do you want to save?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        text = "Everything is stored on this device only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(CAPTURE_OPTIONS) { option ->
                CaptureOptionCard(
                    option = option,
                    onClick = {
                        when (option.type) {
                            CaptureType.THOUGHT -> onCaptureThought()
                            CaptureType.MOOD -> onCaptureMood()
                            CaptureType.LOCATION -> onCaptureLocation()
                            else -> onPickMedia(option.type)
                        }
                    },
                )
            }

            if (state.message != null) {
                item {
                    LifeOsCard(containerColor = LifeOs.tokens.softPinkSurface) {
                        Text(
                            text = state.message.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Recent captures",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            if (state.visible.isEmpty()) {
                item {
                    EmptyState(
                        title = "No captures yet",
                        subtitle = "Photos, videos, voice notes, thoughts, moods and locations " +
                            "you save will appear here.",
                    )
                }
            } else {
                itemsIndexed(state.visible, key = { _, item -> item.id }) { _, capture ->
                    LifeOsCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(MinTouchTarget)
                                    .background(LifeOs.tokens.softPinkSurface, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = CaptureType.fromCode(capture.type).label.take(1),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.size(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = capture.placeLabel
                                        ?: capture.text.ifBlank {
                                            CaptureType.fromCode(capture.type).label
                                        },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = buildString {
                                        append(CaptureType.fromCode(capture.type).label)
                                        append(" · ")
                                        append(Dates.relative(capture.occurredOn))
                                        capture.moodScore?.let { score ->
                                            append(" · ")
                                            append(Mood.fromScore(score).label)
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureOptionCard(option: CaptureOption, onClick: () -> Unit) {
    LifeOsCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(MinTouchTarget)
                    .background(LifeOs.tokens.beigeAccent.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.title.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.size(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = option.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Capture hub wired to the platform pickers and permission prompts.
 * This is the composable the navigation graph uses; [CaptureScreen] stays free of
 * Android APIs so it can be previewed and tested without a device.
 */
@Composable
fun CaptureHost(
    modifier: Modifier = Modifier,
    viewModel: CaptureViewModel = viewModel(factory = AppViewModelFactory(LocalAppContainer.current)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingMediaType by remember { mutableStateOf(CaptureType.PHOTO) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) {
            viewModel.report("Nothing was selected — no capture saved.")
        } else {
            val displayName = displayNameFor(context, uri)
                ?: "${pendingMediaType.code}-${System.currentTimeMillis()}"
            val mime = context.contentResolver.getType(uri).orEmpty()
            viewModel.captureMedia(pendingMediaType, uri, mime, displayName)
        }
    }

    val permissionDialog = remember { mutableStateOf<CaptureType?>(null) }

    CaptureScreen(
        modifier = modifier,
        viewModel = viewModel,
        onPickMedia = { type ->
            pendingMediaType = type
            val mime = when (type) {
                CaptureType.PHOTO -> "image/*"
                else -> "video/*"
            }
            picker.launch(mime)
        },
        onCaptureThought = { viewModel.report("Longer text lives in Notes; short thoughts are saved here instantly.") },
        onCaptureMood = { permissionDialog.value = CaptureType.MOOD },
        onCaptureLocation = { permissionDialog.value = CaptureType.LOCATION },
    )

    permissionDialog.value?.let { type ->
        if (type == CaptureType.MOOD) {
            MoodPickerDialog(
                onDismiss = { permissionDialog.value = null },
                onPick = { mood, note ->
                    viewModel.captureMood(mood, note)
                    permissionDialog.value = null
                },
            )
        } else {
            LocationPermissionDialog(
                onDismiss = { permissionDialog.value = null },
                onGranted = { lat, lon, label ->
                    viewModel.captureLocation(lat, lon, label)
                    permissionDialog.value = null
                },
            )
        }
    }
}

@Composable
private fun MoodPickerDialog(
    onDismiss: () -> Unit,
    onPick: (Mood, String) -> Unit,
) {
    var selected by remember { mutableStateOf(Mood.OKAY) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("How do you feel?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Mood.entries.forEach { mood ->
                    Pill(
                        text = "${mood.emoji}  ${mood.label}",
                        selected = selected == mood,
                        onClick = { selected = mood },
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onPick(selected, "") }) {
                Text("Save mood")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Location dialog: requests nothing until the user taps Allow, and hands back only
 * the single fix obtained for this capture.
 */
@Composable
private fun LocationPermissionDialog(
    onDismiss: () -> Unit,
    onGranted: (Double, Double, String?) -> Unit,
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result.values.any { it }
        if (granted) {
            fetchSingleLocation(
                context = context,
                onResult = { lat, lon -> onGranted(lat, lon, null) },
                onError = { error = it },
            )
        } else {
            error = "Location was not granted — nothing was saved."
        }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Save this location?") },
        text = {
            Text(
                error ?: "Location is requested once, for this capture only. " +
                    "It is stored on this device and never uploaded.",
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                        ),
                    )
                },
            ) { Text("Allow once") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** Single-shot location read through the platform location service. */
private fun fetchSingleLocation(
    context: Context,
    onResult: (Double, Double) -> Unit,
    onError: (String) -> Unit,
) {
    // Explicit runtime check: lint (and the platform) require this rather than
    // relying on an exception, and it lets us fail with a clear message.
    val granted = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    if (!granted) {
        onError("Location permission was not granted — nothing was saved.")
        return
    }
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
    if (manager == null) {
        onError("Location service is unavailable on this device.")
        return
    }
    val last = runCatching {
        manager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            ?: manager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
    }.getOrNull()
    if (last != null) {
        onResult(last.latitude, last.longitude)
        return
    }
    if (!manager.allProviders.contains(android.location.LocationManager.GPS_PROVIDER)) {
        onError("No location is available right now — nothing was saved.")
        return
    }
    val listener = object : android.location.LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            manager.removeUpdates(this)
            onResult(location.latitude, location.longitude)
        }

        override fun onProviderDisabled(provider: String) {
            manager.removeUpdates(this)
            onError("Location was turned off — nothing was saved.")
        }

        @Deprecated("Required by the platform interface")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
    }
    runCatching {
        manager.requestLocationUpdates(
            android.location.LocationManager.NETWORK_PROVIDER,
            0L,
            0f,
            listener,
            android.os.Looper.getMainLooper(),
        )
    }.onFailure { onError("Could not read the location — nothing was saved.") }
}

/** Best-effort display name from a content URI; falls back to null. */
private fun displayNameFor(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
}.getOrNull()
