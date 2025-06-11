package com.example.autapp.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.firebase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import kotlin.random.Random

class AdminDashboardViewModel(
    private val studentRepository: FirebaseStudentRepository,
    private val teacherRepository: FirebaseTeacherRepository,
    private val courseRepository: FirebaseCourseRepository,
    private val departmentRepository: FirebaseDepartmentRepository,
    private val activityLogRepository: FirebaseActivityLogRepository,
    private val userRepository: FirebaseUserRepository,
    private val timetableEntryRepository: FirebaseTimetableRepository
) : ViewModel() {
    private val _students = MutableStateFlow<List<FirebaseStudent>>(emptyList())
    val students: StateFlow<List<FirebaseStudent>> = _students

    private val _teachers = MutableStateFlow<List<FirebaseTeacher>>(emptyList())
    val teachers: StateFlow<List<FirebaseTeacher>> = _teachers

    private val _courses = MutableStateFlow<List<FirebaseCourse>>(emptyList())
    val courses: StateFlow<List<FirebaseCourse>> = _courses

    private val _departments = MutableStateFlow<List<FirebaseDepartment>>(emptyList())
    val departments: StateFlow<List<FirebaseDepartment>> = _departments

    private val _activities = MutableStateFlow<List<FirebaseActivityLog>>(emptyList())
    val activities: StateFlow<List<FirebaseActivityLog>> = _activities

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
                _students.value = studentRepository.getAll()
                _teachers.value = teacherRepository.getAll()
                _courses.value = courseRepository.getAll()
                _departments.value = departmentRepository.getAll()
                _activities.value = activityLogRepository.getAll().sortedByDescending { it.timestamp }
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
            val conditions = listOf(QueryCondition("username", QueryOperator.EQUAL_TO, username))
            val existingStudent = studentRepository.query(conditions).firstOrNull()
            val existingTeacher = teacherRepository.query(conditions).firstOrNull()
            isUnique = existingStudent == null && existingTeacher == null
        } while (!isUnique)

        return username
    }

    private suspend fun generateUniqueStudentId(): String {
        var studentId: String
        var isUnique = false

        do {
            studentId = Random.nextInt(10000000, 99999999).toString()
            val conditions = listOf(QueryCondition("studentId", QueryOperator.EQUAL_TO, studentId))
            val existingStudent = studentRepository.query(conditions).firstOrNull()
            isUnique = existingStudent == null
        } while (!isUnique)

        return studentId
    }

    private suspend fun generateUniqueTeacherId(): String {
        var teacherId: String
        var isUnique = false

        do {
            teacherId = Random.nextInt(10000000, 99999999).toString()
            val conditions = listOf(QueryCondition("teacherId", QueryOperator.EQUAL_TO, teacherId))
            val existingTeacher = teacherRepository.query(conditions).firstOrNull()
            isUnique = existingTeacher == null
        } while (!isUnique)

        return teacherId
    }

    private fun generatePassword(firstName: String, dob: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = dateFormat.parse(dob)
        val monthDayFormat = SimpleDateFormat("MMdd", Locale.US)
        return "${firstName.lowercase()}${monthDayFormat.format(date)}"
    }

    private fun addActivity(description: String) {
        viewModelScope.launch {
            try {
                val activityLog = FirebaseActivityLog(
                    description = description,
                    timestamp = System.currentTimeMillis()
                )
                activityLogRepository.create(activityLog)
                loadData()
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
        majorId: String,
        minorId: String?,
        yearOfStudy: Int,
        dob: String,
        selectedCourses: List<Triple<String, Int, Int>>,
        enrollmentYear: Int,
        semester: Int
    ) {
        viewModelScope.launch {
            try {
                Log.d("AdminDashboardViewModel", "Attempting to create student with majorId: $majorId, selectedCourses: $selectedCourses")

                if (firstName.isBlank() || lastName.isBlank()) {
                    throw IllegalArgumentException("First name and last name cannot be empty")
                }

                val allDepartments = departmentRepository.getAll()
                val majorDepartment = allDepartments.find { it.departmentId == majorId && it.type == "Major" }

                if (majorId.isBlank() || majorDepartment == null) {
                    throw IllegalArgumentException("Invalid or missing major")
                }

                minorId?.let { id ->
                    val minorDepartment = allDepartments.find { it.departmentId == id && it.type == "Minor" }
                    if (minorDepartment == null) {
                        throw IllegalArgumentException("Invalid minor")
                    }
                }

                if (yearOfStudy < 1 || yearOfStudy > 5) {
                    throw IllegalArgumentException("Year of study must be between 1 and 5")
                }
                if (dob.isBlank()) {
                    throw IllegalArgumentException("Date of birth is required")
                }
                val generatedUsernamePart = generateUniqueUsername()
                val generatedEmail = "${generatedUsernamePart}@aut.com"
                val generatedStudentId = generateUniqueStudentId()
                val generatedPassword = generatePassword(firstName, dob)

                val authUserId = userRepository.registerUser(
                    generatedEmail,
                    generatedPassword,
                    FirebaseUser(
                        firstName = firstName,
                        lastName = lastName,
                        role = role,
                        username = generatedEmail,
                        password = generatedPassword
                    )
                )

                val student = FirebaseStudent(
                    id = authUserId,
                    firstName = firstName,
                    lastName = lastName,
                    username = generatedEmail,
                    password = generatedPassword,
                    role = role,
                    studentId = generatedStudentId,
                    enrollmentDate = enrollmentDate,
                    majorId = majorId,
                    minorId = minorId,
                    yearOfStudy = yearOfStudy,
                    gpa = 0.0,
                    dob = dob
                )
                studentRepository.createWithId(authUserId, student)
                Log.d("AdminDashboardViewModel", "Student created with ID: $authUserId, Student ID: $generatedStudentId")

                val invalidCourses = mutableListOf<String>()
                selectedCourses.forEach { (courseId, year, semester) ->
                    val course = courseRepository.getCourseByCourseId(courseId)
                    if (course == null) {
                        Log.w("AdminDashboardViewModel", "Invalid course ID: $courseId, skipping enrollment")
                        invalidCourses.add(courseId)
                    } else {
                        try {
                            val enrollment = FirebaseStudentCourse(
                                studentId = student.id,
                                courseId = courseId,
                                year = year,
                                semester = semester.toString()
                            )
                            studentRepository.enrollStudentInCourse(enrollment)
                            Log.d("AdminDashboardViewModel", "Enrolled student $authUserId in course $courseId (Year: $year, Semester: $semester)")
                        } catch (e: Exception) {
                            Log.e("AdminDashboardViewModel", "Failed to enroll in course $courseId: ${e.message}")
                            invalidCourses.add(courseId)
                        }
                    }
                }
                if (invalidCourses.isNotEmpty()) {
                    _errorMessage.value = "Student created, but failed to enroll in some courses: ${invalidCourses.joinToString()}"
                }

                addActivity("Created new student: $firstName $lastName (Student ID: $generatedStudentId, Email: $generatedEmail, Username: $generatedEmail, Default Password: $generatedPassword)")
                _successMessage.value = "Student $firstName $lastName created successfully"
                loadData()
            } catch (e: Exception) {
                Log.e("AdminDashboardViewModel", "Error creating student: ${e.message}")
                _errorMessage.value = "Error creating student: ${e.message}"
            }
        }
    }

    fun createTeacher(
        firstName: String,
        lastName: String,
        role: String,
        departmentId: String,
        title: String,
        officeNumber: String,
        email: String,
        phoneNumber: String,
        dob: String,
        courseAssignments: List<Triple<String, Int, Int>>
    ) {
        viewModelScope.launch {
            try {
                if (firstName.isBlank() || lastName.isBlank()) {
                    throw IllegalArgumentException("First name and last name cannot be empty")
                }
                val departmentConditions = listOf(QueryCondition("id", QueryOperator.EQUAL_TO, departmentId))
                if (departmentId.isBlank() || departmentRepository.query(departmentConditions).isEmpty()) {
                    throw IllegalArgumentException("Invalid or missing department")
                }
                if (dob.isBlank()) {
                    throw IllegalArgumentException("Date of birth is required")
                }
                val generatedUsernamePart = generateUniqueUsername()
                val generatedEmail = "${generatedUsernamePart}@aut.com"
                val generatedTeacherId = generateUniqueTeacherId()
                val generatedPassword = generatePassword(firstName, dob)

                val authUserId = userRepository.registerUser(
                    generatedEmail,
                    generatedPassword,
                    FirebaseUser(
                        firstName = firstName,
                        lastName = lastName,
                        role = role,
                        username = generatedEmail,
                        password = generatedPassword
                    )
                )

                val teacher = FirebaseTeacher(
                    id = authUserId,
                    firstName = firstName,
                    lastName = lastName,
                    username = generatedEmail,
                    password = generatedPassword,
                    role = role,
                    teacherId = generatedTeacherId,
                    departmentId = departmentId,
                    title = title,
                    officeNumber = officeNumber,
                    email = generatedEmail,
                    phoneNumber = phoneNumber,
                    dob = dob,
                    courses = courseAssignments.map { it.first }
                )
                teacherRepository.createWithId(authUserId, teacher)
                addActivity("Created new teacher: $firstName $lastName (Teacher ID: $generatedTeacherId, Email: $generatedEmail, Username: $generatedEmail, Default Password: $generatedPassword)")
                _successMessage.value = "Teacher $firstName $lastName created successfully"
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error creating teacher: ${e.message}"
            }
        }
    }

    fun createCourse(
        name: String,
        code: String,
        credits: Int,
        departmentId: String,
        description: String,
        prerequisites: List<String>,
        timetableEntries: List<TimetableEntryFormData>
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank() || code.isBlank()) {
                    throw IllegalArgumentException("Course name and code cannot be empty")
                }
                val department = departmentRepository.getById(departmentId)
                if (departmentId.isBlank() || department == null || department.type != "Department") {
                    throw IllegalArgumentException("Invalid or missing department. Must be a Department type.")
                }
                val course = FirebaseCourse(
                    name = name,
                    description = description
                )
                val courseId = courseRepository.create(course)

                timetableEntries.forEach { entryData ->
                    val startTime = SimpleDateFormat("HH:mm", Locale.US).parse(entryData.startTime)
                    val endTime = SimpleDateFormat("HH:mm", Locale.US).parse(entryData.endTime)

                    val today = Calendar.getInstance()
                    val startDateTime = Calendar.getInstance().apply {
                        time = startTime
                        set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
                    }.time
                    val endDateTime = Calendar.getInstance().apply {
                        time = endTime
                        set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
                    }.time

                    val timetableEntry = FirebaseTimetableEntry(
                        courseId = courseId,
                        dayOfWeek = entryData.dayOfWeek,
                        startTime = startDateTime,
                        endTime = endDateTime,
                        room = entryData.room,
                        type = entryData.type
                    )
                    timetableEntryRepository.create(timetableEntry)
                }

                addActivity("Created new course: ${course.name} (${courseId})")
                _successMessage.value = "Course ${course.name} (${courseId}) created successfully"
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error creating course: ${e.message}"
            }
        }
    }

    fun createDepartment(
        name: String,
        code: String,
        type: String,
        description: String,
        headTeacherId: String?
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    throw IllegalArgumentException("Department name cannot be empty")
                }
                if (type !in listOf("Major", "Minor", "Department")) {
                    throw IllegalArgumentException("Invalid department type")
                }
                val existingDepartments = departmentRepository.query(
                    listOf(
                        QueryCondition("name", QueryOperator.EQUAL_TO, name),
                        QueryCondition("type", QueryOperator.EQUAL_TO, type)
                    )
                )
                if (existingDepartments.isNotEmpty()) {
                    throw IllegalArgumentException("$type with name '$name' already exists")
                }

                headTeacherId?.let { id ->
                    val teacherConditions = listOf(QueryCondition("teacherId", QueryOperator.EQUAL_TO, id))
                    if (teacherRepository.query(teacherConditions).isEmpty()) {
                        throw IllegalArgumentException("Invalid head teacher")
                    }
                }
                val department = FirebaseDepartment(
                    departmentId = "",
                    name = name,
                    type = type,
                    description = description
                )
                val documentId = departmentRepository.create(department)
                addActivity("Created new department: ${department.name} (${documentId})")
                _successMessage.value = "Department ${department.name} (${documentId}) created successfully"
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error creating department: ${e.message}"
            }
        }
    }

    fun updateStudent(
        student: FirebaseStudent,
        courseEnrollments: List<Triple<String, Int, Int>>,
        enrollmentYear: Int,
        semester: Int
    ) {
        viewModelScope.launch {
            try {
                if (student.yearOfStudy < 1 || student.yearOfStudy > 5) {
                    throw IllegalArgumentException("Year of study must be between 1 and 5")
                }

                studentRepository.update(student.id, student)
                Log.d("AdminDashboardViewModel", "Updated student: ${student.id}")

                val existingEnrollments = studentRepository.getEnrollmentsByStudent(student.id)
                Log.d("AdminDashboardViewModel", "Existing enrollments: $existingEnrollments")

                // Remove enrollments that are no longer selected
                existingEnrollments.forEach { enrollment ->
                    if (courseEnrollments.none { it.first == enrollment.courseId && it.second == enrollment.year && it.third.toString() == enrollment.semester }) {
                        studentRepository.deleteEnrollment(enrollment.studentId, enrollment.courseId, enrollment.year, enrollment.semester)
                        Log.d("AdminDashboardViewModel", "Deleted enrollment: ${enrollment.courseId}, Year: ${enrollment.year}, Semester: ${enrollment.semester}")
                    }
                }

                // Add or update selected enrollments
                val invalidCourses = mutableListOf<String>()
                courseEnrollments.forEach { (courseId, year, semester) ->
                    val course = courseRepository.getCourseByCourseId(courseId)
                    if (course == null) {
                        Log.w("AdminDashboardViewModel", "Invalid course ID: $courseId, skipping enrollment")
                        invalidCourses.add(courseId)
                    } else {
                        val existingEnrollment = existingEnrollments.find {
                            it.courseId == courseId && it.year == year && it.semester == semester.toString()
                        }
                        if (existingEnrollment == null) {
                            try {
                                val enrollment = FirebaseStudentCourse(
                                    studentId = student.id,
                                    courseId = courseId,
                                    year = year,
                                    semester = semester.toString()
                                )
                                studentRepository.enrollStudentInCourse(enrollment)
                                Log.d("AdminDashboardViewModel", "Enrolled student ${student.id} in course $courseId (Year: $year, Semester: $semester)")
                            } catch (e: Exception) {
                                Log.e("AdminDashboardViewModel", "Failed to enroll in course $courseId: ${e.message}")
                                invalidCourses.add(courseId)
                            }
                        }
                    }
                }

                if (invalidCourses.isNotEmpty()) {
                    _errorMessage.value = "Student updated, but failed to enroll in some courses: ${invalidCourses.joinToString()}"
                }

                addActivity("Updated student: ${student.firstName} ${student.lastName} (ID: ${student.studentId})")
                _successMessage.value = "Student ${student.firstName} ${student.lastName} updated successfully"
                loadData()
            } catch (e: Exception) {
                Log.e("AdminDashboardViewModel", "Error updating student: ${e.message}")
                _errorMessage.value = "Error updating student: ${e.message}"
            }
        }
    }

    fun deleteStudents(students: List<FirebaseStudent>) {
        viewModelScope.launch {
            try {
                students.forEach { student ->
                    studentRepository.delete(student.id)
                    addActivity("Deleted student: ${student.firstName} ${student.lastName} (ID: ${student.studentId})")
                }
                _successMessage.value = "${students.size} student(s) deleted successfully"
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting student(s): ${e.message}"
            }
        }
    }

    fun bulkEnroll(studentIds: List<String>, courseId: String, year: Int, semester: Int) {
        viewModelScope.launch {
            try {
                studentIds.forEach { studentId ->
                    val enrollment = FirebaseStudentCourse(
                        studentId = studentId,
                        courseId = courseId,
                        year = year,
                        semester = semester.toString()
                    )
                    studentRepository.enrollStudentInCourse(enrollment)
                }
                addActivity("Bulk enrolled ${studentIds.size} students into course $courseId")
                _successMessage.value = "Bulk enrollment completed for ${studentIds.size} student(s) in course $courseId"
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error during bulk enrollment: ${e.message}"
            }
        }
    }

    fun updateTeacher(teacher: FirebaseTeacher) {
        viewModelScope.launch {
            try {
                teacherRepository.update(teacher.teacherId, teacher)
                addActivity("Updated teacher: ${teacher.firstName} ${teacher.lastName} (ID: ${teacher.teacherId})")
                _successMessage.value = "Teacher ${teacher.firstName} ${teacher.lastName} updated successfully"
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error updating teacher: ${e.message}"
            }
        }
    }

    fun deleteTeachers(teachers: List<FirebaseTeacher>) {
        viewModelScope.launch {
            try {
                teachers.forEach { teacher ->
                    teacherRepository.delete(teacher.teacherId)
                    addActivity("Deleted teacher: ${teacher.firstName} ${teacher.lastName} (ID: ${teacher.teacherId})")
                }
                _successMessage.value = "${teachers.size} teacher(s) deleted successfully"
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting teacher(s): ${e.message}"
            }
        }
    }

    fun updateCourse(
        course: FirebaseCourse,
        timetableEntries: List<TimetableEntryFormData>
    ) {
        viewModelScope.launch {
            try {
                courseRepository.update(course.courseId, course)

                val existingEntries = timetableEntryRepository.getTimetableByCourse(course.courseId)
                existingEntries.forEach { entry ->
                    timetableEntryRepository.delete(entry.entryId)
                }

                timetableEntries.forEach { entryData ->
                    val startTime = SimpleDateFormat("HH:mm", Locale.US).parse(entryData.startTime)
                    val endTime = SimpleDateFormat("HH:mm", Locale.US).parse(entryData.endTime)

                    val today = Calendar.getInstance()
                    val startDateTime = Calendar.getInstance().apply {
                        time = startTime
                        set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
                    }.time
                    val endDateTime = Calendar.getInstance().apply {
                        time = endTime
                        set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
                    }.time

                    val timetableEntry = FirebaseTimetableEntry(
                        courseId = course.courseId,
                        dayOfWeek = entryData.dayOfWeek,
                        startTime = startDateTime,
                        endTime = endDateTime,
                        room = entryData.room,
                        type = entryData.type
                    )
                    timetableEntryRepository.create(timetableEntry)
                }

                addActivity("Updated course: ${course.name} (${course.courseId})")
                _successMessage.value = "Course ${course.name} updated successfully"
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error updating course: ${e.message}"
            }
        }
    }

    fun deleteCourses(courses: List<FirebaseCourse>) {
        viewModelScope.launch {
            try {
                courses.forEach { course ->
                    courseRepository.delete(course.courseId)
                    addActivity("Deleted course: ${course.name} (${course.courseId})")
                }
                _successMessage.value = "${courses.size} course(s) deleted successfully"
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting course(s): ${e.message}"
            }
        }
    }

    fun updateDepartment(department: FirebaseDepartment) {
        viewModelScope.launch {
            try {
                departmentRepository.update(department.departmentId, department)
                addActivity("Updated department: ${department.name} (${department.departmentId})")
                _successMessage.value = "Department ${department.name} updated successfully"
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "Error updating department: ${e.message}"
            }
        }
    }

    fun deleteDepartment(department: FirebaseDepartment) {
        viewModelScope.launch {
            try {
                departmentRepository.delete(department.departmentId)
                addActivity("Deleted department: ${department.name} (${department.departmentId})")
                _successMessage.value = "Department ${department.name} deleted successfully"
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
                val application = this[APPLICATION_KEY] as AUTApplication
                AdminDashboardViewModel(
                    application.studentRepository,
                    application.teacherRepository,
                    application.courseRepository,
                    application.departmentRepository,
                    application.activityLogRepository,
                    application.userRepository,
                    application.timetableEntryRepository
                )
            }
        }
    }
}