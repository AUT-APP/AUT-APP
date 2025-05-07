package com.example.autapp.ui.notification

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.net.Uri
import com.example.autapp.data.repository.NotificationRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autapp.data.models.Course
import com.example.autapp.data.models.Notification
import com.example.autapp.data.models.TimetableEntry
import com.example.autapp.data.models.TimetableNotificationPreference
import com.example.autapp.data.repository.CourseRepository
import com.example.autapp.data.repository.StudentRepository
import com.example.autapp.data.repository.TimetableEntryRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.app.AlarmManager

class NotificationViewModel(
    private val studentRepository: StudentRepository,
    private val timetableEntryRepository: TimetableEntryRepository,
    private val courseRepository: CourseRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    var studentId by mutableStateOf(0)
    var courses by mutableStateOf<List<Course>>(emptyList())
    var notifications by mutableStateOf<List<Notification>>(emptyList())
    var notificationPrefs by mutableStateOf<Map<Int, Int>>(emptyMap()) // classSessionId -> minutesBefore
    var errorMessage by mutableStateOf<String?>(null)
    var timetableEntries by mutableStateOf<List<TimetableEntry>>(emptyList())

    fun initialize(studentId: Int) {
        this.studentId = studentId
        Log.d("NotificationViewModel", "Initializing ViewModel for student ID: $studentId")
        fetchNotificationData()
    }

    fun getTimetableForCourse(courseId: Int) {
       // You’ll probably want to cache this or load asynchronously in a real app
        viewModelScope.launch {
            try {
                timetableEntries = courseRepository.getTimetableForCourse(courseId)
            } catch (e: Exception) {
                errorMessage = "Error loading notification screen: ${e.message}"
            }
        }
    }

    fun setNotificationPreference(
        context: Context,
        studentId: Int,
        classSessionId: Int,
        minutesBefore: Int
    ) {
        viewModelScope.launch {
            try {
                val pref = TimetableNotificationPreference(
                    studentId = studentId,
                    classSessionId = classSessionId,
                    minutesBefore = minutesBefore,
                    enabled = true
                )

                // Save to DB
                notificationRepository.saveTimetableNotificationPreference(pref)

                // Update local state
                notificationPrefs = notificationPrefs.toMutableMap().apply {
                    put(classSessionId, minutesBefore)
                }

                // Schedule notification
                val session = timetableEntryRepository.getTimetableEntryById(classSessionId)

                if (session != null) {
                    notificationRepository.scheduleNotificationForSession(context, pref, session)
                } else {
                    errorMessage = "Class session not found."
                }
            } catch (e: Exception) {
                errorMessage = "Failed to set notification preference: ${e.message}"
            }
        }
    }

    fun deleteNotificationPreference(studentId: Int, classSessionId: Int) {
        viewModelScope.launch {
            try {
                notificationRepository.deleteTimetableNotificationPreference(studentId, classSessionId)
                notificationPrefs = notificationPrefs.toMutableMap().apply {
                    remove(classSessionId)
                }
            } catch (e: Exception) {
                errorMessage = "Failed to delete notification preference: ${e.message}"
            }
        }
    }



    private fun fetchNotificationData() {
        viewModelScope.launch {
            try {
                Log.d("NotificationViewModel", "Starting fetchNotificationData")
                val currentYear = LocalDate.now().year
                val currentSemester = if (LocalDate.now().monthValue in 1..6) 1 else 2
                Log.d("NotificationViewModel", "Current year: $currentYear, semester: $currentSemester")

                val courseWithEnrollmentInfo = studentRepository.getStudentCoursesWithEnrollmentInfo(studentId)
                Log.d("NotificationViewModel", "Fetched courses with enrollment: ${courseWithEnrollmentInfo.size}")

                val filteredCourses = courseWithEnrollmentInfo
                    .filter { it.year == currentYear && it.semester == currentSemester }
                    .map { it.course }
                courses = filteredCourses
                Log.d("NotificationViewModel", "Filtered courses: ${courses.map { it.name }}")

                // Preload all timetable entries for the relevant courses
                val allTimetableEntries = mutableListOf<TimetableEntry>()
                for (course in filteredCourses) {
                    Log.d("NotificationViewModel", "Fetching timetable for course ${course.name}")
                    val entries = courseRepository.getTimetableForCourse(course.courseId)
                    Log.d("NotificationViewModel", "Entries for course ${course.name}: ${entries.map { it.courseId }}")
                    allTimetableEntries += entries
                }
                timetableEntries = allTimetableEntries
                Log.d("NotificationViewModel", "Total timetable entries: ${timetableEntries.size}, Entries: ${timetableEntries.map { it.courseId }}")

                // Load notifications and preferences
                notifications = notificationRepository.getAllNotifications()
                Log.d("NotificationViewModel", "Fetched notifications: ${notifications.size}")
                //notificationPrefs = notificationRepository.getNotificationPreferences(studentId)
                Log.d("NotificationViewModel", "Fetched notification preferences")

                errorMessage = null
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Error in fetchNotificationData: ${e.message}", e)
                errorMessage = "Error loading notification screen: ${e.message}"
            }
        }
    }
}