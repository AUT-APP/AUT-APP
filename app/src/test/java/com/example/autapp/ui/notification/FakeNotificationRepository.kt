package com.example.autapp.ui.notification

import com.example.autapp.data.firebase.FirebaseNotification

class FakeNotificationRepository() {

    private var notifications = mutableListOf<FirebaseNotification>()

    fun setNotifications(newData: List<FirebaseNotification>) {
        notifications  = newData.toMutableList()
    }

    fun create(notification: FirebaseNotification) {
        notifications.add(notification)
    }

    suspend fun getRecentNotificationsForUser(
        limit: Int,
        userId: String,
        isTeacher: Boolean
    ): List<FirebaseNotification> {
        return notifications
            .sortedByDescending { it.scheduledDeliveryTime }
            .take(limit)
    }

    suspend fun deleteNotificationsForUser(userId: String, isTeacher: Boolean): Boolean {
        notifications.clear()
        return true
    }
}