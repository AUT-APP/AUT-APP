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
import com.example.autapp.data.dao.TimetableEntryDao
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import com.example.autapp.data.models.Event
import java.time.ZoneId

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    var showCalendarView by remember { mutableStateOf(viewModel.isCalendarView) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddTodoDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

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

        if (viewModel.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (viewModel.errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            if (showCalendarView) {
                CalendarView(
                    viewModel = viewModel,
                    onEventClick = { selectedEvent = it }
                )
            } else {
                TimetableView(
                    viewModel = viewModel,
                    onEventClick = { selectedEvent = it }
                )
            }
        }
    }

    if (showAddEventDialog) {
        EventDialog(
            event = null,
            isToDoList = false,
            onDismiss = { showAddEventDialog = false },
            onSave = { event ->
                viewModel.addEvent(event)
                showAddEventDialog = false
            }
        )
    }

    if (showAddTodoDialog) {
        EventDialog(
            event = null,
            isToDoList = true,
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
}

@Composable
fun CalendarView(
    viewModel: CalendarViewModel,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(viewModel.selectedDate)) }
    
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
                                    date == viewModel.selectedDate -> MaterialTheme.colorScheme.primary
                                    date.month == currentYearMonth.month -> Color.Transparent
                                    else -> Color.Transparent
                                }
                            )
                            .clickable(
                                enabled = date.month == currentYearMonth.month,
                                onClick = { viewModel.updateSelectedDate(date) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            color = when {
                                date == viewModel.selectedDate -> MaterialTheme.colorScheme.onPrimary
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
        val selectedDateEntries = viewModel.timetableEntries
            .filter { entry ->
                entry.entry.dayOfWeek == viewModel.selectedDate.dayOfWeek.value
            }
            .distinctBy { entry -> 
                "${entry.entry.courseId}_${entry.entry.startTime.time}_${entry.entry.endTime.time}"
            }
            .sortedBy { it.entry.startTime }

        val selectedDateEvents = viewModel.events.filter { event ->
            val eventDate = LocalDate.ofInstant(event.date.toInstant(), ZoneId.systemDefault())
            eventDate == viewModel.selectedDate
        }.sortedBy { it.startTime }

        if (selectedDateEntries.isNotEmpty() || selectedDateEvents.isNotEmpty()) {
            Text(
                text = "Schedule for ${viewModel.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))}",
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
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    var date by remember { mutableStateOf(event?.date ?: Date()) }
    var startTime by remember { mutableStateOf(event?.startTime) }
    var endTime by remember { mutableStateOf(event?.endTime) }
    var location by remember { mutableStateOf(event?.location ?: "") }
    var details by remember { mutableStateOf(event?.details ?: "") }
    var frequency by remember { mutableStateOf(event?.frequency ?: "Does not repeat") }
    var showFrequencyMenu by remember { mutableStateOf(false) }

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
                            .padding(bottom = 8.dp),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                // Show date picker
                            }) {
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
                            value = startTime?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: "",
                            onValueChange = {},
                            label = { Text("Start time") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    // Show time picker
                                }) {
                                    Icon(Icons.Default.Schedule, "Select start time")
                                }
                            }
                        )

                        OutlinedTextField(
                            value = endTime?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: "",
                            onValueChange = {},
                            label = { Text("End time") },
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    // Show time picker
                                }) {
                                    Icon(Icons.Default.Schedule, "Select end time")
                                }
                            }
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
                                startTime = startTime,
                                endTime = endTime,
                                location = location.takeIf { it.isNotBlank() },
                                details = details.takeIf { it.isNotBlank() },
                                isToDoList = isToDoList,
                                frequency = frequency.takeIf { it != "Does not repeat" },
                                studentId = event?.studentId ?: 0 // This should be set from the current user
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
}

@Composable
fun TimetableView(
    viewModel: CalendarViewModel,
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
        val nextTwoWeeksEntries = viewModel.timetableEntries
        val nextTwoWeeksEvents = viewModel.events

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
                    val eventDate = LocalDate.ofInstant(event.date.toInstant(), ZoneId.systemDefault())
                    eventDate == date
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

private fun generateDaysForMonth(yearMonth: YearMonth): List<LocalDate?> {
    val firstOfMonth = yearMonth.atDay(1)
    // Adjust for Sunday start (Sunday = 7 in Java Time, we want it to be 0)
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