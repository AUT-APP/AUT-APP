package com.example.autapp.ui.teacher

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.firebase.*
import com.example.autapp.data.repository.CourseMaterialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TeacherDashboardViewModel(
    private val teacherRepository: FirebaseTeacherRepository,
    private val courseRepository: FirebaseCourseRepository,
    private val assignmentRepository: FirebaseAssignmentRepository,
    private val gradeRepository: FirebaseGradeRepository,
    private val courseMaterialRepository: CourseMaterialRepository,
    private val studentRepository: FirebaseStudentRepository
) : ViewModel() {

    var teacherId by mutableStateOf("")
    var teacher by mutableStateOf<FirebaseTeacher?>(null)
    var courses by mutableStateOf<List<FirebaseCourse>>(emptyList())
    var assignments by mutableStateOf<List<FirebaseAssignment>>(emptyList())
    var grades by mutableStateOf<List<FirebaseGrade>>(emptyList())
    var errorMessage by mutableStateOf<String?>(null)
    var allStudents by mutableStateOf<List<FirebaseStudent>>(emptyList())

    private val _selectedCourse = MutableStateFlow<FirebaseCourse?>(null)
    val selectedCourse: StateFlow<FirebaseCourse?> = _selectedCourse.asStateFlow()

    private val _studentsInSelectedCourse = MutableStateFlow<List<FirebaseStudent>>(emptyList())
    val studentsInSelectedCourse: StateFlow<List<FirebaseStudent>> = _studentsInSelectedCourse.asStateFlow()

    // Holds the grades for the selected assignment
    private val _gradesForAssignment = MutableStateFlow<List<FirebaseGrade>>(emptyList())
    val gradesForAssignment: StateFlow<List<FirebaseGrade>> = _gradesForAssignment.asStateFlow()

    private val studentCourseCollection = FirebaseFirestore.getInstance().collection("studentCourses")

    fun initialize(teacherId: String) {
        this.teacherId = teacherId
        fetchTeacherData()
    }

    private fun fetchTeacherData() {
        viewModelScope.launch {
            Log.d(TAG, "Fetching teacher data...")
            try {
                // Fetch teacher data
                Log.d(TAG, "Attempting to fetch teacher with ID: $teacherId")
                val fetchedTeacher = teacherRepository.getById(teacherId)
                teacher = fetchedTeacher
                Log.d(TAG, "Fetched teacher: $teacher")

                // Fetch teacher's courses
                Log.d(TAG, "Attempting to fetch courses for teacher ID: $teacherId")
                val fetchedCourses = courseRepository.getCoursesByTeacher(teacherId)
                courses = fetchedCourses
                Log.d(TAG, "Fetched courses: ${courses.size}")
                courses.forEach { Log.d(TAG, "Fetched course: ${it.courseId}, Name: ${it.name}") }

                // Fetch assignments for teacher's courses
                Log.d(TAG, "Attempting to fetch all assignments")
                val allAssignments = assignmentRepository.getAll()
                assignments = allAssignments.filter { assignment ->
                    courses.any { course -> course.courseId == assignment.courseId }
                }
                Log.d(TAG, "Fetched assignments: ${assignments.size}")
                assignments.forEach { Log.d(TAG, "Fetched assignment: ${it.assignmentId}, Name: ${it.name}, CourseId: ${it.courseId}") }

                // Fetch all grades and filter those related to the teacher's courses' assignments
                val allGrades = gradeRepository.getAll()
                grades = allGrades.filter { grade ->
                    assignments.any { assignment -> assignment.assignmentId == grade.assignmentId }
                }
                Log.d(TAG, "Fetched grades: ${grades.size}")
                grades.forEach { Log.d(TAG, "Fetched grade: ${it.gradeId}, AssignmentId: ${it.assignmentId}, StudentId: ${it.studentId}") }

                // Fetch all students for all courses taught by this teacher
                Log.d(TAG, "Attempting to fetch all students for teacher: $teacherId")
                val studentIdsForTeacherCourses = mutableSetOf<String>()
                for (course in fetchedCourses) {
                    val enrollments = studentCourseCollection.whereEqualTo("courseId", course.courseId).get().await()
                    enrollments.documents.forEach { document ->
                        document.data?.get("studentId")?.let { studentId ->
                            studentIdsForTeacherCourses.add(studentId as String)
                        }
                    }
                }
                // Fetch student details for the collected IDs
                val fetchedStudents = studentIdsForTeacherCourses.mapNotNull { studentId ->
                    studentRepository.getById(studentId)
                }
                allStudents = fetchedStudents
                Log.d(TAG, "Fetched ${allStudents.size} students for teacher $teacherId")

                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Error loading dashboard: ${e.message}"
                Log.e(TAG, "Error loading dashboard: ${e.message}", e)
            }
        }
    }

    fun selectCourse(course: FirebaseCourse) {
        _selectedCourse.value = course
        loadStudentsForCourse(course)
    }

    // Modified to accept teacherId and the incomplete FirebaseCourse object from the dialog
    fun createCourse(teacherId: String, course: FirebaseCourse) {
        viewModelScope.launch {
            try {
                // Create the complete FirebaseCourse object
                val courseToCreate = course.copy(teacherId = teacherId)
                courseRepository.create(courseToCreate)
                fetchTeacherData() // Refresh data after creating course
            } catch (e: Exception) {
                errorMessage = "Error creating course: ${e.message}"
                Log.e(TAG, "Error creating course: ${e.message}", e)
            }
        }
    }

    fun createAssignment(assignment: FirebaseAssignment) {
        viewModelScope.launch {
            try {
                assignmentRepository.create(assignment)
                fetchTeacherData() // Refresh data after creating assignment
            } catch (e: Exception) {
                errorMessage = "Error creating assignment: ${e.message}"
                Log.e(TAG, "Error creating assignment: ${e.message}", e)
            }
        }
    }

    fun addGrade(grade: FirebaseGrade) {
        viewModelScope.launch {
            try {
                // Check if grade already exists
                val existingGrade = gradeRepository.getStudentGradeForAssignment(grade.studentId, grade.assignmentId)
                if (existingGrade != null) {
                    // Update existing grade
                    gradeRepository.update(grade.gradeId, grade)
                } else {
                    // Create new grade
                    gradeRepository.create(grade)
                }
                fetchTeacherData() // Refresh data after adding grade
            } catch (e: Exception) {
                errorMessage = "Error adding grade: ${e.message}"
                Log.e(TAG, "Error adding grade: ${e.message}", e)
            }
        }
    }

    fun updateCourse(course: FirebaseCourse) {
        viewModelScope.launch {
            try {
                courseRepository.update(course.courseId, course)
                fetchTeacherData() // Refresh data after updating course
            } catch (e: Exception) {
                errorMessage = "Error updating course: ${e.message}"
                Log.e(TAG, "Error updating course: ${e.message}", e)
            }
        }
    }

    fun updateCourseDescription(course: FirebaseCourse) {
        viewModelScope.launch {
            try {
                courseRepository.update(course.courseId, course)
                fetchTeacherData() // Refresh data after updating course description
            } catch (e: Exception) {
                errorMessage = "Error updating course description: ${e.message}"
                Log.e(TAG, "Error updating course description: ${e.message}", e)
            }
        }
    }

    // Add dummy update functions for Firebase models if needed by the UI/other parts
    fun updateAssignment(assignment: FirebaseAssignment) { /* TODO: Implement update logic */ }
    fun updateGrade(grade: FirebaseGrade) { /* TODO: Implement update logic */ }
    fun addMaterial(material: CourseMaterial) {
        viewModelScope.launch {
            try {
                courseMaterialRepository.insertMaterial(material)
                fetchTeacherData() // refresh view
            } catch (e: Exception) {
                errorMessage = "Error adding material: ${e.message}"
            }
        }
    }


    // Add dummy update functions to satisfy ViewModel dependencies if needed by the UI/other parts
    fun updateAssignment(assignment: Assignment) { /* TODO: Implement update logic */ }
    fun updateGrade(grade: Grade) { /* TODO: Implement update logic */ }

    fun loadStudentsForCourse(course: FirebaseCourse) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Attempting to fetch students for course ID: ${course.courseId}")
                val studentIdsForCourse = mutableSetOf<String>()
                val enrollments = studentCourseCollection.whereEqualTo("courseId", course.courseId).get().await()
                enrollments.documents.forEach { document ->
                    document.data?.get("studentId")?.let { studentId ->
                        studentIdsForCourse.add(studentId as String)
                    }
                }
                Log.d(TAG, "Retrieved ${studentIdsForCourse.size} student IDs from studentCourses for course ${course.courseId}: ${studentIdsForCourse.toList()}")
                // Fetch student details for the collected IDs
                val students = studentIdsForCourse.mapNotNull { studentId ->
                    studentRepository.getStudentByStudentId(studentId)
                }
                _studentsInSelectedCourse.value = students
                Log.d(TAG, "Fetched ${students.size} students for course ${course.courseId}. Student IDs: ${students.map { it.studentId }}")
            } catch (e: Exception) {
                errorMessage = "Error loading students for course: ${e.message}"
                Log.e(TAG, "Error loading students for course: ${e.message}", e)
            }
        }
    }

    fun loadGradesForAssignment(assignmentId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Attempting to fetch grades for assignment ID: ${assignmentId}")
            val fetchedGrades = gradeRepository.getGradesByAssignment(assignmentId)
            _gradesForAssignment.value = fetchedGrades
            Log.d(TAG, "Fetched ${fetchedGrades.size} grades for assignment ${assignmentId}.")
            fetchedGrades.forEach { Log.d(TAG, "Grade - ID: ${it.gradeId}, Student ID: ${it.studentId}, Assignment ID: ${it.assignmentId}, Score: ${it.score}") }
        }
    }

    fun saveGradesForAssignment(grades: List<FirebaseGrade>) {
        viewModelScope.launch {
            try {
                grades.forEach { grade ->
                    Log.d(TAG, "Saving grade for student: ${grade.studentId}, assignment: ${grade.assignmentId}, score: ${grade.score}")
                    // Check if grade already exists
                    val existingGrade = gradeRepository.getStudentGradeForAssignment(grade.studentId, grade.assignmentId)
                    if (existingGrade != null) {
                        // Update existing grade
                        gradeRepository.update(existingGrade.gradeId, grade)
                    } else {
                        // Create new grade
                        gradeRepository.create(grade)
                    }
                }
                // Refresh grades after saving
                if (grades.isNotEmpty()) loadGradesForAssignment(grades.first().assignmentId)
            } catch (e: Exception) {
                errorMessage = "Error saving grades: ${e.message}"
                Log.e(TAG, "Error saving grades: ${e.message}", e)
            }
        }
    }

    fun deleteAssignment(assignment: FirebaseAssignment) {
        viewModelScope.launch {
            try {
                assignmentRepository.delete(assignment.assignmentId)
                fetchTeacherData() // Refresh data after deleting assignment
            } catch (e: Exception) {
                errorMessage = "Error deleting assignment: ${e.message}"
                Log.e(TAG, "Error deleting assignment: ${e.message}", e)
            }
        }
    }

    fun loadAllGrades() {
        viewModelScope.launch {
            fetchTeacherData()
        }
    }

    companion object {
        private const val TAG = "TeacherDashboardVM"
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AUTApplication
                // Access repositories directly from the application object and add student repository
                TeacherDashboardViewModel(
                    application.teacherRepository,
                    application.courseRepository,
                    application.assignmentRepository,
                    application.courseMaterialRepository,
                    application.gradeRepository,
                    application.studentRepository
                )
            }
        }
    }
}