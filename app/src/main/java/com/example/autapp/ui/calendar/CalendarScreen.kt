package com.example.autapp.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autapp.data.firebase.FirebaseEvent

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    onNavigateToManageEvents: () -> Unit,
    snackbarHostState: SnackbarHostState
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
    var selectedEvent by remember { mutableStateOf<FirebaseEvent?>(null) }

    val notificationsEnabled: Boolean by viewModel.notificationsEnabled.collectAsState(initial = true)
    val remindersEnabled: Boolean by viewModel.remindersEnabled.collectAsState(initial = true)


    // LaunchedEffect observes navigateToManageEvents. When true, it triggers navigation
    // and then calls a ViewModel function to reset the navigation trigger.
    LaunchedEffect(navigateToManageEvents) {
        if (navigateToManageEvents) {
            onNavigateToManageEvents()
            viewModel.onManageEventsNavigated()
        }
    }

    // LaunchedEffect to fetch data when the view changes or the screen is recomposed
    LaunchedEffect(uiState.isCalendarView) {
        if (uiState.isCalendarView) {
            viewModel.fetchTimetableData() // Fetch data for the selected date in Calendar View
            viewModel.fetchEventsForDate() // Fetch events for the selected date
        } else {
            viewModel.fetchNextTwoWeeksData() // Fetch data for the next two weeks in Timetable View
        }
    }

    val onSetReminder = rememberReminderScheduler(
        snackbarHostState = snackbarHostState,
        viewModel = viewModel,
        uiState = uiState,
        notificationsEnabled = notificationsEnabled,
        remindersEnabled = remindersEnabled
    )

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
                text = if (showCalendarView)
                    if (uiState.isTeacher) "Teacher Calendar View" else "Calendar View"
                else
                    if (uiState.isTeacher) "Teacher Timetable View" else "Timetable View",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp
            )

            Row {
                // Only show booking-related buttons for students
                if (!uiState.isTeacher) {
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
                        viewModel.navigateToManageEvents()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Manage Events"
                        )
                    }
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
            if (showCalendarView) {
                CalendarView(
                    uiState = uiState,
                    onDateSelected = viewModel::updateSelectedDate,
                    onSetReminder = onSetReminder,
                    onEventClick = { selectedEvent = it }
                )
            } else {
                TimetableView(
                    uiState = uiState,
                    onSetReminder = onSetReminder,
                    onEventClick = { selectedEvent = it }
                )
            }
        }
    }

    // Only show event dialogs for students
    if (!uiState.isTeacher) {
        if (showAddTodoDialog) {
            EventDialog(
                event = null,
                isToDoList = true,
                selectedDate = uiState.selectedDate,
                userId = uiState.userId,
                isTeacher = uiState.isTeacher,
                onDismiss = { showAddTodoDialog = false },
                onSave = { event ->
                    viewModel.addEvent(FirebaseEvent(
                        eventId = event.eventId,
                        title = event.title,
                        date = event.date,
                        startTime = event.startTime,
                        endTime = event.endTime,
                        location = event.location,
                        details = event.details,
                        isToDoList = event.isToDoList,
                        frequency = event.frequency,
                        studentId = event.studentId,
                        teacherId = event.teacherId,
                        isTeacherEvent = event.isTeacherEvent
                    ))
                    showAddTodoDialog = false
                }
            )
        }

        selectedEvent?.let { event ->
            EventDialog(
                event = event.toEvent(),
                isToDoList = event.isToDoList,
                selectedDate = uiState.selectedDate,
                userId = uiState.userId,
                isTeacher = uiState.isTeacher,
                onDismiss = { selectedEvent = null },
                onSave = { updatedEvent ->
                    viewModel.updateEvent(FirebaseEvent(
                        eventId = updatedEvent.eventId,
                        title = updatedEvent.title,
                        date = updatedEvent.date,
                        startTime = updatedEvent.startTime,
                        endTime = updatedEvent.endTime,
                        location = updatedEvent.location,
                        details = updatedEvent.details,
                        isToDoList = updatedEvent.isToDoList,
                        frequency = updatedEvent.frequency,
                        studentId = updatedEvent.studentId,
                        teacherId = updatedEvent.teacherId,
                        isTeacherEvent = updatedEvent.isTeacherEvent
                    ))
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
                    viewModel.addEvent(FirebaseEvent(
                        eventId = event.eventId,
                        title = event.title,
                        date = event.date,
                        startTime = event.startTime,
                        endTime = event.endTime,
                        location = event.location,
                        details = event.details,
                        isToDoList = event.isToDoList,
                        frequency = event.frequency,
                        studentId = event.studentId,
                        teacherId = event.teacherId,
                        isTeacherEvent = event.isTeacherEvent
                    ))
                    showAddEventDialog = false
                },
                isTeacher = uiState.isTeacher,
                userId = uiState.userId
            )
        }
    }
}