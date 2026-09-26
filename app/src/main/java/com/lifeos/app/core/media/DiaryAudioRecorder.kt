package com.lifeos.app.core.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import com.lifeos.app.core.util.MediaStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Real microphone recording for diary voice notes, backed by the platform
 * [MediaRecorder].
 *
 * Design notes:
 *  - `RECORD_AUDIO` is checked by the caller *before* [start] is ever invoked,
 *    so the system consent dialog is always triggered by a user tap on
 *    "Add voice note" and never at app launch.
 *  - The duration returned by [stop] is the recorder's own measured value
 *    (`MediaRecorder.maxAmplitude` is polled for a live level; the elapsed
 *    time comes from [SystemClock.elapsedRealtime]). It is never a constant.
 *  - Every failure path releases the recorder and deletes the partial file, so
 *    a failed take can never leave a corrupt clip referenced by an entry.
 */
class DiaryAudioRecorder(private val context: Context) {

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var targetFile: File? = null
    private var startedAtElapsedRealtime: Long = 0L

    /**
     * Begins a real recording into app-private storage.
     *
     * @return the file being written, or a [RecordingState.Failure] if the
     *   platform refused to start (mic in use, permission revoked mid-flight,
     *   storage error). The file is only handed back once the recorder has
     *   genuinely started.
     */
    fun start(): RecordingState {
        if (_state.value is RecordingState.Recording) return _state.value

        val file = MediaStorage.newDiaryAudioFile(context)
        val created = createRecorder(file)
        if (created == null) {
            _state.value = RecordingState.Failure(RecordingFailure.START_FAILED)
            return _state.value
        }

        recorder = created
        targetFile = file
        startedAtElapsedRealtime = SystemClock.elapsedRealtime()
        _state.value = RecordingState.Recording(elapsedMillis = 0L, amplitude = 0)
        return _state.value
    }

    private fun createRecorder(file: File): MediaRecorder? {
        var instance: MediaRecorder? = null
        return try {
            instance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            instance.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(AUDIO_BIT_RATE)
                setAudioSamplingRate(AUDIO_SAMPLE_RATE)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            instance
        } catch (_: Exception) {
            // A half-configured recorder holds the mic open; release it before
            // giving up, and remove the empty file prepare() may have created.
            runCatching { instance?.release() }
            file.delete()
            null
        }
    }

    /**
     * Polls the live recording clock. Call from a UI ticker while recording;
     * returns the state so a composable can render a real duration and a real
     * input level.
     */
    fun tick(): RecordingState {
        val active = _state.value
        if (active !is RecordingState.Recording) return active
        val elapsed = SystemClock.elapsedRealtime() - startedAtElapsedRealtime
        val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrElse { 0 }
        val next = RecordingState.Recording(elapsedMillis = elapsed, amplitude = amplitude)
        _state.value = next
        return next
    }

    /**
     * Stops the recording and returns the finished clip.
     *
     * A take shorter than [MIN_DURATION_MILLIS] is treated as an accidental tap
     * and discarded (file deleted) rather than persisted as an unplayable clip.
     */
    fun stop(): RecordingState {
        val active = recorder
        val file = targetFile
        val elapsed = SystemClock.elapsedRealtime() - startedAtElapsedRealtime

        if (active == null || file == null) {
            _state.value = RecordingState.Idle
            return _state.value
        }

        // Detach first: whether stop() throws or not, we must not leave a
        // released recorder reachable.
        recorder = null
        targetFile = null

        val stopped = runCatching { active.stop() }.isSuccess
        runCatching { active.release() }

        if (!stopped) {
            file.delete()
            _state.value = RecordingState.Failure(RecordingFailure.STOP_FAILED)
            return _state.value
        }
        if (elapsed < MIN_DURATION_MILLIS) {
            file.delete()
            _state.value = RecordingState.Failure(RecordingFailure.TOO_SHORT)
            return _state.value
        }
        if (!file.exists() || file.length() == 0L) {
            file.delete()
            _state.value = RecordingState.Failure(RecordingFailure.STOP_FAILED)
            return _state.value
        }

        _state.value = RecordingState.Finished(filePath = file.absolutePath, durationMillis = elapsed)
        return _state.value
    }

    /** Abandons an in-progress take and removes the partial file. */
    fun cancel() {
        recorder?.let { active ->
            runCatching { active.stop() }
            runCatching { active.release() }
        }
        recorder = null
        targetFile?.delete()
        targetFile = null
        _state.value = RecordingState.Idle
    }

    /** True while the mic is actually open, so the composable can gate the ticker. */
    fun isRecording(): Boolean = _state.value is RecordingState.Recording

    private companion object {
        const val AUDIO_BIT_RATE = 96_000
        const val AUDIO_SAMPLE_RATE = 44_100
        const val MIN_DURATION_MILLIS = 700L
    }
}

/** Live state of the recorder, mirrored into the UI. */
sealed interface RecordingState {
    data object Idle : RecordingState

    /** [elapsedMillis] and [amplitude] are measured, never estimated. */
    data class Recording(val elapsedMillis: Long, val amplitude: Int) : RecordingState

    data class Finished(val filePath: String, val durationMillis: Long) : RecordingState

    data class Failure(val reason: RecordingFailure) : RecordingState
}

enum class RecordingFailure { START_FAILED, STOP_FAILED, TOO_SHORT }

/** `m:ss` / `h:mm:ss` for a measured duration. */
fun formatAudioDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
