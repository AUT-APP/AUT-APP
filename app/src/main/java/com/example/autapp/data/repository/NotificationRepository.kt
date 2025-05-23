package com.example.autapp.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RequiresPermission
import com.example.autapp.data.dao.NotificationDao
import com.example.autapp.data.dao.TimetableNotificationPreferenceDao
import com.example.autapp.data.models.Notification
import com.example.autapp.data.models.TimetableEntry
import com.example.autapp.data.models.TimetableNotificationPreference
import com.example.autapp.util.NotificationHelper
import com.example.autapp.util.NotificationScheduler
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class NotificationRepository(
    private val notificationDao: NotificationDao,
    private val timetableNotificationPreferenceDao: TimetableNotificationPreferenceDao
) {
    suspend fun insertNotification(notification: Notification) {
        notificationDao.insertNotification(notification)
    }

    suspend fun getNotificationById(id: Int): Notification? {
        return notificationDao.getNotificationById(id)
    }

    suspend fun getAllNotifications(): List<Notification> {
        return notificationDao.getAllNotifications()
    }

    suspend fun deleteNotification(notification: Notification) {
        notificationDao.deleteNotification(notification)
    }

    suspend fun updateNotification(notification: Notification) {
        notificationDao.updateNotification(notification)
    }

    suspend fun deleteAll() {
        notificationDao.deleteAll()
    }

    private fun convertDateToLocalDateTime(date: Date): LocalDateTime {
        return date.toInstant()  // Convert Date to Instant
            .atZone(ZoneId.systemDefault())  // Convert Instant to ZonedDateTime
            .toLocalDateTime()  // Convert ZonedDateTime to LocalDateTime
    }

    private fun calculateNotificationTime(startTime: LocalDateTime, minutesBefore: Int): Long {
        // Calculate the time when the notification should fire
        val notifyAt = startTime.minusMinutes(minutesBefore.toLong())
        return notifyAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}