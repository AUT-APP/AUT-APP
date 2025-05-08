package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.Notification

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Query("SELECT * FROM notification_table WHERE notificationId = :notificationId")
    suspend fun getNotificationById(notificationId: Int): Notification?

    @Query("SELECT * FROM notification_table")
    suspend fun getAllNotifications(): List<Notification>

    @Delete
    suspend fun deleteNotification(notification: Notification)

    @Update
    suspend fun updateNotification(notification: Notification)

    @Query("DELETE FROM notification_table")
    suspend fun deleteAll()
}