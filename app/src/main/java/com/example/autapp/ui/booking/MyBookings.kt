package com.example.autapp.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.text.SimpleDateFormat
import java.util.*

enum class BookingStatusFilter { ALL, ACTIVE, UPCOMING }
enum class DateRange { ALL, TODAY, THIS_WEEK, THIS_MONTH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    viewModel: BookingViewModel,
    navController: NavController,
    studentId: String,
    isDarkTheme: Boolean,
    paddingValues: PaddingValues
) {
    val bookings by viewModel.myBookings.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val containerColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F5)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF333333)

    var selectedStatusFilter by remember { mutableStateOf(BookingStatusFilter.ALL) }
    var selectedDateRange by remember { mutableStateOf(DateRange.ALL) }

    LaunchedEffect(studentId, selectedStatusFilter, selectedDateRange) {
        viewModel.fetchMyBookings(studentId)
    }

    val filteredBookings = remember(selectedStatusFilter, selectedDateRange, bookings) {
        bookings.filter { booking ->
            val bookingDate = Calendar.getInstance().apply { time = booking.bookingDate }
            val matchesStatus = when (selectedStatusFilter) {
                BookingStatusFilter.ALL -> true
                BookingStatusFilter.ACTIVE -> booking.isActive
                BookingStatusFilter.UPCOMING -> booking.isUpcoming
            }
            val matchesDate = when (selectedDateRange) {
                DateRange.ALL -> true
                DateRange.TODAY -> {
                    val today = Calendar.getInstance()
                    today.get(Calendar.YEAR) == bookingDate.get(Calendar.YEAR) &&
                            today.get(Calendar.DAY_OF_YEAR) == bookingDate.get(Calendar.DAY_OF_YEAR)
                }
                DateRange.THIS_WEEK -> {
                    val today = Calendar.getInstance()
                    today.get(Calendar.WEEK_OF_YEAR) == bookingDate.get(Calendar.WEEK_OF_YEAR) &&
                            today.get(Calendar.YEAR) == bookingDate.get(Calendar.YEAR)
                }
                DateRange.THIS_MONTH -> {
                    val today = Calendar.getInstance()
                    today.get(Calendar.MONTH) == bookingDate.get(Calendar.MONTH) &&
                            today.get(Calendar.YEAR) == bookingDate.get(Calendar.YEAR)
                }
            }
            matchesStatus && matchesDate
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor)
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        MyBookingsFilter(
            selectedStatusFilter = selectedStatusFilter,
            onStatusFilterSelected = { selectedStatusFilter = it },
            selectedDateRange = selectedDateRange,
            onDateRangeSelected = { selectedDateRange = it },
            isDarkTheme = isDarkTheme
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredBookings.isEmpty()) {
            EmptyBookingsState(isDarkTheme = isDarkTheme)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredBookings) { booking ->
                    BookingCard(
                        booking = booking,
                        onCancel = {
                            viewModel.cancelBooking(
                                booking = booking,
                                spaceId = booking.roomId,
                                building = booking.building,
                                campus = booking.campus,
                                level = booking.level,
                                date = booking.bookingDate
                            )
                        },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }
    }
}

@Composable
fun EmptyBookingsState(isDarkTheme: Boolean) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF333333)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EventBusy,
                contentDescription = "No Bookings",
                tint = textColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No bookings found",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Try adjusting the filters or book a new study space.",
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun BookingCard(booking: Booking, onCancel: () -> Unit, isDarkTheme: Boolean) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF333333)
    var showCancelConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF242424) else Color.White
        )
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
                    "${booking.level} - ${booking.roomId}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BookingStatusTag(booking = booking, isDarkTheme = isDarkTheme)
            }
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(
                icon = Icons.Default.LocationCity,
                label = "Campus",
                value = booking.campus,
                textColor = textColor
            )
            HorizontalDivider(color = textColor.copy(alpha = 0.1f), thickness = 1.dp)
            DetailRow(
                icon = Icons.Default.Home,
                label = "Building",
                value = booking.building,
                textColor = textColor
            )
            HorizontalDivider(color = textColor.copy(alpha = 0.1f), thickness = 1.dp)
            DetailRow(
                icon = Icons.Default.CalendarToday,
                label = "Date",
                value = SimpleDateFormat("dd MMM yyyy").format(booking.bookingDate),
                textColor = textColor
            )
            HorizontalDivider(color = textColor.copy(alpha = 0.1f), thickness = 1.dp)
            DetailRow(
                icon = Icons.Default.AccessTime,
                label = "Time",
                value = "${SimpleDateFormat("HH:mm").format(booking.startTime)} - ${
                    SimpleDateFormat("HH:mm").format(booking.endTime)
                }",
                textColor = textColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (booking.isActive || booking.isUpcoming) {
                Button(
                    onClick = { showCancelConfirmation = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text("Cancel Booking")
                }
            }
        }
    }

    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = {
                Text("Confirm Cancellation", color = textColor, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Are you sure you want to cancel this booking?", color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Study Space: ${booking.level} - ${booking.roomId}", color = textColor)
                    Text("Campus: ${booking.campus}", color = textColor)
                    Text("Building: ${booking.building}", color = textColor)
                    Text(
                        "Date: ${SimpleDateFormat("dd MMM yyyy").format(booking.bookingDate)}",
                        color = textColor
                    )
                    Text(
                        "Time: ${SimpleDateFormat("HH:mm").format(booking.startTime)} - " +
                                "${SimpleDateFormat("HH:mm").format(booking.endTime)}",
                        color = textColor
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancel()
                        showCancelConfirmation = false
                    }
                ) {
                    Text("Confirm", color = textColor)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelConfirmation = false }
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
fun BookingStatusTag(booking: Booking, isDarkTheme: Boolean) {
    val textColor = if (isDarkTheme) Color.White else Color.White
    val backgroundColor = when {
        booking.isActive -> Color(0xFF6ABF69) // Green for Active
        booking.isUpcoming -> Color(0xFF42A5F5) // Blue for Upcoming
        else -> Color.Gray // Fallback (should not occur)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = when {
                booking.isActive -> "Active"
                booking.isUpcoming -> "Upcoming"
                else -> "Unknown"
            },
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MyBookingsFilter(
    selectedStatusFilter: BookingStatusFilter,
    onStatusFilterSelected: (BookingStatusFilter) -> Unit,
    selectedDateRange: DateRange,
    onDateRangeSelected: (DateRange) -> Unit,
    isDarkTheme: Boolean
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF333333)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            var statusExpanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                OutlinedTextField(
                    value = selectedStatusFilter.name,
                    onValueChange = {},
                    label = { Text("Status", color = textColor) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(
                            imageVector = if (statusExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle Status Dropdown",
                            tint = textColor
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                DropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false },
                    modifier = Modifier.background(if (isDarkTheme) Color(0xFF242424) else Color.White)
                ) {
                    BookingStatusFilter.values().forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.name, color = textColor, fontSize = 14.sp) },
                            onClick = {
                                onStatusFilterSelected(status)
                                statusExpanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { statusExpanded = true }
                )
            }
            var dateRangeExpanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                OutlinedTextField(
                    value = selectedDateRange.name,
                    onValueChange = {},
                    label = { Text("Date Range", color = textColor) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(
                            imageVector = if (dateRangeExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle Date Range Dropdown",
                            tint = textColor
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                DropdownMenu(
                    expanded = dateRangeExpanded,
                    onDismissRequest = { dateRangeExpanded = false },
                    modifier = Modifier.background(if (isDarkTheme) Color(0xFF242424) else Color.White)
                ) {
                    DateRange.values().forEach { range ->
                        DropdownMenuItem(
                            text = { Text(range.name, color = textColor, fontSize = 14.sp) },
                            onClick = {
                                onDateRangeSelected(range)
                                dateRangeExpanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { dateRangeExpanded = true }
                )
            }
        }
    }
}