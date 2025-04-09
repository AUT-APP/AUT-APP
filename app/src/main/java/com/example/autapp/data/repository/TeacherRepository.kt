package com.example.autapp.data.repository

import com.example.autapp.data.dao.TeacherDao
import com.example.autapp.data.models.Teacher

class TeacherRepository(private val teacherDao: TeacherDao) {

    suspend fun insertTeacher(teacher: Teacher) {
        teacherDao.insertTeacher(teacher)
    }

    suspend fun getTeacherByUsername(username: String): Teacher? {
        return teacherDao.getTeacherByUsername(username)
    }

    suspend fun getAllTeachers(): List<Teacher> {
        return teacherDao.getAllTeachers()
    }

    suspend fun deleteTeacher(teacher: Teacher) {
        teacherDao.deleteTeacher(teacher)
    }

    suspend fun updateTeacher(teacher: Teacher) {
        teacherDao.updateTeacher(teacher)
    }
}