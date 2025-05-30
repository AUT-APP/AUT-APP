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
import com.example.autapp.data.firebase.FirebaseStudentRepository
import com.example.autapp.data.firebase.FirebaseTeacherRepository
import com.example.autapp.data.firebase.FirebaseCourseRepository
import com.example.autapp.data.firebase.FirebaseDepartmentRepository
import com.example.autapp.data.firebase.FirebaseActivityLogRepository
import com.example.autapp.data.firebase.QueryCondition
import com.example.autapp.data.firebase.QueryOperator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import kotlin.random.Random
import com.example.autapp.ui.admin.TimetableEntryFormData
import java.util.Date
import java.util.Locale
import java.util.Calendar

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
                _activities.value = activityLogRepository.getAll()
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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val date = dateFormat.parse(dob)
        val monthDayFormat = SimpleDateFormat("MMdd")
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
        gpa: Double,
        dob: String,
        selectedCourses: List<Triple<String, Int, Int>>,
        enrollmentYear: Int,
        semester: Int
    ) {
        viewModelScope.launch {
            try {
                // Add logging for majorId
                Log.d("AdminDashboardViewModel", "Attempting to create student with majorId: $majorId")

                if (firstName.isBlank() || lastName.isBlank()) {
                    throw IllegalArgumentException("First name and last name cannot be empty")
                }

                // Fetch all departments and filter manually as a workaround for query issue
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

                if (yearOfStudy < 1) {
                    throw IllegalArgumentException("Year of study must be at least 1")
                }
                if (dob.isBlank()) {
                    throw IllegalArgumentException("Date of birth is required")
                }
                val generatedUsernamePart = generateUniqueUsername()
                val generatedEmail = "${generatedUsernamePart}@aut.com" // Generate a valid email
                val generatedStudentId = generateUniqueStudentId()
                val generatedPassword = generatePassword(firstName, dob)

                // Create user in Firebase Authentication using the generated email
                val authUserId = userRepository.registerUser(generatedEmail, generatedPassword, FirebaseUser(
                    firstName = firstName,
                    lastName = lastName,
                    role = role,
                    username = generatedEmail, // Use generated email as username
                    password = generatedPassword // It's generally not recommended to store passwords in Firestore, but keeping for now based on existing model
                ))

                // Create student document in Firestore using the authUserId as the document ID
                val student = FirebaseStudent(
                    id = authUserId, // Use authUserId as document ID
                    firstName = firstName,
                    lastName = lastName,
                    username = generatedEmail, // Use generated email as username
                    password = generatedPassword,
                    role = role,
                    studentId = generatedStudentId, // Keep generatedStudentId for the specific app ID
                    enrollmentDate = enrollmentDate,
                    majorId = majorId,
                    minorId = minorId,
                    yearOfStudy = yearOfStudy,
                    gpa = gpa,
                    dob = dob
                )
                // Use createWithId to set the document ID explicitly
                studentRepository.createWithId(authUserId, student)

                selectedCourses.forEach { (courseId, year, semester) ->
                    val enrollment = FirebaseStudentCourse(
                        studentId = student.id,
                        courseId = courseId,
                        year = year,
                        semester = semester.toString()
                    )
                    studentRepository.enrollStudentInCourse(enrollment)
                }
                addActivity("Created new student: $firstName $lastName (ID: ${student.id})")
                _successMessage.value = "Student $firstName $lastName created successfully"
                loadData()
            } catch (e: Exception) {
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
                val generatedEmail = "${generatedUsernamePart}@aut.com" // Generate a valid email
                val generatedTeacherId = generateUniqueTeacherId()
                val generatedPassword = generatePassword(firstName, dob)

                // Create user in Firebase Authentication using the generated email
                val authUserId = userRepository.registerUser(generatedEmail, generatedPassword, FirebaseUser(
                    firstName = firstName,
                    lastName = lastName,
                    role = role,
                    username = generatedEmail, // Use generated email as username
                    password = generatedPassword
                ))

                // Create teacher document in Firestore using the authUserId as the document ID
                val teacher = FirebaseTeacher(
                    id = authUserId, // Use authUserId as document ID
                    firstName = firstName,
                    lastName = lastName,
                    username = generatedEmail, // Use generated email as username
                    password = generatedPassword,
                    role = role,
                    teacherId = generatedTeacherId,
                    departmentId = departmentId,
                    title = title,
                    officeNumber = officeNumber,
                    email = generatedEmail, // Use generated email
                    phoneNumber = phoneNumber,
                    dob = dob,
                    courses = courseAssignments.map { it.first }
                )
                teacherRepository.createWithId(authUserId, teacher)
                addActivity("Created new teacher: ${teacher.firstName} ${teacher.lastName} (ID: ${teacher.teacherId})")
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
                val departmentConditions = listOf(QueryCondition("id", QueryOperator.EQUAL_TO, departmentId))
                if (departmentId.isBlank() || departmentRepository.query(departmentConditions).isEmpty()) {
                    throw IllegalArgumentException("Invalid or missing department")
                }
                val course = FirebaseCourse(
                    name = name,
                    description = description
                )
                val courseId = courseRepository.create(course)

                // Create and save timetable entries
                timetableEntries.forEach { entryData ->
                    val startTime = SimpleDateFormat("HH:mm", Locale.US).parse(entryData.startTime)
                    val endTime = SimpleDateFormat("HH:mm", Locale.US).parse(entryData.endTime)

                    // Combine with current date
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
                val existingDepartments = departmentRepository.query(listOf(
                    QueryCondition("name", QueryOperator.EQUAL_TO, name),
                    QueryCondition("type", QueryOperator.EQUAL_TO, type)
                ))
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
                studentRepository.update(student.id, student)
                // Note: Updating course enrollments requires separate logic, potentially deleting and recreating or finding and updating.
                // For simplicity here, we'll just update the student details.
                addActivity("Updated student: ${student.firstName} ${student.lastName} (ID: ${student.studentId})")
                _successMessage.value = "Student ${student.firstName} ${student.lastName} updated successfully"
            } catch (e: Exception) {
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
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting student(s): ${e.message}"
            }
        }
    }

    fun bulkEnroll(studentIds: List<String>, courseId: String, year: Int, semester: Int) {
        viewModelScope.launch {
            try {
                // This implementation assumes you have a method in your repository to handle bulk enrollments.
                // You might need to implement the actual bulk enrollment logic based on your data model and requirements.
                // For now, this is a placeholder.
                addActivity("Bulk enrolled students: ${studentIds.joinToString()} into course $courseId")
                _successMessage.value = "Bulk enrollment initiated for ${studentIds.size} student(s) in course $courseId"
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

                // Delete existing timetable entries for this course
                val existingEntries = timetableEntryRepository.getTimetableByCourse(course.courseId)
                existingEntries.forEach { entry ->
                    timetableEntryRepository.delete(entry.entryId)
                }

                // Create and save new timetable entries
                timetableEntries.forEach { entryData ->
                    val startTime = SimpleDateFormat("HH:mm", Locale.US).parse(entryData.startTime)
                    val endTime = SimpleDateFormat("HH:mm", Locale.US).parse(entryData.endTime)
                   
                   // Combine with current date
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