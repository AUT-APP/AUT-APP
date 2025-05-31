package com.example.autapp.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import com.example.autapp.data.models.Event
import org.threeten.bp.LocalDate
import java.util.*
import java.text.SimpleDateFormat
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDialog(
    event: Event?,
    isToDoList: Boolean,
    selectedDate: LocalDate,
    userId: String,
    isTeacher: Boolean,
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
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showTimeError by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance().apply { time = date }
    val startCalendar = Calendar.getInstance().apply { startTime?.let { setTime(it) } }
    val endCalendar = Calendar.getInstance().apply { endTime?.let { setTime(it) } }

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
                if (!isToDoList) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (startTime != null) SimpleDateFormat("h:mm a", Locale.getDefault()).format(startTime) else "",
                                onValueChange = {},
                                label = { Text("Start time") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                enabled = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                trailingIcon = {
                                    Icon(Icons.Default.Schedule, "Select start time")
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showStartTimePicker = true }
                                    .background(Color.Transparent)
                            ) {}
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (endTime != null) SimpleDateFormat("h:mm a", Locale.getDefault()).format(endTime) else "",
                                onValueChange = {},
                                label = { Text("End time") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                enabled = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                trailingIcon = {
                                    Icon(Icons.Default.Schedule, "Select end time")
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showEndTimePicker = true }
                                    .background(Color.Transparent)
                            ) {}
                        }
                    }
                    if (showTimeError) {
                        Text(
                            text = "End time must be after start time",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Details") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        minLines = 3
                    )
                }
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
                                eventId = event?.eventId ?: "",
                                title = title,
                                date = date,
                                startTime = if (isToDoList) null else startTime,
                                endTime = if (isToDoList) null else endTime,
                                location = location.takeIf { it.isNotBlank() },
                                details = details.takeIf { it.isNotBlank() },
                                isToDoList = isToDoList,
                                frequency = null,
                                studentId = userId,
                                teacherId = if (isTeacher) userId else null,
                                isTeacherEvent = isTeacher
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
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startCalendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = startCalendar.get(Calendar.MINUTE)
        )
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Select Start Time") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newStartTime = Calendar.getInstance().apply {
                            time = date
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        if (endTime != null && newStartTime.time.after(endTime)) {
                            showTimeError = true
                        } else {
                            showTimeError = false
                            startTime = newStartTime.time
                        }
                        showStartTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
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
            title = { Text("Select End Time") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newEndTime = Calendar.getInstance().apply {
                            time = date
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        if (startTime != null && newEndTime.time.before(startTime)) {
                            showTimeError = true
                        } else {
                            showTimeError = false
                            endTime = newEndTime.time
                        }
                        showEndTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}