package com.example.autapp.ui.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autapp.data.dao.TimetableEntryDao
import com.example.autapp.data.models.Event
import com.example.autapp.data.repository.TimetableEntryRepository
import com.example.autapp.data.repository.StudentRepository
import com.example.autapp.data.repository.EventRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import android.util.Log

class CalendarViewModel(
    private val timetableEntryRepository: TimetableEntryRepository,
    private val studentRepository: StudentRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    var selectedDate: LocalDate by mutableStateOf(LocalDate.now())
        private set
    var timetableEntries by mutableStateOf<List<TimetableEntryDao.TimetableEntryWithCourse>>(emptyList())
        private set
    var events by mutableStateOf<List<Event>>(emptyList())
        private set
    var isCalendarView by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var studentId by mutableStateOf<Int?>(null)
        private set

    fun initialize(studentId: Int) {
        this.studentId = studentId
        if (isCalendarView) {
            fetchTimetableData()
            fetchEventsForDate()
        } else {
            fetchNextTwoWeeksData()
        }
    }

    private fun fetchTimetableData() {
        viewModelScope.launch {
            isLoading = true
            try {
                val studentWithCourses = studentId?.let { studentRepository.getStudentWithCourses(it) }
                val courseIds = studentWithCourses?.courses?.map { it.courseId } ?: emptyList()
                
                val dayOfWeek = selectedDate.dayOfWeek.value
                val allEntries = timetableEntryRepository.getTimetableEntriesWithCourseByDay(dayOfWeek)
                
                timetableEntries = allEntries
                    .filter { entry -> courseIds.contains(entry.entry.courseId) }
                    .sortedBy { it.entry.startTime }
                
                errorMessage = null
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error fetching timetable: ${e.message}", e)
                errorMessage = "Error loading timetable: ${e.message}"
                timetableEntries = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    private fun fetchEventsForDate() {
        viewModelScope.launch {
            try {
                studentId?.let { id ->
                    val date = Date.from(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    events = eventRepository.getEventsByDate(id, date)
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error fetching events: ${e.message}", e)
                errorMessage = "Error loading events: ${e.message}"
                events = emptyList()
            }
        }
    }

    private fun fetchNextTwoWeeksData() {
        viewModelScope.launch {
            isLoading = true
            try {
                val studentWithCourses = studentId?.let { studentRepository.getStudentWithCourses(it) }
                val courseIds = studentWithCourses?.courses?.map { it.courseId } ?: emptyList()
                
                val today = LocalDate.now()
                val allEntries = mutableListOf<Pair<LocalDate, TimetableEntryDao.TimetableEntryWithCourse>>()
                
                // Get entries for each day in the next two weeks
                for (dayOffset in 0..13) {
                    val date = today.plusDays(dayOffset.toLong())
                    val dayOfWeek = date.dayOfWeek.value // 1-7, Monday = 1
                    
                    // Get entries for this day and filter by student's courses
                    val entriesForDay = timetableEntryRepository.getTimetableEntriesWithCourseByDay(dayOfWeek)
                        .filter { entry -> courseIds.contains(entry.entry.courseId) }
                        .distinctBy { entry -> 
                            "${entry.entry.courseId}_${entry.entry.startTime.time}_${entry.entry.endTime.time}"
                        }
                        .sortedBy { it.entry.startTime }
                    
                    // Add each entry with its actual date
                    entriesForDay.forEach { entry ->
                        allEntries.add(date to entry)
                    }
                }

                // Sort by date and then by start time
                timetableEntries = allEntries
                    .sortedWith(compareBy<Pair<LocalDate, TimetableEntryDao.TimetableEntryWithCourse>> 
                        { it.first }
                        .thenBy { it.second.entry.startTime })
                    .map { it.second }
                
                errorMessage = null
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error fetching timetable data: ${e.message}", e)
                errorMessage = "Error loading timetable: ${e.message}"
                timetableEntries = emptyList()
            } finally {
                isLoading = false
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
        selectedDate = date
        if (isCalendarView) {
            fetchTimetableData()
            fetchEventsForDate()
        }
    }

    fun toggleView() {
        isCalendarView = !isCalendarView
        if (!isCalendarView) {
            fetchNextTwoWeeksData()
        } else {
            fetchTimetableData()
            fetchEventsForDate()
        }
    }

    fun addEvent(event: Event) {
        viewModelScope.launch {
            try {
                eventRepository.insertEvent(event)
                if (isCalendarView) {
                    fetchEventsForDate()
                } else {
                    fetchNextTwoWeeksData()
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error adding event: ${e.message}", e)
                errorMessage = "Error adding event: ${e.message}"
            }
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            try {
                eventRepository.updateEvent(event)
                if (isCalendarView) {
                    fetchEventsForDate()
                } else {
                    fetchNextTwoWeeksData()
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error updating event: ${e.message}", e)
                errorMessage = "Error updating event: ${e.message}"
            }
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            try {
                eventRepository.deleteEvent(event)
                if (isCalendarView) {
                    fetchEventsForDate()
                } else {
                    fetchNextTwoWeeksData()
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error deleting event: ${e.message}", e)
                errorMessage = "Error deleting event: ${e.message}"
            }
        }
    }
}