package com.example.autapp.ui.calendar

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.autapp.data.firebase.FirebaseBooking
import com.example.autapp.data.firebase.FirebaseEvent
import com.example.autapp.data.firebase.FirebaseTimetableEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val classRemindersEnabled: Boolean by viewModel.classRemindersEnabled.collectAsState(initial = true)


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

    val context = LocalContext.current
    // State to track notification permission
    var hasNotificationsPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Permission is automatically granted on versions below Android 13
            }
        )
    }

    // Activity result launcher for permission request
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasNotificationsPermission = isGranted }
    )

    // Request permission only once when the Composable is first launched
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun requestExactAlarmPermissionIfNeeded(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:${activity.packageName}".toUri()
                }
                activity.startActivity(intent)
            }
        }
    }

    // Shared onSetReminder logic
    val coroutineScope = rememberCoroutineScope()
    val onSetReminder: (Any, Int) -> Unit = { item, minutes ->
        coroutineScope.launch {
            // Prevent scheduling notification for a time before the current time
            val now = System.currentTimeMillis()
            val itemStartTimeMillis = when (item) {
                is FirebaseEvent -> item.startTime?.time
                is FirebaseBooking -> item.startTime.time
                else -> null
            }

            if (itemStartTimeMillis != null && itemStartTimeMillis < now) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Cannot set reminder for a past item.")
                return@launch
            }

            // Check and request exact alarm permission if needed
            val activity = context as? Activity
            activity?.let {
                requestExactAlarmPermissionIfNeeded(it)
            }

            val scheduledTimeMillis = when (item) {
                is FirebaseTimetableEntry -> viewModel.updateReminder(context, item, minutes)
                is FirebaseEvent -> viewModel.updateReminder(context, item, minutes)
                is FirebaseBooking -> viewModel.updateReminder(context, item, minutes)
                else -> null
            }
            // Dismiss any currently showing snackbar
            snackbarHostState.currentSnackbarData?.dismiss()
            val warning = when {
                !notificationsEnabled -> " (Notifications disabled in settings)"
                !classRemindersEnabled -> " (Reminders disabled in settings)"
                else -> ""
            }
            val baseMessage = if (scheduledTimeMillis != null) {
                // Format the scheduled time into a readable string
                val scheduledDate = Date(scheduledTimeMillis)
                val formatter = SimpleDateFormat("MMM dd 'at' h:mm a", Locale.getDefault())
                val scheduledTimeString = formatter.format(scheduledDate)
                // Construct the message
                when (item) {
                    is FirebaseTimetableEntry -> {
                        val courseName = uiState.courses.find { it.courseId == item.courseId }?.name ?: item.courseId
                        "$courseName ${item.type} notification scheduled for $scheduledTimeString"
                    }                    is FirebaseEvent -> "${item.title} event notification scheduled for $scheduledTimeString"
                    is FirebaseBooking -> "${item.roomId} booking notification scheduled for $scheduledTimeString"
                    else -> "Failed to schedule notification: Unknown item type"
                }
            } else {
                when (item) {
                    is FirebaseTimetableEntry -> {
                        val courseName = uiState.courses.find { it.courseId == item.courseId }?.name ?: item.courseId
                        "Failed to schedule $courseName ${item.type} notification"
                    }
                    is FirebaseEvent -> "Failed to schedule ${item.title} event notification"
                    is FirebaseBooking -> "Failed to schedule ${item.roomId} booking notification "
                    else -> "Failed to schedule notification."
                }
            }

            snackbarHostState.showSnackbar(
                message = "$baseMessage$warning",
                duration = SnackbarDuration.Short
            )
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