package com.example.autapp.data.repository

import android.util.Log
import androidx.room.Transaction
import com.example.autapp.data.dao.TeacherDao
import com.example.autapp.data.dao.UserDao
import com.example.autapp.data.models.Student
import com.example.autapp.data.models.Teacher
import com.example.autapp.data.models.User

class TeacherRepository(
    private val teacherDao: TeacherDao,
    private val userDao: UserDao
) {
    @Transaction
    suspend fun insertTeacher(teacher: Teacher) {
        // Check if user already exists
        val existingUser = userDao.getUserByUsername(teacher.username)
        var userId: Int
        if (existingUser == null) {
            // Insert user and get the generated ID
            val user = User(
                firstName = teacher.firstName,
                lastName = teacher.lastName,
                username = teacher.username,
                password = teacher.password,
                role = teacher.role
            )
            userId = userDao.insertUser(user).toInt()
            Log.d("TeacherRepository", "Inserted new user: ${teacher.username} with ID: $userId")
        } else {
            userId = existingUser.id
            Log.d("TeacherRepository", "User ${teacher.username} already exists with ID: $userId")
        }

        // Set the teacher's ID to match the user's ID
        val teacherWithUserId = teacher.copy(teacherId = userId)
        teacherDao.insertTeacher(teacherWithUserId)
        Log.d("TeacherRepository", "Inserted teacher: ${teacher.firstName} ${teacher.lastName} with ID: $userId")
    }

    suspend fun getTeacherByUsername(username: String): Teacher? {
        return teacherDao.getTeacherByUsername(username)
    }

    suspend fun getTeacherById(teacherId: Int): Teacher? {
        return teacherDao.getTeacherById(teacherId)
    }

    suspend fun getAllTeachers(): List<Teacher> {
        return teacherDao.getAllTeachers()
    }

    suspend fun updateTeacher(teacher: Teacher) {
        teacherDao.updateTeacher(teacher)
        val user = userDao.getUserByUsername(teacher.username)
        user?.let {
            val updatedUser = user.copy(
                firstName = teacher.firstName,
                lastName = teacher.lastName,
                username = teacher.username,
                password = teacher.password,
                role = teacher.role
            )
            userDao.updateUser(updatedUser)
            Log.d("TeacherRepository", "Updated user: ${teacher.username}")
        } ?: Log.w("TeacherRepository", "No user found for username: ${teacher.username}")
    }

    suspend fun getStudentsForCourse(teacherId: Int, courseId: Int): List<Student> {
        return teacherDao.getStudentsInCourse(teacherId, courseId)
    }

    @Transaction
    suspend fun deleteTeacher(teacher: Teacher) {
        // Delete teacher
        teacherDao.deleteTeacher(teacher)
        // Delete associated user
        userDao.getUserByUsername(teacher.username)?.let {
            userDao.deleteUser(it)
            Log.d("TeacherRepository", "Deleted user: ${teacher.username}")
        }
        Log.d("TeacherRepository", "Deleted teacher: ${teacher.firstName} ${teacher.lastName} (ID: ${teacher.teacherId})")
    }
}