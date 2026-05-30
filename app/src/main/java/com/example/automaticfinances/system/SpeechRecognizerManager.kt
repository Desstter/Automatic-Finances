package com.example.automaticfinances.system

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** High-level outcome/error categories for speech recognition, decoupled from platform codes. */
enum class SttError {
    NOT_AVAILABLE,    // No recognition service on device
    PERMISSION,       // RECORD_AUDIO not granted
    NETWORK,          // Online recognizer unreachable
    AUDIO,            // Mic/audio capture problem
    NO_MATCH,         // Heard nothing intelligible
    SPEECH_TIMEOUT,   // Silence / user didn't speak
    BUSY,             // Recognizer busy
    OTHER,
}

/** Streamed events from a single listening session. The flow completes after a final result or error. */
sealed interface SttEvent {
    data object ReadyForSpeech : SttEvent
    data object BeginningOfSpeech : SttEvent
    data class RmsChanged(val rms: Float) : SttEvent
    data class PartialResult(val text: String) : SttEvent
    data class FinalResult(val text: String) : SttEvent
    data class Failed(val error: SttError, val message: String) : SttEvent
}

/**
 * Wraps Android's [SpeechRecognizer] behind a cold [Flow]. Collecting [listen] starts a session;
 * cancelling the collector (e.g. the user dismisses the sheet) tears the recognizer down. All
 * platform calls are marshaled onto the main thread, which [SpeechRecognizer] requires.
 *
 * The flow is single-shot: it emits intermediate events and then completes on the first
 * [SttEvent.FinalResult] or [SttEvent.Failed].
 */
@Singleton
class SpeechRecognizerManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun listen(languageTag: String = "es-CO"): Flow<SttEvent> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(SttEvent.Failed(SttError.NOT_AVAILABLE, "El reconocimiento de voz no está disponible en este dispositivo"))
            close()
            return@callbackFlow
        }

        val mainHandler = Handler(Looper.getMainLooper())
        var recognizer: SpeechRecognizer? = null

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { trySend(SttEvent.ReadyForSpeech) }
            override fun onBeginningOfSpeech() { trySend(SttEvent.BeginningOfSpeech) }
            override fun onRmsChanged(rmsdB: Float) { trySend(SttEvent.RmsChanged(rmsdB)) }
            override fun onBufferReceived(buffer: ByteArray?) { /* no-op */ }
            override fun onEndOfSpeech() { /* recognizer keeps processing until onResults */ }

            override fun onError(error: Int) {
                trySend(SttEvent.Failed(mapError(error), messageFor(error)))
                close()
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                    .trim()
                trySend(SttEvent.FinalResult(text))
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                    .trim()
                if (text.isNotEmpty()) trySend(SttEvent.PartialResult(text))
            }

            override fun onEvent(eventType: Int, params: Bundle?) { /* no-op */ }
        }

        mainHandler.post {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
                startListening(buildRecognizerIntent(languageTag))
            }
        }

        awaitClose {
            mainHandler.post {
                recognizer?.run {
                    runCatching { stopListening() }
                    runCatching { cancel() }
                    runCatching { destroy() }
                }
                recognizer = null
            }
        }
    }

    private fun buildRecognizerIntent(languageTag: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Favour offline if the es-CO pack is installed, but allow online fallback.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

    private fun mapError(error: Int): SttError = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SttError.PERMISSION
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SttError.NETWORK
        SpeechRecognizer.ERROR_AUDIO -> SttError.AUDIO
        SpeechRecognizer.ERROR_NO_MATCH -> SttError.NO_MATCH
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SttError.SPEECH_TIMEOUT
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SttError.BUSY
        else -> SttError.OTHER
    }

    private fun messageFor(error: Int): String = when (mapError(error)) {
        SttError.PERMISSION -> "Falta el permiso de micrófono"
        SttError.NETWORK -> "Sin conexión para reconocer la voz"
        SttError.AUDIO -> "No se pudo acceder al micrófono"
        SttError.NO_MATCH -> "No te entendí, intenta de nuevo"
        SttError.SPEECH_TIMEOUT -> "No escuché nada, intenta de nuevo"
        SttError.BUSY -> "El reconocedor está ocupado, intenta de nuevo"
        SttError.NOT_AVAILABLE -> "El reconocimiento de voz no está disponible"
        SttError.OTHER -> "Ocurrió un error al reconocer la voz"
    }
}
