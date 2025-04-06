data class Teacher(
    val id: String,
    var name: String,
    var email: String,
    var department: String,
    var officeHours: String,
    var courses: MutableList<String> = mutableListOf()
) {


    fun displayInfo(): String {
        return """
            Name: $name
            Email: $email
            Department: $department
            Office Hours: $officeHours
            Courses: ${courses.joinToString(", ")}
        """.trimIndent()
    }
}
