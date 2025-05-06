package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.Event
import java.util.Date

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event)

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("SELECT * FROM event_table WHERE eventId = :eventId")
    suspend fun getEventById(eventId: Int): Event?

    @Query("SELECT * FROM event_table WHERE studentId = :studentId ORDER BY date, startTime")
    suspend fun getEventsByStudent(studentId: Int): List<Event>

    @Query("SELECT * FROM event_table WHERE studentId = :studentId AND date = :date ORDER BY startTime")
    suspend fun getEventsByDate(studentId: Int, date: Date): List<Event>

    @Query("SELECT * FROM event_table WHERE studentId = :studentId AND isToDoList = :isToDoList ORDER BY date, startTime")
    suspend fun getEventsByType(studentId: Int, isToDoList: Boolean): List<Event>

    @Query("SELECT * FROM event_table WHERE studentId = :studentId AND date BETWEEN :startDate AND :endDate ORDER BY date, startTime")
    suspend fun getEventsBetweenDates(studentId: Int, startDate: Date, endDate: Date): List<Event>

    @Query("DELETE FROM event_table WHERE studentId = :studentId")
    suspend fun deleteAllEventsByStudent(studentId: Int)
} 