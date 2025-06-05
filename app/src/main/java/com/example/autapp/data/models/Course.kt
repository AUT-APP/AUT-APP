package com.example.autapp.data.models

data class Course(
    var courseId: Int = 0,
    var name: String,
    var title: String,
    var description: String,
    var objectives: String = "",
    var location: String? = null,
    var teacherId: Int
) {
    fun updateCourseDescription(title: String, description: String, objectives: String) {
        this.title = title
        this.description = description
        this.objectives = objectives
    }
}
