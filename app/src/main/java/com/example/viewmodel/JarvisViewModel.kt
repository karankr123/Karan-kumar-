package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.JarvisState
import com.example.model.Message
import com.example.model.MessageStatus
import com.example.model.Sender
import com.example.service.JarvisAiService
import com.example.speech.JarvisSpeechRecognizer
import com.example.speech.JarvisTtsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "JarvisViewModel"

    private val aiService = JarvisAiService()

    private val _state = MutableStateFlow<JarvisState>(JarvisState.Idle)
    val state: StateFlow<JarvisState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isSpeechAvailable = MutableStateFlow(true)
    val isSpeechAvailable: StateFlow<Boolean> = _isSpeechAvailable.asStateFlow()

    private val _lastModelSource = MutableStateFlow("JARVIS Core")
    val lastModelSource: StateFlow<String> = _lastModelSource.asStateFlow()

    private var speechRecognizer: JarvisSpeechRecognizer? = null
    private var ttsManager: JarvisTtsManager? = null
    private var processingJob: Job? = null

    init {
        initializeTts()
        initializeSpeechRecognizer()
        addInitialJarvisGreeting()
    }

    private fun addInitialJarvisGreeting() {
        val initialGreeting = Message(
            text = "JARVIS systems online. Standing by for voice directives.",
            sender = Sender.JARVIS,
            status = MessageStatus.SENT,
            isMock = true
        )
        _messages.value = listOf(initialGreeting)
    }

    private fun initializeTts() {
        ttsManager = JarvisTtsManager(getApplication()) { isReady ->
            _isTtsReady.value = isReady
            if (isReady) {
                Log.d(tag, "TTS ready for audio synthesis")
            }
        }.apply {
            onSpeechStart = {
                val current = _state.value
                if (current !is JarvisState.Speaking) {
                    // Update state to speaking if not already
                    val lastMsg = _messages.value.lastOrNull { it.sender == Sender.JARVIS }
                    _state.value = JarvisState.Speaking(lastMsg?.text.orEmpty())
                }
            }
            onSpeechDone = {
                _state.value = JarvisState.Idle
            }
            onSpeechError = { errorMsg ->
                Log.w(tag, "TTS playback error: $errorMsg")
                _state.value = JarvisState.Idle
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = JarvisSpeechRecognizer(
            context = getApplication(),
            onReady = {
                _state.value = JarvisState.Listening(0f)
                _partialText.value = ""
            },
            onRmsChanged = { rmsDb ->
                // Normalize rmsDb (-2f to 10f approx) to 0f..1f for smooth UI pulses
                val normalized = ((rmsDb + 2f) / 12f).coerceIn(0f, 1f)
                _rmsLevel.value = normalized
                if (_state.value is JarvisState.Listening) {
                    _state.value = JarvisState.Listening(normalized)
                }
            },
            onPartialResult = { partial ->
                _partialText.value = partial
            },
            onResult = { recognizedText ->
                _partialText.value = ""
                _rmsLevel.value = 0f
                handleRecognizedSpeech(recognizedText)
            },
            onError = { errorMessage ->
                _partialText.value = ""
                _rmsLevel.value = 0f
                _state.value = JarvisState.Error(errorMessage)
            },
            onEndOfSpeech = {
                _state.value = JarvisState.Processing
            }
        )

        _isSpeechAvailable.value = speechRecognizer?.isAvailable() == true
    }

    fun onMicTapped(hasPermission: Boolean, requestPermission: () -> Unit) {
        if (!hasPermission) {
            requestPermission()
            return
        }

        when (val currentState = _state.value) {
            is JarvisState.Listening -> {
                // User tapped to stop listening early
                speechRecognizer?.stopListening()
            }
            is JarvisState.Speaking -> {
                // If speaking, tap stops speaking and transitions to listening
                stopSpeaking()
                startListeningInternal()
            }
            is JarvisState.Processing -> {
                // If currently processing, cancel and listen afresh
                processingJob?.cancel()
                startListeningInternal()
            }
            else -> {
                startListeningInternal()
            }
        }
    }

    private fun startListeningInternal() {
        // Stop any running TTS before recording speech
        ttsManager?.stop()
        _partialText.value = ""
        _state.value = JarvisState.Listening(0f)
        speechRecognizer?.startListening()
    }

    fun handleRecognizedSpeech(speechText: String) {
        if (speechText.isBlank()) {
            _state.value = JarvisState.Idle
            return
        }
        sendUserMessage(speechText)
    }

    fun sendUserMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        // Stop any speech synthesis
        ttsManager?.stop()

        // 1. Add user message to history
        val userMsg = Message(
            text = trimmed,
            sender = Sender.USER,
            status = MessageStatus.SENT
        )
        _messages.value = _messages.value + userMsg

        // 2. Transition state to Processing
        _state.value = JarvisState.Processing

        // 3. Process AI query asynchronously
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            try {
                // Prepare conversation history
                val history = _messages.value.mapNotNull { msg ->
                    if (msg.sender == Sender.USER) {
                        Pair(msg.text, "")
                    } else {
                        null
                    }
                }

                val aiResponse = aiService.getResponse(trimmed, history)
                _lastModelSource.value = aiResponse.modelName

                val jarvisMsg = Message(
                    text = aiResponse.replyText,
                    sender = Sender.JARVIS,
                    status = MessageStatus.SENT,
                    isMock = aiResponse.isMock
                )
                _messages.value = _messages.value + jarvisMsg

                // 4. Trigger TTS speech playback
                _state.value = JarvisState.Speaking(aiResponse.replyText)
                ttsManager?.speak(aiResponse.replyText)

            } catch (e: Exception) {
                Log.e(tag, "Error processing AI response", e)
                val errorMsg = "Unable to process query: ${e.localizedMessage ?: "Unknown error"}"
                _state.value = JarvisState.Error(errorMsg)
            }
        }
    }

    fun stopSpeaking() {
        ttsManager?.stop()
        if (_state.value is JarvisState.Speaking) {
            _state.value = JarvisState.Idle
        }
    }

    fun clearConversation() {
        stopSpeaking()
        speechRecognizer?.stopListening()
        processingJob?.cancel()
        _state.value = JarvisState.Idle
        _partialText.value = ""
        _messages.value = emptyList()
    }

    fun replayMessage(message: Message) {
        if (message.sender == Sender.JARVIS) {
            stopSpeaking()
            _state.value = JarvisState.Speaking(message.text)
            ttsManager?.speak(message.text)
        }
    }

    fun resetStateToIdle() {
        _state.value = JarvisState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        ttsManager?.shutdown()
        processingJob?.cancel()
    }
}
