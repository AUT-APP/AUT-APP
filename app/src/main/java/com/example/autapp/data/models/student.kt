package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "student_table",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("id")]
)
data class Student(
    var id: Int = 0,
    var firstName: String,
    var lastName: String,
    var username: String,
    var password: String,
    var role: String = "Student",
    @PrimaryKey var studentId: Int,
    var enrollmentDate: String,
    var major: String,
    var yearOfStudy: Int,
    var gpa: Double
) {
    override fun toString(): String {
        return "Student(id=$id, firstName='$firstName', lastName='$lastName', role='$role', username='$username', password='$password', " +
                "studentId=$studentId, enrollmentDate='$enrollmentDate', major='$major', yearOfStudy=$yearOfStudy, gpa=$gpa)"
    }
}