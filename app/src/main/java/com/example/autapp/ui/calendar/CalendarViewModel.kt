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

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val timetableEntries: List<TimetableEntryDao.TimetableEntryWithCourse> = emptyList(),
    val events: List<Event> = emptyList(),
    val filteredEvents: List<Event> = emptyList(),
    val bookings: List<Booking> = emptyList(),
    val filteredBookings: List<Booking> = emptyList(),
    val isCalendarView: Boolean = true,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

class CalendarViewModel(
    private val timetableEntryRepository: TimetableEntryRepository,
    private val studentRepository: StudentRepository,
    private val eventRepository: EventRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    
    private var _studentId: Int = 0
    val studentId: Int get() = _studentId

    private val _navigateToManageEvents = MutableStateFlow(false)
    val navigateToManageEvents: StateFlow<Boolean> = _navigateToManageEvents.asStateFlow()

    fun navigateToManageEvents() {
        _navigateToManageEvents.value = true
    }

    fun onManageEventsNavigated() {
        _navigateToManageEvents.value = false
    }

    // Helper function to convert between java.util.Date and LocalDate
    private fun Date.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    private fun LocalDate.toDate(): Date {
        return Date(this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    fun initialize(studentId: Int) {
        _studentId = studentId
        CoroutineScope(Dispatchers.Main + Job()).launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val timetableEntries = timetableEntryRepository.getTimetableEntriesWithCourseByDay(_uiState.value.selectedDate.dayOfWeek.value)
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
                    timetableEntries = timetableEntries,
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

    fun updateSelectedDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        if (_uiState.value.isCalendarView) {
            fetchTimetableData()
            fetchEventsForDate()
        }
    }

    fun toggleView() {
        _uiState.update { it.copy(isCalendarView = !it.isCalendarView) }
        if (!_uiState.value.isCalendarView) {
            fetchNextTwoWeeksData()
        } else {
            fetchTimetableData()
            fetchEventsForDate()
        }
    }

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

    private fun hasOverlappingEvents(newEvent: Event, excludeEventId: Int = -1): Boolean {
        if (newEvent.isToDoList) return false
        if (newEvent.startTime == null || newEvent.endTime == null) return false

        val eventDate = newEvent.date.toLocalDate()
        
        return _uiState.value.events.any { existingEvent ->
            if (existingEvent.eventId == excludeEventId) return@any false
            if (existingEvent.isToDoList) return@any false
            if (existingEvent.startTime == null || existingEvent.endTime == null) return@any false
            
            val existingDate = existingEvent.date.toLocalDate()
            if (eventDate != existingDate) return@any false
            
            !(newEvent.endTime.before(existingEvent.startTime) || 
              newEvent.startTime.after(existingEvent.endTime))
        }
    }

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