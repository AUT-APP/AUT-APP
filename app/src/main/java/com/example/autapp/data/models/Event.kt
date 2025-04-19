package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "event_table")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val eventId: Int = 0,
    val title: String,
    val date: Date,
    val startTime: Date?,
    val endTime: Date?,
    val location: String?,
    val details: String?,
    val isToDoList: Boolean, // true for todo list items, false for regular events
    val frequency: String?, // "Does not repeat", "Daily", "Weekly", "Monthly", "Yearly"
    val studentId: Int // to associate events with specific students
) 