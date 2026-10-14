package com.lifeos.app.core.ai.runtime

import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Local-only voice input bridge for LIFE.
 * On Android 12+ it requires the platform's on-device recognizer. There is
 * deliberately no network fallback because LIFE must remain offline-first.
 */
class LifeVoiceInputController(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun start(
        languageTags: List<String> = listOf("en-IN", "hi-IN", "gu-IN"),
        onPartial: (String) -> Unit = {},
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stop()
        if (!isAvailable()) {
            onError("On-device voice recognition is not available on this device.")
            return
        }

        val speech = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer = speech
        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
            override fun onPartialResults(results: android.os.Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onPartial)
            }
            override fun onResults(results: android.os.Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onResult)
                stop()
            }
            override fun onError(error: Int) {
                onError(errorMessage(error))
                stop()
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag(languageTags.first()).toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, ArrayList(languageTags))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // The recognizer itself is the on-device implementation. No web fallback is requested.
        }
        speech.startListening(intent)
    }

    fun stop() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network voice recognition is unavailable; LIFE only uses on-device recognition."
        SpeechRecognizer.ERROR_NO_MATCH -> "I couldn't understand that."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything."
        else -> "Voice recognition failed."
    }
}
