package com.example.autapp.ui.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testChatScreenInitialState() {
        // Given
        val viewModel = ChatViewModel()

        // When
        composeTestRule.setContent {
            ChatScreen(
                viewModel = viewModel,
                navController = rememberNavController(),
                paddingValues = androidx.compose.foundation.layout.PaddingValues()
            )
        }

        // Then
        composeTestRule.onNodeWithText("Type a message...").assertExists()
        composeTestRule.onNodeWithContentDescription("Send").assertExists()
    }

    @Test
    fun testSendMessage() {
        // Given
        val viewModel = ChatViewModel()
        val testMessage = "Hello AI"

        // When
        composeTestRule.setContent {
            ChatScreen(
                viewModel = viewModel,
                navController = rememberNavController(),
                paddingValues = androidx.compose.foundation.layout.PaddingValues()
            )
        }

        // Then
        composeTestRule.onNodeWithText("Type a message...").performTextInput(testMessage)
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        
        // Verify message was sent
        composeTestRule.onNodeWithText(testMessage).assertExists()
    }

    @Test
    fun testEmptyMessageNotSent() {
        // Given
        val viewModel = ChatViewModel()

        // When
        composeTestRule.setContent {
            ChatScreen(
                viewModel = viewModel,
                navController = rememberNavController(),
                paddingValues = androidx.compose.foundation.layout.PaddingValues()
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }
} 