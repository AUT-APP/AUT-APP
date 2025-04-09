package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.Teacher

@Dao
interface TeacherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher)

    @Query("SELECT * FROM teacher_table WHERE username = :username")
    suspend fun getTeacherByUsername(username: String): Teacher?

    @Query("SELECT * FROM teacher_table")
    suspend fun getAllTeachers(): List<Teacher>

    @Delete
    suspend fun deleteTeacher(teacher: Teacher)

    @Update
    suspend fun updateTeacher(teacher: Teacher)
}