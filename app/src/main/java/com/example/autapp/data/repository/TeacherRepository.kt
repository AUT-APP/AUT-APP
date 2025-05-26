package com.example.autapp.data.repository

import android.util.Log
import com.example.autapp.data.dao.TeacherDao
import com.example.autapp.data.dao.UserDao
import com.example.autapp.data.models.Teacher
import com.example.autapp.data.models.User

class TeacherRepository(
    private val teacherDao: TeacherDao,
    private val userDao: UserDao
) {
    suspend fun insertTeacher(teacher: Teacher) {
        // Check if user already exists
        val existingUser = userDao.getUserByUsername(teacher.username)
        if (existingUser == null) {
            // Insert user if it doesn't exist
            val user = User(
                firstName = teacher.firstName,
                lastName = teacher.lastName,
                username = teacher.username,
                password = teacher.password,
                role = teacher.role
            )
            userDao.insertUser(user)
            Log.d("TeacherRepository", "Inserted new user: ${teacher.username}")
        } else {
            Log.d("TeacherRepository", "User ${teacher.username} already exists, skipping user insertion")
        }

        // Insert teacher
        teacherDao.insertTeacher(teacher)
        Log.d("TeacherRepository", "Inserted teacher: ${teacher.firstName} ${teacher.lastName}")
    }

    suspend fun getTeacherByUsername(username: String): Teacher? {
        return teacherDao.getTeacherByUsername(username)
    }

    suspend fun getTeacherById(teacherId: Int): Teacher? {
        return teacherDao.getTeacherById(teacherId)
    }

    suspend fun getTeacherByTeacherId(teacherId: Int): Teacher? {
        return teacherDao.getTeacherByTeacherId(teacherId)
    }

    suspend fun getAllTeachers(): List<Teacher> {
        return teacherDao.getAllTeachers()
    }

    suspend fun deleteTeacher(teacher: Teacher) {
        teacherDao.deleteTeacher(teacher)
        val user = userDao.getUserByUsername(teacher.username)
        user?.let {
            userDao.deleteUser(it)
            Log.d("TeacherRepository", "Deleted user: ${teacher.username}")
        }
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
}