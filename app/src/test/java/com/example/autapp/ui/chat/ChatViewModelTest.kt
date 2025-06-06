package com.example.autapp.ui.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private lateinit var viewModel: ChatViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ChatViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state has empty messages`() = runTest {
        // Given
        val messages = viewModel.messages.value
        
        // Then
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `test sending message adds user message to chat`() = runTest {
        // Given
        val testMessage = "Hello"
        
        // When
        viewModel.sendMessage(testMessage)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val messages = viewModel.messages.value
        assertEquals(2, messages.size)
        assertEquals(testMessage, messages[0].content)
        assertTrue(messages[0].isUser)
        assertFalse(messages[1].isUser)
    }

    @Test
    fun `test sending empty message does not add to chat`() = runTest {
        // Given
        val emptyMessage = ""
        
        // When
        viewModel.sendMessage(emptyMessage)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val messages = viewModel.messages.value
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `test clearChat removes all messages`() = runTest {
        // Given
        viewModel.sendMessage("Test message")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.clearChat()
        
        // Then
        val messages = viewModel.messages.value
        assertTrue(messages.isEmpty())
    }
} 