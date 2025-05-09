package com.example.autapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    navController: NavController, // Although passed, it's not used in the current implementation of ChatScreen.
    paddingValues: PaddingValues // Padding values from the Scaffold, to be applied to the main layout.
) {
    // Collect states from the ViewModel.
    val messages by viewModel.messages.collectAsState() // List of chat messages.
    val isLoading by viewModel.isLoading.collectAsState() // Boolean indicating if the AI is processing.
    var messageText by remember { mutableStateOf("") } // State for the text input field.
    val listState = rememberLazyListState() // State for the LazyColumn to control scrolling.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues) // Apply padding from Scaffold (e.g., for top app bar).
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Box to contain the list of messages, taking up available vertical space.
        Box(
            modifier = Modifier
                .weight(1f) // Ensures this Box takes up all space not used by ChatInput.
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                state = listState, // Attach the listState for programmatic scrolling.
                contentPadding = PaddingValues(vertical = 8.dp), // Padding inside the scrollable area.
                verticalArrangement = Arrangement.spacedBy(8.dp), // Space between message items.
                reverseLayout = false // Messages are added to the bottom, so no need to reverse.
            ) {
                items(messages) { message ->
                    ChatMessageItem(message) // Composable for rendering each individual message.
                }
            }
        }

        // Input field and send button component.
        ChatInput(
            messageText = messageText,
            onMessageTextChange = { messageText = it },
            onSendClick = {
                if (messageText.isNotBlank()) {
                    viewModel.sendMessage(messageText)
                    messageText = "" // Clear input field after sending.
                }
            },
            isLoading = isLoading
        )
    }

    // When the number of messages changes (i.e., a new message is added),
    // scroll to the last item in the list to make it visible.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
}

/**
 * Composable function to display a single chat message item.
 * It styles the message differently based on whether it's from the user or the AI.
 */
@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.isUser
    // Determine background color based on who sent the message.
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    // Determine text color based on who sent the message for optimal contrast.
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        // Align user messages to the end (right), and AI messages to the start (left).
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp) // Max width for a message bubble.
                .clip(
                    // Apply specific rounded corners to create a chat bubble effect.
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp, // Flat corner for user's own message bubble tail side.
                        bottomEnd = if (isUser) 4.dp else 16.dp   // Flat corner for AI's message bubble tail side.
                    )
                )
                .background(backgroundColor)
                .padding(12.dp) // Inner padding for the text inside the bubble.
        ) {
            Text(
                text = message.content,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Composable function for the chat input field and send button.
 */
@Composable
fun ChatInput(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean // To disable send button and show loader while AI is processing.
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // Adds padding for system navigation bars (bottom).
            .imePadding(), // Adds padding for the on-screen keyboard.
        tonalElevation = 2.dp, // Slight elevation to distinguish from the message list.
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier
                    .weight(1f) // TextField takes up available horizontal space.
                    .padding(end = 8.dp),
                placeholder = { Text("Type a message...") },
                colors = TextFieldDefaults.colors(
                    // Make TextField background transparent to blend with the Surface.
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                ),
                maxLines = 5 // Allow multi-line input up to 5 lines.
            )

            IconButton(
                onClick = onSendClick,
                // Enable button only if message is not blank and AI is not currently loading.
                enabled = messageText.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    // Show a progress indicator when waiting for AI response.
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // Show Send icon when ready to send.
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}