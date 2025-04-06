package com.example.autapp

class Teacher(
    firstName: String,
    lastName: String,
    id: Int,
    username: String,
    password: String,
    var department: String,
    var officeHours: String,
    var courses: MutableList<String> = mutableListOf()
) : User(firstName, lastName, id, "Teacher", username, password) {

    fun addCourse(courseName: String) {
        if (!courses.contains(courseName)) {
            courses.add(courseName)
        }
    }

    fun removeCourse(courseName: String) {
        courses.remove(courseName)
    }

    fun getCourses(): List<String> = courses

    fun displayTeacherInfo(): String {
        return """
            Name: ${getFirstName()} ${getLastName()}
            ID: ${getId()}
            Role: ${getRole()}
            Department: $department
            Office Hours: $officeHours
            Courses: ${courses.joinToString(", ")}
        """.trimIndent()
    }
}
