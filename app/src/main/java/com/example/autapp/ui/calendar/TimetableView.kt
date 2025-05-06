package com.example.autapp.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.autapp.data.dao.TimetableEntryDao
import com.example.autapp.data.models.Event
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

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
            val entriesByDate = mutableMapOf<LocalDate, List<Any>>()
            for (dayOffset in 0..13) {
                val date = today.plusDays(dayOffset.toLong())
                val timetableEntriesForDay = nextTwoWeeksEntries
                    .filter { it.entry.dayOfWeek == date.dayOfWeek.value }
                    .distinctBy { entry ->
                        "${entry.entry.courseId}_${entry.entry.startTime.time}_${entry.entry.endTime.time}"
                    }
                val eventsForDay = nextTwoWeeksEvents.filter { event ->
                    event.date.toLocalDate() == date
                }
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

fun Date.format(pattern: String): String = SimpleDateFormat(pattern, Locale.getDefault()).format(this) 