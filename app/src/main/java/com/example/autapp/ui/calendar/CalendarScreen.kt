package com.example.autapp.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autapp.data.dao.TimetableEntryDao
import com.example.autapp.data.models.Booking
import com.example.autapp.data.models.Event
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    onNavigateToManageEvents: () -> Unit
) {
    // StateFlow for UI state from the ViewModel, collected as State
    val uiState by viewModel.uiState.collectAsState()
    // StateFlow to trigger navigation to the manage events screen
    val navigateToManageEvents by viewModel.navigateToManageEvents.collectAsState()
    // State to toggle between Calendar and Timetable views
    var showCalendarView by remember { mutableStateOf(uiState.isCalendarView) }
    // State to control the visibility of the add event dialog
    var showAddEventDialog by remember { mutableStateOf(false) }
    // State to control the visibility of the add to-do dialog
    var showAddTodoDialog by remember { mutableStateOf(false) }
    // State to hold the currently selected event for editing or viewing details
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    // LaunchedEffect observes navigateToManageEvents. When true, it triggers navigation
    // and then calls a ViewModel function to reset the navigation trigger.
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
                // IconButton to open the dialog for adding a new event
                IconButton(onClick = { showAddEventDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Event"
                    )
                }
                // IconButton to open the dialog for adding a new to-do item
                IconButton(onClick = { showAddTodoDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Add Todo"
                    )
                }
                // IconButton to navigate to the manage events screen
                IconButton(onClick = { 
                    // Navigate to manage events screen
                    viewModel.navigateToManageEvents()
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Manage Events"
                    )
                }
                // IconButton to toggle between Calendar and Timetable views
                IconButton(onClick = {
                    showCalendarView = !showCalendarView
                    viewModel.toggleView() // Notify ViewModel about the view change
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

        // Display a loading indicator while data is being fetched
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        // Display an error message if data fetching fails
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
        // Display the main content (Calendar or Timetable view) if data is loaded successfully
        } else {
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current
            if (showCalendarView) {
                CalendarView(
                    uiState = uiState,
                    onDateSelected = viewModel::updateSelectedDate,
                    onSetReminder = { item, minutes ->
                        coroutineScope.launch {
                            when (item) {
                                is TimetableEntryDao.TimetableEntryWithCourse -> viewModel.updateReminder(context, item, minutes)
                                is Event -> viewModel.updateReminder(context, item, minutes)
                                is Booking -> viewModel.updateReminder(context, item, minutes)
                            }
                        }
                    },  // Passing a general handler that checks the type of item
                    onEventClick = { selectedEvent = it }
                )
            } else {
                TimetableView(
                    uiState = uiState,
                    onSetReminder = { item, minutes ->
                        coroutineScope.launch {
                            when (item) {
                                is TimetableEntryDao.TimetableEntryWithCourse -> viewModel.updateReminder(context, item, minutes)
                                is Event -> viewModel.updateReminder(context, item, minutes)
                                is Booking -> viewModel.updateReminder(context, item, minutes)
                            }
                        }
                    },  // Passing a general handler that checks the type of item
                    onEventClick = { selectedEvent = it }
                )
            }
        }
    }

    // Dialog for adding a new to-do item
    if (showAddTodoDialog) {
        EventDialog(
            event = null, // Pass null for a new event
            isToDoList = true, // Specify that this is a to-do item
            selectedDate = uiState.selectedDate,
            onDismiss = { showAddTodoDialog = false }, // Close dialog on dismiss
            onSave = { event ->
                viewModel.addEvent(event) // Save the new to-do item
                showAddTodoDialog = false // Close dialog on save
            }
        )
    }

    // Dialog for editing an existing event or to-do item
    // Shows when 'selectedEvent' is not null
    selectedEvent?.let { event ->
        EventDialog(
            event = event, // Pass the selected event to prefill fields
            isToDoList = event.isToDoList,
            selectedDate = uiState.selectedDate,
            onDismiss = { selectedEvent = null }, // Close dialog and clear selection on dismiss
            onSave = { updatedEvent ->
                viewModel.updateEvent(updatedEvent) // Update the event
                selectedEvent = null // Close dialog and clear selection
            },
            onDelete = {
                viewModel.deleteEvent(event) // Delete the event
                selectedEvent = null // Close dialog and clear selection
            }
        )
    }

    // Dialog for adding a new calendar event
    if (showAddEventDialog) {
        EventDialog(
            event = null, // Pass null for a new event
            isToDoList = false, // Specify that this is a calendar event, not a to-do
            selectedDate = uiState.selectedDate,
            onDismiss = { showAddEventDialog = false }, // Close dialog on dismiss
            onSave = { event ->
                viewModel.addEvent(event) // Save the new event
                showAddEventDialog = false // Close dialog on save
            }
        )
    }
}