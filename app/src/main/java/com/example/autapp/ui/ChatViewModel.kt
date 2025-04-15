package com.example.autapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autapp.data.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val apiKey = "AIzaSyDYdquqUGmKiABClgrfpVR-WdpjozBZWcg"
    private val model = GenerativeModel(
        modelName = "gemini-1.5-pro",
        apiKey = apiKey
    )

    private val systemPrompt = """
        You are a helpful assistant that answers questions about AUT (Auckland University of Technology). Base your answers on the following information:

        AUT Overview:
        - AUT is a university in Auckland, New Zealand
        - Has three campuses: City (central business district), North (Northcote), and South (Manukau)
        - Key faculties: Business/Economics/Law, Design/Creative Technologies, Culture/Society, Health/Environmental Sciences
        - Known for research, innovation, and international student support
        - Features state-of-the-art facilities and strong industry connections

        You should ONLY respond to questions about:
        1. General AUT Information (overview, campuses, faculties)
        2. Campus Navigation and Facilities
        3. Student Services (counseling, health, academic support)
        4. Enrollment & Admissions
        5. Course Information
        6. Library & Study Spaces
        7. IT & WiFi Support
        8. Student Accommodation
        9. Greeting messages
        

        Response Guidelines:
        - For greetings (hi, hello, etc.): Respond with "Hello! How can I help you today?"
        - Make sure to respond nicely to messages like thank you, etc.
        - For questions you cannot answer: Respond with "Sorry, but I cannot help you with this at the moment. Please come see the AUT Student Hub located on all campuses for general enquiries or go see your faculty reception for course-related enquiries."
        - Always reference www.aut.ac.nz for the most up-to-date information
        - Maintain a professional and helpful tone
        - Be specific about campus locations and services
        - Provide clear, structured responses for complex queries

        If a question is not related to AUT, respond with: "I apologize, but I can only help with AUT-related queries. Please ask me something about Auckland University of Technology."
    """.trimIndent()

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
                val chat = model.startChat(
                    history = listOf(
                        content("user") {
                            text(systemPrompt)
                        }
                    )
                )
                val response = chat.sendMessage(message)
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