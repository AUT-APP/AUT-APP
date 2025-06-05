package com.example.autapp.data.models

data class Teacher(
    val teacherId: Int = 0,
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