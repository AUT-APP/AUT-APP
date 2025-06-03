package com.example.autapp.data.models

data class TimetableNotificationPreference(
    val studentId: Int,
    val classSessionId: Int,
    val enabled: Boolean = true,
    val minutesBefore: Int = 15 // default to 15 minutes before
)