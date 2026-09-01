package com.example.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class JarvisTtsManager(
    context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) {
    private val tag = "JarvisTtsManager"
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    var onSpeechStart: () -> Unit = {}
    var onSpeechDone: () -> Unit = {}
    var onSpeechError: (String) -> Unit = {}

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                configureTts()
                onInitComplete(true)
            } else {
                Log.e(tag, "TTS Initialization failed with status: $status")
                isInitialized = false
                onInitComplete(false)
            }
        }
    }

    private fun configureTts() {
        textToSpeech?.let { tts ->
            // Try Indian English or US English or device default for crisp natural voice
            val inLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()
            val localeResult = tts.setLanguage(inLocale)
            if (localeResult == TextToSpeech.LANG_MISSING_DATA || localeResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US)
            }
            // Calm, confident, futuristic JARVIS pacing
            tts.setPitch(0.92f)
            tts.setSpeechRate(1.02f)

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(tag, "TTS Started: $utteranceId")
                    onSpeechStart()
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(tag, "TTS Done: $utteranceId")
                    onSpeechDone()
                }

                override fun onError(utteranceId: String?) {
                    Log.w(tag, "TTS Error: $utteranceId")
                    onSpeechError("Error synthesizing speech")
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.w(tag, "TTS Error code: $errorCode for $utteranceId")
                    onSpeechError("Error synthesizing speech ($errorCode)")
                }
            })
        }
    }

    fun speak(text: String, utteranceId: String = "JARVIS_${System.currentTimeMillis()}") {
        if (!isInitialized || textToSpeech == null) {
            Log.w(tag, "TTS not initialized yet")
            return
        }

        stop()

        val cleanText = sanitizeForSpeech(text)
        if (cleanText.isBlank()) return

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.w(tag, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isInitialized = false
        } catch (e: Exception) {
            Log.w(tag, "Error shutting down TTS", e)
        }
    }

    private fun sanitizeForSpeech(raw: String): String {
        return raw
            .replace(Regex("[*#`_~>]"), "") // Remove markdown asterisks, hashtags, backticks
            .replace(Regex("https?://\\S+"), "link") // Simplify URLs
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
