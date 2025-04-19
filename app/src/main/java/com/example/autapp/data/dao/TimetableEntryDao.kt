package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.TimetableEntry
import com.example.autapp.data.models.Course

@Dao
interface TimetableEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableEntry(entry: TimetableEntry)

    @Query("SELECT * FROM timetable_entry_table WHERE entryId = :entryId")
    suspend fun getTimetableEntryById(entryId: Int): TimetableEntry?

    @Query("SELECT * FROM timetable_entry_table WHERE courseId = :courseId")
    suspend fun getTimetableEntriesByCourse(courseId: Int): List<TimetableEntry>

    @Query("SELECT * FROM timetable_entry_table WHERE dayOfWeek = :dayOfWeek")
    suspend fun getTimetableEntriesByDay(dayOfWeek: Int): List<TimetableEntry>

    @Query("SELECT * FROM timetable_entry_table")
    suspend fun getAllTimetableEntries(): List<TimetableEntry>

    @Delete
    suspend fun deleteTimetableEntry(entry: TimetableEntry)

    @Update
    suspend fun updateTimetableEntry(entry: TimetableEntry)

    data class TimetableEntryWithCourse(
        @Embedded val entry: TimetableEntry,
        @Relation(
            parentColumn = "courseId",
            entityColumn = "courseId"
        )
        val course: Course
    )

    @Transaction
    @Query("SELECT * FROM timetable_entry_table WHERE dayOfWeek = :dayOfWeek ORDER BY startTime")
    suspend fun getTimetableEntriesWithCourseByDay(dayOfWeek: Int): List<TimetableEntryWithCourse>

    @Query("DELETE FROM timetable_entry_table")
    suspend fun deleteAll()
} 