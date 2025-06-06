    package com.example.autapp.ui.booking

    import androidx.lifecycle.SavedStateHandle
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.ViewModelProvider
    import androidx.lifecycle.createSavedStateHandle
    import androidx.lifecycle.viewModelScope
    import androidx.lifecycle.viewmodel.initializer
    import androidx.lifecycle.viewmodel.viewModelFactory
    import com.example.autapp.AUTApplication
    import com.example.autapp.data.models.Booking
    import com.example.autapp.data.models.BookingSlot
    import com.example.autapp.data.models.SlotStatus
    import com.example.autapp.data.models.StudySpace
    import com.example.autapp.data.firebase.*
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.launch
    import java.util.*
    import java.util.logging.Logger
    import java.text.SimpleDateFormat

    class BookingViewModel(
        private val bookingRepository: FirebaseBookingRepository,
        private val studySpaceRepository: FirebaseStudySpaceRepository,
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

        private val _allLevels = MutableStateFlow<List<String>>(emptyList())
        val allLevels: StateFlow<List<String>> = _allLevels

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading

        private val _selectedCampus = MutableStateFlow(savedStateHandle.get<String>("selectedCampus") ?: "")
        private val _selectedBuilding = MutableStateFlow(savedStateHandle.get<String>("selectedBuilding") ?: "")
        private val _selectedSpaceId = MutableStateFlow(savedStateHandle.get<String>("selectedSpaceId") ?: "All")
        private val _selectedLevel = MutableStateFlow(savedStateHandle.get<String>("selectedLevel") ?: "All")
        private val _selectedDate = MutableStateFlow(
            savedStateHandle.get<Long>("selectedDate")?.let { Date(it) } ?: Date()
        )

        val selectedCampus: StateFlow<String> = _selectedCampus
        val selectedBuilding: StateFlow<String> = _selectedBuilding
        val selectedSpaceId: StateFlow<String> = _selectedSpaceId
        val selectedLevel: StateFlow<String> = _selectedLevel
        val selectedDate: StateFlow<Date> = _selectedDate

        // Caches for optimizing redundant queries
        private val levelsCache = mutableMapOf<String, List<String>>()
        private val spacesCache = mutableMapOf<String, List<StudySpace>>()

        init {
            initializeStudySpaces(forceClear = false)
            fetchCampuses()
            viewModelScope.launch {
                if (_selectedCampus.value.isNotEmpty() && _campuses.value.isNotEmpty() && _selectedCampus.value !in _campuses.value) {
                    _selectedCampus.value = _campuses.value.firstOrNull() ?: ""
                    _selectedLevel.value = "All"
                    savedStateHandle["selectedCampus"] = _selectedCampus.value
                    savedStateHandle["selectedLevel"] = _selectedLevel.value
                    Logger.getLogger("BookingViewModel").info("Reset selectedCampus to ${_selectedCampus.value}, selectedLevel to ${_selectedLevel.value}")
                }
                if (_selectedBuilding.value.isNotEmpty() && _buildings.value.isNotEmpty() && _selectedBuilding.value !in _buildings.value) {
                    _selectedBuilding.value = ""
                    _selectedLevel.value = "All"
                    savedStateHandle["selectedBuilding"] = _selectedBuilding.value
                    savedStateHandle["selectedLevel"] = _selectedLevel.value
                    Logger.getLogger("BookingViewModel").info("Reset selectedBuilding to empty, selectedLevel to ${_selectedLevel.value}")
                }
                if (_selectedSpaceId.value != "All" && _studySpaces.value.isNotEmpty() && _studySpaces.value.none { it.spaceId == _selectedSpaceId.value }) {
                    _selectedSpaceId.value = "All"
                    savedStateHandle["selectedSpaceId"] = _selectedSpaceId.value
                    Logger.getLogger("BookingViewModel").info("Reset selectedSpaceId to All")
                }
                if (_selectedLevel.value != "All" && _studySpaces.value.isNotEmpty() && _studySpaces.value.none { it.level == _selectedLevel.value }) {
                    _selectedLevel.value = "All"
                    savedStateHandle["selectedLevel"] = _selectedLevel.value
                    Logger.getLogger("BookingViewModel").info("Reset selectedLevel to All")
                }
            }
        }

        fun updateFilters(campus: String, building: String, spaceId: String, level: String, date: Date) {
            _selectedCampus.value = campus
            _selectedBuilding.value = building
            _selectedSpaceId.value = spaceId
            _selectedLevel.value = level
            _selectedDate.value = date
            savedStateHandle["selectedCampus"] = campus
            savedStateHandle["selectedBuilding"] = building
            savedStateHandle["selectedSpaceId"] = spaceId
            savedStateHandle["selectedLevel"] = level
            savedStateHandle["selectedDate"] = date.time
            Logger.getLogger("BookingViewModel").info("Filters updated: campus=$campus, building=$building, spaceId=$spaceId, level=$level, date=$date")
        }

        fun clearErrorMessage() {
            _errorMessage.value = null
            Logger.getLogger("BookingViewModel").info("Error message cleared")
        }

        fun clearBookingSuccess() {
            _bookingSuccess.value = false
            Logger.getLogger("BookingViewModel").info("Booking success cleared")
        }

        private fun initializeStudySpaces(forceClear: Boolean = false) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val existingSpaces = studySpaceRepository.getAll()
                    Logger.getLogger("BookingViewModel").info("Existing study spaces: ${existingSpaces.size}")

                    if (forceClear) {
                        existingSpaces.forEach { space ->
                            studySpaceRepository.delete(space.documentId)
                        }
                        Logger.getLogger("BookingViewModel").info("Cleared study spaces")
                    }

                    if (forceClear || existingSpaces.isEmpty()) {
                        val studySpaces = listOf(
                            FirebaseStudySpace("", "Room 1", "WA", "City", "Level 3", 10, true),
                            FirebaseStudySpace("", "Room 2", "WA", "City", "Level 3", 10, true),
                            FirebaseStudySpace("", "Room 3", "WA", "City", "Level 3", 10, true),
                            FirebaseStudySpace("", "Room 4", "WA", "City", "Level 3", 10, true),
                            FirebaseStudySpace("", "Room 5", "WA", "City", "Level 3", 10, true),
                            FirebaseStudySpace("", "Room 6", "WA", "City", "Level 5", 10, true),
                            FirebaseStudySpace("", "Room 7", "WA", "City", "Level 5", 10, true),
                            FirebaseStudySpace("", "Room 8", "WA", "City", "Level 5", 10, true),
                            FirebaseStudySpace("", "Room 9", "WA", "City", "Level 5", 10, true),
                            FirebaseStudySpace("", "Room 11", "WA", "City", "Level 6", 10, true),
                            FirebaseStudySpace("", "Room 12", "WA", "City", "Level 6", 10, true),
                            FirebaseStudySpace("", "Room 13", "WA", "City", "Level 6", 10, true),
                            FirebaseStudySpace("", "Room 14", "WA", "City", "Level 6", 10, true),
                            FirebaseStudySpace("", "Room 409", "WG", "City", "Level 4", 10, true),
                            FirebaseStudySpace("", "Room 410", "WG", "City", "Level 4", 10, true),
                            FirebaseStudySpace("", "Room 411", "WG", "City", "Level 4", 10, true),
                            FirebaseStudySpace("", "Room 412", "WG", "City", "Level 4", 10, true),
                            FirebaseStudySpace("", "Room N1", "NB1 Study Rooms", "North", "Level 1", 8, true),
                            FirebaseStudySpace("", "Room N2", "NB1 Study Rooms", "North", "Level 1", 8, true),
                            FirebaseStudySpace("", "Room N3", "NB2 Study Rooms", "North", "Level 2", 12, true),
                            FirebaseStudySpace("", "Room N4", "NB2 Study Rooms", "North", "Level 2", 12, true),
                            FirebaseStudySpace("", "Room S1", "SB1 Study Rooms", "South", "Level 1", 6, true),
                            FirebaseStudySpace("", "Room S2", "SB1 Study Rooms", "South", "Level 1", 6, true),
                            FirebaseStudySpace("", "Room S3", "SB2 Study Rooms", "South", "Level 2", 10, true),
                            FirebaseStudySpace("", "Room S4", "SB2 Study Rooms", "South", "Level 2", 10, true)
                        )
                        studySpaces.forEach { space ->
                            studySpaceRepository.create(space)
                        }
                        val insertedSpaces = studySpaceRepository.getAll()
                        if (insertedSpaces.isEmpty()) {
                            _errorMessage.value = "Failed to insert study spaces"
                        } else {
                            _errorMessage.value = null
                        }
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Error initializing study spaces: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun fetchCampuses() {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val spaces = studySpaceRepository.getAll()
                    val campusList = spaces.map { it.campus }.distinct()
                    _campuses.value = campusList
                    _errorMessage.value = if (campusList.isEmpty()) "No campuses found" else null
                } catch (e: Exception) {
                    _errorMessage.value = "Error fetching campuses: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun fetchBuildings(campus: String) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val spaces = studySpaceRepository.queryByField("campus", campus)
                    val buildingList = spaces.map { it.building }.distinct().filter { it != "None" }
                    _buildings.value = buildingList
                    _errorMessage.value = if (buildingList.isEmpty()) "No buildings found for $campus" else null
                } catch (e: Exception) {
                    _errorMessage.value = "Error fetching buildings: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun fetchStudySpaces(campus: String, building: String, level: String? = null) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val cacheKey = "$campus/$building${level?.let { "/$it" } ?: ""}"
                    val cachedSpaces = spacesCache[cacheKey]
                    if (cachedSpaces != null) {
                        _studySpaces.value = cachedSpaces
                        return@launch
                    }

                    val conditions = mutableListOf(
                        QueryCondition("campus", QueryOperator.EQUAL_TO, campus),
                        QueryCondition("building", QueryOperator.EQUAL_TO, building)
                    )
                    if (level != null && level != "All") {
                        conditions.add(QueryCondition("level", QueryOperator.EQUAL_TO, level))
                    }
                    val spaces = studySpaceRepository.query(conditions).map { it.toStudySpace() }
                    spacesCache[cacheKey] = spaces
                    _studySpaces.value = spaces
                    _errorMessage.value = if (spaces.isEmpty()) "No study spaces found" else null
                } catch (e: Exception) {
                    _errorMessage.value = "Error fetching study spaces: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun fetchAllLevels(campus: String, building: String) {
            viewModelScope.launch {
                try {
                    val cacheKey = "$campus/$building"
                    val cachedLevels = levelsCache[cacheKey]
                    if (cachedLevels != null) {
                        _allLevels.value = cachedLevels
                        return@launch
                    }

                    val conditions = listOf(
                        QueryCondition("campus", QueryOperator.EQUAL_TO, campus),
                        QueryCondition("building", QueryOperator.EQUAL_TO, building)
                    )
                    val spaces = studySpaceRepository.query(conditions)
                    val levels = spaces.map { it.level }.distinct()
                    levelsCache[cacheKey] = levels
                    _allLevels.value = levels
                } catch (e: Exception) {
                    _allLevels.value = emptyList()
                }
            }
        }

        fun fetchAvailableSlots(
            spaceId: String?,
            building: String,
            campus: String,
            level: String?,
            date: Date,
            studentId: String
        ) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    bookingRepository.deleteCompletedAndCancelledBookings()
                    bookingRepository.updateCompletedBookings() // Update status of past bookings

                    val studySpaces = if (spaceId != null && spaceId != "All") {
                        listOfNotNull(studySpaceRepository.getStudySpaceById(spaceId))
                    } else {
                        studySpaceRepository.getStudySpacesByCampusAndBuilding(campus, building)
                    }

                    val filteredStudySpaces = if (level != null && level != "All") {
                        studySpaces.filter { it.level == level }
                    } else {
                        studySpaces
                    }

                    val relevantSpaceDocumentIds = filteredStudySpaces.map { it.spaceId }
                    val existingBookings = bookingRepository.getBookingsByDate(date)
                        .filter { it.roomId in relevantSpaceDocumentIds }

                    Logger.getLogger("BookingViewModel").info("Found ${existingBookings.size} active bookings for date $date")
                    existingBookings.forEach {
                        Logger.getLogger("BookingViewModel").info("Booking: roomId=${it.roomId}, student=${it.studentId}, start=${it.startTime}, end=${it.endTime}, status=${it.status}")
                    }

                    val slots = mutableListOf<BookingSlot>()
                    val calendar = Calendar.getInstance().apply { time = date }
                    val now = Date()

                    filteredStudySpaces.forEach { studySpace ->
                        var hour = 8
                        var minute = 0
                        while (hour < 21 || (hour == 21 && minute == 0)) {
                            val timeSlotString = String.format(Locale.US, "%02d:%02d", hour, minute)

                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            val slotStart = calendar.time

                            calendar.add(Calendar.MINUTE, 30)
                            val slotEnd = calendar.time
                            calendar.add(Calendar.MINUTE, -30)

                            val isPast = slotStart.before(now) // Changed to check startTime

                            val conflicts = existingBookings.filter { booking ->
                                booking.roomId == studySpace.spaceId &&
                                        booking.status == "ACTIVE" &&
                                        (booking.startTime < slotEnd && booking.endTime > slotStart)
                            }

                            Logger.getLogger("BookingViewModel").info("Slot ${studySpace.spaceId} $timeSlotString: conflicts=${conflicts.size}, isPast=$isPast")

                            val status = when {
                                isPast -> SlotStatus.PAST
                                conflicts.any { it.studentId == studentId } -> SlotStatus.MY_BOOKING
                                conflicts.any { it.startTime <= now && it.endTime > now } -> SlotStatus.IN_USE
                                conflicts.isNotEmpty() -> SlotStatus.BOOKED
                                else -> SlotStatus.AVAILABLE
                            }

                            slots.add(
                                BookingSlot(
                                    roomId = studySpace.spaceId,
                                    building = studySpace.building,
                                    campus = studySpace.campus,
                                    level = studySpace.level,
                                    timeSlot = timeSlotString,
                                    status = status
                                )
                            )

                            minute += 30
                            if (minute >= 60) {
                                hour += 1
                                minute = 0
                            }
                        }
                    }

                    _availableSlots.value = slots.sortedWith(compareBy({ it.level }, { it.roomId }, { it.timeSlot }))
                    Logger.getLogger("BookingViewModel").info("Generated ${slots.size} slots for $campus/$building/${spaceId ?: "All"}/${level ?: "All"}")
                    Logger.getLogger("BookingViewModel").info("Slot status distribution: ${slots.groupingBy { it.status }.eachCount()}")
                } catch (e: Exception) {
                    _errorMessage.value = "Error fetching slots: ${e.message}"
                    Logger.getLogger("BookingViewModel").severe("Error fetching slots: ${e.message}")
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun fetchAvailableDurations(spaceId: String, date: Date, startHour: Int, startMinute: Int) {
            viewModelScope.launch {
                try {
                    val calendar = Calendar.getInstance().apply { time = date }
                    calendar.set(Calendar.HOUR_OF_DAY, startHour)
                    calendar.set(Calendar.MINUTE, startMinute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startTime = calendar.time

                    val endOfDay = Calendar.getInstance().apply { time = date }
                    endOfDay.set(Calendar.HOUR_OF_DAY, 21)
                    endOfDay.set(Calendar.MINUTE, 0)
                    val endOfDayTime = endOfDay.time

                    val studySpace = studySpaceRepository.getStudySpacesByName(spaceId).firstOrNull()
                    val studySpaceDocumentId = studySpace?.spaceId ?: return@launch

                    val nextBooking = bookingRepository.getNextBooking(studySpaceDocumentId, startTime)
                    val maxDurationMinutes = if (nextBooking != null) {
                        ((nextBooking.startTime.time - startTime.time) / (1000 * 60)).toInt().coerceAtMost(120)
                    } else {
                        ((endOfDayTime.time - startTime.time) / (1000 * 60)).toInt().coerceAtMost(120)
                    }

                    val durations = listOf(30, 60, 90, 120).filter { it <= maxDurationMinutes }
                    _availableDurations.value = durations
                    _errorMessage.value = if (durations.isEmpty()) "No durations available" else null
                } catch (e: Exception) {
                    _errorMessage.value = "Error fetching durations: ${e.message}"
                }
            }
        }

        fun createBooking(
            spaceId: String,
            level: String,
            date: String,
            timeSlot: String,
            studentId: String,
            durationMinutes: Int,
            campus: String,
            building: String,
            onSuccess: () -> Unit,
            onFailure: (String) -> Unit
        ) {
            viewModelScope.launch {
                try {
                    // Check active bookings limit
                    val activeBookingsCount = bookingRepository.getActiveBookingsCountForStudent(studentId)
                    Logger.getLogger("BookingViewModel").info("Student $studentId has $activeBookingsCount active bookings")
                    if (activeBookingsCount >= 2) {
                        val message = "You cannot have more than 2 active bookings"
                        _errorMessage.value = message
                        onFailure(message)
                        return@launch
                    }

                    val studySpace = studySpaceRepository.getStudySpacesByName(spaceId).firstOrNull()
                    val studySpaceDocumentId = studySpace?.spaceId ?: run {
                        onFailure("Study space not found")
                        return@launch
                    }

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val bookingDateParsed = dateFormat.parse(date) ?: Date()

                    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
                    val startTimeParsedTimeOnly = timeFormat.parse(timeSlot) ?: Date()

                    val calendar = Calendar.getInstance().apply { time = bookingDateParsed }
                    val timeCalendar = Calendar.getInstance().apply { time = startTimeParsedTimeOnly }
                    calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                    calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val finalStartTime = calendar.time
                    val endTimeParsed = Date(finalStartTime.time + durationMinutes * 60 * 1000)

                    // Check if slot is in the past
                    val now = Date()
                    if (finalStartTime.before(now)) {
                        onFailure("Cannot book past time slots")
                        return@launch
                    }

                    // Check for conflicts before creating
                    val isAvailable = bookingRepository.checkRoomAvailability(studySpaceDocumentId, bookingDateParsed, finalStartTime, endTimeParsed)
                    if (!isAvailable) {
                        // Debug active bookings
                        val activeBookings = bookingRepository.getActiveBookingsForRoomOnDate(studySpaceDocumentId, bookingDateParsed)
                        Logger.getLogger("BookingViewModel").info("Active bookings for $studySpaceDocumentId on $bookingDateParsed: ${activeBookings.size}")
                        onFailure("This time slot is already booked")
                        return@launch
                    }

                    val booking = Booking(
                        bookingId = 0,
                        studentId = studentId,
                        roomId = studySpaceDocumentId,
                        building = building,
                        campus = campus,
                        level = level,
                        bookingDate = bookingDateParsed,
                        startTime = finalStartTime,
                        endTime = endTimeParsed,
                        status = "ACTIVE"
                    )
                    bookingRepository.create(bookingRepository.fromBooking(booking))
                    fetchAvailableSlots(studySpaceDocumentId, building, campus, level, bookingDateParsed, studentId)
                    fetchMyBookings(studentId)
                    _bookingSuccess.value = true
                    onSuccess()
                } catch (e: Exception) {
                    val userFriendlyMessage = when (e.message) {
                        "Booking duration must be at least 30 minutes" -> "Booking duration must be at least 30 minutes"
                        "Booking duration cannot exceed 2 hours" -> "Booking duration cannot exceed 2 hours"
                        "Start time must be before end time" -> "Invalid time selection"
                        "Booking date must match start time date" -> "Invalid date selection"
                        "Booking cannot be more than 30 days in the future" -> "Cannot book more than 30 days in advance"
                        else -> "Error creating booking: ${e.message}"
                    }
                    _errorMessage.value = userFriendlyMessage
                    onFailure(userFriendlyMessage)
                }
            }
        }

        fun fetchMyBookings(studentId: String) {
            viewModelScope.launch {
                try {
                    bookingRepository.deleteCompletedAndCancelledBookings()
                    bookingRepository.updateCompletedBookings() // Update status of past bookings
                    val bookings = bookingRepository.getBookingsByStudent(studentId)
                    val myBookingsWithSpaceNames = bookings
                        .filter { booking ->
                            // Exclude completed bookings: those with status CANCELED, COMPLETED, or endTime before now
                            !(booking.status == "CANCELED" ||
                                    booking.status == "COMPLETED" ||
                                    booking.endTime.before(Date()))
                        }
                        .mapNotNull { firebaseBooking ->
                            val studySpace = studySpaceRepository.getStudySpaceById(firebaseBooking.roomId)
                            studySpace?.let {
                                firebaseBooking.toBooking().copy(
                                    roomId = it.spaceId,
                                    documentId = firebaseBooking.id
                                )
                            }
                        }
                    _myBookings.value = myBookingsWithSpaceNames
                    _errorMessage.value = null
                } catch (e: Exception) {
                    _errorMessage.value = "Error fetching bookings: ${e.message}"
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
                    val bookingDocumentId = booking.documentId ?: run {
                        _errorMessage.value = "Booking ID not found"
                        return@launch
                    }
                    bookingRepository.delete(bookingDocumentId)
                    fetchMyBookings(booking.studentId)
                    fetchAvailableSlots(spaceId, building, campus, level, date, booking.studentId)
                } catch (e: Exception) {
                    _errorMessage.value = "Error canceling booking: ${e.message}"
                }
            }
        }

        companion object {
            val Factory: ViewModelProvider.Factory = viewModelFactory {
                initializer {
                    val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AUTApplication
                    val savedStateHandle = createSavedStateHandle()
                    BookingViewModel(
                        application.bookingRepository,
                        application.studySpaceRepository,
                        savedStateHandle
                    )
                }
            }
        }
    }