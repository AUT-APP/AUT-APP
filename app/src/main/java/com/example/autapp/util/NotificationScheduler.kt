package com.example.autapp.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import java.util.Calendar
import java.util.Date
import androidx.core.net.toUri

object NotificationScheduler {
    const val TAG = "NotificationScheduler"

    fun scheduleClassNotification(
        context: Context,
        notificationId: Int,
        title: String,
        text: String,
        dayOfWeek: Int,
        startTime: Date,
        minutesBefore: Int
    ) {
        Log.d(TAG, "scheduleClassNotification() called")
        val calendar = Calendar.getInstance().apply { time = startTime }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // Calculate the time when the notification should be triggered
        val notifyAtMillis = getNextDateTimeMillis(dayOfWeek, hour, minute) - minutesBefore * 60_000L

        // Create the PendingIntent to trigger the NotificationReceiver
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notificationId", notificationId)
            putExtra("title", title)
            putExtra("text", text)
            putExtra("dayOfWeek", dayOfWeek)
            putExtra("startTimeMillis", startTime.time)
            putExtra("minutesBefore", minutesBefore)
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
            Log.d(TAG, "Scheduled alarm with ID: $notificationId at: $notifyAtMillis")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule alarm: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}", e)
        }
    }

    // Helper method to calculate the next notification time in milliseconds
    fun getNextDateTimeMillis(dayOfWeek: Int, hour: Int, minute: Int): Long {
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
        if (daysUntilTarget == 0 && target.before(now)) {
            daysUntilTarget = 7
        }

        target.add(Calendar.DAY_OF_YEAR, daysUntilTarget)
        return target.timeInMillis
    }
}
