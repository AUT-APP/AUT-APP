package com.example.autapp.ui.booking
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.autapp.data.models.Booking
import com.example.autapp.data.models.BookingSlot
import com.example.autapp.data.models.SlotStatus
import com.example.autapp.data.models.StudySpace
import com.example.autapp.data.repository.BookingRepository
import com.example.autapp.data.repository.StudySpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*class BookingViewModel(
    private val bookingRepository: BookingRepository,
    private val studySpaceRepository: StudySpaceRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _availableSlots = MutableStateFlow<List<BookingSlot>>(emptyList())
    val availableSlots: StateFlow<List<BookingSlot>> = _availableSlots

    private val _myBookings = MutableStateFlow<List<Booking>>(emptyList())
    val myBookings: StateFlow<List<Booking>> = _myBookings

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _bookingSuccess = MutableStateFlow(false)
    val bookingSuccess: StateFlow<Boolean> = _bookingSuccess

    private val _availableDurations = MutableStateFlow<List<Int>>(listOf(30, 60, 90, 120))
    val availableDurations: StateFlow<List<Int>> = _availableDurations

    private val _campuses = MutableStateFlow<List<String>>(emptyList())
    val campuses: StateFlow<List<String>> = _campuses

    private val _buildings = MutableStateFlow<List<String>>(emptyList())
    val buildings: StateFlow<List<String>> = _buildings

    private val _studySpaces = MutableStateFlow<List<StudySpace>>(emptyList())
    val studySpaces: StateFlow<List<StudySpace>> = _studySpaces

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val selectedPurpose = mutableStateOf("")

    // Persisted filter states
    private val _selectedCampus = MutableStateFlow(savedStateHandle.get<String>("selectedCampus") ?: "")
    private val _selectedBuilding = MutableStateFlow(savedStateHandle.get<String>("selectedBuilding") ?: "")
    private val _selectedSpaceId = MutableStateFlow(savedStateHandle.get<String>("selectedSpaceId") ?: "All")
    private val _selectedDate = MutableStateFlow(
        savedStateHandle.get<Long>("selectedDate")?.let { Date(it) } ?: Date()
    )

    val selectedCampus: StateFlow<String> = _selectedCampus
    val selectedBuilding: StateFlow<String> = _selectedBuilding
    val selectedSpaceId: StateFlow<String> = _selectedSpaceId
    val selectedDate: StateFlow<Date> = _selectedDate

    init {
        // Initialize study spaces and fetch campuses
        initializeStudySpaces(forceClear = false) // Avoid clearing unless necessary
        fetchCampuses()
        // Validate restored state
        viewModelScope.launch {
            if (_selectedCampus.value.isNotEmpty() && _campuses.value.isNotEmpty() && _selectedCampus.value !in _campuses.value) {
                _selectedCampus.value = _campuses.value.firstOrNull() ?: ""
                savedStateHandle["selectedCampus"] = _selectedCampus.value
                Log.d("BookingViewModel", "Reset selectedCampus to ${_selectedCampus.value}")
            }
            if (_selectedBuilding.value.isNotEmpty() && _buildings.value.isNotEmpty() && _selectedBuilding.value !in _buildings.value) {
                _selectedBuilding.value = ""
                savedStateHandle["selectedBuilding"] = _selectedBuilding.value
                Log.d("BookingViewModel", "Reset selectedBuilding to empty")
            }
            if (_selectedSpaceId.value != "All" && _studySpaces.value.isNotEmpty() && _studySpaces.value.none { it.spaceId == _selectedSpaceId.value }) {
                _selectedSpaceId.value = "All"
                savedStateHandle["selectedSpaceId"] = _selectedSpaceId.value
                Log.d("BookingViewModel", "Reset selectedSpaceId to All")
            }
        }
    }

    fun updateFilters(campus: String, building: String, spaceId: String, date: Date) {
        _selectedCampus.value = campus
        _selectedBuilding.value = building
        _selectedSpaceId.value = spaceId
        _selectedDate.value = date
        savedStateHandle["selectedCampus"] = campus
        savedStateHandle["selectedBuilding"] = building
        savedStateHandle["selectedSpaceId"] = spaceId
        savedStateHandle["selectedDate"] = date.time
        Log.d("BookingViewModel", "Filters updated: campus=$campus, building=$building, spaceId=$spaceId, date=$date")
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
        Log.d("BookingViewModel", "Error message cleared")
    }

    fun clearBookingSuccess() {
        _bookingSuccess.value = false
        Log.d("BookingViewModel", "Booking success cleared")
    }

    private fun initializeStudySpaces(forceClear: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val existingSpaces = studySpaceRepository.getAllStudySpaces()
                Log.d("BookingViewModel", "Existing study spaces: ${existingSpaces.size}")
                existingSpaces.forEach { Log.d("BookingViewModel", "Space: $it") }

                if (forceClear) {
                    studySpaceRepository.deleteAll()
                    Log.d("BookingViewModel", "Cleared study_space_table")
                }

                if (forceClear || existingSpaces.isEmpty()) {
                    val studySpaces = listOf(
                        // City Campus
                        StudySpace("Room 1", "WA3 Study Rooms", "City", "Level 3", 10, true),
                        StudySpace("Room 2", "WA3 Study Rooms", "City", "Level 3", 10, true),
                        StudySpace("Room 3", "WA3 Study Rooms", "City", "Level 3", 10, true),
                        StudySpace("Room 4", "WA3 Study Rooms", "City", "Level 3", 10, true),
                        StudySpace("Room 5", "WA3 Study Rooms", "City", "Level 3", 10, true),
                        StudySpace("Room 6", "WA5 Study Rooms", "City", "Level 5", 10, true),
                        StudySpace("Room 7", "WA5 Study Rooms", "City", "Level 5", 10, true),
                        StudySpace("Room 8", "WA5 Study Rooms", "City", "Level 5", 10, true),
                        StudySpace("Room 9", "WA5 Study Rooms", "City", "Level 5", 10, true),
                        StudySpace("Room 11", "WA6 Study Rooms", "City", "Level 6", 10, true),
                        StudySpace("Room 12", "WA6 Study Rooms", "City", "Level 6", 10, true),
                        StudySpace("Room 13", "WA6 Study Rooms", "City", "Level 6", 10, true),
                        StudySpace("Room 14", "WA6 Study Rooms", "City", "Level 6", 10, true),
                        StudySpace("Room 409", "WG4 Study Rooms", "City", "Level 4", 10, true),
                        StudySpace("Room 410", "WG4 Study Rooms", "City", "Level 4", 10, true),
                        StudySpace("Room 411", "WG4 Study Rooms", "City", "Level 4", 10, true),
                        StudySpace("Room 412", "WG4 Study Rooms", "City", "Level 4", 10, true),
                        // North Campus
                        StudySpace("Room N1", "NB1 Study Rooms", "North", "Level 1", 8, true),
                        StudySpace("Room N2", "NB1 Study Rooms", "North", "Level 1", 8, true),
                        StudySpace("Room N3", "NB2 Study Rooms", "North", "Level 2", 12, true),
                        StudySpace("Room N4", "NB2 Study Rooms", "North", "Level 2", 12, true),
                        // South Campus
                        StudySpace("Room S1", "SB1 Study Rooms", "South", "Level 1", 6, true),
                        StudySpace("Room S2", "SB1 Study Rooms", "South", "Level 1", 6, true),
                        StudySpace("Room S3", "SB2 Study Rooms", "South", "Level 2", 10, true),
                        StudySpace("Room S4", "SB2 Study Rooms", "South", "Level 2", 10, true)
                    )
                    Log.d("BookingViewModel", "Attempting to insert ${studySpaces.size} study spaces")
                    studySpaces.forEach { space ->
                        try {
                            studySpaceRepository.insertStudySpace(space)
                            Log.d("BookingViewModel", "Inserted: $space")
                        } catch (e: Exception) {
                            Log.e("BookingViewModel", "Failed to insert $space: ${e.message}", e)
                        }
                    }
                    val insertedSpaces = studySpaceRepository.getAllStudySpaces()
                    Log.d("BookingViewModel", "After insertion, study spaces: ${insertedSpaces.size}")
                    insertedSpaces.forEach { Log.d("BookingViewModel", "Verified: $it") }
                    if (insertedSpaces.isEmpty()) {
                        _errorMessage.value = "Failed to insert study spaces: No spaces found after insertion"
                        Log.e("BookingViewModel", "No spaces found after insertion")
                    } else {
                        _errorMessage.value = null
                        Log.d("BookingViewModel", "Successfully initialized ${insertedSpaces.size} study spaces")
                    }
                } else {
                    Log.d("BookingViewModel", "Study spaces already exist, skipping insertion")
                    _errorMessage.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error initializing study spaces: ${e.message}"
                Log.e("BookingViewModel", "Error initializing study spaces", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun debugStudySpaces() {
        viewModelScope.launch {
            try {
                val spaces = studySpaceRepository.getAllStudySpaces()
                Log.d("BookingViewModel", "Debug: Current study spaces: ${spaces.size}")
                spaces.forEach { Log.d("BookingViewModel", "Space: $it") }
                val campuses = studySpaceRepository.getCampuses()
                Log.d("BookingViewModel", "Debug: Current campuses: $campuses")
                _errorMessage.value = "Debug: Found ${spaces.size} study spaces and ${campuses.size} campuses"
            } catch (e: Exception) {
                _errorMessage.value = "Error debugging study spaces: ${e.message}"
                Log.e("BookingViewModel", "Error debugging study spaces", e)
            }
        }
    }

    fun resetStudySpaces() {
        initializeStudySpaces(forceClear = true)
    }

    fun fetchCampuses() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val campusList = studySpaceRepository.getCampuses()
                _campuses.value = campusList
                Log.d("BookingViewModel", "Fetched campuses: $campusList")
                _errorMessage.value = if (campusList.isEmpty()) {
                    "No campuses found in database"
                } else {
                    null
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching campuses: ${e.message}"
                Log.e("BookingViewModel", "Error fetching campuses", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchBuildings(campus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val buildingList = studySpaceRepository.getBuildingsByCampus(campus)
                    .filter { it != "None" }
                _buildings.value = buildingList
                Log.d("BookingViewModel", "Fetched buildings for $campus: $buildingList")
                _errorMessage.value = if (buildingList.isEmpty()) {
                    "No buildings found for campus: $campus"
                } else {
                    null
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching buildings: ${e.message}"
                Log.e("BookingViewModel", "Error fetching buildings", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchStudySpaces(campus: String, building: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val spaces = studySpaceRepository.getStudySpacesByCampusAndBuilding(campus, building)
                _studySpaces.value = spaces
                Log.d("BookingViewModel", "Fetched study spaces for $campus/$building: ${spaces.size}")
                _errorMessage.value = if (spaces.isEmpty()) {
                    "No study spaces found for $campus/$building"
                } else {
                    null
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching study spaces: ${e.message}"
                Log.e("BookingViewModel", "Error fetching study spaces", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAvailableSlots(
        spaceId: String?,
        building: String,
        campus: String,
        level: String,
        date: Date,
        studentId: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                bookingRepository.deleteCompletedAndCancelledBookings()
                val spaces = if (spaceId != null) {
                    listOfNotNull(studySpaceRepository.getStudySpaceById(spaceId))
                } else {
                    studySpaceRepository.getStudySpacesByCampusAndBuilding(campus, building)
                }
                val slots = spaces.flatMap { space ->
                    generateTimeSlots(space.spaceId, space.building, space.campus, space.level, date, studentId)
                }
                _availableSlots.value = slots
                Log.d("BookingViewModel", "Fetched ${slots.size} slots for $campus/$building/${spaceId ?: "All"}")
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching slots: ${e.message}"
                Log.e("BookingViewModel", "Error fetching slots", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAvailableDurations(spaceId: String, date: Date, startHour: Int, startMinute: Int) {
        viewModelScope.launch {
            try {
                Log.d("BookingViewModel", "Fetching durations for spaceId=$spaceId, date=$date, time=$startHour:$startMinute")
                val calendar = Calendar.getInstance().apply { time = date }
                calendar.set(Calendar.HOUR_OF_DAY, startHour)
                calendar.set(Calendar.MINUTE, startMinute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.time
                Log.d("BookingViewModel", "Calculated startTime=$startTime")

                val now = Date()
                if (startTime.before(now)) {
                    _availableDurations.value = emptyList()
                    _errorMessage.value = "Cannot book past time slots"
                    Log.d("BookingViewModel", "Start time is in the past, no durations available")
                    return@launch
                }

                val endOfDay = Calendar.getInstance().apply { time = date }
                endOfDay.set(Calendar.HOUR_OF_DAY, 21)
                endOfDay.set(Calendar.MINUTE, 0)
                endOfDay.set(Calendar.SECOND, 0)
                endOfDay.set(Calendar.MILLISECOND, 0)
                val endOfDayTime = endOfDay.time
                Log.d("BookingViewModel", "End of day time=$endOfDayTime")

                val nextBooking = bookingRepository.getNextBooking(spaceId, startTime)
                val maxDurationMinutes = if (nextBooking != null) {
                    val minutesUntilNext = ((nextBooking.startTime.time - startTime.time) / (1000 * 60)).toInt()
                    Log.d("BookingViewModel", "Next booking found at ${nextBooking.startTime}, minutes until next=$minutesUntilNext")
                    minOf(minutesUntilNext, 120)
                } else {
                    val minutesUntilEndOfDay = ((endOfDayTime.time - startTime.time) / (1000 * 60)).toInt()
                    Log.d("BookingViewModel", "No next booking, minutes until end of day=$minutesUntilEndOfDay")
                    minOf(minutesUntilEndOfDay, 120)
                }

                val validMaxDuration = maxOf(0, minOf(maxDurationMinutes, 120))
                val durations = listOf(30, 60, 90, 120).filter { it <= validMaxDuration }
                _availableDurations.value = durations
                _errorMessage.value = if (durations.isEmpty()) {
                    "No durations available for this slot"
                } else {
                    null
                }
                Log.d("BookingViewModel", "Available durations=$durations, maxDuration=$validMaxDuration")
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching durations: ${e.message}"
                Log.e("BookingViewModel", "Error fetching durations", e)
            }
        }
    }

    fun createBooking(
        studentId: Int,
        spaceId: String,
        building: String,
        campus: String,
        level: String,
        bookingDate: Date,
        startTime: Date,
        endTime: Date
    ) {
        viewModelScope.launch {
            try {
                Log.d("BookingViewModel", "Creating booking: studentId=$studentId, spaceId=$spaceId, startTime=$startTime, endTime=$endTime")
                val booking = Booking(
                    bookingId = 0,
                    studentId = studentId,
                    roomId = spaceId,
                    building = building,
                    campus = campus,
                    level = level,
                    bookingDate = bookingDate,
                    startTime = startTime,
                    endTime = endTime,
                    status = "ACTIVE"
                )
                bookingRepository.insertBooking(booking)
                _errorMessage.value = null // Clear any previous errors on success
                _bookingSuccess.value = true // Set success state
                Log.d("BookingViewModel", "Booking inserted successfully")
                fetchMyBookings(studentId)
                fetchAvailableSlots(spaceId, building, campus, level, bookingDate, studentId)
            } catch (e: Exception) {
                // Provide more user-friendly error messages
                val userFriendlyMessage = when (e.message) {
                    "Cannot create more than 2 active bookings" -> "You already have 2 active bookings. Please cancel one to book another."
                    "Study space does not exist" -> "The selected study space is not available."
                    "Study space is not available" -> "The selected study space is currently unavailable."
                    "This time slot is already booked" -> "This time slot is already taken. Please choose another."
                    "Cannot book a time slot in the past" -> "You cannot book a time slot in the past."
                    "Booking cannot extend past 21:00" -> "Bookings cannot extend beyond 21:00."
                    "Booking insertion failed not found in database" -> "Failed to create booking. Please try again."
                    else -> "Error creating booking: ${e.message}"
                }
                _errorMessage.value = userFriendlyMessage
                Log.e("BookingViewModel", "Error creating booking: ${e.message}", e)
            }
        }
    }

    fun createBooking(
        studentId: Int,
        spaceId: String,
        building: String,
        campus: String,
        level: String,
        bookingDate: Date,
        startTime: Date,
        endTime: Date,
        purpose: String,
        notes: String? = null
    ) {
        viewModelScope.launch {
            try {
                Log.d("BookingViewModel", "Creating booking with purpose: studentId=$studentId, spaceId=$spaceId, startTime=$startTime, endTime=$endTime")
                val booking = Booking(
                    bookingId = 0,
                    studentId = studentId,
                    roomId = spaceId,
                    building = building,
                    campus = campus,
                    level = level,
                    bookingDate = bookingDate,
                    startTime = startTime,
                    endTime = endTime,
                    status = "ACTIVE"
                )
                bookingRepository.insertBooking(booking)
                _errorMessage.value = null // Clear any previous errors on success
                _bookingSuccess.value = true // Set success state
                Log.d("BookingViewModel", "Booking inserted successfully")
                fetchMyBookings(studentId)
                fetchAvailableSlots(spaceId, building, campus, level, bookingDate, studentId)
            } catch (e: Exception) {
                // Provide more user-friendly error messages
                val userFriendlyMessage = when (e.message) {
                    "Cannot create more than 2 active bookings" -> "You already have 2 active bookings. Please cancel one to book another."
                    "Study space does not exist" -> "The selected study space is not available."
                    "Study space is not available" -> "The selected study space is currently unavailable."
                    "This time slot is already booked" -> "This time slot is already taken. Please choose another."
                    "Cannot book a time slot in the past" -> "You cannot book a time slot in the past."
                    "Booking cannot extend past 21:00" -> "Bookings cannot extend beyond 21:00."
                    "Booking insertion failed not found in database" -> "Failed to create booking. Please try again."
                    else -> "Error creating booking: ${e.message}"
                }
                _errorMessage.value = userFriendlyMessage
                Log.e("BookingViewModel", "Error creating booking: ${e.message}", e)
            }
        }
    }

    fun fetchMyBookings(studentId: Int) {
        viewModelScope.launch {
            try {
                bookingRepository.deleteCompletedAndCancelledBookings()
                val bookings = bookingRepository.getBookingsByStudent(studentId)
                _myBookings.value = bookings.sortedBy { it.startTime }
                Log.d("BookingViewModel", "Fetched ${bookings.size} bookings for studentId=$studentId")
                bookings.forEach { Log.d("BookingViewModel", "Booking: $it") }
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching bookings: ${e.message}"
                Log.e("BookingViewModel", "Error fetching bookings", e)
            }
        }
    }

    fun cancelBooking(
        booking: Booking,
        spaceId: String,
        building: String,
        campus: String,
        level: String,
        date: Date
    ) {
        viewModelScope.launch {
            try {
                bookingRepository.deleteBooking(booking)
                _errorMessage.value = null
                fetchMyBookings(booking.studentId)
                fetchAvailableSlots(spaceId, building, campus, level, date, booking.studentId)
            } catch (e: Exception) {
                _errorMessage.value = "Error canceling booking: ${e.message}"
                Log.e("BookingViewModel", "Error canceling booking", e)
            }
        }
    }

    private suspend fun generateTimeSlots(
        spaceId: String,
        building: String,
        campus: String,
        level: String,
        date: Date,
        studentId: Int
    ): List<BookingSlot> {
        val slots = mutableListOf<BookingSlot>()
        val calendar = Calendar.getInstance().apply { time = date }
        calendar.set(Calendar.HOUR_OF_DAY, 8)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val timeSlots = mutableListOf<String>()
        var hour = 8
        var minute = 0
        while (hour < 21 || (hour == 21 && minute == 0)) {
            timeSlots.add(String.format("%02d:%02d", hour, minute))
            minute += 30
            if (minute >= 60) {
                hour += 1
                minute = 0
            }
        }

        val now = Date()
        timeSlots.forEach { timeSlot ->
            val (hour, minute) = timeSlot.split(":").map { it.toInt() }
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            val slotStart = calendar.time
            calendar.add(Calendar.MINUTE, 30)
            val slotEnd = calendar.time

            val isPast = slotStart.before(now)
            Log.d("BookingViewModel", "Slot $timeSlot: start=$slotStart, end=$slotEnd, isPast=$isPast")

            val conflicts = bookingRepository.checkBookingConflict(spaceId, slotStart, slotEnd)
            // Filter conflicts to only include bookings for the specified date
            val validConflicts = conflicts.filter { booking ->
                val bookingCalendar = Calendar.getInstance().apply { time = booking.bookingDate }
                val slotCalendar = Calendar.getInstance().apply { time = date }
                bookingCalendar.get(Calendar.YEAR) == slotCalendar.get(Calendar.YEAR) &&
                        bookingCalendar.get(Calendar.DAY_OF_YEAR) == slotCalendar.get(Calendar.DAY_OF_YEAR)
            }

            val status = when {
                isPast -> SlotStatus.PAST
                validConflicts.isEmpty() -> SlotStatus.AVAILABLE
                validConflicts.any { it.studentId == studentId && it.status == "ACTIVE" } -> SlotStatus.MY_BOOKING
                validConflicts.any { it.startTime <= now && it.endTime >= now && it.status == "ACTIVE" } -> SlotStatus.IN_USE
                else -> SlotStatus.BOOKED
            }

            slots.add(
                BookingSlot(
                    roomId = spaceId,
                    building = building,
                    campus = campus,
                    level = level,
                    timeSlot = timeSlot,
                    status = status
                )
            )
        }
        return slots
    }

    fun formatDateTime(date: Date): String {
        return SimpleDateFormat("dd MMM yyyy, HH:mm").format(date)
    }

}class BookingViewModelFactory(
    private val bookingRepository: BookingRepository,
    private val studySpaceRepository: StudySpaceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(BookingViewModel::class.java)) {
            val savedStateHandle = extras.createSavedStateHandle()
            @Suppress("UNCHECKED_CAST")
            return BookingViewModel(bookingRepository, studySpaceRepository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

