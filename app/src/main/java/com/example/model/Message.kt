package com.example.model

enum class Sender {
    USER,
    JARVIS
}

enum class MessageStatus {
    SENT,
    SPEAKING,
    ERROR
}

data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val isMock: Boolean = false
)
