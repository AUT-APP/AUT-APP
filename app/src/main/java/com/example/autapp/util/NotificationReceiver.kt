package com.example.autapp.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.autapp.R
import com.example.autapp.data.models.Notification
import java.util.Date

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Class Reminder"
        val text = intent.getStringExtra("text") ?: "Don't forget your class!"
        val notificationId = intent.getIntExtra("notificationId", 0)
        val dayOfWeek = intent.getIntExtra("dayOfWeek", -1) // Ensure you pass this from your scheduler
        val startTimeMillis = intent.getLongExtra("startTimeMillis", 0L)
        val minutesBefore = intent.getIntExtra("minutesBefore", 0)

        // Create and show the notification
        val notification = Notification(
            notificationId = notificationId,
            iconResId = R.drawable.ic_notification, // Your icon
            title = title,
            text = text,
            channelId = NotificationHelper.HIGH_PRIORITY_CHANNEL_ID
        )
        NotificationHelper.pushNotification(context, notification)

        val startTime = Date(startTimeMillis)

        // Schedule again
        NotificationScheduler.scheduleClassNotification(
            context,
            notificationId,
            title,
            text,
            dayOfWeek,
            startTime,
            minutesBefore
        )
    }
}
