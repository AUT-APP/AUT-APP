package com.example.autapp.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autapp.data.models.Event

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    onNavigateToManageEvents: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val navigateToManageEvents by viewModel.navigateToManageEvents.collectAsState()
    var showCalendarView by remember { mutableStateOf(uiState.isCalendarView) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddTodoDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    LaunchedEffect(navigateToManageEvents) {
        if (navigateToManageEvents) {
            onNavigateToManageEvents()
            viewModel.onManageEventsNavigated()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showCalendarView) "Calendar View" else "Timetable View",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp
            )

            Row {
                IconButton(onClick = { showAddEventDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Event"
                    )
                }
                IconButton(onClick = { showAddTodoDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Add Todo"
                    )
                }
                IconButton(onClick = { 
                    // Navigate to manage events screen
                    viewModel.navigateToManageEvents()
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Manage Events"
                    )
                }
                IconButton(onClick = {
                    showCalendarView = !showCalendarView
                    viewModel.toggleView()
                }) {
                    Icon(
                        imageVector = if (showCalendarView)
                            Icons.AutoMirrored.Filled.ViewList else Icons.Default.CalendarMonth,
                        contentDescription = if (showCalendarView) 
                            "Switch to List View" else "Switch to Calendar View"
                    )
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            if (showCalendarView) {
                CalendarView(
                    uiState = uiState,
                    onDateSelected = viewModel::updateSelectedDate,
                    onEventClick = { selectedEvent = it }
                )
            } else {
                TimetableView(
                    uiState = uiState,
                    onEventClick = { selectedEvent = it }
                )
            }
        }
    }

    if (showAddTodoDialog) {
        EventDialog(
            event = null,
            isToDoList = true,
            selectedDate = uiState.selectedDate,
            onDismiss = { showAddTodoDialog = false },
            onSave = { event ->
                viewModel.addEvent(event)
                showAddTodoDialog = false
            }
        )
    }

    selectedEvent?.let { event ->
        EventDialog(
            event = event,
            isToDoList = event.isToDoList,
            selectedDate = uiState.selectedDate,
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

    if (showAddEventDialog) {
        EventDialog(
            event = null,
            isToDoList = false,
            selectedDate = uiState.selectedDate,
            onDismiss = { showAddEventDialog = false },
            onSave = { event ->
                viewModel.addEvent(event)
                showAddEventDialog = false
            }
        )
    }
}