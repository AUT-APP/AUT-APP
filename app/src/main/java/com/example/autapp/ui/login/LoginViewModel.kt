package com.example.autapp.ui.theme

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autapp.data.models.*
import com.example.autapp.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class LoginViewModel(
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val courseRepository: CourseRepository,
    private val assignmentRepository: AssignmentRepository,
    private val gradeRepository: GradeRepository,
    private val timetableEntryRepository: TimetableEntryRepository
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
                gradeRepository.deleteAll()
                Log.d("LoginViewModel", "Grades deleted")
                assignmentRepository.deleteAll()
                Log.d("LoginViewModel", "Assignments deleted")
                timetableEntryRepository.deleteAll()
                Log.d("LoginViewModel", "Timetable entries deleted")
                courseRepository.deleteAll()
                Log.d("LoginViewModel", "Courses deleted")
                studentRepository.deleteAll()
                Log.d("LoginViewModel", "Students and cross-references deleted")
                userRepository.deleteAll()
                Log.d("LoginViewModel", "Users deleted")

                // Insert test User
                val testUser = User(
                    firstName = "Test",
                    lastName = "Student",
                    role = "Student",
                    username = "teststudent",
                    password = "password123"
                )
                val userIdLong = userRepository.insertUser(testUser)
                val userId = userIdLong.toInt()
                Log.d("LoginViewModel", "Inserted test user: ${testUser.username} with ID: $userId")

                // Insert test Student with matching user ID
                val testStudent = Student(
                    id = userId,
                    firstName = "Test",
                    lastName = "Student",
                    username = "teststudent",
                    password = "password123",
                    role = "Student",
                    studentId = 1001,
                    enrollmentDate = "2024-01-01",
                    major = "Computer Science",
                    yearOfStudy = 2,
                    gpa = 0.0
                )
                studentRepository.insertStudent(testStudent)
                Log.d("LoginViewModel", "Inserted test student: ${testStudent.firstName} ${testStudent.lastName}")

                // Insert Courses
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
                courses.forEach {
                    courseRepository.insertCourse(it)
                    Log.d("LoginViewModel", "Inserted course: ${it.name}")
                }

                // Link Student to Courses
                courses.forEach { course ->
                    studentRepository.insertStudentCourseCrossRef(
                        StudentCourseCrossRef(studentId = 1001, courseId = course.courseId)
                    )
                    Log.d("LoginViewModel", "Linked student 1001 to course ${course.courseId}")
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

                // Past assignments with grades
                val gradeScores = listOf(
                    95.0, 92.0, 88.0, 86.0, 83.0, 81.0, 78.0, 76.0,
                    73.0, 71.0, 68.0, 66.0, 63.0, 58.0, 53.0, 45.0
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
                            grade = "",
                            feedback = "Feedback for ${course.name}"
                        )
                    )
                }

                // Insert assignments
                (upcomingAssignments + pastAssignments.map { it.first }).forEach {
                    assignmentRepository.insertAssignment(it)
                    Log.d("LoginViewModel", "Inserted assignment: ${it.name}")
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
                        Log.d("LoginViewModel", "Inserted grade for assignment: ${assignment.name}")
                    }
                }

                // Create timetable entries
                val csEntries = listOf(
                    TimetableEntry(
                        courseId = 101,
                        dayOfWeek = 1,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 11)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        room = "WB101",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 102,
                        dayOfWeek = 1,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 11)
                            set(Calendar.MINUTE, 30)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 13)
                            set(Calendar.MINUTE, 30)
                        }.time,
                        room = "WB102",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 103,
                        dayOfWeek = 1,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 14)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 16)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        room = "WB103",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 104,
                        dayOfWeek = 2,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 11)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        room = "WB104",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 105,
                        dayOfWeek = 2,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 11)
                            set(Calendar.MINUTE, 30)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 13)
                            set(Calendar.MINUTE, 30)
                        }.time,
                        room = "WB105",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 106,
                        dayOfWeek = 2,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 14)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 16)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        room = "WB106",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 107,
                        dayOfWeek = 3,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 11)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        room = "WB107",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 108,
                        dayOfWeek = 3,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 11)
                            set(Calendar.MINUTE, 30)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 13)
                            set(Calendar.MINUTE, 30)
                        }.time,
                        room = "WB108",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 109,
                        dayOfWeek = 3,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 14)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 16)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        room = "WB109",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 110,
                        dayOfWeek = 4,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 11)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        room = "WB110",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 111,
                        dayOfWeek = 4,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 11)
                            set(Calendar.MINUTE, 30)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 13)
                            set(Calendar.MINUTE, 30)
                        }.time,
                        room = "WB111",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 112,
                        dayOfWeek = 4,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 14)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 16)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        room = "WB112",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 113,
                        dayOfWeek = 5,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 11)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        room = "WB113",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 114,
                        dayOfWeek = 5,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 11)
                            set(Calendar.MINUTE, 30)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 13)
                            set(Calendar.MINUTE, 30)
                        }.time,
                        room = "WB114",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 115,
                        dayOfWeek = 5,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 14)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 16)
                            set(Calendar.MINUTE, 0)
                        }.time,
                        room = "WB115",
                        type = "Lecture"
                    ),
                    TimetableEntry(
                        courseId = 116,
                        dayOfWeek = 1,
                        startTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 16)
                            set(Calendar.MINUTE, 30)
                        }.time,
                        endTime = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 18)
                            set(Calendar.MINUTE, 30)
                        }.time,
                        room = "WB116",
                        type = "Lecture"
                    )
                )

                csEntries.forEach {
                    timetableEntryRepository.insertTimetableEntry(it)
                    Log.d("LoginViewModel", "Inserted timetable entry for course ${it.courseId}")
                }

                // Verify insertions
                val users = userRepository.getAllUsers()
                val students = withContext(Dispatchers.IO) { studentRepository.getAllStudents() }
                Log.d("LoginViewModel", "Inserted ${users.size} users: $users")
                Log.d("LoginViewModel", "Inserted ${students.size} students: $students")

                loginResult = if (students.isNotEmpty()) {
                    "Test data inserted successfully"
                } else {
                    "Test data insertion failed: No students inserted"
                }
            } catch (e: Exception) {
                loginResult = "Test data insertion failed: ${e.message}"
                Log.e("LoginViewModel", "Error inserting test data: ${e.message}", e)
            }
        }
    }
}