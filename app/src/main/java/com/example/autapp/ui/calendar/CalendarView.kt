package com.example.autapp.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.autapp.data.firebase.FirebaseEvent
import com.example.autapp.data.firebase.FirebaseBooking
import com.example.autapp.data.firebase.FirebaseTimetableEntry
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import java.util.Locale

@Composable
fun CalendarView(
    uiState: CalendarUiState,
    onDateSelected: (LocalDate) -> Unit,
    onEventClick: (FirebaseEvent) -> Unit,
    onSetReminder: (Any, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // State to keep track of the currently displayed month and year.
    // Initialized with the month and year of the initially selected date from uiState.
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(uiState.selectedDate)) }
    var selectedEntryForReminder by remember { mutableStateOf<Any?>(null) }

    selectedEntryForReminder?.let { selectedEntry ->
        ReminderBottomSheet(
            selectedEntry = selectedEntry,
            onDismiss = { selectedEntryForReminder = null },
            onSelectTime = { minutes ->
                onSetReminder(selectedEntry, minutes)
                selectedEntryForReminder = null
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Section for month navigation (Previous Month, Month Year Display, Next Month)
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
                Icon(Icons.Filled.ChevronLeft, "Previous month")
            }

            // Display the current month and year (e.g., "May 2024")
            Text(
                text = currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + currentYearMonth.year,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // IconButton to navigate to the next month.
            IconButton(onClick = {
                currentYearMonth = currentYearMonth.plusMonths(1)
            }) {
                Icon(Icons.Filled.ChevronRight, "Next month")
            }
        }

        // Header row displaying the abbreviated days of the week (Sun, Mon, Tue, etc.)
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

        // Calendar grid displaying the days of the month.
        // `days` is a list of LocalDate? (nullable) generated by `generateDaysForMonth`.
        // It's remembered and re-calculated whenever `currentYearMonth` changes.
        val days = remember(currentYearMonth) {
            generateDaysForMonth(currentYearMonth)
        }

        // LazyVerticalGrid is used to efficiently display the 7-column calendar grid.
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            items(days.size) { index ->
                val date = days[index] // Get the LocalDate for the current cell, or null if it's an empty cell.
                if (date != null) { // Only render content if it's an actual date, not a padding cell.
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(
                                // Conditional background for each date cell:
                                when {
                                    // Highlight the currently selected date.
                                    date == uiState.selectedDate -> MaterialTheme.colorScheme.primary
                                    // Dates belonging to the current month have a transparent background.
                                    date.month == currentYearMonth.month -> Color.Transparent
                                    // Dates not belonging to the current month (padding days from prev/next month) also transparent.
                                    else -> Color.Transparent
                                }
                            )
                            .clickable(
                                // Dates are clickable only if they belong to the currently displayed month.
                                enabled = date.month == currentYearMonth.month,
                                onClick = { onDateSelected(date) } // Call lambda when a date is selected.
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            // Conditional text color for each date number:
                            color = when {
                                // Text color for the selected date.
                                date == uiState.selectedDate -> MaterialTheme.colorScheme.onPrimary
                                // Text color for dates within the current month.
                                date.month == currentYearMonth.month -> MaterialTheme.colorScheme.onSurface
                                // Dimmed text color for dates outside the current month (padding days).
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            }
                        )
                    }
                } else {
                    // If 'date' is null, it's an empty cell in the grid (padding before/after month days).
                    // Render an empty Box to maintain grid structure.
                    Box(modifier = Modifier.aspectRatio(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section to display entries, events, and bookings for the selected date.
        // Filter timetable entries to show only those matching the selected date's day of the week.
        // Then, make them distinct based on course ID, start time, and end time to avoid duplicates.
        // Finally, sort them by start time.
        val selectedDateEntries = uiState.timetableEntries
            .filter { entry ->
                entry.dayOfWeek == uiState.selectedDate.dayOfWeek.value
            }
            .distinctBy { entry ->
                // Create a unique key for distinctBy based on course, start and end time
                "${entry.courseId}_${entry.startTime.time}_${entry.endTime.time}"
            }
            .sortedBy { it.startTime }

        // Events for the selected date (already filtered in ViewModel, just sort by start time).
        val selectedDateEvents = uiState.filteredEvents.sortedBy { it.startTime }
        // Bookings for the selected date (already filtered in ViewModel, just sort by start time).
        val selectedDateBookings = uiState.filteredBookings.sortedBy { it.startTime }

        // Display the schedule list if there are any entries, events, or bookings for the selected date.
        if (selectedDateEntries.isNotEmpty() || selectedDateEvents.isNotEmpty() || selectedDateBookings.isNotEmpty()) {
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
                items(selectedDateEntries) { entry: FirebaseTimetableEntry ->
                    val course = uiState.courses.find { it.courseId == entry.courseId.toString() }
                    if (course != null) {
                        TimetableEntryCard(
                            timetableEntry = entry,
                            course = course,
                            onReminderClick = {
                                selectedEntryForReminder = entry
                            }
                        )
                    }
                }

                items(selectedDateEvents) { event: FirebaseEvent ->
                    EventCard(
                        event = event.toEvent(),
                        onClick = { onEventClick(event) },
                        onReminderClick = {
                            selectedEntryForReminder = event
                        }
                    )
                }

                items(selectedDateBookings) { booking: FirebaseBooking ->
                    BookingCard(
                        booking = booking.toBooking(),
                        onReminderClick = {
                            selectedEntryForReminder = booking
                        }
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