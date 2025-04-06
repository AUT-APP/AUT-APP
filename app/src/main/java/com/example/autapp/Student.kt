package com.example.autapp
import java.time.LocalDate

class Student(
    var firstName: String,
    var lastName: String,
    var id: Int,
    var role: String,
    var username: String,
    var password: String,
    var studentId: Int,
    var enrollmentDate: String,
    var major: String,
    var yearOfStudy: Int,
    var gpa: Double,
    var courseList: List<String>
): User(firstName, lastName, id, role, password) {
    fun getStudentId(): Int = studentId
    fun getRole(): Int = studentId
}