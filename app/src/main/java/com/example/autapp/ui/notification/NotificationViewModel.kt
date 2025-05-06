package com.example.autapp.ui.notification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.autapp.data.repository.NotificationRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autapp.data.models.Course
import com.example.autapp.data.models.Notification
import com.example.autapp.data.repository.StudentRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

class NotificationViewModel(
    private val studentRepository: StudentRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    var studentId by mutableStateOf(0)
    var courses by mutableStateOf<List<Course>>(emptyList())
    var notifications by mutableStateOf<List<Notification>>(emptyList())
    var errorMessage by mutableStateOf<String?>(null)

    fun initialize(studentId: Int) {
        this.studentId = studentId
        fetchNotificationData()
    }

    private fun fetchNotificationData() {
        viewModelScope.launch {
            try {
                // Fetch student's courses

                // TODO: Update api to 26 to use LocalDate.now()
                //val currentYear = LocalDate.now().year
                //val currentSemester = if (LocalDate.now().monthValue in 1..6) 1 else 2
                val currentYear = 2025
                val currentSemester = 1

                val courseWithEnrollmentInfo = studentRepository.getStudentCoursesWithEnrollmentInfo(studentId)

                // Filter courses based on current year and semester
                courses = courseWithEnrollmentInfo
                    .filter { it.year == currentYear && it.semester == currentSemester }
                    .map { it.course }

                // Fetch notifications
                notifications = notificationRepository.getAllNotifications()
            } catch (e: Exception) {
                errorMessage = "Error loading notification screen: ${e.message}"
            }
        }
        }
}