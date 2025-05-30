package com.example.autapp.data.models

import java.util.Date

data class TimetableEntry(
    val entryId: Int = 0,
    var courseId: Int,
    var dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    var startTime: Date,
    var endTime: Date,
    var room: String,
    var type: String // Lecture, Lab, Tutorial, etc.
)