package com.lifeos.app.core.media

import kotlinx.coroutines.flow.StateFlow

/**
 * The recording capability the diary composer needs, expressed without any
 * Android type.
 *
 * This exists so the composer's logic — when a recording may start, what happens
 * to the file when it is cancelled, what duration gets stored — can be tested on
 * a plain JVM. The real implementation ([DiaryAudioRecorder]) needs a `Context`
 * and a `MediaRecorder`, neither of which a JVM unit test can provide.
 */
interface AudioRecorder {
    /** Live capture state, including the measured elapsed time. */
    val state: StateFlow<RecordingState>

    /** Begins capture. Returns the resulting state, or a failure state. */
    fun start(): RecordingState

    /** Advances the elapsed-time reading. Driven by the UI while recording. */
    fun tick(): RecordingState

    /** Stops and keeps the file, returning the state carrying its duration. */
    fun stop(): RecordingState

    /** Stops and deletes the file. */
    fun cancel()

    fun isRecording(): Boolean
}
