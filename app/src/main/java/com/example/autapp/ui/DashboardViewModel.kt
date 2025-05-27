package com.example.autapp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.dao.GradeDao
import com.example.autapp.data.models.Assignment
import com.example.autapp.data.models.Course
import com.example.autapp.data.repository.AssignmentRepository
import com.example.autapp.data.repository.CourseRepository
import com.example.autapp.data.repository.GradeRepository
import com.example.autapp.data.repository.StudentRepository
import com.example.autapp.ui.login.LoginViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.autapp.data.models.TimetableEntry
import com.example.autapp.data.repository.TimetableEntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel(
    private val studentRepository: StudentRepository,
    private val courseRepository: CourseRepository,
    private val gradeRepository: GradeRepository,
    private val assignmentRepository: AssignmentRepository,
    private val timetableEntryRepository: TimetableEntryRepository
) : ViewModel() {

    var studentId by mutableStateOf(0)
    var assignments by mutableStateOf<List<Assignment>>(emptyList())
    var grades by mutableStateOf<List<GradeDao.GradeWithAssignment>>(emptyList())
    var courses by mutableStateOf<List<Course>>(emptyList())
    var studentGpa by mutableStateOf<Double?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    private val _timetableEntries = MutableStateFlow<List<TimetableEntry>>(emptyList())
    val timetableEntries: StateFlow<List<TimetableEntry>> = _timetableEntries.asStateFlow()

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
                courses = studentWithCourses?.courses?.sortedBy { it.name } ?: emptyList()

                // Fetch all grades with assignments
                grades = gradeRepository.getGradesWithAssignmentsSortedByDate(studentId)

                // Calculate GPA
                studentGpa = gradeRepository.calculateGPA(studentId)

                // Fetch upcoming assignments
                assignments = assignmentRepository.getAllAssignments()
                    .filter { it.due.after(Date()) }
                    .sortedBy { it.due }

                // fetch timetable entries
                _timetableEntries.value = timetableEntryRepository.getAllTimetableEntries()

                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Error loading dashboard: ${e.message}"
            }
        }
    }

    fun formatDate(date: Date): String = dateFormat.format(date)
    fun formatTime(date: Date): String = timeFormat.format(date)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as AUTApplication

                DashboardViewModel(
                    application.studentRepository,
                    application.courseRepository,
                    application.gradeRepository,
                    application.assignmentRepository,
                    application.timetableEntryRepository
                )
            }
        }
    }
}