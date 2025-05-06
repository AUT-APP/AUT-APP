package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.Course
import com.example.autapp.data.models.TimetableEntry

@Dao
interface CourseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    @Query("SELECT * FROM course_table WHERE name = :name")
    suspend fun getCourseByName(name: String): Course?

    @Query("SELECT * FROM course_table WHERE courseId = :courseId")
    suspend fun getCourseById(courseId: Int): Course?

    @Query("SELECT * FROM course_table")
    suspend fun getAllCourses(): List<Course>

    @Query("SELECT * FROM timetable_entry_table WHERE courseId = :courseId")
    fun getTimetableForCourse(courseId: Int): List<TimetableEntry>

    @Delete
    suspend fun deleteCourse(course: Course)

    @Update
    suspend fun updateCourse(course: Course)

    @Query("DELETE FROM course_table")
    suspend fun deleteAll()
}