package com.example.autapp.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.autapp.R
import com.example.autapp.data.database.AUTDatabase
import com.example.autapp.data.models.Notification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Class Reminder"
        val text = intent.getStringExtra("text") ?: "Don't forget your class!"
        val notificationId = intent.getIntExtra("notificationId", 0)
        val dayOfWeek = intent.getIntExtra("dayOfWeek", -1) // Ensure you pass this from your scheduler
        val startTimeMillis = intent.getLongExtra("startTimeMillis", 0L)
        val minutesBefore = intent.getIntExtra("minutesBefore", 0)
        val deepLinkUri = intent.getStringExtra("deepLinkUri") ?: "myapp://dashboard"

        // Create and show the notification
        val notification = Notification(
            notificationId = notificationId,
            iconResId = R.drawable.ic_notification, // Your icon
            title = title,
            text = text,
            channelId = NotificationHelper.HIGH_PRIORITY_CHANNEL_ID,
            deepLinkUri = deepLinkUri

        )
        NotificationHelper.pushNotification(context, notification)

        // Log notification in Notifications table
        CoroutineScope(Dispatchers.IO).launch {
            val db = AUTDatabase.getDatabase(context)
            db.notificationDao().insertNotification(notification)
        }

        val startTime = Date(startTimeMillis)

        // Schedule again
        NotificationScheduler.scheduleClassNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            text = text,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            deepLinkUri = deepLinkUri,
            minutesBefore = minutesBefore,
        )
    }
}
