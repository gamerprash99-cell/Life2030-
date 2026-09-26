package com.lifeos.app.core.media

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Plays back a recorded diary voice note with the platform [MediaPlayer].
 *
 * The clip is read from app-private storage, so playback never needs a storage
 * permission and never depends on a content provider still holding a grant.
 * A missing file (deleted by the user, or a stale row from a restore) resolves
 * to [PlaybackState.MissingFile] rather than throwing, so a historical entry
 * can still be read.
 */
class DiaryAudioPlayer {

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var playingPath: String? = null

    /**
     * Starts (or restarts) playback of [filePath]. Calling it for the clip that
     * is already playing toggles to a stop, which is what the single play/pause
     * button in the UI needs.
     */
    fun toggle(filePath: String): PlaybackState {
        if (playingPath == filePath && _state.value is PlaybackState.Playing) {
            stop()
            return _state.value
        }
        return play(filePath)
    }

    fun play(filePath: String): PlaybackState {
        releasePlayer()

        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            _state.value = PlaybackState.MissingFile
            return _state.value
        }

        val created: MediaPlayer = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
        } catch (_: Exception) {
            _state.value = PlaybackState.Failed
            return _state.value
        }

        player = created
        playingPath = filePath
        _state.value = PlaybackState.Playing(filePath = filePath, positionMillis = 0L)
        return _state.value
    }

    fun stop() {
        releasePlayer()
        _state.value = PlaybackState.Idle
    }

    private fun releasePlayer() {
        player?.let { active ->
            runCatching { if (active.isPlaying) active.stop() }
            runCatching { active.release() }
        }
        player = null
        playingPath = null
    }

    /** Releases the decoder. Safe to call from `onDispose`. */
    fun release() = stop()
}

/** Playback state mirrored into the UI. */
sealed interface PlaybackState {
    data object Idle : PlaybackState
    data class Playing(val filePath: String, val positionMillis: Long) : PlaybackState
    data object Failed : PlaybackState

    /** The referenced file no longer exists on disk. */
    data object MissingFile : PlaybackState
}
