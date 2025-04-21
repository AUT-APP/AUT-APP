package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teacher_table")
data class Teacher(
    var firstName: String,
    var lastName: String,
    var username: String,
    var password: String,
    var role: String = "Teacher",
    @PrimaryKey(autoGenerate = true) var teacherId: Int = 0,
    var department: String,
    var officeHours: String,
    var courses: MutableList<String> = mutableListOf()
) {
    fun displayTeacherInfo(): String {
        return """
               Name: $firstName $lastName
               Teacher ID: $teacherId
               Role: $role  
               Department: $department
               Office Hours: $officeHours
               Courses: ${courses.joinToString(", ")}
           """.trimIndent()
    }

    override fun toString(): String {
        return "Teacher(firstName='$firstName', lastName='$lastName', role='$role', username='$username', password='$password', " +
                "teacherId=$teacherId, department='$department', officeHours='$officeHours', courses=$courses)"
    }
}