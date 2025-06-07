package com.example.autapp.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.autapp.R
import com.example.autapp.data.models.Notification
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.os.PersistableBundle
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri

object NotificationHelper {

    private const val TAG = "NotificationHelper"

    // Channel constants
    const val DEFAULT_CHANNEL_ID = "default_channel"
    const val DEFAULT_CHANNEL_NAME = "Default Notifications"
    const val DEFAULT_CHANNEL_DESC = "General app notifications"

    const val HIGH_PRIORITY_CHANNEL_ID = "high_priority_channel"
    const val HIGH_PRIORITY_CHANNEL_NAME = "High Priority Notifications"
    const val HIGH_PRIORITY_CHANNEL_DESC = "Urgent app notifications"

    /*
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)

            LaunchedEffect(Unit) {
                permissionState.launchPermissionRequest()
            }
        }
    }
    */

    /**
     * Builds and displays a system notification based on notification.
     * Requires POST_NOTIFICATIONS permission on Android 13+.
     */
    fun pushNotification(
        context: Context,
        notification: Notification,
        titleArgs: Array<out Any> = emptyArray(),
        textArgs: Array<out Any> = emptyArray()
    ) {
        val notificationManager = NotificationManagerCompat.from(context)

        // Permission Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Log an error or handle missing permission
            Log.w(TAG, "POST_NOTIFICATIONS permission missing.")
            return
        }

        // Format the text template with dynamic data
        val formattedTitle = notification.getFormattedTitle(*titleArgs)
        val formattedText = notification.getFormattedText(*textArgs)

        // Create PendingIntent for touch action
        val pendingIntent: PendingIntent? = notification.deepLinkUri?.let { uriString ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, uriString.toUri()).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                PendingIntent.getActivity(
                    context,
                    notification.notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } catch (e: Exception) {
                // Handle invalid URI
                println("Error creating PendingIntent from URI: ${e.message}")
                null // Fallback to no intent or a default intent
            }
        }

        // Build the notification
        val builder = NotificationCompat.Builder(context, notification.channelId)
            .setSmallIcon(notification.iconResId)
            .setContentTitle(formattedTitle)
            .setContentText(formattedText)
            .setPriority(notification.priority)
            .setAutoCancel(true) // Dismiss notification when tapped

        // Set content intent if available
        pendingIntent?.let {
            builder.setContentIntent(it)
        }

        // This allows updating/cancelling this specific notification later
        notificationManager.notify(notification.notificationId, builder.build())
    }

    /**
     * Creates notification channels. Call this early, e.g., in Application.onCreate().
     * Safe to call multiple times; creating an existing channel performs no operation.
     */
    fun createNotificationChannels(context: Context) {
        // Default Channel
        val defaultChannel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            DEFAULT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT // Adjust importance as needed
        ).apply {
            description = DEFAULT_CHANNEL_DESC
        }

        // High Priority Channel
        val highPriorityChannel = NotificationChannel(
            HIGH_PRIORITY_CHANNEL_ID,
            HIGH_PRIORITY_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = HIGH_PRIORITY_CHANNEL_DESC
        }

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(defaultChannel)
        notificationManager.createNotificationChannel(highPriorityChannel)
    }

    /**
     * Cancels a specific notification.
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }
}