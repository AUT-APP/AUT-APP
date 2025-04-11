package com.example.autapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autapp.data.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val apiKey = "AIzaSyCPrI4G5qO157tuWeJ_cv_LK2CMs4diueM"
    private val model = GenerativeModel(
        modelName = "gemini-1.5-pro",
        apiKey = apiKey
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        // Add user message
        val userMessage = ChatMessage(content = message, isUser = true)
        _messages.update { currentMessages -> currentMessages + userMessage }

        // Generate AI response
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = model.generateContent(message)
                val aiMessage = ChatMessage(
                    content = response.text ?: "Sorry, I couldn't generate a response.",
                    isUser = false
                )
                _messages.update { currentMessages -> currentMessages + aiMessage }
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    content = "Error: ${e.message ?: "Unknown error occurred"}",
                    isUser = false
                )
                _messages.update { currentMessages -> currentMessages + errorMessage }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}