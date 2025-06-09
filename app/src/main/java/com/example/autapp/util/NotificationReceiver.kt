package com.example.autapp.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.autapp.AUTApplication
import com.example.autapp.R
import com.example.autapp.data.datastores.SettingsDataStore
import com.example.autapp.data.models.Notification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

const val TAG = "NotificationReceiver"
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            val title = intent.getStringExtra("title") ?: "Class Reminder"
            val text = intent.getStringExtra("text") ?: "Don't forget your class!"
            val notificationId = intent.getIntExtra("notificationId", 0)
            val dayOfWeek = intent.getIntExtra("dayOfWeek", -1)
            val startTimeMillis = intent.getLongExtra("startTimeMillis", 0L)
            val minutesBefore = intent.getIntExtra("minutesBefore", 0)
            val deepLinkUri = intent.getStringExtra("deepLinkUri") ?: "myapp://dashboard"

            // Extract additional data needed for logging to Firebase
            val userId = intent.getStringExtra("userId") ?: "unknown_user" // You'll need to pass this when scheduling the notification
            val isTeacher = intent.getBooleanExtra("isTeacher", false) // You'll need to pass this when scheduling the notification
            val notificationType = intent.getStringExtra("notificationType") ?: "CLASS_REMINDER" // Define notification types as needed
            val relatedItemId = intent.getStringExtra("relatedItemId") ?: "" // e.g., class ID, assignment ID
            val scheduledDeliveryTime = Date(System.currentTimeMillis()) // Use current time as actual delivery time

            Log.d(TAG, "Received notification ID: $notificationId, dayOfWeek: $dayOfWeek," +
                    " minutesBefore: $minutesBefore")

            val settingsDataStore = SettingsDataStore(context)
            val notificationsEnabled = settingsDataStore.isNotificationsEnabled.first()
            val remindersEnabled = settingsDataStore.isRemindersEnabled.first()

            if (notificationsEnabled && remindersEnabled) {
                // Create and show the notification
                val notification = Notification(
                    notificationId = notificationId,
                    iconResId = R.drawable.ic_notification, // Your icon
                    title = title,
                    text = text,
                    priority = NotificationCompat.PRIORITY_DEFAULT,
                    deepLinkUri = deepLinkUri,
                    channelId = NotificationHelper.HIGH_PRIORITY_CHANNEL_ID,
                    timestamp = System.currentTimeMillis()
                )
                NotificationHelper.pushNotification(context, notification)


                // Log notification in Notifications table
                try {
                    val app = context.applicationContext as AUTApplication
                    val notificationRepository = app.notificationRepository

                    // Pass all required arguments to toFirebaseNotification
                    notificationRepository.create(
                        notification.toFirebaseNotification(
                            userId = userId,
                            isTeacher = isTeacher,
                            notificationType = notificationType,
                            relatedItemId = relatedItemId,
                            scheduledDeliveryTime = scheduledDeliveryTime,
                            channelId = NotificationHelper.HIGH_PRIORITY_CHANNEL_ID
                        )
                    )
                    Log.d(TAG, "Logged notification ID: ${notification.notificationId}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to log notification: ${e.message}", e)
                }
            }

            // Reschedule for next occurrence
            if (dayOfWeek != -1 && startTimeMillis > 0) {
                val startTime = Date(startTimeMillis)
                NotificationScheduler.scheduleClassNotification(
                    context = context,
                    notificationId = notificationId,
                    title = title,
                    text = text,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    deepLinkUri = deepLinkUri,
                    minutesBefore = minutesBefore,
                    userId = userId,
                    isTeacher = isTeacher,
                    notificationType = notificationType,
                    relatedItemId = relatedItemId
                )
            } else {
                Log.w(TAG, "Invalid dayOfWeek or startTime, not rescheduling")
            }
        }
    }
}
