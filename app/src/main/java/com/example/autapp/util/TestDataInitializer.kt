package com.example.autapp.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.autapp.AUTApplication
import com.example.autapp.data.models.Assignment
import com.example.autapp.data.models.Course
import com.example.autapp.data.models.Grade
import com.example.autapp.data.models.Student
import com.example.autapp.data.models.StudentCourseCrossRef
import com.example.autapp.data.models.TimetableEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object TestDataInitializer {
    val TAG = "TestDataInitializer"
    suspend fun insertTestData(context: Context) {
        val app = context.applicationContext as AUTApplication
        val userRepository = app.userRepository
        val studentRepository = app.studentRepository
        val courseRepository = app.courseRepository
        val assignmentRepository = app.assignmentRepository
        val gradeRepository = app.gradeRepository
        val timetableEntryRepository = app.timetableEntryRepository
        val database = app.database
        try {
            // Clear database to prevent duplicates
            withContext(Dispatchers.IO) {
                database.clearAllTables()
                Log.d(TAG, "Database cleared")
            }
            // Insert test students
            val students = listOf(
                Student(
                    firstName = "Test",
                    lastName = "Student",
                    studentId = 1000,
                    username = "teststudent",
                    password = "password123",
                    role = "Student",
                    enrollmentDate = "2024-01-01",
                    major = "Computer Science",
                    yearOfStudy = 2,
                    gpa = 0.0
                ),
                Student(
                    firstName = "John",
                    lastName = "Doe",
                    studentId = 1,
                    username = "john_doe",
                    password = "password123",
                    enrollmentDate = "2023-09-01",
                    major = "Computer Science",
                    yearOfStudy = 2,
                    gpa = 3.5
                ),
                Student(
                    firstName = "Jane",
                    lastName = "Smith",
                    studentId = 2,
                    username = "jane_smith",
                    password = "password123",
                    enrollmentDate = "2022-09-01",
                    major = "Engineering",
                    yearOfStudy = 3,
                    gpa = 3.8
                ),
                Student(
                    firstName = "Alex",
                    lastName = "Johnson",
                    studentId = 3,
                    username = "alex_johnson",
                    password = "password123",
                    enrollmentDate = "2024-02-01",
                    major = "Business",
                    yearOfStudy = 1,
                    gpa = 3.2
                ),
                Student(
                    firstName = "Emma",
                    lastName = "Brown",
                    studentId = 4,
                    username = "emma_brown",
                    password = "password123",
                    enrollmentDate = "2023-02-01",
                    major = "Psychology",
                    yearOfStudy = 2,
                    gpa = 3.6
                )
            )

            withContext(Dispatchers.IO) {
                students.forEach { student ->
                    studentRepository.insertStudent(student)
                    Log.d(
                        TAG,
                        "Inserted student: ${student.firstName} ${student.lastName} (ID: ${student.studentId})"
                    )
                }
            }

            // Insert test courses
            val courses = listOf(
                Course(
                    courseId = 1,
                    name = "COMP101",
                    title = "Introduction to Programming",
                    description = "Basics of programming in Python",
                    location = "WG-301"
                ),
                Course(
                    courseId = 2,
                    name = "ENG201",
                    title = "Engineering Mechanics",
                    description = "Fundamentals of statics and dynamics",
                    location = "WA-402"
                ),
                Course(
                    courseId = 3,
                    name = "BUS301",
                    title = "Business Management",
                    description = "Principles of management and leadership",
                    location = "WZ-503"
                )
            )

            withContext(Dispatchers.IO) {
                courses.forEach { course ->
                    courseRepository.insertCourse(course)
                    Log.d(
                        TAG,
                        "Inserted course: ${course.name} (ID: ${course.courseId})"
                    )
                }
            }

            // Insert student-course relationships
            val studentCourseCrossRefs = listOf(
                StudentCourseCrossRef(
                    studentId = 1000,
                    courseId = 1,
                    year = 2025,
                    semester = 1
                ), // Test student enrolled in COMP101
                StudentCourseCrossRef(studentId = 1, courseId = 1, year = 2025, semester = 1),
                StudentCourseCrossRef(studentId = 1, courseId = 2, year = 2025, semester = 1),
                StudentCourseCrossRef(studentId = 1, courseId = 3, year = 2025, semester = 1),
                StudentCourseCrossRef(studentId = 2, courseId = 2, year = 2025, semester = 1),
                StudentCourseCrossRef(studentId = 3, courseId = 3, year = 2025, semester = 1),
                StudentCourseCrossRef(studentId = 4, courseId = 1, year = 2025, semester = 1)
            )

            withContext(Dispatchers.IO) {
                studentCourseCrossRefs.forEach { crossRef ->
                    studentRepository.insertStudentCourseCrossRef(crossRef)
                    Log.d(
                        TAG,
                        "Inserted student-course: studentId=${crossRef.studentId}, courseId=${crossRef.courseId}"
                    )
                }
            }

            // Insert test assignments
            val calendar = Calendar.getInstance()
            val assignments = listOf(
                Assignment(
                    assignmentId = 1,
                    name = "Python Project",
                    location = "WG-301",
                    due = calendar.apply { add(Calendar.DAY_OF_MONTH, 7) }.time,
                    weight = 0.4,
                    maxScore = 100.0,
                    type = "Project",
                    courseId = 1
                ),
                Assignment(
                    assignmentId = 2,
                    name = "Quiz 1",
                    location = "WG-301",
                    due = calendar.apply { add(Calendar.DAY_OF_MONTH, 3) }.time,
                    weight = 0.2,
                    maxScore = 50.0,
                    type = "Quiz",
                    courseId = 1
                ),
                Assignment(
                    assignmentId = 3,
                    name = "Mechanics Lab",
                    location = "WA-402",
                    due = calendar.apply { add(Calendar.DAY_OF_MONTH, 5) }.time,
                    weight = 0.3,
                    maxScore = 80.0,
                    type = "Lab",
                    courseId = 2
                ),
                Assignment(
                    assignmentId = 4,
                    name = "Case Study",
                    location = "WZ-503",
                    due = calendar.apply { add(Calendar.DAY_OF_MONTH, 10) }.time,
                    weight = 0.25,
                    maxScore = 60.0,
                    type = "Case Study",
                    courseId = 3
                )
            )

            withContext(Dispatchers.IO) {
                assignments.forEach { assignment ->
                    assignmentRepository.insertAssignment(assignment)
                    Log.d(
                        TAG,
                        "Inserted assignment: ${assignment.name} (ID: ${assignment.assignmentId})"
                    )
                }
            }

            // Insert test grades
            val grades = listOf(
                Grade(
                    assignmentId = 1,
                    studentId = 1000, // Test student grade
                    _score = 90.0,
                    grade = "A",
                    feedback = "Excellent project submission."
                ),
                Grade(
                    assignmentId = 1,
                    studentId = 1,
                    _score = 85.0,
                    grade = "A",
                    feedback = "Great work on the project!"
                ),
                Grade(
                    assignmentId = 2,
                    studentId = 1,
                    _score = 45.0,
                    grade = "A+",
                    feedback = "Excellent quiz performance."
                ),
                Grade(
                    assignmentId = 3,
                    studentId = 2,
                    _score = 60.0,
                    grade = "B+",
                    feedback = "Good lab report, improve calculations."
                ),
                Grade(
                    assignmentId = 4,
                    studentId = 3,
                    _score = 45.0,
                    grade = "C+",
                    feedback = "Adequate analysis, needs more depth."
                ),
                Grade(
                    assignmentId = 1,
                    studentId = 4,
                    _score = 88.0,
                    grade = "A",
                    feedback = "Solid project submission."
                )
            )

            withContext(Dispatchers.IO) {
                grades.forEach { grade ->
                    gradeRepository.insertGrade(grade)
                    Log.d(
                        TAG,
                        "Inserted grade: studentId=${grade.studentId}, assignmentId=${grade.assignmentId}, score=${grade.score}"
                    )
                }
            }

            // Create timetable entries
            val csEntries = listOf(
                TimetableEntry(
                    courseId = 1,
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
                    courseId = 2,
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
                    courseId = 3,
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
                    courseId = 1,
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
                    courseId = 2,
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
                    courseId = 3,
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
                    courseId = 1,
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
                    courseId = 2,
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
                    courseId = 3,
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
                    courseId = 1,
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
                    courseId = 2,
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
                    courseId = 3,
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
                    courseId = 1,
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
                    courseId = 2,
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
                    courseId = 3,
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
                    courseId = 1,
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
                ),
                TimetableEntry(
                    courseId = 1,
                    dayOfWeek = 5,
                    startTime = calendar.apply {
                        set(Calendar.HOUR_OF_DAY, 13)
                        set(Calendar.MINUTE, 24)
                    }.time,
                    endTime = calendar.apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                    }.time,
                    room = "WB116",
                    type = "Lecture"
                ),
            )

            csEntries.forEach {
                timetableEntryRepository.insertTimetableEntry(it)
                Log.d(TAG, "Inserted timetable entry for course ${it.courseId}")
            }
            // Verify insertions
            val users = userRepository.getAllUsers()
            val retrievedStudents =
                withContext(Dispatchers.IO) { studentRepository.getAllStudents() }
            Log.d(TAG, "Inserted ${users.size} users: $users")
            Log.d(
                TAG,
                "Inserted ${retrievedStudents.size} students: $retrievedStudents"
            )

            if (retrievedStudents.isEmpty()) {
                Log.e(TAG, "No students were inserted!")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Failed to insert test students!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting test data: ${e.message}", e)
        }
    }
}