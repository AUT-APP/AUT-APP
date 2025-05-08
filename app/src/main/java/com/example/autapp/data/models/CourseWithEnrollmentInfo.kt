package com.example.autapp.data.models

import androidx.room.Embedded

data class CourseWithEnrollmentInfo(
    @Embedded val course: Course,
    val year: Int,
    val semester: Int
)