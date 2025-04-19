package com.example.autapp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autapp.data.dao.GradeDao
import com.example.autapp.data.models.Assignment
import com.example.autapp.data.models.Course
import com.example.autapp.data.repository.AssignmentRepository
import com.example.autapp.data.repository.CourseRepository
import com.example.autapp.data.repository.GradeRepository
import com.example.autapp.data.repository.StudentRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(
    private val studentRepository: StudentRepository,
    private val courseRepository: CourseRepository,
    private val gradeRepository: GradeRepository,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {

    var studentId by mutableStateOf(0)
    var assignments by mutableStateOf<List<Assignment>>(emptyList())
    var grades by mutableStateOf<List<GradeDao.GradeWithAssignment>>(emptyList())
    var courses by mutableStateOf<List<Course>>(emptyList())
    var studentGpa by mutableStateOf<Double?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    private val dateFormat = SimpleDateFormat("EEEE - dd MMMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun initialize(studentId: Int) {
        this.studentId = studentId
        fetchDashboardData()
    }

    private fun fetchDashboardData() {
        viewModelScope.launch {
            try {
                // Fetch student's courses
                val studentWithCourses = studentRepository.getStudentWithCourses(studentId)
                courses = studentWithCourses?.courses ?: emptyList()

                // Fetch grades with assignments
                grades = gradeRepository.getGradesWithAssignmentsSortedByDate(studentId)

                // Calculate GPA
                studentGpa = gradeRepository.calculateGPA(studentId)

                // Fetch upcoming assignments
                assignments = assignmentRepository.getAllAssignments()
                    .filter { it.due.after(Date()) }
                    .sortedBy { it.due }
                    .take(2) // Limit to 2 for UI

                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Error loading dashboard: ${e.message}"
            }
        }
    }

    fun formatDate(date: Date): String = dateFormat.format(date)
    fun formatTime(date: Date): String = timeFormat.format(date)
}