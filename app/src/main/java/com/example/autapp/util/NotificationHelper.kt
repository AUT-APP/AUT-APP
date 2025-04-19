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
import com.example.autapp.ui.DashboardViewModel // Example activity to open

object NotificationHelper {

    // Channel constants (define these preferably in a constants file or companion object)
    const val DEFAULT_CHANNEL_ID = "default_channel"
    const val DEFAULT_CHANNEL_NAME = "Default Notifications"
    const val DEFAULT_CHANNEL_DESC = "General app notifications"

    /**
     * Creates notification channels. Call this early, e.g., in Application.onCreate().
     * Safe to call multiple times; creating an existing channel performs no operation.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Default Channel
            val defaultChannel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                DEFAULT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT // Adjust importance as needed
            ).apply {
                description = DEFAULT_CHANNEL_DESC
                // Configure other channel settings here (sound, vibration, etc.)
            }

            // Add more channels if needed (e.g., for different priorities/types)
            // val highPriorityChannel = ...

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(defaultChannel)
            // notificationManager.createNotificationChannel(highPriorityChannel)
        }
    }

    /**
     * Builds and displays a system notification based on notification.
     * Requires POST_NOTIFICATIONS permission on Android 13+.
     */
    fun pushNotification(context: Context, notification: Notification) {
        val notificationManager = NotificationManagerCompat.from(context)

        // --- Permission Check ---
        // This check should ideally happen *before* calling pushNotification,
        // in the Activity/Fragment that triggers it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Log an error or handle the missing permission appropriately.
            // DO NOT try to request permission from here directly.
            // It's better to inform the user or prevent the action earlier.
            println("Error: POST_NOTIFICATIONS permission missing.")
            return
        }

        // --- Create PendingIntent for touch action ---
        val pendingIntent: PendingIntent? = notification.deepLinkUri?.let { uriString ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                    //setPackage(com.example.autapp.ui.DashboardViewModel)
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
        } // If deepLinkUri is null, pendingIntent will be null.

        // Build the notification
        val builder = NotificationCompat.Builder(context, notification.channelId)
            // Use icon resource ID from stored data
            .setSmallIcon(notification.iconResId)
            .setContentTitle(notification.title)
            .setContentText(notification.text)
            .setPriority(notification.priority)
            .setAutoCancel(true) // Dismiss notification when tapped

        // Set content intent if available
        pendingIntent?.let {
            builder.setContentIntent(it)
        }

        // This allows updating/cancelling this specific notification later.
        notificationManager.notify(notification.notificationId, builder.build())
    }

    /**
     * Cancels a specific notification.
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }
}