package com.example.autapp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar
import java.util.Date

object NotificationScheduler {
    const val TAG = "NotificationScheduler"

    fun scheduleClassNotification(
        context: Context,
        deepLinkUri: String,
        notificationId: Int,
        title: String,
        text: String,
        dayOfWeek: Int,
        startTime: Date,
        minutesBefore: Int
    ) {
        Log.d(TAG, "Scheduling notification ID: $notificationId for dayOfWeek: $dayOfWeek, minutesBefore: $minutesBefore")
        val calendar = Calendar.getInstance().apply { time = startTime }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // Calculate the time when the notification should be triggered
        val notifyAtMillis = getNextDateTimeMillis(dayOfWeek, hour, minute, minutesBefore)

        // Create the PendingIntent to trigger the NotificationReceiver
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notificationId", notificationId)
            putExtra("title", title)
            putExtra("text", text)
            putExtra("dayOfWeek", dayOfWeek)
            putExtra("startTimeMillis", startTime.time)
            putExtra("minutesBefore", minutesBefore)
            putExtra("deepLinkUri", deepLinkUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the alarm using AlarmManager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                notifyAtMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled alarm ID: $notificationId for ${Date(notifyAtMillis)}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule alarm: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}", e)
        }
    }

    // Helper method to calculate the next notification time in milliseconds
    fun getNextDateTimeMillis(dayOfWeek: Int, hour: Int, minute: Int, minutesBefore: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val today = now.get(Calendar.DAY_OF_WEEK)
        val adjustedDayOfWeek = dayOfWeek + 1
        var daysUntilTarget = (adjustedDayOfWeek - today + 7) % 7

        // Move to next week if target time is in the past or now
        if (daysUntilTarget == 0 && target.timeInMillis <= now.timeInMillis) {
            daysUntilTarget = 7
        }

        target.add(Calendar.DAY_OF_YEAR, daysUntilTarget)
        val targetMillis = target.timeInMillis - minutesBefore * 60_000L

        // Ensure the final notification time is at least 30 seconds in the future
        if (targetMillis <= now.timeInMillis + 30_000L) {
            target.add(Calendar.DAY_OF_YEAR, 7) // Move to next week
            Log.d(TAG, "Target time was too close to now, rescheduling for next week: ${target.time}")
        }

        return target.timeInMillis - minutesBefore * 60_000L
    }
}
