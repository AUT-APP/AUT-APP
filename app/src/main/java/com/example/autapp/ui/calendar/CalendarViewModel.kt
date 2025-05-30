package com.example.autapp.ui.calendar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.models.Event
import com.example.autapp.data.models.Booking
import com.example.autapp.data.firebase.*
import com.example.autapp.ui.DashboardViewModel
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
import com.example.autapp.data.firebase.FirebaseTimetableEntry
import com.example.autapp.data.firebase.FirebaseEvent
import com.example.autapp.data.firebase.FirebaseBooking
import com.example.autapp.data.firebase.FirebaseCourseRepository
import com.example.autapp.data.firebase.QueryCondition
import com.example.autapp.data.firebase.QueryOperator
import java.util.*
import java.text.SimpleDateFormat

/**
 * Data class representing the UI state for the Calendar screen.
 * It holds all the data needed to render the calendar, timetable, events, and bookings.
 */

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(), // The currently selected date in the calendar.
    val timetableEntries: List<FirebaseTimetableEntry> = emptyList(), // List of all timetable entries for the relevant period (selected day or next two weeks).
    val events: List<FirebaseEvent> = emptyList(), // List of all events for the student.
    val filteredEvents: List<FirebaseEvent> = emptyList(), // List of events filtered for the selectedDate.
    val bookings: List<FirebaseBooking> = emptyList(), // List of all active bookings for the student.
    val filteredBookings: List<FirebaseBooking> = emptyList(), // List of bookings filtered for the selectedDate.
    val isCalendarView: Boolean = true, // Flag to determine if the calendar view or timetable list view is active.
    val errorMessage: String? = null, // Holds any error message to be displayed to the user.
    val isLoading: Boolean = false, // Flag to indicate if data is currently being loaded.
    val isTeacher: Boolean,  // New field to track if user is a teacher
    val userId: String,
    val courses: List<FirebaseCourse> = emptyList() // Added courses property
)

class CalendarViewModel(
    private val timetableEntryRepository: FirebaseTimetableRepository,
    private val studentRepository: FirebaseStudentRepository,
    private val eventRepository: FirebaseEventRepository,
    private val bookingRepository: FirebaseBookingRepository,
    private val courseRepository: FirebaseCourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(isTeacher = false, userId = "")) // Private MutableStateFlow to hold the UI state.
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow() // Publicly exposed StateFlow for observing UI state changes.

    private var _userId: String = ""
    private var _isTeacher: Boolean = false
    val userId: String get() = _userId
    val isTeacher: Boolean get() = _isTeacher

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

    fun initialize(userId: String, isTeacher: Boolean) {
        _userId = userId
        _isTeacher = isTeacher
        _uiState.value = _uiState.value.copy(isTeacher = isTeacher, userId = userId)

        CoroutineScope(Dispatchers.Main + Job()).launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val courseIds = if (isTeacher) {
                    courseRepository.queryByField("teacherId", userId).map { it.courseId }
                } else {
                    studentRepository.getStudentWithCourses(userId).second.map { it.courseId }
                }

                val dayOfWeek = _uiState.value.selectedDate.dayOfWeek.value
                val conditions = if (isTeacher) {
                    listOf(
                        QueryCondition("teacherId", QueryOperator.EQUAL_TO, userId),
                        QueryCondition("dayOfWeek", QueryOperator.EQUAL_TO, dayOfWeek)
                    )
                } else {
                    listOf(
                        QueryCondition("dayOfWeek", QueryOperator.EQUAL_TO, dayOfWeek)
                    )
                }

                val allEntries = timetableEntryRepository.query(conditions)
                val filteredEntries: List<FirebaseTimetableEntry> = withContext(Dispatchers.Default) {
                    if (isTeacher) {
                        allEntries
                    } else {
                        allEntries.filter { entry: FirebaseTimetableEntry -> courseIds.contains(entry.courseId as String) }
                            .distinctBy { entry: FirebaseTimetableEntry ->
                                "${entry.courseId}_${entry.startTime}_${entry.endTime}"
                            }
                    }.sortedBy { it.startTime }
                }

                val events = eventRepository.queryByField("userId", userId)
                val filteredEvents = withContext(Dispatchers.Default) {
                    events.filter { event: FirebaseEvent -> event.date.toLocalDate() == _uiState.value.selectedDate }
                }

                val bookings = if (!isTeacher) {
                    bookingRepository.getBookingsByStudent(userId).filter { booking: FirebaseBooking -> booking.status == "ACTIVE" }
                } else {
                    emptyList()
                }

                val filteredBookings = withContext(Dispatchers.Default) {
                    bookings.filter { booking: FirebaseBooking -> booking.bookingDate.toLocalDate() == _uiState.value.selectedDate }
                }

                // Fetch courses based on courseIds
                val courses = courseIds.mapNotNull { courseId ->
                    courseRepository.getCourseByCourseId(courseId as String) // Assuming courseId is a String
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    timetableEntries = filteredEntries,
                    events = events,
                    filteredEvents = filteredEvents,
                    bookings = bookings,
                    filteredBookings = filteredBookings,
                    courses = courses, // Include courses in the uiState
                    errorMessage = null
                )
                fetchNextTwoWeeksData() // Also fetch next two weeks data during initialization
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Unknown error occurred"
                )
            }
        }
        // Ensure next two weeks data is fetched upon initialization
        // fetchNextTwoWeeksData()
    }

    /**
     * Fetches timetable entries and bookings relevant to the currently selected date.
     * Used when in CalendarView or when initially loading.
     */

    internal fun fetchTimetableData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val courseIds = if (_isTeacher) {
                    courseRepository.queryByField("teacherId", _userId).map { it.courseId }
                } else {
                    studentRepository.getStudentWithCourses(_userId).second.map { it.courseId }
                }

                val selectedLocalDate = _uiState.value.selectedDate
                val dayOfWeek = selectedLocalDate.dayOfWeek.value

                val allEntries = timetableEntryRepository.query(emptyList())

                val filteredEntries: List<FirebaseTimetableEntry> = withContext(Dispatchers.Default) {
                    allEntries.filter { entry: FirebaseTimetableEntry ->
                        // Filter by day of week and relevant courses
                        val entryDayOfWeek = entry.dayOfWeek
                        val isCorrectDay = if (entryDayOfWeek == 7) dayOfWeek == 7 else entryDayOfWeek == dayOfWeek
                        val isRelevantCourse = if (_isTeacher) true else courseIds.contains(entry.courseId.toString())
                        isCorrectDay && isRelevantCourse
                    }.map { entry ->
                        // Create a new entry with the selected date but original time
                        val calendar = Calendar.getInstance().apply { time = entry.startTime }
                        val year = selectedLocalDate.year
                        val month = selectedLocalDate.monthValue - 1 // Calendar month is 0-indexed
                        val day = selectedLocalDate.dayOfMonth
                        calendar.set(year, month, day)
                        val startTime = calendar.time

                        calendar.time = entry.endTime
                        calendar.set(year, month, day)
                        val endTime = calendar.time

                        entry.copy(startTime = startTime, endTime = endTime)
                    }.sortedBy { it.startTime }
                }

                // Collect all unique course IDs from the filtered timetable entries
                val uniqueCourseIds = filteredEntries.map { it.courseId.toString() }.toSet().toList()

                // Fetch the corresponding courses
                val coursesForSelectedDate = courseRepository.getCoursesByIds(uniqueCourseIds)

                _uiState.update { currentState ->
                    currentState.copy(
                        timetableEntries = filteredEntries,
                        courses = coursesForSelectedDate, // Include the fetched courses
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

    internal fun fetchEventsForDate() {
        viewModelScope.launch {
            try {
                // Don't fetch events again, just filter the existing ones
                val filteredEvents = _uiState.value.events.filter { event: FirebaseEvent ->
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

    internal fun fetchNextTwoWeeksData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val courseIds = if (_isTeacher) {
                    courseRepository.queryByField("teacherId", _userId).map { it.courseId }
                } else {
                    studentRepository.getStudentWithCourses(_userId).second.map { it.courseId }
                }

                val today = LocalDate.now()
                val timetableEntries = timetableEntryRepository.query(emptyList())

                // Apply distinctBy before iterating through days to avoid processing duplicate base entries
                val distinctBaseEntries = timetableEntries.distinctBy {
                    "${it.courseId}_${it.startTime.time}_${it.endTime.time}"
                }

                Log.d("CalendarViewModel", "Fetched distinct base entries: ${distinctBaseEntries.size}")
                distinctBaseEntries.forEach { entry ->
                    Log.d("CalendarViewModel", "Distinct Base Entry: CourseId=${entry.courseId}, StartTime=${entry.startTime}, EndTime=${entry.endTime}, DayOfWeek=${entry.dayOfWeek}")
                }

                val entriesForNextTwoWeeks = mutableListOf<FirebaseTimetableEntry>()

                for (dayOffset in 0..13) {
                    val date = today.plusDays(dayOffset.toLong())
                    val dayOfWeek = date.dayOfWeek.value

                    val entriesForDay = distinctBaseEntries.filter { entry ->
                         // Filter by day of week and relevant courses
                         val entryDayOfWeek = entry.dayOfWeek
                         val isCorrectDay = if (entryDayOfWeek == 7) dayOfWeek == 7 else entryDayOfWeek == dayOfWeek
                         val isRelevantCourse = if (_isTeacher) true else courseIds.contains(entry.courseId.toString())
                         isCorrectDay && isRelevantCourse
                    }.map { entry ->
                        // Create a new entry with the current date in the loop
                        val calendar = Calendar.getInstance().apply { time = entry.startTime }
                        val year = date.year
                        val month = date.monthValue - 1 // Calendar month is 0-indexed
                        val day = date.dayOfMonth
                        calendar.set(year, month, day)
                        val startTime = calendar.time

                        calendar.time = entry.endTime
                        calendar.set(year, month, day)
                        val endTime = calendar.time

                        entry.copy(startTime = startTime, endTime = endTime, dayOfWeek = date.dayOfWeek.value)
                    }
                    // Apply distinctBy after collecting entries for all days to catch duplicates across days
                    entriesForNextTwoWeeks.addAll(entriesForDay)
                }

                val distinctEntries = entriesForNextTwoWeeks.distinctBy {
                    // Use courseId, dayOfWeek, and time components for a more robust distinct key
                    val calendar = Calendar.getInstance()
                    calendar.time = it.startTime
                    val startHour = calendar.get(Calendar.HOUR_OF_DAY)
                    val startMinute = calendar.get(Calendar.MINUTE)
                    calendar.time = it.endTime
                    val endHour = calendar.get(Calendar.HOUR_OF_DAY)
                    val endMinute = calendar.get(Calendar.MINUTE)
                    "${it.courseId}_${it.dayOfWeek}_${startHour}_${startMinute}_${endHour}_${endMinute}"
                }

                val sortedEntries = withContext(Dispatchers.Default) {
                     distinctEntries.sortedWith(compareBy<FirebaseTimetableEntry> { it.startTime })
                }

                val bookings = if (!_isTeacher) {
                    bookingRepository.getBookingsByStudent(_userId).filter { it.status == "ACTIVE" }
                } else {
                    emptyList()
                }

                // Collect all unique course IDs from the fetched timetable entries
                val uniqueCourseIds = sortedEntries.map { it.courseId.toString() }.toSet().toList()

                // Fetch the corresponding courses
                val coursesForNextTwoWeeks = courseRepository.getCoursesByIds(uniqueCourseIds)

                _uiState.update { currentState ->
                    currentState.copy(
                        timetableEntries = sortedEntries,
                        bookings = bookings,
                        courses = coursesForNextTwoWeeks, // Include the fetched courses
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

    fun addEvent(event: FirebaseEvent) {
        if (hasOverlappingEvents(event)) {
            _uiState.value = _uiState.value.copy(errorMessage = "This event overlaps with an existing event")
            return
        }

        viewModelScope.launch {
            try {
                val eventToInsert = if (_isTeacher) {
                    event.copy(teacherId = _userId, studentId = "", isTeacherEvent = true)
                } else {
                    event.copy(studentId = _userId, teacherId = null, isTeacherEvent = false)
                }
                eventRepository.create(eventToInsert)
                // Fetch all events again to update both lists
                val updatedEvents = eventRepository.queryByField("userId", _userId)
                _uiState.value = _uiState.value.copy(
                    events = updatedEvents,
                    filteredEvents = updatedEvents.filter { event: FirebaseEvent -> event.date.toLocalDate() == _uiState.value.selectedDate },
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

    fun updateEvent(event: FirebaseEvent) {
        viewModelScope.launch {
            try {
                // Use event ID to update the specific event
                eventRepository.update(event.eventId, event)

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

    private fun hasOverlappingEvents(newEvent: FirebaseEvent, excludeEventId: String = ""): Boolean {
        if (newEvent.isToDoList) return false // To-do items don't have times, so no overlap.
        // Event must have start/end times. Check nullability and assign to local variables for smart cast.
        val newEventStartTime = newEvent.startTime
        val newEventEndTime = newEvent.endTime
        if (newEventStartTime == null || newEventEndTime == null) return false

        val eventDate = newEvent.date.toLocalDate()

        return _uiState.value.events.any { existingEvent: FirebaseEvent ->
            if (existingEvent.eventId == excludeEventId) return@any false // Don't compare an event with itself.
            if (existingEvent.isToDoList) return@any false // Ignore to-do items for overlap checks.
            // Existing event must have times. Check nullability and assign to local variables for smart cast.
            val existingEventStartTime = existingEvent.startTime
            val existingEventEndTime = existingEvent.endTime
            if (existingEventStartTime == null || existingEventEndTime == null) return@any false

            val existingDate = existingEvent.date.toLocalDate()
            if (eventDate != existingDate) return@any false // Events must be on the same date to overlap.

            // Overlap condition: Not (New event ends before existing one starts OR New event starts after existing one ends)
            // Use local immutable variables for comparison
            !(newEventEndTime.before(existingEventStartTime) ||
              newEventStartTime.after(existingEventEndTime))
        }
    }

    // Deletes an event from the database and refreshes the event list in the UI state.
    fun deleteEvent(event: FirebaseEvent) {
        viewModelScope.launch {
            try {
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
                val events = eventRepository.queryByField("userId", _userId)
                _uiState.value = _uiState.value.copy(
                    events = events,
                    filteredEvents = events.filter { event: FirebaseEvent -> event.date.toLocalDate() == _uiState.value.selectedDate },
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as AUTApplication
                CalendarViewModel(
                    application.timetableEntryRepository,
                    application.studentRepository,
                    application.eventRepository,
                    application.bookingRepository,
                    application.courseRepository
                )
            }
        }
    }
}