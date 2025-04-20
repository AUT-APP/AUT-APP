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
class Student(
    firstName: String,
    lastName: String,
    id: Int = 0,
    username: String,
    password: String,
    @PrimaryKey var studentId: Int,
    var enrollmentDate: String,
    var major: String,
    var yearOfStudy: Int,
    var gpa: Double
) : User(firstName, lastName, id, "Student", username, password) {
    override fun toString(): String {
        return "Student(firstName='$firstName', lastName='$lastName', id=$id, role='$role', username='$username', password='$password', " +
                "studentId=$studentId, enrollmentDate='$enrollmentDate', major='$major', yearOfStudy=$yearOfStudy, gpa=$gpa)"
    }
}