package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "course_table")
data class Course(
    @PrimaryKey var courseId: Int,
    var name: String,
    var title: String,
    var description: String,
    var location: String? = null
)