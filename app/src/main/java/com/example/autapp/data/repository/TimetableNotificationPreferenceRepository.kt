package com.example.autapp.data.repository

import com.example.autapp.data.dao.StudentDao
import com.example.autapp.data.models.TimetableEntry
import com.example.autapp.data.dao.TimetableNotificationPreferenceDao
import com.example.autapp.data.dao.UserDao
import com.example.autapp.data.models.TimetableNotificationPreference

class TimetableNotificationPreferenceRepository  (
    private val timetableNotificationPreferenceDao: TimetableNotificationPreferenceDao,
){

    suspend fun insertOrUpdatePreference(entry: TimetableNotificationPreference) {
        timetableNotificationPreferenceDao.insertOrUpdatePreference(entry)
    }

    suspend fun getPreference(studentId: Int, courseId: Int): TimetableNotificationPreference? {
        return timetableNotificationPreferenceDao.getPreference(studentId, courseId)
    }

}