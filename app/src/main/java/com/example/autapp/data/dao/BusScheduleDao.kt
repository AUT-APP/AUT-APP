package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.ui.transport.BusSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface BusScheduleDao {
    @Query("SELECT * FROM bus_schedules ORDER BY departureTime")
    fun getAllSchedules(): Flow<List<BusSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<BusSchedule>)

    @Query("DELETE FROM bus_schedules")
    suspend fun deleteAll()
} 