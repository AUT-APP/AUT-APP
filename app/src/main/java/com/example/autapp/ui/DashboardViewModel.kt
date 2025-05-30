package com.example.autapp.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.firebase.*
import com.example.autapp.data.firebase.FirebaseStudentRepository
import com.example.autapp.data.firebase.FirebaseCourseRepository
import com.example.autapp.data.firebase.FirebaseGradeRepository
import com.example.autapp.data.firebase.FirebaseAssignmentRepository
import com.example.autapp.data.firebase.FirebaseTimetableRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel(
    private val studentRepository: FirebaseStudentRepository,
    private val courseRepository: FirebaseCourseRepository,
    private val gradeRepository: FirebaseGradeRepository,
    private val assignmentRepository: FirebaseAssignmentRepository,
    private val timetableEntryRepository: FirebaseTimetableRepository
) : ViewModel() {

    var studentId by mutableStateOf("")
    var assignments by mutableStateOf<List<FirebaseAssignment>>(emptyList())
    var grades by mutableStateOf<List<AssignmentGradeDisplay>>(emptyList())
    var courses by mutableStateOf<List<FirebaseCourse>>(emptyList())
    var studentGpa by mutableStateOf<Double?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var isRefreshing by mutableStateOf(false)
    private val _timetableEntries = MutableStateFlow<List<FirebaseTimetableEntry>>(emptyList())
    val timetableEntries: StateFlow<List<FirebaseTimetableEntry>> = _timetableEntries.asStateFlow()

    private val dateFormat = SimpleDateFormat("EEEE - dd MMMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun initialize(studentId: String) {
        this.studentId = studentId
        fetchDashboardData()
    }

    fun fetchDashboardData() {
        viewModelScope.launch {
            isRefreshing = true
            try {
                // Fetch student's enrolled course IDs via enrollments
                val enrollments = studentRepository.getEnrollmentsByStudent(studentId)
                val studentCourseIds = enrollments.map { it.courseId }

                Log.d("DashboardViewModel", "Fetching data for studentId: $studentId")

                // Fetch student's courses using the enrolled course IDs (which are Strings)
                val fetchedCourses = if (studentCourseIds.isNotEmpty()) {
                    courseRepository.getCoursesByIds(studentCourseIds)
                } else {
                    emptyList()
                }
                courses = fetchedCourses.sortedBy { it.name }
                Log.d("DashboardViewModel", "Fetched courses: ${fetchedCourses.size}")

                // Fetch all grades for the student
                val allGrades = gradeRepository.getGradesByStudent(studentId)
                Log.d("DashboardViewModel", "Fetched grades: ${allGrades.size}")
                allGrades.forEach { Log.d("DashboardViewModel", "Grade: ${it.gradeId}, AssignmentId: ${it.assignmentId}, Score: ${it.score}, StudentId: ${it.studentId}") }

                // Fetch all assignments
                val allAssignments = assignmentRepository.getAll()
                Log.d("DashboardViewModel", "Fetched assignments: ${allAssignments.size}")
                allAssignments.forEach { Log.d("DashboardViewModel", "Assignment: ${it.assignmentId}, Name: ${it.name}, CourseId: ${it.courseId}") }

                // Combine grades and corresponding assignments
                grades = allGrades.mapNotNull { firebaseGrade ->
                    val correspondingAssignment = allAssignments.find { firebaseAssignment ->
                        firebaseAssignment.assignmentId == firebaseGrade.assignmentId
                    }
                    correspondingAssignment?.let { firebaseAssignment ->
                        AssignmentGradeDisplay(
                            assignmentName = firebaseAssignment.name,
                            grade = firebaseGrade.grade,
                            score = firebaseGrade.score,
                            maxScore = firebaseAssignment.maxScore,
                            due = firebaseAssignment.due,
                            feedback = firebaseGrade.feedback,
                            courseId = firebaseAssignment.courseId
                        )
                    }
                }.sortedBy { it.due }
                Log.d("DashboardViewModel", "Combined grades for display: ${grades.size}")
                grades.forEach { Log.d("DashboardViewModel", "Display Grade: ${it.assignmentName}, Grade: ${it.grade}, Score: ${it.score}, CourseId: ${it.courseId}") }

                // Calculate GPA manually from fetched grades
                val numericScores = allGrades.map { it.score }
                studentGpa = if (numericScores.isNotEmpty()) numericScores.average() else 0.0

                // Fetch upcoming assignments for the student's courses
                val currentDate = Date()
                assignments = allAssignments
                    .filter { assignment ->
                        // Filter assignments for courses the student is enrolled in and are upcoming
                        studentCourseIds.contains(assignment.courseId) && assignment.due.after(currentDate)
                    }
                    .sortedBy { it.due }

                // Fetch timetable entries for the student's courses (convert String courseIds to Int for timetable repository)
                val fetchedTimetableEntries = if (studentCourseIds.isNotEmpty()) {
                    // Assuming getTimetableByCourse can handle a list of Int IDs or we need to call it for each ID
                    // Based on FirebaseTimetableRepository, getTimetableByCourse takes a single Int. Need to adjust.
                    studentCourseIds.flatMap { courseId ->
                        timetableEntryRepository.getTimetableByCourse(courseId)
                    }
                } else {
                    emptyList()
                }
                _timetableEntries.value = fetchedTimetableEntries.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })) // Sort by day and time
                Log.d("DashboardViewModel", "Fetched timetable entries: ${fetchedTimetableEntries.size}")

                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Error loading dashboard: ${e.message}"
                Log.e("DashboardViewModel", "Error loading dashboard:", e)
            } finally {
                isRefreshing = false
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