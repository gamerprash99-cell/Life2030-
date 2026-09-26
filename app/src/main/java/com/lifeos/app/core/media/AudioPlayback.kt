package com.lifeos.app.core.media

import kotlinx.coroutines.flow.StateFlow

/**
 * The playback capability the diary screens need, expressed without any Android
 * type, so screen logic can be tested on a plain JVM. The real implementation
 * ([DiaryAudioPlayer]) wraps `MediaPlayer`.
 */
interface AudioPlayback {
    val state: StateFlow<PlaybackState>

    /** Starts the file, or stops it if it is the one already playing. */
    fun toggle(filePath: String): PlaybackState

    fun play(filePath: String): PlaybackState

    fun stop()

    /** Stops and drops the decoder. Safe to call more than once. */
    fun release()
}
