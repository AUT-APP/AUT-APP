package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "course_table")
data class Course(
    @PrimaryKey var courseId: Int, // not auto-generated as it's a course code which'll manually be added
    var name: String,
    var title: String,
    var description: String
)