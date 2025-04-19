package com.example.autapp.data.models

import androidx.core.app.NotificationCompat
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_table")
class Notification(
    @PrimaryKey(autoGenerate = true)
    var notificationId: Int  = 0,
    var iconResId: Int,
    var title: String,
    var text: String,
    var priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    var deepLinkUri: String,
    var channelId: String,
    ) {
}