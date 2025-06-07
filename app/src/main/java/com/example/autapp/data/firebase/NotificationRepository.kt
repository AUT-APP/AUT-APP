package com.example.autapp.data.firebase

interface NotificationRepository {
    suspend fun getRecentNotificationsForUser(limit: Int, userId: String, isTeacher: Boolean): List<FirebaseNotification>
    suspend fun deleteNotificationsForUser(userId: String, isTeacher: Boolean): Boolean
}
