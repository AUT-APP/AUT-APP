package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.Assignment

@Dao
interface AssignmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment)

    @Query("SELECT * FROM assignment_table WHERE assignmentId = :assignmentId")
    suspend fun getAssignmentById(assignmentId: Int): Assignment?

    @Query("SELECT * FROM assignment_table WHERE courseId = :courseId")
    suspend fun getAssignmentsByCourse(courseId: Int): List<Assignment>

    @Query("SELECT * FROM assignment_table")
    suspend fun getAllAssignments(): List<Assignment>

    @Delete
    suspend fun deleteAssignment(assignment: Assignment)

    @Update
    suspend fun updateAssignment(assignment: Assignment)

    // Added for sorting as wanted in the acceptance tests
    @Query("SELECT * FROM assignment_table WHERE courseId = :courseId ORDER BY due DESC")
    suspend fun getAssignmentsByCourseSortedByDate(courseId: Int): List<Assignment>
}