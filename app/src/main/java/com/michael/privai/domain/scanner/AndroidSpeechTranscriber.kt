package com.michael.privai.domain.scanner

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

sealed interface SpeechTranscriptionEvent {
    data class Partial(val text: String) : SpeechTranscriptionEvent
    data class FinalSegment(val text: String) : SpeechTranscriptionEvent
    data class LanguagesReady(val locales: ResolvedSpeechLocales) : SpeechTranscriptionEvent
}

class AndroidSpeechTranscriber(private val context: Context) {

    fun startContinuousListeningFlow(): Flow<SpeechTranscriptionEvent> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            close(IllegalStateException("Speech recognition is not available on this device."))
            return@callbackFlow
        }

        val resolvedLocales = SpeechLocaleResolver.resolve(context)
        trySend(SpeechTranscriptionEvent.LanguagesReady(resolvedLocales))

        val languages = resolvedLocales.allowedLanguageTags
        var activeIndex = 0
        var intent = SpeechLocaleResolver.buildRecognizerIntent(
            resolvedLocales.copy(primaryLanguageTag = languages.first())
        )

        val mainHandler = Handler(Looper.getMainLooper())
        var isActive = true
        var consecutiveErrors = 0

        val recognizer = withContext(Dispatchers.Main) {
            SpeechLocaleResolver.createSpeechRecognizer(context)
        } ?: run {
            close(IllegalStateException("Speech recognizer is unavailable on this device."))
            return@callbackFlow
        }

        fun rebuildIntentForActiveLanguage() {
            val primary = languages[activeIndex.coerceIn(languages.indices)]
            intent = SpeechLocaleResolver.buildRecognizerIntent(
                resolvedLocales.copy(primaryLanguageTag = primary)
            )
        }

        fun restartListening(delayMs: Long = 0L) {
            if (!isActive) return
            mainHandler.postDelayed({
                if (!isActive) return@postDelayed
                try {
                    recognizer.startListening(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "startListening failed", e)
                    close(e)
                }
            }, delayMs)
        }

        fun rotateLanguageAfterSuccess() {
            if (languages.size <= 1) return
            activeIndex = (activeIndex + 1) % languages.size
            rebuildIntentForActiveLanguage()
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                consecutiveErrors = 0
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                if (!isActive) return
                Log.w(TAG, "Speech error=$error lang=${languages.getOrNull(activeIndex)}")
                consecutiveErrors++

                if (consecutiveErrors >= 8) {
                    close(
                        IllegalStateException(
                            "Speech engine failed repeatedly (error $error). " +
                                "Install Google app speech languages or check microphone access."
                        )
                    )
                    return
                }

                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        close(IllegalStateException("Microphone permission was denied."))
                        return
                    }

                    SpeechRecognizer.ERROR_AUDIO -> {
                        close(IllegalStateException("Microphone is unavailable or in use by another app."))
                        return
                    }

                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        if (languages.size > 1) rotateLanguageAfterSuccess()
                    }
                }

                mainHandler.post {
                    if (!isActive) return@post
                    try {
                        recognizer.cancel()
                    } catch (_: Exception) {
                    }
                    restartListening(delayMs = 250L)
                }
            }

            override fun onResults(results: Bundle?) {
                if (!isActive) return
                consecutiveErrors = 0
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    trySend(SpeechTranscriptionEvent.FinalSegment(text))
                    rotateLanguageAfterSuccess()
                }
                restartListening(delayMs = 100L)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (!isActive) return
                consecutiveErrors = 0
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    trySend(SpeechTranscriptionEvent.Partial(text))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        withContext(Dispatchers.Main) {
            recognizer.setRecognitionListener(listener)
            recognizer.startListening(intent)
        }

        awaitClose {
            isActive = false
            mainHandler.post {
                try {
                    recognizer.cancel()
                } catch (_: Exception) {
                }
                recognizer.destroy()
            }
        }
    }

    private companion object {
        const val TAG = "AndroidSpeechTranscriber"
    }
}
