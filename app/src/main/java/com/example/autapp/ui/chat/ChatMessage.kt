package com.example.autapp.ui.chat

/**
 * Data class representing a single chat message in the conversation.
 *
 * @property content The text content of the message
 * @property isUser Flag indicating if the message is from the user (true) or AI (false)
 * @property timestamp The timestamp when the message was created (defaults to current time)
 */
data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)