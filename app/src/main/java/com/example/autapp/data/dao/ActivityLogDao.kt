package com.example.autapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.autapp.data.models.ActivityLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Insert
    suspend fun insert(activityLog: ActivityLog)

    @Query("SELECT * FROM activity_log ORDER BY timestamp DESC LIMIT 50")
    fun getRecentActivities(): Flow<List<ActivityLog>>

    @Query("DELETE FROM activity_log")
    suspend fun clearAll()
}