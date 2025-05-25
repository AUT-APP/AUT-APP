package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.Assignment

@Dao
interface AssignmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment): Long

    @Query("SELECT * FROM assignment_table WHERE assignmentId = :assignmentId")
    suspend fun getAssignmentById(assignmentId: Int): Assignment?

    @Query("SELECT * FROM assignment_table")
    suspend fun getAllAssignments(): List<Assignment>

    @Query("SELECT * FROM assignment_table WHERE courseId IN (SELECT courseId FROM course_table WHERE teacherId = :teacherId)")
    suspend fun getAssignmentsForTeacherCourses(teacherId: Int): List<Assignment>

    @Update
    suspend fun updateAssignment(assignment: Assignment)

    @Delete
    suspend fun deleteAssignment(assignment: Assignment)
}