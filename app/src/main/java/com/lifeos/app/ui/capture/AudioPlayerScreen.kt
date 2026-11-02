package com.lifeos.app.ui.capture

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.media.VoiceAudioPlayer
import com.lifeos.app.core.media.VoicePlaybackState
import com.lifeos.app.core.media.formatAudioDuration
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.ui.components.LifeOSAudioPlayer
import com.lifeos.app.ui.components.LifeOSEmptyState
import com.lifeos.app.ui.components.LifeOSIconButton
import com.lifeos.app.ui.components.LifeOSLoadingState
import com.lifeos.app.ui.components.LifeOSSurface
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class AudioPlayerUiState(
    val loading: Boolean = true,
    val capture: CaptureEntity? = null,
    val deleted: Boolean = false
)

/**
 * The voice capture player.
 *
 * Reads the row the recorder wrote and hands the file to the app's single audio
 * transport, so a clip started here and a clip started on the timeline can
 * never play at the same time. Deleting removes both the row and its file — the
 * existing [CaptureRepository.delete] contract — which is also what removes the
 * entry from the Life timeline.
 */
class AudioPlayerViewModel(
    private val captureRepository: CaptureRepository,
    private val player: VoiceAudioPlayer,
    private val captureId: String
) : ViewModel() {

    private val _ui = MutableStateFlow(AudioPlayerUiState())
    val ui: StateFlow<AudioPlayerUiState> = _ui.asStateFlow()

    val playback: StateFlow<VoicePlaybackState> = player.state

    init {
        viewModelScope.launch {
            val capture = runCatching { captureRepository.getById(captureId) }.getOrNull()
            _ui.update { it.copy(loading = false, capture = capture) }
        }
    }

    fun togglePlayback() {
        val capture = _ui.value.capture ?: return
        val path = capture.filePath ?: return
        player.toggle(path, capture.durationMillis ?: 0L)
    }

    fun seekTo(positionMillis: Long) = player.seekTo(positionMillis)

    /** Called by the screen's ticker while a clip is playing. */
    fun tickPlayback() = player.tick()

    fun delete() {
        viewModelScope.launch {
            player.release()
            captureRepository.delete(captureId)
            _ui.update { it.copy(deleted = true) }
        }
    }

    override fun onCleared() {
        // Leaving the player stops the audio: a journal app should never keep
        // playing a voice note from a screen the user has walked away from.
        player.release()
        super.onCleared()
    }
}

/**
 * The full-window player for one saved voice capture: the clip's real title,
 * when it was recorded, how long it is, how big it is, and the transport.
 */
@Composable
fun AudioPlayerScreen(captureId: String, onBack: () -> Unit) {
    val locator = LocalServiceLocator.current
    val viewModel: AudioPlayerViewModel = viewModel(
        factory = LambdaViewModelFactory {
            AudioPlayerViewModel(locator.captureRepository, locator.voiceAudioPlayer, captureId)
        }
    )
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()

    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(playback is VoicePlaybackState.Playing) {
        while (playback is VoicePlaybackState.Playing) {
            kotlinx.coroutines.delay(PLAYBACK_TICK_MILLIS)
            viewModel.tickPlayback()
        }
    }

    DisposableEffect(Unit) {
        onDispose { locator.voiceAudioPlayer.release() }
    }

    LaunchedEffect(ui.deleted) {
        if (ui.deleted) onBack()
    }

    BackHandler(onBack = onBack)

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this recording?") },
            text = { Text("The clip and its place in your timeline are removed from this device. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } }
        )
    }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.lg)
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    start = LifeOSSpacing.screenPadding,
                    end = LifeOSSpacing.screenPadding,
                    top = LifeOSSpacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LifeOSIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
            Column(Modifier.weight(1f).padding(start = LifeOSSpacing.md)) {
                Text(
                    "Voice note",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Stored privately on this device",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ui.capture?.let {
                LifeOSIconButton(
                    icon = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete recording",
                    onClick = { confirmDelete = true }
                )
            }
        }

        when {
            ui.loading -> LifeOSLoadingState(label = "Opening your recording…")

            ui.capture == null -> LifeOSEmptyState(
                icon = Icons.Filled.GraphicEq,
                title = "This recording is gone",
                body = "It was deleted, so there is nothing left to play. Your other memories are untouched."
            )

            else -> {
                val capture = ui.capture!!
                LifeOSAudioPlayer(
                    state = playback,
                    onToggle = viewModel::togglePlayback,
                    onSeek = viewModel::seekTo,
                    title = capture.text ?: "Voice note",
                    modifier = Modifier.padding(horizontal = LifeOSSpacing.screenPadding)
                )

                VoiceMetaCard(
                    capture = capture,
                    modifier = Modifier.padding(horizontal = LifeOSSpacing.screenPadding)
                )
            }
        }
    }
}

/** What is actually stored for this clip: date, length, size, file type. */
@Composable
private fun VoiceMetaCard(capture: CaptureEntity, modifier: Modifier = Modifier) {
    val path = capture.filePath
    val sizeBytes = remember(path) {
        path?.let { runCatching { File(it).length() }.getOrDefault(0L) } ?: 0L
    }
    val fileExists = remember(path) { path != null && runCatching { File(path).exists() }.getOrDefault(false) }

    LifeOSSurface(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
            MetaRow("Recorded", DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(DateTimeUtils.epochDayOf(capture.createdAt))) +
                " · " + DateTimeUtils.formatMinutes(DateTimeUtils.minutesOfDay(capture.createdAt)))
            MetaRow(
                "Length",
                capture.durationMillis?.takeIf { it > 0 }?.let { formatAudioDuration(it) } ?: "Unknown"
            )
            MetaRow("File", capture.mimeType ?: "audio/mp4")
            if (sizeBytes > 0) MetaRow("Size", formatFileSize(sizeBytes))
            if (!fileExists) {
                Text(
                    "The audio file is no longer on this device, so this entry can be read but not played.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private const val PLAYBACK_TICK_MILLIS = 120L

/** Padding used by screens that host the player directly. */
internal val voiceScreenPadding = PaddingValues(LifeOSSpacing.screenPadding)
