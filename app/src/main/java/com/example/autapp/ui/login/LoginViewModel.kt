package com.example.autapp.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autapp.data.models.*
import com.example.autapp.data.repository.*
import kotlinx.coroutines.launch
import java.util.*

class LoginViewModel(
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val courseRepository: CourseRepository,
    private val assignmentRepository: AssignmentRepository,
    private val gradeRepository: GradeRepository
) : ViewModel() {

    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var loginResult by mutableStateOf<String?>(null)
        private set

    fun updateUsername(newUsername: String) {
        username = newUsername
    }

    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    fun checkLogin() {
        viewModelScope.launch {
            try {
                val isValid = userRepository.checkUser(username, password)
                loginResult = if (isValid) "Login successful" else "Invalid credentials"
            } catch (e: Exception) {
                loginResult = "Login error: ${e.message}"
            }
        }
    }

    fun onLoginSuccess(callback: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val student = studentRepository.getStudentByUsername(username)
                student?.studentId?.let { studentId ->
                    callback(studentId)
                } ?: run {
                    loginResult = "Error: Student not found"
                }
            } catch (e: Exception) {
                loginResult = "Error retrieving student: ${e.message}"
            }
        }
    }

    fun insertTestData() {
        viewModelScope.launch {
            try {
                // Clear existing data
                try {
                    gradeRepository.deleteAll()
                    println("Grades deleted")
                } catch (e: Exception) {
                    println("Failed to delete grades: ${e.message}")
                }
                try {
                    assignmentRepository.deleteAll()
                    println("Assignments deleted")
                } catch (e: Exception) {
                    println("Failed to delete assignments: ${e.message}")
                }
                try {
                    studentRepository.deleteAllCrossRefs()
                    println("Cross-references deleted")
                } catch (e: Exception) {
                    println("Failed to delete cross-references: ${e.message}")
                }
                try {
                    courseRepository.deleteAll()
                    println("Courses deleted")
                } catch (e: Exception) {
                    println("Failed to delete courses: ${e.message}")
                    throw e
                }
                try {
                    studentRepository.deleteAll()
                    println("Students deleted")
                } catch (e: Exception) {
                    println("Failed to delete students: ${e.message}")
                }
                try {
                    userRepository.deleteAll()
                    println("Users deleted")
                } catch (e: Exception) {
                    println("Failed to delete users: ${e.message}")
                }

                // Insert User
                val testUser = User(
                    firstName = "Test",
                    lastName = "Student",
                    role = "Student",
                    username = "teststudent",
                    password = "password123"
                )
                userRepository.insertUser(testUser)

                // Insert Student
                val testStudent = Student(
                    firstName = "Test",
                    lastName = "Student",
                    username = "teststudent",
                    password = "password123",
                    studentId = 1001,
                    enrollmentDate = "2023-01-01",
                    major = "Computer Science",
                    yearOfStudy = 2,
                    gpa = 0.0
                )
                studentRepository.insertStudent(testStudent)

                // Insert Courses (16 courses, 15 credits each = 240 credits)
                val courses = listOf(
                    Course(101, "CS101", "Introduction to Programming", "Basic programming", "WZ101"),
                    Course(102, "CS102", "Data Structures", "Advanced data structures", "WZ102"),
                    Course(103, "CS103", "Algorithms", "Study of algorithms", "WZ103"),
                    Course(104, "CS104", "Operating Systems", "System design", "WZ104"),
                    Course(105, "CS105", "Databases", "Database management", "WZ105"),
                    Course(106, "CS106", "Networking", "Network protocols", "WZ106"),
                    Course(107, "CS107", "Software Engineering", "Software development", "WZ107"),
                    Course(108, "CS108", "Web Development", "Web technologies", "WZ108"),
                    Course(109, "CS109", "Artificial Intelligence", "AI concepts", "WZ109"),
                    Course(110, "CS110", "Machine Learning", "ML algorithms", "WZ110"),
                    Course(111, "CS111", "Cybersecurity", "Security principles", "WZ111"),
                    Course(112, "CS112", "Cloud Computing", "Cloud services", "WZ112"),
                    Course(113, "CS113", "Mobile Development", "Mobile apps", "WZ113"),
                    Course(114, "CS114", "Graphics", "Computer graphics", "WZ114"),
                    Course(115, "CS115", "Parallel Computing", "Parallel systems", "WZ115"),
                    Course(116, "CS116", "Ethics in Computing", "Ethical issues", "WZ116")
                )
                courses.forEach { courseRepository.insertCourse(it) }

                // Link Student to Courses
                courses.forEach { course ->
                    studentRepository.insertStudentCourseCrossRef(
                        StudentCourseCrossRef(studentId = 1001, courseId = course.courseId)
                    )
                }

                // Set up dates
                val calendar = Calendar.getInstance()

                // Upcoming assignments
                val upcomingAssignments = listOf(
                    Assignment(
                        assignmentId = 0,
                        name = "Lab 1",
                        location = "Room WZ101",
                        due = calendar.apply {
                            add(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 17)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        weight = 10.0,
                        maxScore = 100.0,
                        type = "Lab",
                        courseId = 101
                    ),
                    Assignment(
                        assignmentId = 0,
                        name = "Exam 1",
                        location = "Room WZ102",
                        due = calendar.apply {
                            add(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        weight = 20.0,
                        maxScore = 100.0,
                        type = "Exam",
                        courseId = 102
                    )
                )

                // Past assignments with grades (16 grades, diverse types)
                val gradeScores = listOf(
                    95.0, // A+
                    92.0, // A+
                    88.0, // A
                    86.0, // A
                    83.0, // A-
                    81.0, // A-
                    78.0, // B+
                    76.0, // B+
                    73.0, // B
                    71.0, // B
                    68.0, // B-
                    66.0, // B-
                    63.0, // C+
                    58.0, // C
                    53.0, // C-
                    45.0  // D
                )

                val pastAssignments = courses.mapIndexed { index, course ->
                    Pair(
                        Assignment(
                            assignmentId = 0,
                            name = "Final ${course.name}",
                            location = course.location ?: "TBD",
                            due = calendar.apply {
                                time = Date()
                                add(Calendar.DAY_OF_MONTH, -(16 - index))
                            }.time,
                            weight = 100.0,
                            maxScore = 100.0,
                            type = "Final",
                            courseId = course.courseId
                        ),
                        Grade(
                            gradeId = 0,
                            assignmentId = 0,
                            studentId = 1001,
                            _score = gradeScores[index],
                            grade = "", // Auto-set by Grade logic
                            feedback = "Feedback for ${course.name}"
                        )
                    )
                }

                // Insert assignments
                (upcomingAssignments + pastAssignments.map { it.first }).forEach {
                    assignmentRepository.insertAssignment(it)
                }

                // Insert grades
                val insertedAssignments = assignmentRepository.getAllAssignments()
                pastAssignments.forEach { (assignment, grade) ->
                    val insertedAssignment = insertedAssignments.find {
                        it.name == assignment.name && it.courseId == assignment.courseId
                    }
                    if (insertedAssignment != null) {
                        grade.assignmentId = insertedAssignment.assignmentId
                        gradeRepository.insertGrade(grade)
                    }
                }

                loginResult = "Test data inserted successfully"
            } catch (e: Exception) {
                loginResult = "Test data insertion failed: ${e.message}"
                println("Insert test data error: ${e.stackTraceToString()}")
            }
        }
    }
}