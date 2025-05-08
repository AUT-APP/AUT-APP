package com.example.autapp.ui.calendar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autapp.data.dao.TimetableEntryDao
import com.example.autapp.data.models.Event
import com.example.autapp.data.models.Booking
import com.example.autapp.data.repository.TimetableEntryRepository
import com.example.autapp.data.repository.StudentRepository
import com.example.autapp.data.repository.EventRepository
import com.example.autapp.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.Instant
import java.util.Date

/**
 * Data class representing the UI state for the Calendar screen.
 * It holds all the data needed to render the calendar, timetable, events, and bookings.
 */

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(), // The currently selected date in the calendar.
    val timetableEntries: List<TimetableEntryDao.TimetableEntryWithCourse> = emptyList(), // List of all timetable entries for the relevant period (selected day or next two weeks).
    val events: List<Event> = emptyList(), // List of all events for the student.
    val filteredEvents: List<Event> = emptyList(), // List of events filtered for the selectedDate.
    val bookings: List<Booking> = emptyList(), // List of all active bookings for the student.
    val filteredBookings: List<Booking> = emptyList(), // List of bookings filtered for the selectedDate.
    val isCalendarView: Boolean = true, // Flag to determine if the calendar view or timetable list view is active.
    val errorMessage: String? = null, // Holds any error message to be displayed to the user.
    val isLoading: Boolean = false // Flag to indicate if data is currently being loaded.
)

class CalendarViewModel(
    private val timetableEntryRepository: TimetableEntryRepository,
    private val studentRepository: StudentRepository,
    private val eventRepository: EventRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState()) // Private MutableStateFlow to hold the UI state.
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow() // Publicly exposed StateFlow for observing UI state changes.
    
    private var _studentId: Int = 0 // Stores the ID of the current student.
    val studentId: Int get() = _studentId

    // StateFlow to signal navigation to the ManageEventsScreen.
    private val _navigateToManageEvents = MutableStateFlow(false)
    val navigateToManageEvents: StateFlow<Boolean> = _navigateToManageEvents.asStateFlow()

    // Triggers navigation to the ManageEventsScreen.
    fun navigateToManageEvents() {
        _navigateToManageEvents.value = true
    }

    // Resets the navigation trigger after navigation has occurred.
    fun onManageEventsNavigated() {
        _navigateToManageEvents.value = false
    }

    // Helper function to convert java.util.Date to org.threeten.bp.LocalDate.
    private fun Date.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    // Helper function to convert org.threeten.bp.LocalDate to java.util.Date.
    private fun LocalDate.toDate(): Date {
        return Date(this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    /**
     * Initializes the ViewModel with the student's ID and fetches initial data.
     * This should be called once when the ViewModel is created.
     */

    fun initialize(studentId: Int) {
        _studentId = studentId
        CoroutineScope(Dispatchers.Main + Job()).launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val studentWithCourses = studentRepository.getStudentWithCourses(studentId)
                val courseIds = studentWithCourses?.courses?.map { it.courseId } ?: emptyList()
                
                val allEntries = timetableEntryRepository.getTimetableEntriesWithCourseByDay(_uiState.value.selectedDate.dayOfWeek.value)
                val filteredEntries = allEntries
                    .filter { entry -> courseIds.contains(entry.entry.courseId) }
                    .distinctBy { entry -> 
                        "${entry.entry.courseId}_${entry.entry.startTime.time}_${entry.entry.endTime.time}"
                    }
                    .sortedBy { it.entry.startTime }
                
                val events = eventRepository.getEventsByStudent(studentId)
                val filteredEvents = events.filter { event ->
                    event.date.toLocalDate() == _uiState.value.selectedDate
                }
                val bookings = bookingRepository.getActiveBookingsByStudent(studentId)
                val filteredBookings = bookings.filter { booking ->
                    booking.bookingDate.toLocalDate() == _uiState.value.selectedDate
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    timetableEntries = filteredEntries,
                    events = events,
                    filteredEvents = filteredEvents,
                    bookings = bookings,
                    filteredBookings = filteredBookings,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Fetches timetable entries and bookings relevant to the currently selected date.
     * Used when in CalendarView or when initially loading.
     */

    private fun fetchTimetableData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val studentWithCourses = studentRepository.getStudentWithCourses(_studentId)
                val courseIds = studentWithCourses?.courses?.map { it.courseId } ?: emptyList()
                
                val dayOfWeek = _uiState.value.selectedDate.dayOfWeek.value
                val allEntries = timetableEntryRepository.getTimetableEntriesWithCourseByDay(dayOfWeek)
                
                val filteredEntries = allEntries
                    .filter { entry -> courseIds.contains(entry.entry.courseId) }
                    .distinctBy { entry ->
                        "${entry.entry.courseId}_${entry.entry.startTime.time}_${entry.entry.endTime.time}"
                    }
                    .sortedBy { it.entry.startTime }

                val bookings = bookingRepository.getActiveBookingsByStudent(_studentId)
                val filteredBookings = bookings.filter { booking ->
                    booking.bookingDate.toLocalDate() == _uiState.value.selectedDate
                }
                
                _uiState.update { currentState ->
                    currentState.copy(
                        timetableEntries = filteredEntries,
                        bookings = bookings,
                        filteredBookings = filteredBookings,
                        errorMessage = null,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error fetching timetable: ${e.message}", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        errorMessage = "Error loading timetable: ${e.message}",
                        timetableEntries = emptyList(),
                        bookings = emptyList(),
                        filteredBookings = emptyList(),
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Filters the already fetched list of all events to show only those for the selectedDate.
     * This avoids redundant database calls when only the selected date changes.
     */

    private fun fetchEventsForDate() {
        viewModelScope.launch {
            try {
                // Don't fetch events again, just filter the existing ones
                val filteredEvents = _uiState.value.events.filter { event ->
                    event.date.toLocalDate() == _uiState.value.selectedDate
                }
                
                _uiState.value = _uiState.value.copy(
                    filteredEvents = filteredEvents,
                    errorMessage = null
                )
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error filtering events: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error filtering events: ${e.message}",
                    filteredEvents = emptyList()
                )
            }
        }
    }

    /**
     * Fetches timetable entries and bookings for the next two weeks from today.
     * Used when switching to the TimetableView (list view).
     */

    private fun fetchNextTwoWeeksData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val studentWithCourses = studentRepository.getStudentWithCourses(_studentId)
                val courseIds = studentWithCourses?.courses?.map { it.courseId } ?: emptyList()
                
                val today = LocalDate.now()
                val allEntries = mutableListOf<Pair<LocalDate, TimetableEntryDao.TimetableEntryWithCourse>>()
                
                for (dayOffset in 0..13) {
                    val date = today.plusDays(dayOffset.toLong())
                    val dayOfWeek = date.dayOfWeek.value
                    
                    val entriesForDay = timetableEntryRepository.getTimetableEntriesWithCourseByDay(dayOfWeek)
                        .filter { entry -> courseIds.contains(entry.entry.courseId) }
                        .distinctBy { entry -> 
                            "${entry.entry.courseId}_${entry.entry.startTime.time}_${entry.entry.endTime.time}"
                        }
                        .sortedBy { it.entry.startTime }
                    
                    entriesForDay.forEach { entry ->
                        allEntries.add(date to entry)
                    }
                }

                val sortedEntries = allEntries
                    .sortedWith(compareBy<Pair<LocalDate, TimetableEntryDao.TimetableEntryWithCourse>> 
                        { it.first }
                        .thenBy { it.second.entry.startTime })
                    .map { it.second }

                val bookings = bookingRepository.getActiveBookingsByStudent(_studentId)
                
                _uiState.update { currentState ->
                    currentState.copy(
                        timetableEntries = sortedEntries,
                        bookings = bookings,
                        errorMessage = null,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error fetching timetable data: ${e.message}", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        errorMessage = "Error loading timetable: ${e.message}",
                        timetableEntries = emptyList(),
                        bookings = emptyList(),
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Calculates a LocalDate for a given day of the week (1=Monday, 7=Sunday)
     * relative to the current week. For example, if today is Wednesday and dayOfWeek is 1 (Monday),
     * it will return the date of the Monday of the current week.
     * If dayOfWeek is 5 (Friday), it will return the date of the Friday of the current week.
     */

    fun getDayRelativeToToday(dayOfWeek: Int): LocalDate {
        val today = LocalDate.now()
        val todayDayOfWeek = today.dayOfWeek.value
        
        // Convert Sunday from 7 to 0 for calculation
        val adjustedDayOfWeek = if (dayOfWeek == 7) 0 else dayOfWeek
        val adjustedTodayDayOfWeek = if (todayDayOfWeek == 7) 0 else todayDayOfWeek
        
        // Calculate the difference in days
        val dayDifference = (adjustedDayOfWeek - adjustedTodayDayOfWeek + 7) % 7
        return today.plusDays(dayDifference.toLong())
    }

    // Updates the selectedDate in the UI state and fetches relevant data if in CalendarView.
    fun updateSelectedDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        // If in calendar view, refresh timetable and events for the newly selected date.
        if (_uiState.value.isCalendarView) {
            fetchTimetableData()
            fetchEventsForDate()
        }
    }

    // Toggles between CalendarView and TimetableView (list view).
    // Fetches appropriate data based on the new view.
    fun toggleView() {
        _uiState.update { it.copy(isCalendarView = !it.isCalendarView) }
        if (!_uiState.value.isCalendarView) {
            // Switched to Timetable list view, fetch next two weeks data.
            fetchNextTwoWeeksData()
        } else {
            // Switched back to Calendar view, fetch data for the selected date.
            fetchTimetableData()
            fetchEventsForDate()
        }
    }

    /**
     * Adds a new event to the database and updates the UI state.
     * Includes a check for overlapping events (excluding to-do items).
     */

    fun addEvent(event: Event) {
        if (hasOverlappingEvents(event)) {
            _uiState.value = _uiState.value.copy(errorMessage = "This event overlaps with an existing event")
            return
        }
        
        viewModelScope.launch {
            try {
                eventRepository.insertEvent(event.copy(studentId = _studentId))
                // Fetch all events again to update both lists
                val updatedEvents = eventRepository.getEventsByStudent(_studentId)
                _uiState.value = _uiState.value.copy(
                    events = updatedEvents,
                    filteredEvents = updatedEvents.filter { it.date.toLocalDate() == _uiState.value.selectedDate },
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to add event: ${e.message}")
            }
        }
    }

    /**
     * Updates an existing event. This implementation deletes all events with the same title and date,
     * then inserts the updated event. This handles cases where an event might change significantly (e.g. recurring to single).
     * Consider a more targeted update if events have unique persistent IDs that don't change based on title/date.
     */

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            try {
                // Get all events with the same title and date
                val existingEvents = eventRepository.getEventsByTitleAndDate(event.title, event.date)
                
                // Delete all existing events
                existingEvents.forEach { existingEvent ->
                    eventRepository.deleteEvent(existingEvent)
                }

                // Insert the updated event
                eventRepository.insertEvent(event)

                // Fetch and update the UI state
                fetchEvents()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    /**
     * Checks if a new event overlaps with any existing non-to-do events on the same day.
     * Can optionally exclude a specific eventId from the check (used during updates).
     */

    private fun hasOverlappingEvents(newEvent: Event, excludeEventId: Int = -1): Boolean {
        if (newEvent.isToDoList) return false // To-do items don't have times, so no overlap.
        if (newEvent.startTime == null || newEvent.endTime == null) return false // Event must have start/end times.

        val eventDate = newEvent.date.toLocalDate()
        
        return _uiState.value.events.any { existingEvent ->
            if (existingEvent.eventId == excludeEventId) return@any false // Don't compare an event with itself.
            if (existingEvent.isToDoList) return@any false // Ignore to-do items for overlap checks.
            if (existingEvent.startTime == null || existingEvent.endTime == null) return@any false // Existing event must have times.
            
            val existingDate = existingEvent.date.toLocalDate()
            if (eventDate != existingDate) return@any false // Events must be on the same date to overlap.
            
            // Overlap condition: Not (New event ends before existing one starts OR New event starts after existing one ends)
            !(newEvent.endTime.before(existingEvent.startTime) || 
              newEvent.startTime.after(existingEvent.endTime))
        }
    }

    // Deletes an event from the database and refreshes the event list in the UI state.
    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            try {
                // Delete the specific event
                eventRepository.deleteEvent(event)
                fetchEvents()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    /**
     * Fetches all events for the current student from the repository.
     * and updates both the raw 'events' list and the 'filteredEvents' list in the UI state
     */

    private fun fetchEvents() {
        viewModelScope.launch {
            try {
                val events = eventRepository.getEventsByStudent(_studentId)
                _uiState.value = _uiState.value.copy(
                    events = events,
                    filteredEvents = events.filter { it.date.toLocalDate() == _uiState.value.selectedDate },
                    errorMessage = null
                )
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error fetching events: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error loading events: ${e.message}",
                    events = emptyList(),
                    filteredEvents = emptyList()
                )
            }
        }
    }
}