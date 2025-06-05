package com.example.autapp.data.models

data class Student(
    var id: Int = 0, // Matches User id
    var firstName: String,
    var lastName: String,
    var username: String,
    var password: String,
    var role: String = "Student",
    var studentId: Int,
    var enrollmentDate: String,
    var majorId: Int, // References Department entity with type "Major" (replaces major)
    var minorId: Int? = null, // Optional, references Department entity with type "Minor"
    var yearOfStudy: Int,
    var gpa: Double,
    var dob: String
) {
    override fun toString(): String {
        return "Student(id=$id, firstName='$firstName', lastName='$lastName', role='$role', username='$username', password='$password', " +
                "studentId=$studentId, enrollmentDate='$enrollmentDate', majorId=$majorId, minorId=$minorId, yearOfStudy=$yearOfStudy, gpa=$gpa, dob='$dob')"
    }
}