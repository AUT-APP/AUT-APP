package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "timetable_notification_prefs",
    primaryKeys = ["studentId", "classSessionId"],
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TimetableEntry::class,
            parentColumns = ["entryId"],  // assuming entryId is the primary key in TimetableEntry
            childColumns = ["classSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TimetableNotificationPreference(
    val studentId: Int,
    val classSessionId: Int,
    val enabled: Boolean = true,
    val minutesBefore: Int = 15 // default to 15 minutes before
)