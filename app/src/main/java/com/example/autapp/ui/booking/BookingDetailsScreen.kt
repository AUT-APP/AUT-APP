package com.example.autapp.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.autapp.data.models.Booking
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Logger

@Composable
fun BookingDetailsScreen(
    viewModel: BookingViewModel,
    navController: NavController,
    spaceId: String,
    level: String,
    date: String,
    timeSlot: String,
    studentId: String,
    campus: String,
    building: String,
    isDarkTheme: Boolean,
    paddingValues: PaddingValues,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var durationMinutes by remember { mutableStateOf(30) }
    val errorMessage by viewModel.errorMessage.collectAsState()
    val bookingSuccess by viewModel.bookingSuccess.collectAsState()
    val availableDurations by viewModel.availableDurations.collectAsState()
    val containerColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F5)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF333333)
    var showConfirmation by remember { mutableStateOf(false) }
    var pendingBooking by remember { mutableStateOf<Booking?>(null) }

    val isValidInput = spaceId.isNotEmpty() && level.isNotEmpty() && date.isNotEmpty() &&
            timeSlot.isNotEmpty() && studentId.isNotEmpty() && durationMinutes > 0 &&
            campus.isNotEmpty() && building.isNotEmpty()

    val parsedDate = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: Date()
    } catch (e: Exception) {
        Date()
    }
    val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.US).format(parsedDate)
    val (hour, minute) = try {
        timeSlot.split(":").map { it.toInt() }
    } catch (e: Exception) {
        listOf(0, 0)
    }

    val calendar = Calendar.getInstance()
    calendar.time = parsedDate
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val slotStartTime = calendar.time

    val maxDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 30) }.time
    val isToday = Calendar.getInstance().apply { time = parsedDate }.let { parsed ->
        val today = Calendar.getInstance()
        parsed.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                parsed.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    }
    val isDateValid = !parsedDate.after(maxDate) && (isToday || parsedDate.after(Date()))

    LaunchedEffect(errorMessage, bookingSuccess) {
        when {
            errorMessage != null -> {
                Logger.getLogger("BookingDetailsScreen").info("Showing error: $errorMessage")
                snackbarHostState.showSnackbar(
                    message = errorMessage!!,
                    actionLabel = "Dismiss",
                    duration = SnackbarDuration.Long
                )
                viewModel.clearErrorMessage()
            }
            bookingSuccess -> {
                Logger.getLogger("BookingDetailsScreen").info("Showing success message")
                snackbarHostState.showSnackbar(
                    message = "Booking created successfully!",
                    actionLabel = "OK",
                    duration = SnackbarDuration.Short
                )
                delay(1000L)
                navController.popBackStack()
                viewModel.clearBookingSuccess()
            }
        }
    }

    LaunchedEffect(spaceId, date, timeSlot) {
        viewModel.fetchAvailableDurations(spaceId, parsedDate, hour, minute)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor)
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (!isValidInput || !isDateValid) {
            Text(
                text = when {
                    !isValidInput -> "Invalid booking details provided"
                    !isDateValid -> "Booking date must be today or within the next 30 days"
                    else -> "Invalid input"
                },
                color = MaterialTheme.colorScheme.error,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) Color(0xFF006060) else Color(0xFF006B6B),
                    contentColor = Color.White
                )
            ) {
                Text("Go Back")
            }
        } else {
            Text(
                text = "Booking Details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF242424) else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    DetailRow(
                        icon = Icons.Default.MeetingRoom,
                        label = "Study Space",
                        value = "$level - $spaceId",
                        textColor = textColor
                    )
                    HorizontalDivider(color = textColor.copy(alpha = 0.1f), thickness = 1.dp)
                    DetailRow(
                        icon = Icons.Default.LocationCity,
                        label = "Campus",
                        value = campus,
                        textColor = textColor
                    )
                    HorizontalDivider(color = textColor.copy(alpha = 0.1f), thickness = 1.dp)
                    DetailRow(
                        icon = Icons.Default.Home,
                        label = "Building",
                        value = building,
                        textColor = textColor
                    )
                    HorizontalDivider(color = textColor.copy(alpha = 0.1f), thickness = 1.dp)
                    DetailRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Date",
                        value = formattedDate,
                        textColor = textColor
                    )
                    HorizontalDivider(color = textColor.copy(alpha = 0.1f), thickness = 1.dp)
                    DetailRow(
                        icon = Icons.Default.AccessTime,
                        label = "Time",
                        value = timeSlot,
                        textColor = textColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Duration",
                fontWeight = FontWeight.Medium,
                color = textColor,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            DurationDropdown(
                selectedDuration = durationMinutes,
                onDurationSelected = { durationMinutes = it },
                availableDurations = availableDurations,
                textColor = textColor,
                isDarkTheme = isDarkTheme
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    calendar.time = parsedDate
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startTime = calendar.time
                    calendar.add(Calendar.MINUTE, durationMinutes)
                    val endTime = calendar.time

                    pendingBooking = Booking(
                        bookingId = 0,
                        studentId = studentId,
                        roomId = spaceId,
                        building = building,
                        campus = campus,
                        level = level,
                        bookingDate = parsedDate,
                        startTime = startTime,
                        endTime = endTime,
                        status = "ACTIVE"
                    )
                    showConfirmation = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValidInput && availableDurations.isNotEmpty() && isDateValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) Color(0xFF006060) else Color(0xFF006B6B),
                    contentColor = Color.White
                )
            ) {
                Text("Create Booking")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showConfirmation && pendingBooking != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmation = false
                pendingBooking = null
            },
            title = {
                Text("Confirm Booking", color = textColor, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Study Space: ${pendingBooking!!.roomId}", color = textColor)
                    Text("Campus: ${pendingBooking!!.campus}", color = textColor)
                    Text("Building: ${pendingBooking!!.building}", color = textColor)
                    Text("Level: ${pendingBooking!!.level}", color = textColor)
                    Text(
                        SimpleDateFormat("dd MMM yyyy", Locale.US).format(pendingBooking!!.bookingDate),
                        color = textColor
                    )
                    Text(
                        "${SimpleDateFormat("HH:mm", Locale.US).format(pendingBooking!!.startTime)} - " +
                                "${SimpleDateFormat("HH:mm", Locale.US).format(pendingBooking!!.endTime)}",
                        color = textColor
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingBooking?.let { booking ->
                            Logger.getLogger("BookingDetailsScreen").info("Confirming booking: $booking")
                            viewModel.createBooking(
                                spaceId = booking.roomId,
                                level = booking.level,
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(booking.bookingDate),
                                timeSlot = SimpleDateFormat("HH:mm", Locale.US).format(booking.startTime),
                                studentId = booking.studentId,
                                durationMinutes = durationMinutes,
                                campus = booking.campus,
                                building = booking.building,
                                onSuccess = { navController.popBackStack() },
                                onFailure = { errorMessage ->
                                    // Handle failure if needed, maybe show a snackbar
                                    // _errorMessage.value = errorMessage
                                }
                            )
                            pendingBooking = null
                            showConfirmation = false
                        }
                    },
                    enabled = pendingBooking != null
                ) {
                    Text("Confirm", color = textColor)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmation = false
                        pendingBooking = null
                    }
                ) {
                    Text("Cancel", color = textColor)
                }
            },
            containerColor = if (isDarkTheme) Color(0xFF242424) else Color.White,
            titleContentColor = textColor,
            textContentColor = textColor
        )
    }
}

@Composable
fun DurationDropdown(
    selectedDuration: Int,
    onDurationSelected: (Int) -> Unit,
    availableDurations: List<Int>,
    textColor: Color,
    isDarkTheme: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = formatDuration(selectedDuration),
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = "Toggle Duration Dropdown",
                    tint = textColor
                )
            },
            enabled = availableDurations.isNotEmpty(),
            placeholder = { if (availableDurations.isEmpty()) Text("No durations available", color = textColor.copy(alpha = 0.5f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedLabelColor = textColor,
                unfocusedLabelColor = textColor,
                focusedBorderColor = textColor,
                unfocusedBorderColor = textColor.copy(alpha = 0.5f)
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(if (isDarkTheme) Color(0xFF242424) else Color.White)
                .width(200.dp)
        ) {
            availableDurations.forEach { duration ->
                DropdownMenuItem(
                    text = { Text(formatDuration(duration), color = textColor, fontSize = 14.sp) },
                    onClick = {
                        onDurationSelected(duration)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = availableDurations.isNotEmpty()) { expanded = true }
        )
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun formatDuration(minutes: Int): String {
    return when (minutes) {
        30 -> "30 minutes"
        60 -> "1 hour"
        90 -> "1.5 hours"
        120 -> "2 hours"
        else -> "$minutes minutes"
    }
}