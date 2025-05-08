package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.TimetableNotificationPreference

@Dao
interface TimetableNotificationPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePreference(pref: TimetableNotificationPreference)

    @Query("DELETE FROM timetable_notification_prefs WHERE studentId = :studentId AND classSessionId = :classSessionId")
    suspend fun deletePreference(studentId: Int, classSessionId: Int)

    @Query("SELECT * FROM timetable_notification_prefs WHERE studentId = :studentId")
    suspend fun getPreferencesForStudent(studentId: Int): List<TimetableNotificationPreference>

    @Query("SELECT * FROM timetable_notification_prefs WHERE studentId = :studentId AND classSessionId = :classSessionId LIMIT 1")
    suspend fun getPreference(studentId: Int, classSessionId: Int): TimetableNotificationPreference?
}