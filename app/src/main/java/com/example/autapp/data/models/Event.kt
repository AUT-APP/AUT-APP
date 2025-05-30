package com.example.autapp.data.models

import java.util.Date

data class Event(
    val eventId: String = "",
    val title: String,
    val date: Date,
    val startTime: Date?,
    val endTime: Date?,
    val location: String?,
    val details: String?,
    val isToDoList: Boolean, // true for todo list items, false for regular events
    val frequency: String?, // "Does not repeat", "Daily", "Weekly", "Monthly", "Yearly"
    val studentId: String, // the id of the user who created the event
    val teacherId: String?, // the id of the user who created the event
    val isTeacherEvent: Boolean // true if the event is a teacher event, false otherwise
) 