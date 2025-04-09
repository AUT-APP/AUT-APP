package com.example.autapp.data.models

import androidx.room.Entity

@Entity(tableName = "teacher_table")
class Teacher(
    firstName: String,
    lastName: String,
    id: Int = 0,
    username: String,
    password: String,
    var department: String,
    var officeHours: String,
    var courses: MutableList<String> = mutableListOf()
) : User(firstName, lastName, id, "Teacher", username, password) {
    fun displayTeacherInfo(): String {
        return """
               Name: $firstName $lastName
               ID: $id
               Role: $role  
               Department: $department
               Office Hours: $officeHours
               Courses: ${courses.joinToString(", ")}
           """.trimIndent()
    }

    override fun toString(): String {
        return "Teacher(firstName='$firstName', lastName='$lastName', id=$id, role='$role', username='$username', password='$password', " +
                "department='$department', officeHours='$officeHours', courses=$courses)"
    }
}