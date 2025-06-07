package com.example.autapp.ui.notification

import com.example.autapp.data.firebase.FirebaseNotification
import com.example.autapp.data.firebase.NotificationRepository

class FakeNotificationRepository() : NotificationRepository  {

    private var notifications = mutableListOf<FirebaseNotification>()

    fun create(notification: FirebaseNotification) {
        notifications.add(notification)
    }

    override suspend fun getRecentNotificationsForUser(
        limit: Int,
        userId: String,
        isTeacher: Boolean
    ): List<FirebaseNotification> {
        return notifications
            .sortedByDescending { it.scheduledDeliveryTime }
            .take(limit)
    }

    override suspend fun deleteNotificationsForUser(userId: String, isTeacher: Boolean): Boolean {
        notifications.clear()
        return true
    }
}