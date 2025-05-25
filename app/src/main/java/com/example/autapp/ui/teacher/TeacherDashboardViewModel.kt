package com.example.autapp.ui.teacher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.models.*
import com.example.autapp.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class TeacherDashboardViewModel(
    private val teacherRepository: TeacherRepository,
    private val courseRepository: CourseRepository,
    private val assignmentRepository: AssignmentRepository,
    private val gradeRepository: GradeRepository
) : ViewModel() {

    var teacherId by mutableStateOf(0)
    var teacher by mutableStateOf<Teacher?>(null)
    var courses by mutableStateOf<List<Course>>(emptyList())
    var assignments by mutableStateOf<List<Assignment>>(emptyList())
    var grades by mutableStateOf<List<Grade>>(emptyList())
    var errorMessage by mutableStateOf<String?>(null)
    var allStudents by mutableStateOf<List<Student>>(emptyList())

    private val _selectedCourse = MutableStateFlow<Course?>(null)
    val selectedCourse: StateFlow<Course?> = _selectedCourse.asStateFlow()

    private val _studentsInSelectedCourse = MutableStateFlow<List<Student>>(emptyList())
    val studentsInSelectedCourse: StateFlow<List<Student>> = _studentsInSelectedCourse.asStateFlow()

    // Holds the grades for the selected assignment
    private val _gradesForAssignment = MutableStateFlow<List<Grade>>(emptyList())
    val gradesForAssignment: StateFlow<List<Grade>> = _gradesForAssignment.asStateFlow()

    fun initialize(teacherId: Int) {
        this.teacherId = teacherId
        fetchTeacherData()
    }

    private fun fetchTeacherData() {
        viewModelScope.launch {
            try {
                // Fetch teacher data
                teacher = teacherRepository.getTeacherById(teacherId)

                // Fetch teacher's courses
                courses = courseRepository.getTeacherCourses(teacherId)

                // Fetch assignments for teacher's courses
                val allAssignments = assignmentRepository.getAllAssignments()
                assignments = allAssignments.filter { assignment ->
                    courses.any { course -> course.courseId == assignment.courseId }
                }

                // Fetch all grades and filter those related to the teacher's courses' assignments
                val allGrades = gradeRepository.getAllGrades()
                grades = allGrades.filter { grade ->
                    assignments.any { assignment -> assignment.assignmentId == grade.assignmentId }
                }

                // Fetch all students for all courses
                allStudents = courses.flatMap { course ->
                    teacherRepository.getStudentsForCourse(teacherId, course.courseId)
                }.distinctBy { it.studentId }

                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Error loading dashboard: ${e.message}"
            }
        }
    }

    fun selectCourse(course: Course) {
        _selectedCourse.value = course
    }

    // Modified to accept teacherId and the incomplete Course object from the dialog
    fun createCourse(teacherId: Int, course: Course) {
        viewModelScope.launch {
            try {
                // Create the complete Course object with the teacherId
                val courseWithTeacherId = Course(
                    courseId = course.courseId, // Should be 0 from the dialog, assuming auto-generate
                    name = course.name,
                    title = course.title,
                    description = course.description,
                    location = course.location,
                    teacherId = teacherId // Assign the correct teacherId
                )
                courseRepository.insertCourse(courseWithTeacherId)
                fetchTeacherData() // Refresh data after creating course
            } catch (e: Exception) {
                errorMessage = "Error creating course: ${e.message}"
            }
        }
    }

    fun createAssignment(assignment: Assignment) {
        viewModelScope.launch {
            try {
                assignmentRepository.insertAssignment(assignment)
                fetchTeacherData() // Refresh data after creating assignment
            } catch (e: Exception) {
                errorMessage = "Error creating assignment: ${e.message}"
            }
        }
    }

    fun addGrade(grade: Grade) {
        viewModelScope.launch {
            try {
                gradeRepository.insertGrade(grade)
                fetchTeacherData() // Refresh data after adding grade
            } catch (e: Exception) {
                errorMessage = "Error adding grade: ${e.message}"
            }
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch {
            try {
                courseRepository.updateCourse(course)
                fetchTeacherData() // Refresh data after updating course
            } catch (e: Exception) {
                errorMessage = "Error updating course: ${e.message}"
            }
        }
    }

    fun updateCourseDescription(course: Course) {
        viewModelScope.launch {
            try {
                courseRepository.updateCourse(course)
                fetchTeacherData() // Refresh data after updating course description
            } catch (e: Exception) {
                errorMessage = "Error updating course description: ${e.message}"
            }
        }
    }

    // Add dummy update functions to satisfy ViewModel dependencies if needed by the UI/other parts
    fun updateAssignment(assignment: Assignment) { /* TODO: Implement update logic */ }
    fun updateGrade(grade: Grade) { /* TODO: Implement update logic */ }

    fun loadStudentsForCourse(course: Course) {
        viewModelScope.launch {
            val students = teacherRepository.getStudentsForCourse(teacherId, course.courseId)
            _studentsInSelectedCourse.value = students
        }
    }

    fun loadGradesForAssignment(assignmentId: Int) {
        viewModelScope.launch {
            _gradesForAssignment.value = gradeRepository.getGradesByAssignment(assignmentId)
        }
    }

    fun saveGradesForAssignment(grades: List<Grade>) {
        viewModelScope.launch {
            grades.forEach { gradeRepository.insertGrade(it) }
            // Optionally refresh grades
            if (grades.isNotEmpty()) loadGradesForAssignment(grades.first().assignmentId)
        }
    }

    fun deleteAssignment(assignment: Assignment) {
        viewModelScope.launch {
            try {
                assignmentRepository.deleteAssignment(assignment)
                fetchTeacherData() // Refresh data after deleting assignment
            } catch (e: Exception) {
                errorMessage = "Error deleting assignment: ${e.message}"
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AUTApplication
                // Access repositories directly from the application object
                TeacherDashboardViewModel(
                    application.teacherRepository,
                    application.courseRepository,
                    application.assignmentRepository,
                    application.gradeRepository
                )
            }
        }
    }
} 