package com.example.autapp.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.autapp.data.models.Event
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ManageEventsScreen(
    viewModel: CalendarViewModel,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Event?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Header
        Text(
            text = "Manage Events",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (uiState.events.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No events found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.events.sortedBy { it.date }) { event: Event ->
                    ManageEventCard(
                        event = event,
                        onEdit = { selectedEvent = it },
                        onDelete = { showDeleteDialog = it }
                    )
                }
            }
        }
    }

    // Edit Dialog
    selectedEvent?.let { event ->
        EventDialog(
            event = event,
            isToDoList = event.isToDoList,
            selectedDate = event.date.toLocalDate(),
            onDismiss = { selectedEvent = null },
            onSave = { updatedEvent ->
                viewModel.updateEvent(updatedEvent)
                selectedEvent = null
            },
            onDelete = {
                viewModel.deleteEvent(event)
                selectedEvent = null
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { event ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete '${event.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEvent(event)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageEventCard(
    event: Event,
    onEdit: (Event) -> Unit,
    onDelete: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = { onEdit(event) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit event",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { onDelete(event) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete event",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dateFormatter.format(event.date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!event.isToDoList && (event.startTime != null || event.endTime != null)) {
                Text(
                    text = buildString {
                        event.startTime?.let { append(timeFormatter.format(it)) }
                        if (event.startTime != null && event.endTime != null) append(" - ")
                        event.endTime?.let { append(timeFormatter.format(it)) }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!event.location.isNullOrBlank()) {
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (event.frequency != null) {
                Text(
                    text = "Repeats ${event.frequency.lowercase(Locale.getDefault())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}