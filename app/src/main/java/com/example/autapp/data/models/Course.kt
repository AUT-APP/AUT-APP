package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "course_table",
    foreignKeys = [
        ForeignKey(
            entity = Teacher::class,
            parentColumns = ["teacherId"],
            childColumns = ["teacherId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["teacherId"])]
)
data class Course(
    @PrimaryKey(autoGenerate = true) var courseId: Int = 0,
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
