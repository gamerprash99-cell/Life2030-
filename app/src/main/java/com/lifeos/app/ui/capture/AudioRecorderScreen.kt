package com.lifeos.app.ui.capture

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.media.LifeOSAudioRecorder
import com.lifeos.app.core.media.VoiceAudioPlayer
import com.lifeos.app.core.media.VoicePlaybackState
import com.lifeos.app.core.media.VoiceRecordingFailure
import com.lifeos.app.core.media.VoiceRecordingState
import com.lifeos.app.core.util.LifeOSPermissions
import com.lifeos.app.core.util.PermissionStatus
import com.lifeos.app.core.util.rememberPermissionState
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.ui.components.LifeOSAudioPlayer
import com.lifeos.app.ui.components.LifeOSBanner
import com.lifeos.app.ui.components.LifeOSDateHeader
import com.lifeos.app.ui.components.LifeOSEmptyState
import com.lifeos.app.ui.components.LifeOSIconButton
import com.lifeos.app.ui.components.LifeOSLiveWaveform
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.LifeOSRecordingControls
import com.lifeos.app.ui.components.LifeOSSecondaryButton
import com.lifeos.app.ui.components.LifeOSTextField
import com.lifeos.app.ui.components.LifeOSWaveformSeek
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the recorder screen is showing beyond the transport itself: the title
 * being typed, whether the take is being written to the database, and any
 * refusal the user needs to see.
 */
data class AudioRecorderUiState(
    val title: String = "",
    val saving: Boolean = false,
    val savedCaptureId: String? = null,
    val error: String? = null
)

/**
 * The voice recorder.
 *
 * Writes a real file with the platform recorder ([LifeOSAudioRecorder]) and, on
 * save, one row in the existing `captures` table — the same table every other
 * capture writes to, so the recording appears on the Life timeline and in
 * search with no extra bookkeeping and no second store.
 *
 * The frequency of the tick is a UI decision: fast enough for the meter to read
 * as live, slow enough not to burn the battery on a long take.
 */
class AudioRecorderViewModel(
    private val recorder: LifeOSAudioRecorder,
    private val captureRepository: CaptureRepository,
    private val player: VoiceAudioPlayer
) : ViewModel() {

    val recording: StateFlow<VoiceRecordingState> = recorder.state

    private val _ui = MutableStateFlow(AudioRecorderUiState())
    val ui: StateFlow<AudioRecorderUiState> = _ui.asStateFlow()

    val playback: StateFlow<VoicePlaybackState> = player.state

    private var savedTitle: String? = null

    fun dismissError() = _ui.update { it.copy(error = null) }

    fun start() {
        _ui.update { it.copy(error = null) }
        val state = recorder.start()
        if (state is VoiceRecordingState.Failed) report(state.reason)
    }

    fun pause() {
        if (recorder.state.value is VoiceRecordingState.Recording) recorder.pause()
    }

    fun resume() {
        if (recorder.state.value is VoiceRecordingState.Paused) recorder.resume()
    }

    fun stop() {
        val state = recorder.stop()
        if (state is VoiceRecordingState.Failed) report(state.reason)
    }

    /** Called by the screen's ticker while a take is live. */
    fun tick() = recorder.tick()

    fun onTitleChange(value: String) {
        _ui.update { it.copy(title = value) }
    }

    /**
     * Writes the finished clip to the database. The row carries the real file
     * path, the measured duration and the platform MIME type, so playback later
     * never has to guess.
     */
    fun save() {
        val finished = recording.value as? VoiceRecordingState.Finished ?: return
        if (_ui.value.saving || _ui.value.savedCaptureId != null) return
        _ui.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                captureRepository.create(
                    type = CaptureType.AUDIO,
                    text = _ui.value.title.trim().ifBlank { DEFAULT_TITLE },
                    filePath = finished.filePath,
                    mimeType = LifeOSAudioRecorder.MIME_TYPE,
                    durationMillis = finished.durationMillis,
                    tags = listOf("voice")
                )
            }.onSuccess { id ->
                savedTitle = id
                player.release()
                _ui.update { it.copy(saving = false, savedCaptureId = id) }
            }.onFailure {
                _ui.update {
                    it.copy(
                        saving = false,
                        error = "This recording couldn't be saved. Its file is still on the device — try again."
                    )
                }
            }
        }
    }

    /** Throws away the finished take and its file. */
    fun discard() {
        player.release()
        recorder.discardFinished()
        _ui.update { it.copy(title = "", error = null) }
    }

    /**
     * Abandons a live take. Used by Back and by the window losing focus, so an
     * interrupted recording never lingers as a half-open microphone session.
     */
    fun cancelTake() {
        recorder.cancel()
        _ui.update { it.copy(error = null) }
    }

    /** Plays back the take just recorded, through the app-wide transport. */
    fun togglePlayback() {
        val finished = recording.value as? VoiceRecordingState.Finished ?: return
        player.toggle(finished.filePath, finished.durationMillis)
    }

    fun seekTo(positionMillis: Long) = player.seekTo(positionMillis)

    /** Called by the ticker while playback is running so the bar advances. */
    fun tickPlayback() = player.tick()

    private fun report(reason: VoiceRecordingFailure) {
        val message = when (reason) {
            VoiceRecordingFailure.START_FAILED ->
                "Recording couldn't start. Another app may be using the microphone — close it and try again."
            VoiceRecordingFailure.STOP_FAILED ->
                "That recording couldn't be finished, so nothing was saved. Please record it again."
            VoiceRecordingFailure.TOO_SHORT ->
                "That was too short to keep. Hold on for at least a second next time."
            VoiceRecordingFailure.NO_MICROPHONE ->
                "This device doesn't have a microphone LifeOS can record from."
        }
        _ui.update { it.copy(error = message) }
    }

    override fun onCleared() {
        // Leaving the screen mid-take must not leave an orphan file or an open
        // microphone behind; a saved recording is untouched by this.
        player.release()
        recorder.release()
        super.onCleared()
    }

    companion object {
        const val DEFAULT_TITLE = "Voice note"
    }
}

/**
 * The full-window voice recorder: record → pause/resume → stop → review →
 * save. RECORD_AUDIO is requested when the user actually starts a recording,
 * never on entry, and the denied/permanently-denied paths each explain what to
 * do instead of silently failing.
 */
@Composable
fun AudioRecorderScreen(
    onClose: () -> Unit,
    onSaved: (captureId: String) -> Unit
) {
    val locator = LocalServiceLocator.current
    val context = LocalContext.current
    val viewModel: AudioRecorderViewModel = viewModel(
        factory = LambdaViewModelFactory {
            AudioRecorderViewModel(
                recorder = LifeOSAudioRecorder(context.applicationContext),
                captureRepository = locator.captureRepository,
                player = locator.voiceAudioPlayer
            )
        }
    )

    val permission = rememberPermissionState(LifeOSPermissions.RECORD_AUDIO)
    val recording by viewModel.recording.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    val isLive = recording is VoiceRecordingState.Recording || recording is VoiceRecordingState.Paused
    val finished = recording as? VoiceRecordingState.Finished

    // Live meter: only ticks while the microphone is actually open.
    LaunchedEffect(recording is VoiceRecordingState.Recording) {
        while (recording is VoiceRecordingState.Recording) {
            delay(TICK_MILLIS)
            viewModel.tick()
        }
    }

    // Playback progress: only ticks while a clip is actually playing.
    LaunchedEffect(playback is VoicePlaybackState.Playing) {
        while (playback is VoicePlaybackState.Playing) {
            delay(TICK_MILLIS)
            viewModel.tickPlayback()
        }
    }

    // A call, a lock or another app taking the foreground holds the microphone
    // closed rather than recording silence; returning lets the user resume.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (recording is VoiceRecordingState.Recording) viewModel.pause()
    }

    DisposableEffect(Unit) {
        onDispose { locator.voiceAudioPlayer.release() }
    }

    LaunchedEffect(ui.savedCaptureId) {
        ui.savedCaptureId?.let(onSaved)
    }

    BackHandler {
        when {
            isLive -> viewModel.cancelTake()
            finished != null -> viewModel.discard()
            else -> onClose()
        }
    }

    // Tapping the microphone first and granting access second should start the
    // take, not ask the user to tap again.
    var pendingStart by remember { mutableStateOf(false) }
    LaunchedEffect(permission.isGranted) {
        if (permission.isGranted && pendingStart) {
            pendingStart = false
            viewModel.start()
        }
    }

    val micDenied = !permission.isGranted

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LifeOSSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.lg)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = LifeOSSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Voice",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Recorded on this device and stored privately in LifeOS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LifeOSIconButton(Icons.Filled.Close, contentDescription = "Close recorder", onClick = onClose)
        }

        ui.error?.let { message ->
            LifeOSBanner(
                icon = Icons.Filled.GraphicEq,
                text = message,
                action = "Dismiss",
                onAction = viewModel::dismissError
            )
        }

        when {
            micDenied -> MicrophonePermissionPanel(permission.status, permission.request, permission.openSettings)

            finished != null -> FinishedTakePanel(
                finished = finished,
                title = ui.title,
                onTitleChange = viewModel::onTitleChange,
                playback = playback,
                saving = ui.saving,
                onTogglePlayback = viewModel::togglePlayback,
                onSeek = viewModel::seekTo,
                onSave = viewModel::save,
                onDiscard = viewModel::discard,
                onRecordAgain = viewModel::discard
            )

            else -> RecordingPanel(
                recording = recording,
                onStart = {
                    if (permission.isGranted) {
                        viewModel.start()
                    } else {
                        pendingStart = true
                        permission.request()
                    }
                },
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onStop = viewModel::stop
            )
        }

        Spacer(Modifier.height(LifeOSSpacing.xl))
    }
}

/** Recording in progress (or waiting to be started). */
@Composable
private fun RecordingPanel(
    recording: VoiceRecordingState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.lg)) {
        LifeOSLiveWaveform(
            levels = (recording as? VoiceRecordingState.Recording)?.waveform
                ?: (recording as? VoiceRecordingState.Paused)?.waveform
                ?: emptyList(),
            active = recording is VoiceRecordingState.Recording
        )
        LifeOSRecordingControls(
            state = recording,
            onStart = onStart,
            onPause = onPause,
            onResume = onResume,
            onStop = onStop
        )
    }
}

/** Reviewing a finished take: play it back, name it, keep it or throw it away. */
@Composable
private fun FinishedTakePanel(
    finished: VoiceRecordingState.Finished,
    title: String,
    onTitleChange: (String) -> Unit,
    playback: VoicePlaybackState,
    saving: Boolean,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onRecordAgain: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.lg)) {
        LifeOSDateHeader(
            label = "Recording finished",
            epochDay = com.lifeos.app.core.util.DateTimeUtils.today().toEpochDay(),
            entryCount = 1
        )
        LifeOSAudioPlayer(
            state = playback,
            onToggle = onTogglePlayback,
            onSeek = onSeek,
            title = "Preview",
            waveform = finished.waveform
        )
        LifeOSTextField(
            value = title,
            onValueChange = onTitleChange,
            label = "Name this recording",
            placeholder = AudioRecorderViewModel.DEFAULT_TITLE,
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
            LifeOSPrimaryButton(
                text = if (saving) "Saving…" else "Save to my timeline",
                onClick = onSave,
                enabled = !saving
            )
            LifeOSSecondaryButton(text = "Record again", onClick = onRecordAgain)
        }
        Text(
            "Kept on this device: ${formatFileSize(finished.sizeBytes)} · ${com.lifeos.app.core.media.formatAudioDuration(finished.durationMillis)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The microphone refused: say what happened and offer the right next step. */
@Composable
private fun MicrophonePermissionPanel(
    status: PermissionStatus,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val permanentlyDenied = status == PermissionStatus.PERMANENTLY_DENIED
    LifeOSEmptyState(
        icon = Icons.Filled.GraphicEq,
        title = "Microphone access is off",
        body = if (permanentlyDenied) {
            "LifeOS can only record with the microphone permission. Android won't ask again, so turn it on in Settings — the recording never leaves this device either way."
        } else {
            "LifeOS records voice notes with the device microphone. Nothing is uploaded; the clip is written to private app storage."
        },
        actionLabel = if (permanentlyDenied) "Open Settings" else "Allow microphone",
        onAction = if (permanentlyDenied) onOpenSettings else onRequest
    )
}

/** `184 KB` / `1.2 MB` — the real size of the file on disk. */
internal fun formatFileSize(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    return when {
        safe < 1024 -> "$safe B"
        safe < 1024 * 1024 -> "${(safe / 1024)} KB"
        else -> String.format(java.util.Locale.US, "%.1f MB", safe / (1024.0 * 1024.0))
    }
}

private const val TICK_MILLIS = 100L
