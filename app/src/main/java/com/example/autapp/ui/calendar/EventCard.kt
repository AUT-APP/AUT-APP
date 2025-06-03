package com.example.autapp.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.autapp.data.models.Event
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.LocalDate

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onReminderClick: () -> Unit
) {
    // Card composable to display event information.
    // It's clickable to trigger the onClick lambda, likely for opening an edit/detail view.
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            // Use secondaryContainer color for to-do items, surface color for regular events.
            containerColor = if (event.isToDoList)
                MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (event.isToDoList) {
                Text(
                    text = "Due: ${SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(event.date)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Display event time (start - end) if it's not a to-do list and time is available.
            if (!event.isToDoList && (event.startTime != null || event.endTime != null)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = "Time",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buildString {
                            // Append start time if available.
                            event.startTime?.let { append(SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)) }
                            // Append separator if both start and end times are available.
                            if (event.startTime != null && event.endTime != null) append(" - ")
                            // Append end time if available.
                            event.endTime?.let { append(SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            // Display event location if it's not a to-do list and location is provided.
            if (!event.isToDoList && !event.location.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Display event details if provided.
            if (!event.details.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Display event recurrence frequency if provided (e.g., "Repeats daily").
            if (event.frequency != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Repeats ${event.frequency.lowercase(Locale.getDefault())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
} 