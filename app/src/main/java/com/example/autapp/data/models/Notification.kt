package com.example.autapp.data.models

import androidx.core.app.NotificationCompat

class Notification(
    var notificationId: Int = 0,
    var iconResId: Int,
    var title: String,
    var text: String,
    var priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    var deepLinkUri: String? = null,
    var channelId: String,
    var timestamp: Long = System.currentTimeMillis() // New field with default
) {
    // Method to format the text template with dynamic values
    fun getFormattedTitle(vararg values: Any): String {
        return title.format(*values)
    }
    fun getFormattedText(vararg values: Any): String {
        return text.format(*values)
    }

    fun toFirebaseNotification(): com.example.autapp.data.firebase.FirebaseNotification {
        return com.example.autapp.data.firebase.FirebaseNotification(
            notificationId = this.notificationId,
            iconResId = this.iconResId,
            title = this.title,
            text = this.text,
            priority = this.priority,
            deepLinkUri = this.deepLinkUri,
            channelId = this.channelId,
            timestamp = this.timestamp
        )
    }
}