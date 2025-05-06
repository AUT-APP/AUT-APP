package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "timetable_entry_table",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["courseId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["courseId"])]
)
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true)
    val entryId: Int = 0,
    var courseId: Int,
    var dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    var startTime: Date,
    var endTime: Date,
    var room: String,
    var type: String // Lecture, Lab, Tutorial, etc.
) 