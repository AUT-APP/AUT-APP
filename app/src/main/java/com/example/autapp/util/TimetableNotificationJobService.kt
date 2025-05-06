package com.example.autapp.util

import android.annotation.SuppressLint
import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
@SuppressLint("SpecifyJobSchedulerIdRange")
class TimetableNotificationJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("TimetableNotificationJob", "Job started with params: $params")

        val extras = params?.extras
        val title = extras?.getString("title") ?: "Class Reminder"
        val text = extras?.getString("text") ?: "Don't forget your class!"

        // Construct a lightweight notification (no database access)
        val notification = com.example.autapp.data.models.Notification(
            notificationId = params?.jobId ?: 0,
            iconResId = com.example.autapp.R.drawable.ic_notification, // Replace with your icon
            title = title,
            text = text,
            channelId = NotificationHelper.DEFAULT_CHANNEL_ID
        )

        NotificationHelper.pushNotification(applicationContext, notification)

        return false // No background work
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d("TimetableNotificationJob", "Job stopped before completion")
        return false
    }
}
