package com.example.autapp.data.models

import androidx.core.app.NotificationCompat
import java.util.Date

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

    fun toFirebaseNotification(
        userId: String,
        isTeacher: Boolean,
        notificationType: String,
        relatedItemId: String,
        scheduledDeliveryTime: Date,
        channelId: String
    ): com.example.autapp.data.firebase.FirebaseNotification {
        return com.example.autapp.data.firebase.FirebaseNotification(
            userId = userId,
            isTeacher = isTeacher,
            notificationType = notificationType,
            relatedItemId = relatedItemId,
            scheduledDeliveryTime = scheduledDeliveryTime,
            title = this.title,
            text = this.text,
            channelId = channelId
        )
    }
}