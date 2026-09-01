package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class JarvisSpeechRecognizer(
    private val context: Context,
    private val onReady: () -> Unit = {},
    private val onRmsChanged: (Float) -> Unit = {},
    private val onPartialResult: (String) -> Unit = {},
    private val onResult: (String) -> Unit = {},
    private val onError: (String) -> Unit = {},
    private val onEndOfSpeech: () -> Unit = {}
) {
    private val tag = "JarvisSpeech"
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        if (isListening) {
            stopListening()
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(tag, "onReadyForSpeech")
                    isListening = true
                    onReady()
                }

                override fun onBeginningOfSpeech() {
                    Log.d(tag, "onBeginningOfSpeech")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    onRmsChanged(rmsdB)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d(tag, "onEndOfSpeech")
                    isListening = false
                    onEndOfSpeech()
                }

                override fun onError(error: Int) {
                    isListening = false
                    try {
                        speechRecognizer?.destroy()
                        speechRecognizer = null
                    } catch (e: Exception) {
                        Log.w(tag, "Error clearing speech recognizer on error", e)
                    }

                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. Please check microphone."
                        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client paused. Tap to retry."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                        SpeechRecognizer.ERROR_NETWORK -> "Network issue while contacting speech service."
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out."
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap to try again."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Ready to listen."
                        SpeechRecognizer.ERROR_SERVER -> "Speech server issue. Please try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap microphone and speak."
                        else -> "Speech recognition notice ($error)"
                    }
                    Log.w(tag, "onError: $errorMsg ($error)")
                    onError(errorMsg)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognizedText = matches?.firstOrNull().orEmpty().trim()
                    Log.d(tag, "onResults: $recognizedText")
                    if (recognizedText.isNotBlank()) {
                        onResult(recognizedText)
                    } else {
                        onError("Could not recognize any speech.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty().trim()
                    if (text.isNotBlank()) {
                        onPartialResult(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                // Support multiple languages: English, Hindi, Hinglish
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("en-IN", "hi-IN", "en-US", "hi"))
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start speech recognizer", e)
            isListening = false
            onError(e.localizedMessage ?: "Failed to initialize speech recognizer")
        }
    }

    fun stopListening() {
        try {
            isListening = false
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(tag, "Error stopping speech recognizer", e)
        }
    }

    fun destroy() {
        try {
            isListening = false
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.w(tag, "Error destroying speech recognizer", e)
        }
    }
}
