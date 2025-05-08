package com.example.autapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autapp.ui.chat.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val apiKey = "AIzaSyDYdquqUGmKiABClgrfpVR-WdpjozBZWcg"
    private val model = GenerativeModel(
        // The name of the Generative AI model to use.
        modelName = "gemini-1.5-pro",
        // The API key for accessing the model.
        apiKey = apiKey
    )

    // System prompt to guide the AI's behavior and responses.
    // It defines the AI's persona, knowledge domain (AUT-related topics), and response guidelines.
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

    // Private mutable state flow to hold the list of chat messages.
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    // Public immutable state flow for observing chat messages in the UI.
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Private mutable state flow to indicate if the AI is currently processing a message.
    private val _isLoading = MutableStateFlow(false)
    // Public immutable state flow for observing the loading state in the UI.
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Sends a message from the user to the AI model and updates the chat with the response.
     * - Adds the user's message to the chat.
     * - Calls the Generative AI model to get a response.
     * - Adds the AI's response or an error message to the chat.
     * - Manages the loading state.
     */
    fun sendMessage(message: String) {
        // Do not send empty messages.
        if (message.isBlank()) return

        // Add user message to the local list of messages.
        val userMessage = ChatMessage(content = message, isUser = true)
        _messages.update { currentMessages -> currentMessages + userMessage }

        // Launch a coroutine to handle the AI response generation asynchronously.
        viewModelScope.launch {
            try {
                _isLoading.value = true // Set loading state to true.
                // Start a new chat session with the model, providing the system prompt as initial history.
                val chat = model.startChat(
                    history = listOf(
                        content("user") { // The system prompt is provided as if it's from a "user" role for context.
                            text(systemPrompt)
                        }
                    )
                )
                // Send the user's actual message to the ongoing chat session.
                val response = chat.sendMessage(message)
                // Create a ChatMessage object for the AI's response.
                val aiMessage = ChatMessage(
                    content = response.text ?: "Sorry, I couldn't generate a response.", // Use a fallback if response text is null.
                    isUser = false
                )
                // Add the AI's message to the list.
                _messages.update { currentMessages -> currentMessages + aiMessage }
            } catch (e: Exception) {
                // If an error occurs during AI communication, add an error message to the chat.
                val errorMessage = ChatMessage(
                    content = "Error: ${e.message ?: "Unknown error occurred"}",
                    isUser = false
                )
                _messages.update { currentMessages -> currentMessages + errorMessage }
            } finally {
                _isLoading.value = false // Reset loading state regardless of success or failure.
            }
        }
    }

    /**
     * Clears all messages from the chat history.
     */
    fun clearChat() {
        _messages.value = emptyList()
    }
}