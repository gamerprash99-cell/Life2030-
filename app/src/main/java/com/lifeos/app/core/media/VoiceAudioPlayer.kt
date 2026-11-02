package com.lifeos.app.core.media

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Playback state for one voice capture.
 *
 * [durationMillis] comes from the decoder, falling back to the stored duration
 * when the container does not report one; [positionMillis] is the decoder's own
 * position. A file that is no longer on disk resolves to [MissingFile] instead
 * of throwing, so a historical capture can still be read after its media was
 * removed.
 */
sealed interface VoicePlaybackState {
    data object Idle : VoicePlaybackState

    data class Preparing(val filePath: String) : VoicePlaybackState

    data class Playing(
        val filePath: String,
        val positionMillis: Long,
        val durationMillis: Long
    ) : VoicePlaybackState

    data class Paused(
        val filePath: String,
        val positionMillis: Long,
        val durationMillis: Long
    ) : VoicePlaybackState

    data class Completed(val filePath: String, val durationMillis: Long) : VoicePlaybackState

    data class MissingFile(val filePath: String) : VoicePlaybackState

    data class Failed(val filePath: String) : VoicePlaybackState
}

/**
 * One shared transport for every voice capture in the app, over the platform
 * [MediaPlayer].
 *
 * Held by the service locator rather than created per screen on purpose: with a
 * single instance, starting a clip on the timeline stops whatever the player
 * screen was playing, so two recordings can never talk over each other. The
 * decoder itself is released as soon as playback ends (stop, completion, error,
 * [release]), so an idle app holds no audio resources.
 *
 * Seeking and position reporting are real: [tick] reads the decoder's own
 * `currentPosition`, which is what the UI's progress and time labels show.
 */
class VoiceAudioPlayer {

    private val _state = MutableStateFlow<VoicePlaybackState>(VoicePlaybackState.Idle)
    val state: StateFlow<VoicePlaybackState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var currentPath: String? = null

    /** The clip this transport is currently holding, if any. */
    val activeFilePath: String? get() = currentPath

    /**
     * Starts [filePath], or pauses/resumes it when it is the clip already
     * loaded. This is exactly what a single play/pause button needs.
     */
    fun toggle(filePath: String, fallbackDurationMillis: Long = 0L): VoicePlaybackState =
        when (val current = _state.value) {
            is VoicePlaybackState.Playing ->
                if (current.filePath == filePath) pause() else play(filePath, fallbackDurationMillis)
            is VoicePlaybackState.Paused ->
                if (current.filePath == filePath) resume() else play(filePath, fallbackDurationMillis)
            else -> play(filePath, fallbackDurationMillis)
        }

    fun play(filePath: String, fallbackDurationMillis: Long = 0L): VoicePlaybackState {
        releasePlayer()

        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            currentPath = filePath
            _state.value = VoicePlaybackState.MissingFile(filePath)
            return _state.value
        }

        _state.value = VoicePlaybackState.Preparing(filePath)
        val created = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    val duration = runCatching { it.duration.toLong() }
                        .getOrDefault(fallbackDurationMillis)
                        .takeIf { it > 0 } ?: fallbackDurationMillis
                    releasePlayer()
                    currentPath = filePath
                    _state.value = VoicePlaybackState.Completed(filePath, duration)
                }
                setOnErrorListener { mp, _, _ ->
                    runCatching { mp.reset() }
                    releasePlayer()
                    currentPath = filePath
                    _state.value = VoicePlaybackState.Failed(filePath)
                    true
                }
                prepare()
                start()
            }
        } catch (_: Exception) {
            releasePlayer()
            currentPath = filePath
            _state.value = VoicePlaybackState.Failed(filePath)
            return _state.value
        }

        player = created
        currentPath = filePath
        _state.value = VoicePlaybackState.Playing(
            filePath = filePath,
            positionMillis = 0L,
            durationMillis = durationOf(created, fallbackDurationMillis)
        )
        return _state.value
    }

    fun pause(): VoicePlaybackState {
        val active = player ?: return _state.value
        val current = _state.value
        if (current !is VoicePlaybackState.Playing) return current
        runCatching { active.pause() }
        _state.value = VoicePlaybackState.Paused(
            filePath = current.filePath,
            positionMillis = runCatching { active.currentPosition.toLong() }.getOrDefault(current.positionMillis),
            durationMillis = current.durationMillis
        )
        return _state.value
    }

    fun resume(): VoicePlaybackState {
        val active = player ?: return _state.value
        val current = _state.value
        if (current !is VoicePlaybackState.Paused) return current
        runCatching { active.start() }
        _state.value = VoicePlaybackState.Playing(
            filePath = current.filePath,
            positionMillis = current.positionMillis,
            durationMillis = current.durationMillis
        )
        return _state.value
    }

    /** Jumps to [positionMillis]; keeps the transport state (playing stays playing). */
    fun seekTo(positionMillis: Long): VoicePlaybackState {
        val active = player ?: return _state.value
        val current = _state.value
        val duration = when (current) {
            is VoicePlaybackState.Playing -> current.durationMillis
            is VoicePlaybackState.Paused -> current.durationMillis
            else -> return current
        }
        val target = positionMillis.coerceIn(0L, duration.coerceAtLeast(0L))
        runCatching { active.seekTo(target.toInt()) }
        _state.value = when (current) {
            is VoicePlaybackState.Playing -> current.copy(positionMillis = target)
            is VoicePlaybackState.Paused -> current.copy(positionMillis = target)
            else -> current
        }
        return _state.value
    }

    /**
     * Refreshes the position while playing. Called from a UI ticker; the
     * decoder's own clock is the only source of truth for progress.
     */
    fun tick(): VoicePlaybackState {
        val active = player ?: return _state.value
        val current = _state.value
        if (current !is VoicePlaybackState.Playing) return current
        val position = runCatching { active.currentPosition.toLong() }.getOrDefault(current.positionMillis)
        val stillPlaying = runCatching { active.isPlaying }.getOrDefault(false)
        _state.value = current.copy(
            positionMillis = position,
            durationMillis = durationOf(active, current.durationMillis)
        )
        // The completion listener normally lands first; this covers the case
        // where it does not (some devices stop reporting isPlaying at the end).
        if (!stillPlaying && position >= current.durationMillis && current.durationMillis > 0L) {
            releasePlayer()
            currentPath = current.filePath
            _state.value = VoicePlaybackState.Completed(current.filePath, current.durationMillis)
        }
        return _state.value
    }

    /** Stops and drops the decoder. Safe to call more than once. */
    fun stop() {
        val path = currentPath
        releasePlayer()
        _state.value = if (path == null) VoicePlaybackState.Idle else VoicePlaybackState.Completed(path, 0L)
    }

    /** Drops the decoder and resets to idle — called when the owning screen leaves. */
    fun release() {
        releasePlayer()
        currentPath = null
        _state.value = VoicePlaybackState.Idle
    }

    private fun releasePlayer() {
        val active = player ?: return
        player = null
        runCatching { active.stop() }
        runCatching { active.reset() }
        runCatching { active.release() }
    }

    private fun durationOf(player: MediaPlayer, fallback: Long): Long =
        runCatching { player.duration.toLong() }.getOrDefault(-1L)
            .takeIf { it > 0 } ?: fallback.coerceAtLeast(0L)
}
