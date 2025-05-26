package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "teacher_table",
    foreignKeys = [
        ForeignKey(
            entity = Department::class,
            parentColumns = ["departmentId"],
            childColumns = ["departmentId"],
            onDelete = ForeignKey.RESTRICT // Prevents deletion of a department in use
        )
    ],
    indices = [Index("departmentId")]
)
data class Teacher(
    @PrimaryKey(autoGenerate = true) val teacherId: Int = 0,
    val firstName: String,
    val lastName: String,
    val username: String,
    val password: String,
    val departmentId: Int, // References Department entity with type "Department"
    val role: String = "Teacher",
    val officeHours: String,
    val courses: MutableList<String> = mutableListOf(),
    val dob: String
) {
    fun displayTeacherInfo(): String {
        return """
               Name: $firstName $lastName
               Teacher ID: $teacherId
               Department ID: $departmentId
               Office Hours: $officeHours
               Courses: ${courses.joinToString(", ")}
               DOB: $dob
           """.trimIndent()
    }

    override fun toString(): String {
        return "Teacher(firstName='$firstName', lastName='$lastName', username='$username', password='$password', " +
                "teacherId=$teacherId, departmentId=$departmentId, officeHours='$officeHours', courses=$courses, dob='$dob')"
    }
}