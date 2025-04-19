package com.example.autapp.data.repository

import com.example.autapp.data.dao.NotificationDao
import com.example.autapp.data.models.Notification

class NotificationRepository(private val notificationDao: NotificationDao) {
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
}