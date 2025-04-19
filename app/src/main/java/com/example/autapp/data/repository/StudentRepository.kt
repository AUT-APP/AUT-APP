package com.example.autapp.data.repository

import com.example.autapp.data.dao.StudentDao
import com.example.autapp.data.dao.UserDao
import com.example.autapp.data.models.Student
import com.example.autapp.data.models.StudentCourseCrossRef
import com.example.autapp.data.models.StudentWithCourses
import com.example.autapp.data.models.User

class StudentRepository(
    private val studentDao: StudentDao,
    private val userDao: UserDao
) {

    suspend fun insertStudent(student: Student) {
        // First insert the user record
        val user = User(
            firstName = student.firstName,
            lastName = student.lastName,
            id = student.id,
            role = student.role,
            username = student.username,
            password = student.password
        )
        userDao.insertUser(user)
        
        // Then insert the student record
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