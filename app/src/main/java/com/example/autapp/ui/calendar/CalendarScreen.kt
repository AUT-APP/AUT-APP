package com.example.autapp.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.autapp.data.dao.TimetableEntryDao
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.ZoneId
import org.threeten.bp.format.TextStyle
import java.util.*
import java.text.SimpleDateFormat
import org.threeten.bp.format.DateTimeFormatter
import com.example.autapp.data.models.Event
import java.util.Calendar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState

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
                style = MaterialTheme.typography.headlineMedium
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

@Composable
fun CalendarView(
    uiState: CalendarUiState,
    onDateSelected: (LocalDate) -> Unit,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(uiState.selectedDate)) }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Month navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                currentYearMonth = currentYearMonth.minusMonths(1)
            }) {
                Icon(Icons.Default.ChevronLeft, "Previous month")
            }
            
            Text(
                text = currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + currentYearMonth.year,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = {
                currentYearMonth = currentYearMonth.plusMonths(1)
            }) {
                Icon(Icons.Default.ChevronRight, "Next month")
            }
        }

        // Days of week header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid
        val days = remember(currentYearMonth) {
            generateDaysForMonth(currentYearMonth)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            items(days.size) { index ->
                val date = days[index]
                if (date != null) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    date == uiState.selectedDate -> MaterialTheme.colorScheme.primary
                                    date.month == currentYearMonth.month -> Color.Transparent
                                    else -> Color.Transparent
                                }
                            )
                            .clickable(
                                enabled = date.month == currentYearMonth.month,
                                onClick = { onDateSelected(date) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            color = when {
                                date == uiState.selectedDate -> MaterialTheme.colorScheme.onPrimary
                                date.month == currentYearMonth.month -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            }
                        )
                    }
                } else {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected date entries
        val selectedDateEntries = uiState.timetableEntries
            .filter { entry ->
                entry.entry.dayOfWeek == uiState.selectedDate.dayOfWeek.value
            }
            .distinctBy { entry -> 
                "${entry.entry.courseId}_${entry.entry.startTime.time}_${entry.entry.endTime.time}"
            }
            .sortedBy { it.entry.startTime }

        val selectedDateEvents = uiState.filteredEvents.sortedBy { it.startTime }

        if (selectedDateEntries.isNotEmpty() || selectedDateEvents.isNotEmpty()) {
            Text(
                text = "Schedule for ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedDateEntries) { entry ->
                    TimetableEntryCard(entry = entry)
                }

                items(selectedDateEvents) { event ->
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No events scheduled for this date",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDialog(
    event: Event?,
    isToDoList: Boolean,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    var date by remember { mutableStateOf(event?.date ?: selectedDate.toDate()) }
    var startTime by remember { mutableStateOf(event?.startTime) }
    var endTime by remember { mutableStateOf(event?.endTime) }
    var location by remember { mutableStateOf(event?.location ?: "") }
    var details by remember { mutableStateOf(event?.details ?: "") }
    var frequency by remember { mutableStateOf(event?.frequency ?: "Does not repeat") }
    var showFrequencyMenu by remember { mutableStateOf(false) }
    
    // State for date and time pickers
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showTimeError by remember { mutableStateOf(false) }

    // Convert Date to Calendar for easier manipulation
    val calendar = Calendar.getInstance().apply { time = date }
    val startCalendar = Calendar.getInstance().apply { 
        startTime?.let { time = it }
    }
    val endCalendar = Calendar.getInstance().apply {
        endTime?.let { time = it }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isToDoList) "My To Do" else "Event",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                if (!isToDoList) {
                    // Date picker
                    OutlinedTextField(
                        value = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(date),
                        onValueChange = {},
                        label = { Text("Date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { showDatePicker = true },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, "Select date")
                            }
                        }
                    )

                    // Time pickers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = startTime?.let { 
                                SimpleDateFormat("h:mm a", Locale.getDefault()).format(it) 
                            } ?: "",
                            onValueChange = {},
                            label = { Text("Start time") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                                .clickable { showStartTimePicker = true },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showStartTimePicker = true }) {
                                    Icon(Icons.Default.Schedule, "Select start time")
                                }
                            }
                        )

                        OutlinedTextField(
                            value = endTime?.let { 
                                SimpleDateFormat("h:mm a", Locale.getDefault()).format(it) 
                            } ?: "",
                            onValueChange = {},
                            label = { Text("End time") },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showEndTimePicker = true },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showEndTimePicker = true }) {
                                    Icon(Icons.Default.Schedule, "Select end time")
                                }
                            }
                        )
                    }

                    if (showTimeError) {
                        Text(
                            text = "End time must be after start time",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Location
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    // Frequency
                    ExposedDropdownMenuBox(
                        expanded = showFrequencyMenu,
                        onExpandedChange = { showFrequencyMenu = !showFrequencyMenu },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = frequency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Frequency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFrequencyMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showFrequencyMenu,
                            onDismissRequest = { showFrequencyMenu = false }
                        ) {
                            listOf("Does not repeat", "Daily", "Weekly", "Monthly", "Yearly").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        frequency = option
                                        showFrequencyMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Details
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    minLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(
                        onClick = {
                            val newEvent = Event(
                                eventId = event?.eventId ?: 0,
                                title = title,
                                date = date,
                                startTime = if (isToDoList) null else startTime,
                                endTime = if (isToDoList) null else endTime,
                                location = location.takeIf { it.isNotBlank() },
                                details = details.takeIf { it.isNotBlank() },
                                isToDoList = isToDoList,
                                frequency = frequency.takeIf { it != "Does not repeat" },
                                studentId = event?.studentId ?: 0
                            )
                            onSave(newEvent)
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.time
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val newDate = Calendar.getInstance().apply {
                            timeInMillis = it
                            // Preserve the original time
                            set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                        }
                        date = newDate.time
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialogs
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startCalendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = startCalendar.get(Calendar.MINUTE)
        )
        
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = {
                val newStartTime = Calendar.getInstance().apply {
                    time = date // Use the selected date
                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    set(Calendar.MINUTE, timePickerState.minute)
                }
                
                // Validate that start time is before end time if end time exists
                if (endTime != null && newStartTime.time.after(endTime)) {
                    showTimeError = true
                } else {
                    showTimeError = false
                    startTime = newStartTime.time
                }
                showStartTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = endCalendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = endCalendar.get(Calendar.MINUTE)
        )
        
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = {
                val newEndTime = Calendar.getInstance().apply {
                    time = date // Use the selected date
                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    set(Calendar.MINUTE, timePickerState.minute)
                }
                
                // Validate that end time is after start time if start time exists
                if (startTime != null && newEndTime.time.before(startTime)) {
                    showTimeError = true
                } else {
                    showTimeError = false
                    endTime = newEndTime.time
                }
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    TextButton(onClick = onConfirm) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableView(
    uiState: CalendarUiState,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Get all entries and events for next two weeks
        val nextTwoWeeksEntries = uiState.timetableEntries
        val nextTwoWeeksEvents = uiState.events

        if (nextTwoWeeksEntries.isEmpty() && nextTwoWeeksEvents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No events scheduled for the next two weeks",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Create entries for each day in the next two weeks
            val entriesByDate = mutableMapOf<LocalDate, List<Any>>()
            
            // Populate entries for each day
            for (dayOffset in 0..13) {
                val date = today.plusDays(dayOffset.toLong())
                
                // Get timetable entries for this day
                val timetableEntriesForDay = nextTwoWeeksEntries
                    .filter { it.entry.dayOfWeek == date.dayOfWeek.value }
                    .distinctBy { entry -> 
                        "${entry.entry.courseId}_${entry.entry.startTime.time}_${entry.entry.endTime.time}"
                    }
                
                // Get events for this day
                val eventsForDay = nextTwoWeeksEvents.filter { event ->
                    event.date.toLocalDate() == date
                }
                
                // Combine and sort all entries
                val allEntries = (timetableEntriesForDay + eventsForDay).sortedWith(compareBy { entry ->
                    when (entry) {
                        is TimetableEntryDao.TimetableEntryWithCourse -> entry.entry.startTime
                        is Event -> entry.startTime ?: Date(0)
                        else -> Date(0)
                    }
                })
                
                if (allEntries.isNotEmpty()) {
                    entriesByDate[date] = allEntries
                }
            }

            // Display entries grouped by date
            entriesByDate.toSortedMap().forEach { (date, entries) ->
                item {
                    val headerText = when (date) {
                        today -> "Today"
                        today.plusDays(1) -> "Tomorrow"
                        else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    entries.forEach { entry ->
                        when (entry) {
                            is TimetableEntryDao.TimetableEntryWithCourse -> {
                                TimetableEntryCard(
                                    entry = entry,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                            is Event -> {
                                EventCard(
                                    event = entry,
                                    onClick = { onEventClick(entry) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableEntryCard(
    entry: TimetableEntryDao.TimetableEntryWithCourse,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Course code and name
            Text(
                text = "${entry.course.courseId} - ${entry.course.name}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Time with icon
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
                    text = "${entry.entry.startTime.format()} - ${entry.entry.endTime.format()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Location with icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.entry.room,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (entry.entry.type.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.entry.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
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
                            event.startTime?.let { append(SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)) }
                            if (event.startTime != null && event.endTime != null) append(" - ")
                            event.endTime?.let { append(SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            if (!event.isToDoList && !event.location.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
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
            
            if (!event.details.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (event.frequency != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Repeats ${event.frequency.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

private fun Date.format(): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(this)
}

private fun Date.toLocalDate(): LocalDate {
    return org.threeten.bp.Instant.ofEpochMilli(time)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun LocalDate.toDate(): Date {
    return Date(this.atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli())
}

private fun generateDaysForMonth(yearMonth: YearMonth): List<LocalDate?> {
    val firstOfMonth = yearMonth.atDay(1)
    // Adjust for Sunday start (Sunday = 7 in ThreeTenABP, we want it to be 0)
    val firstDayOfWeek = if (firstOfMonth.dayOfWeek == DayOfWeek.SUNDAY) 0 else firstOfMonth.dayOfWeek.value
    
    val days = mutableListOf<LocalDate?>()
    
    // Add empty spaces for days before the first of the month
    repeat(firstDayOfWeek) {
        days.add(null)
    }
    
    // Add all days of the month
    for (i in 1..yearMonth.lengthOfMonth()) {
        days.add(yearMonth.atDay(i))
    }
    
    // Add empty spaces to complete the last week if needed
    while (days.size % 7 != 0) {
        days.add(null)
    }
    
    return days
}