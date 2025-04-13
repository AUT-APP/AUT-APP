package com.example.autapp.data.repository

import com.example.autapp.data.dao.StudentDao
import com.example.autapp.data.models.Student
import com.example.autapp.data.models.StudentCourseCrossRef
import com.example.autapp.data.models.StudentWithCourses

class StudentRepository(private val studentDao: StudentDao) {

    suspend fun insertStudent(student: Student) {
        studentDao.insertStudent(student)
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
    }

    suspend fun updateStudent(student: Student) {
        studentDao.updateStudent(student)
    }

    suspend fun getStudentWithCourses(studentId: Int): StudentWithCourses? {
        return studentDao.getStudentWithCourses(studentId)
    }

    suspend fun insertStudentCourseCrossRef(crossRef: StudentCourseCrossRef) {
        studentDao.insertStudentCourseCrossRef(crossRef)
    }

    suspend fun deleteAll() {
        studentDao.deleteAll()
    }

    suspend fun deleteAllCrossRefs() {
        studentDao.deleteAllCrossRefs()
    }
}