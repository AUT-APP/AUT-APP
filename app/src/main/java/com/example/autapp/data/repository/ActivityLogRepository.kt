package com.example.autapp.data.repository

import com.example.autapp.data.dao.ActivityLogDao
import com.example.autapp.data.models.ActivityLog
import kotlinx.coroutines.flow.Flow

class ActivityLogRepositoryImpl(
    private val activityLogDao: ActivityLogDao
) : ActivityLogRepository {
    override suspend fun insert(activityLog: ActivityLog) {
        activityLogDao.insert(activityLog)
    }

    override fun getRecentActivities(): Flow<List<ActivityLog>> {
        return activityLogDao.getRecentActivities()
    }

    override suspend fun clearActivities() {
        activityLogDao.clearAll()
    }
}

interface ActivityLogRepository {
    suspend fun insert(activityLog: ActivityLog)
    fun getRecentActivities(): Flow<List<ActivityLog>>
    suspend fun clearActivities()
}