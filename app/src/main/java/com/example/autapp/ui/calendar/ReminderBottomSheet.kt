package com.example.autapp.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.autapp.data.firebase.FirebaseBooking
import com.example.autapp.data.firebase.FirebaseEvent
import com.example.autapp.data.firebase.FirebaseTimetableEntry
import java.util.Date

// Helper function to get the start time regardless of the item type
fun getStartTime(item: Any): Date? {
    return when (item) {
        is FirebaseEvent -> item.startTime
        is FirebaseBooking -> item.startTime
        else -> null
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderBottomSheet(
    selectedEntry: Any,
    onDismiss: () -> Unit,
    onSelectTime: (Int) -> Unit // e.g., 60, 30, etc.
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val startTime = getStartTime(selectedEntry)
        val currentTime = System.currentTimeMillis() // Get current time in milliseconds
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Remind me before", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            listOf(60, 30, 15, 0).forEach { minutes ->
                val label = when (minutes) {
                    0 -> "At start time"
                    60 -> "1 hour before"
                    else -> "$minutes minutes before"
                }

                // Determine if the button should be enabled
                val isEnabled = when (selectedEntry) {
                    // For TimetableEntry, we generally assume all reminder options are valid
                    is FirebaseTimetableEntry -> {
                        true
                    } else -> {
                        // For one-off events (FirebaseEvent, FirebaseBooking),
                        if (startTime != null) {
                            val scheduledTriggerTime = startTime.time - (minutes * 60 * 1000L)
                            scheduledTriggerTime > currentTime
                        } else {
                            false // Disable if start time is not available
                        }
                    }
                }

                Button(
                    onClick = { onSelectTime(minutes) },
                    enabled = isEnabled,
                    colors = ButtonDefaults.buttonColors(
                        // Set colors based on whether the button is enabled
                        containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        contentColor = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(text = label)
                }
            }
        }
    }
}