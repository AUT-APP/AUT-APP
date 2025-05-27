package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.Assignment
import com.example.autapp.data.models.Grade

@Dao
interface GradeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: Grade)

    @Query("SELECT * FROM grade_table WHERE gradeId = :gradeId")
    suspend fun getGradeById(gradeId: Int): Grade?

    @Query("SELECT * FROM grade_table WHERE assignmentId = :assignmentId")
    suspend fun getGradesByAssignment(assignmentId: Int): List<Grade>

    @Query("SELECT * FROM grade_table WHERE studentId = :studentId")
    suspend fun getGradesByStudent(studentId: Int): List<Grade>

    @Query("SELECT * FROM grade_table")
    suspend fun getAllGrades(): List<Grade>

    @Update
    suspend fun updateGrade(grade: Grade)

    data class GradeWithAssignment(
        @Embedded val grade: Grade,
        @Relation(
            parentColumn = "assignmentId",
            entityColumn = "assignmentId"
        )
        val assignment: Assignment
    )

    @Transaction
    @Query("SELECT * FROM grade_table WHERE studentId = :studentId")
    suspend fun getGradesWithAssignments(studentId: Int): List<GradeWithAssignment>

    @Transaction
    @Query("SELECT * FROM grade_table WHERE studentId = :studentId ORDER BY (SELECT due FROM assignment_table WHERE assignment_table.assignmentId = grade_table.assignmentId) DESC")
    suspend fun getGradesWithAssignmentsSortedByDate(studentId: Int): List<GradeWithAssignment>

    @Transaction
    @Query("SELECT * FROM grade_table WHERE studentId = :studentId AND assignmentId IN (SELECT assignmentId FROM assignment_table WHERE courseId = :courseId)")
    suspend fun getGradesWithAssignmentsByCourse(studentId: Int, courseId: Int): List<GradeWithAssignment>

    @Transaction
    @Query("SELECT * FROM grade_table WHERE studentId = :studentId AND assignmentId IN (SELECT assignmentId FROM assignment_table WHERE type = :type)")
    suspend fun getGradesWithAssignmentsByType(studentId: Int, type: String): List<GradeWithAssignment>

}