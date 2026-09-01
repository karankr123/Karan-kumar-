package com.example.model

sealed interface JarvisState {
    data object Idle : JarvisState
    data class Listening(val rmsDb: Float = 0f) : JarvisState
    data object Processing : JarvisState
    data class Speaking(val text: String) : JarvisState
    data class Error(val message: String) : JarvisState
}
