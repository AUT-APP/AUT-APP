package com.example.autapp.ui.calendar

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.autapp.data.firebase.FirebaseEvent
import com.example.autapp.data.firebase.FirebaseBooking
import com.example.autapp.data.firebase.FirebaseTimetableEntry
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


@Composable
fun TimetableView(
    uiState: CalendarUiState,
    onEventClick: (FirebaseEvent) -> Unit,
    onSetReminder: (Any, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEntryForReminder by remember { mutableStateOf<Any?>(null) }

    selectedEntryForReminder?.let { selectedEntry ->
        ReminderBottomSheet(
            onDismiss = { selectedEntryForReminder = null },
            onSelectTime = { minutes ->
                onSetReminder(selectedEntry, minutes)
                selectedEntryForReminder = null
            }
        )
    }
    val today = LocalDate.now()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        val allEntries = mutableListOf<Pair<LocalDate, Any>>()

        // Combine and group timetable entries, events, and bookings by date
        for (dayOffset in 0..13) {
            val date = today.plusDays(dayOffset.toLong())

            val timetableEntriesForDay = uiState.timetableEntries
                .filter { it.dayOfWeek == date.dayOfWeek.value }

            Log.d("TimetableView", "Processing entries for day ${date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}: ${timetableEntriesForDay.size} entries")
            timetableEntriesForDay.forEach { entry ->
                 Log.d("TimetableView", "Timetable Entry for Day: CourseId=${entry.courseId}, StartTime=${entry.startTime}, EndTime=${entry.endTime}, DayOfWeek=${entry.dayOfWeek}")
            }

            val eventsForDay = uiState.events.filter { event ->
                event.date?.toLocalDate() == date
            }

            val bookingsForDay = uiState.bookings.filter { booking ->
                booking.bookingDate?.toLocalDate() == date
            }

            val entriesForThisDate = (timetableEntriesForDay + eventsForDay + bookingsForDay).sortedWith(compareBy { entry ->
                when (entry) {
                    is FirebaseTimetableEntry -> entry.startTime
                    is FirebaseEvent -> entry.startTime ?: Date(0)
                    is FirebaseBooking -> entry.startTime
                    else -> Date(0)
                }
            })

            entriesForThisDate.forEach { entry ->
                allEntries.add(date to entry)
            }
        }

        val sortedEntriesByDate = allEntries.groupBy { it.first }.toSortedMap()

        if (sortedEntriesByDate.isEmpty()) {
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
            sortedEntriesByDate.forEach { (date, entries) ->
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
                }
                items(entries) { (date, entry) ->
                    when (entry) {
                        is FirebaseTimetableEntry -> {
                            val timetableEntry = entry as FirebaseTimetableEntry
                            val course = uiState.courses.find { it.courseId == timetableEntry.courseId.toString() }
                            if (course != null) {
                                TimetableEntryCard(
                                    timetableEntry = timetableEntry,
                                    course = course,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onReminderClick = {
                                        selectedEntryForReminder = entry
                                    }
                                )
                            }
                        }
                        is FirebaseEvent -> {
                            val event = entry as FirebaseEvent
                            EventCard(
                                event = event.toEvent(),
                                onClick = { onEventClick(event) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onReminderClick = {
                                    selectedEntryForReminder = entry
                                }
                            )
                        }
                        is FirebaseBooking -> {
                            val booking = entry as FirebaseBooking
                            BookingCard(
                                booking = booking.toBooking(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onReminderClick = {
                                    selectedEntryForReminder = entry
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}