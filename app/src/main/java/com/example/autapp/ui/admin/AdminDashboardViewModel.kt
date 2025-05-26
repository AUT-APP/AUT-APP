package com.example.autapp.ui.admin

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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import kotlin.random.Random

class AdminDashboardViewModel(
    private val studentRepository: StudentRepository,
    private val teacherRepository: TeacherRepository,
    private val courseRepository: CourseRepository,
    private val departmentRepository: DepartmentRepository,
    private val activityLogRepository: ActivityLogRepository
) : ViewModel() {
    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students

    private val _teachers = MutableStateFlow<List<Teacher>>(emptyList())
    val teachers: StateFlow<List<Teacher>> = _teachers

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses

    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments

    private val _activities = MutableStateFlow<List<ActivityLog>>(emptyList())
    val activities: StateFlow<List<ActivityLog>> = _activities

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                _students.value = studentRepository.getAllStudents()
                _teachers.value = teacherRepository.getAllTeachers()
                _courses.value = courseRepository.getAllCourses()
                _departments.value = departmentRepository.getAllDepartments()
                activityLogRepository.getRecentActivities().collect { activities ->
                    _activities.value = activities
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading data: ${e.message}"
            }
        }
    }

    private suspend fun generateUniqueUsername(): String {
        val letters = ('a'..'z').toList()
        val numbers = ('0'..'9').toList()
        var username: String
        var isUnique = false

        do {
            username = buildString {
                repeat(3) { append(letters[Random.nextInt(letters.size)]) }
                repeat(4) { append(numbers[Random.nextInt(numbers.size)]) }
            }
            val existingStudent = studentRepository.getStudentByUsername(username)
            val existingTeacher = teacherRepository.getTeacherByUsername(username)
            isUnique = existingStudent == null && existingTeacher == null
        } while (!isUnique)

        return username
    }

    private suspend fun generateUniqueStudentId(): Int {
        var studentId: Int
        var isUnique = false

        do {
            studentId = Random.nextInt(10000000, 99999999)
            val existingStudent = studentRepository.getStudentByStudentId(studentId)
            isUnique = existingStudent == null
        } while (!isUnique)

        return studentId
    }

    private suspend fun generateUniqueTeacherId(): Int {
        var teacherId: Int
        var isUnique = false

        do {
            teacherId = Random.nextInt(10000000, 99999999)
            val existingTeacher = teacherRepository.getTeacherByTeacherId(teacherId)
            isUnique = existingTeacher == null
        } while (!isUnique)

        return teacherId
    }

    private fun generatePassword(firstName: String, dob: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val date = dateFormat.parse(dob)
        val monthDayFormat = SimpleDateFormat("MMdd")
        return "${firstName.lowercase()}${monthDayFormat.format(date)}"
    }

    private fun addActivity(description: String) {
        viewModelScope.launch {
            try {
                activityLogRepository.insert(
                    ActivityLog(
                        description = description,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error logging activity: ${e.message}"
            }
        }
    }

    fun createStudent(
        firstName: String,
        lastName: String,
        role: String,
        enrollmentDate: String,
        majorId: Int,
        minorId: Int?,
        yearOfStudy: Int,
        gpa: Double,
        dob: String,
        courseEnrollments: List<Triple<Int, Int, Int>>,
        enrollmentYear: Int,
        semester: Int
    ) {
        viewModelScope.launch {
            try {
                if (firstName.isBlank() || lastName.isBlank()) {
                    throw IllegalArgumentException("First name and last name cannot be empty")
                }
                if (majorId == 0 || departmentRepository.getDepartmentById(majorId)?.type != "Major") {
                    throw IllegalArgumentException("Invalid or missing major")
                }
                minorId?.let { id ->
                    if (departmentRepository.getDepartmentById(id)?.type != "Minor") {
                        throw IllegalArgumentException("Invalid minor")
                    }
                }
                if (yearOfStudy < 1) {
                    throw IllegalArgumentException("Year of study must be at least 1")
                }
                if (dob.isBlank()) {
                    throw IllegalArgumentException("Date of birth is required")
                }
                val generatedUsername = generateUniqueUsername()
                val generatedStudentId = generateUniqueStudentId()
                val generatedPassword = generatePassword(firstName, dob)
                val student = Student(
                    id = 0, // Matches User id, auto-generated via User insertion
                    firstName = firstName,
                    lastName = lastName,
                    username = generatedUsername,
                    password = generatedPassword,
                    role = role,
                    studentId = generatedStudentId, // Primary key
                    enrollmentDate = enrollmentDate,
                    majorId = majorId,
                    minorId = minorId,
                    yearOfStudy = yearOfStudy,
                    gpa = gpa,
                    dob = dob
                )
                studentRepository.insertStudent(student)
                val newStudent = studentRepository.getStudentByUsername(generatedUsername)
                    ?: throw Exception("Failed to retrieve new student")
                var enrolledCount = 0
                var skippedCount = 0
                courseEnrollments.forEach { (courseId, year, semester) ->
                    if (!studentRepository.isEnrolled(newStudent.studentId, courseId, year, semester)) {
                        studentRepository.insertStudentCourseCrossRef(
                            StudentCourseCrossRef(newStudent.studentId, courseId, year, semester)
                        )
                        enrolledCount++
                    } else {
                        skippedCount++
                    }
                }
                val successMessage = buildString {
                    append("Student ${student.firstName} ${student.lastName} created with username $generatedUsername, student ID $generatedStudentId, and initial password $generatedPassword")
                    if (enrolledCount > 0) append(". Enrolled in $enrolledCount course(s)")
                    if (skippedCount > 0) append(". Skipped $skippedCount duplicate enrollment(s)")
                }
                _successMessage.value = successMessage
                addActivity(successMessage)
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error creating student: ${e.message}"
            }
        }
    }

    fun updateStudent(
        student: Student,
        courseEnrollments: List<Triple<Int, Int, Int>>,
        enrollmentYear: Int,
        semester: Int
    ) {
        viewModelScope.launch {
            try {
                if (student.firstName.isBlank() || student.lastName.isBlank()) {
                    throw IllegalArgumentException("First name and last name cannot be empty")
                }
                if (student.majorId == 0 || departmentRepository.getDepartmentById(student.majorId)?.type != "Major") {
                    throw IllegalArgumentException("Invalid or missing major")
                }
                student.minorId?.let { id ->
                    if (departmentRepository.getDepartmentById(id)?.type != "Minor") {
                        throw IllegalArgumentException("Invalid minor")
                    }
                }
                if (student.yearOfStudy < 1) {
                    throw IllegalArgumentException("Year of study must be at least 1")
                }
                if (student.dob.isBlank()) {
                    throw IllegalArgumentException("Date of birth is required")
                }
                studentRepository.updateStudent(student)
                studentRepository.deleteCrossRefsByStudentId(student.studentId)
                var enrolledCount = 0
                var skippedCount = 0
                courseEnrollments.forEach { (courseId, year, semester) ->
                    if (!studentRepository.isEnrolled(student.studentId, courseId, year, semester)) {
                        studentRepository.insertStudentCourseCrossRef(
                            StudentCourseCrossRef(student.studentId, courseId, year, semester)
                        )
                        enrolledCount++
                    } else {
                        skippedCount++
                    }
                }
                val successMessage = buildString {
                    append("Student ${student.firstName} ${student.lastName} updated successfully")
                    if (enrolledCount > 0) append(". Enrolled in $enrolledCount course(s)")
                    if (skippedCount > 0) append(". Skipped $skippedCount duplicate enrollment(s)")
                }
                _successMessage.value = successMessage
                addActivity(successMessage)
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error updating student: ${e.message}"
            }
        }
    }

    fun deleteStudents(students: List<Student>) {
        viewModelScope.launch {
            try {
                students.forEach { student ->
                    studentRepository.deleteStudent(student)
                }
                val successMessage = if (students.size == 1) {
                    "Student ${students[0].firstName} ${students[0].lastName} deleted successfully"
                } else {
                    "${students.size} students deleted successfully"
                }
                _successMessage.value = successMessage
                addActivity(successMessage)
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting students: ${e.message}"
            }
        }
    }

    fun bulkEnroll(studentIds: List<Int>, courseId: Int, year: Int, semester: Int) {
        viewModelScope.launch {
            try {
                if (studentIds.isEmpty()) {
                    throw IllegalArgumentException("No students selected")
                }
                if (courseId == 0 || courseRepository.getCourseById(courseId) == null) {
                    throw IllegalArgumentException("Invalid course")
                }
                if (year <= 0 || semester <= 0) {
                    throw IllegalArgumentException("Year and semester must be positive")
                }
                var enrolledCount = 0
                var skippedCount = 0
                studentIds.forEach { studentId ->
                    if (!studentRepository.isEnrolled(studentId, courseId, year, semester)) {
                        studentRepository.insertStudentCourseCrossRef(
                            StudentCourseCrossRef(studentId, courseId, year, semester)
                        )
                        enrolledCount++
                    } else {
                        skippedCount++
                    }
                }
                val course = courseRepository.getCourseById(courseId)
                val successMessage = buildString {
                    append("Enrolled $enrolledCount student(s) in ${course?.name} (Year: $year, Semester: $semester)")
                    if (skippedCount > 0) append(". Skipped $skippedCount duplicate enrollment(s)")
                }
                _successMessage.value = successMessage
                addActivity(successMessage)
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error during bulk enrollment: ${e.message}"
            }
        }
    }

    fun createTeacher(
        firstName: String,
        lastName: String,
        role: String,
        departmentId: Int,
        officeHours: String,
        courses: List<String>,
        dob: String
    ) {
        viewModelScope.launch {
            try {
                if (firstName.isBlank() || lastName.isBlank()) {
                    throw IllegalArgumentException("First name and last name cannot be empty")
                }
                if (departmentId == 0 || departmentRepository.getDepartmentById(departmentId)?.type != "Department") {
                    throw IllegalArgumentException("Invalid or missing department")
                }
                if (officeHours.isBlank()) {
                    throw IllegalArgumentException("Office hours are required")
                }
                if (dob.isBlank()) {
                    throw IllegalArgumentException("Date of birth is required")
                }
                val generatedUsername = generateUniqueUsername()
                val generatedTeacherId = generateUniqueTeacherId()
                val generatedPassword = generatePassword(firstName, dob)
                val teacher = Teacher(
                    teacherId = 0, // Auto-generated by Room
                    firstName = firstName,
                    lastName = lastName,
                    username = generatedUsername,
                    password = generatedPassword,
                    departmentId = departmentId,
                    role = role,
                    officeHours = officeHours,
                    courses = courses.toMutableList(),
                    dob = dob
                )
                teacherRepository.insertTeacher(teacher)
                val successMessage = "Teacher ${teacher.firstName} ${teacher.lastName} created with username $generatedUsername, teacher ID $generatedTeacherId, and initial password $generatedPassword"
                _successMessage.value = successMessage
                addActivity(successMessage)
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error creating teacher: ${e.message}"
            }
        }
    }

    fun updateTeacher(teacher: Teacher) {
        viewModelScope.launch {
            try {
                if (teacher.firstName.isBlank() || teacher.lastName.isBlank()) {
                    throw IllegalArgumentException("First name and last name cannot be empty")
                }
                if (teacher.departmentId == 0 || departmentRepository.getDepartmentById(teacher.departmentId)?.type != "Department") {
                    throw IllegalArgumentException("Invalid or missing department")
                }
                if (teacher.officeHours.isBlank()) {
                    throw IllegalArgumentException("Office hours are required")
                }
                if (teacher.dob.isBlank()) {
                    throw IllegalArgumentException("Date of birth is required")
                }
                teacherRepository.updateTeacher(teacher)
                val successMessage = "Teacher ${teacher.firstName} ${teacher.lastName} updated successfully"
                _successMessage.value = successMessage
                addActivity(successMessage)
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error updating teacher: ${e.message}"
            }
        }
    }

    fun deleteTeachers(teachers: List<Teacher>) {
        viewModelScope.launch {
            try {
                teachers.forEach { teacher ->
                    teacherRepository.deleteTeacher(teacher)
                }
                val successMessage = if (teachers.size == 1) {
                    "Teacher ${teachers[0].firstName} ${teachers[0].lastName} deleted successfully"
                } else {
                    "${teachers.size} teachers deleted successfully"
                }
                _successMessage.value = successMessage
                addActivity(successMessage)
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting teachers: ${e.message}"
            }
        }
    }

    fun createDepartment(name: String, type: String, description: String?) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    throw IllegalArgumentException("Department name cannot be empty")
                }
                if (type !in listOf("Department", "Major", "Minor")) {
                    throw IllegalArgumentException("Invalid department type")
                }
                val department = Department(
                    departmentId = 0,
                    name = name,
                    type = type,
                    description = description
                )
                departmentRepository.insertDepartment(department)
                val successMessage = "Department $name ($type) created successfully"
                _successMessage.value = successMessage
                addActivity(successMessage)
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error creating department: ${e.message}"
            }
        }
    }

    fun updateDepartment(department: Department) {
        viewModelScope.launch {
            try {
                if (department.name.isBlank()) {
                    throw IllegalArgumentException("Department name cannot be empty")
                }
                if (department.type !in listOf("Department", "Major", "Minor")) {
                    throw IllegalArgumentException("Invalid department type")
                }
                departmentRepository.updateDepartment(department)
                val successMessage = "Department ${department.name} (${department.type}) updated successfully"
                _successMessage.value = successMessage
                addActivity(successMessage)
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error updating department: ${e.message}"
            }
        }
    }

    fun deleteDepartment(department: Department) {
        viewModelScope.launch {
            try {
                departmentRepository.deleteDepartment(department)
                val successMessage = "Department ${department.name} deleted successfully"
                _successMessage.value = successMessage
                addActivity(successMessage)
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting department: ${e.message}"
            }
        }
    }

    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AUTApplication
                AdminDashboardViewModel(
                    studentRepository = application.studentRepository,
                    teacherRepository = application.teacherRepository,
                    courseRepository = application.courseRepository,
                    departmentRepository = application.departmentRepository,
                    activityLogRepository = application.activityLogRepository
                )
            }
        }
    }
}