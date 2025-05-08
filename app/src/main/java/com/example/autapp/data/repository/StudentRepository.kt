package com.example.autapp.data.repository

import android.util.Log
import androidx.room.Transaction
import com.example.autapp.data.dao.StudentDao
import com.example.autapp.data.models.CourseWithEnrollmentInfo
import com.example.autapp.data.dao.UserDao
import com.example.autapp.data.models.Student
import com.example.autapp.data.models.StudentCourseCrossRef
import com.example.autapp.data.models.StudentWithCourses
import com.example.autapp.data.models.User

class StudentRepository(
    private val studentDao: StudentDao,
    private val userDao: UserDao
) {
    @Transaction
    suspend fun insertStudent(student: Student) {
        // Check if user already exists
        val existingUser = userDao.getUserByUsername(student.username)
        var userId: Int
        if (existingUser == null) {
            // Insert user and get the generated ID
            val user = User(
                firstName = student.firstName,
                lastName = student.lastName,
                username = student.username,
                password = student.password,
                role = student.role
            )
            userId = userDao.insertUser(user).toInt()
            Log.d("StudentRepository", "Inserted new user: ${student.username} with ID: $userId")
        } else {
            userId = existingUser.id
            Log.d("StudentRepository", "User ${student.username} already exists with ID: $userId")
        }

        // Set the student's ID to match the user's ID
        val studentWithUserId = student.copy(id = userId)
        studentDao.insertStudent(studentWithUserId)
        Log.d("StudentRepository", "Inserted student: ${student.firstName} ${student.lastName} with ID: $userId")
    }

    suspend fun getStudentByUsername(username: String): Student? {
        return studentDao.getStudentByUsername(username)
    }

    suspend fun getStudentById(studentId: Int): Student? {
        return studentDao.getStudentById(studentId)
    }

    suspend fun getAllStudents(): List<Student> {
        return studentDao.getAllStudents()
    }

    suspend fun deleteStudent(student: Student) {
        studentDao.deleteStudent(student)
        val user = userDao.getUserByUsername(student.username)
        user?.let {
            userDao.deleteUser(it)
            Log.d("StudentRepository", "Deleted user: ${student.username}")
        }
    }

    suspend fun updateStudent(student: Student) {
        studentDao.updateStudent(student)
        val user = userDao.getUserByUsername(student.username)
        user?.let {
            val updatedUser = user.copy(
                firstName = student.firstName,
                lastName = student.lastName,
                username = student.username,
                password = student.password,
                role = student.role
            )
            userDao.updateUser(updatedUser)
            Log.d("StudentRepository", "Updated user: ${student.username}")
        } ?: Log.w("StudentRepository", "No user found for username: ${student.username}")
    }

    suspend fun getStudentWithCourses(studentId: Int): StudentWithCourses? {
        return studentDao.getStudentWithCourses(studentId)
    }

    suspend fun getStudentCoursesWithEnrollmentInfo(studentId: Int): List<CourseWithEnrollmentInfo> {
        return studentDao.getStudentCoursesWithEnrollmentInfo(studentId)
    }

    suspend fun insertStudentCourseCrossRef(crossRef: StudentCourseCrossRef) {
        studentDao.insertStudentCourseCrossRef(crossRef)
    }

    suspend fun deleteAll() {
        studentDao.deleteAll()
        Log.d("StudentRepository", "Deleted all students and cross-references")
    }

    suspend fun getStudentCount(): Int {
        val count = studentDao.getAllStudents().size
        Log.d("StudentRepository", "Student count: $count")
        return count
    }
}