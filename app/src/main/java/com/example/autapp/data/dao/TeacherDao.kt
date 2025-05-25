package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.*

@Dao
interface TeacherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher)

    @Query("SELECT * FROM teacher_table WHERE username = :username")
    suspend fun getTeacherByUsername(username: String): Teacher?

    @Query("SELECT * FROM teacher_table WHERE teacherId = :teacherId")
    suspend fun getTeacherById(teacherId: Int): Teacher?

    @Query("SELECT * FROM teacher_table")
    suspend fun getAllTeachers(): List<Teacher>

    @Delete
    suspend fun deleteTeacher(teacher: Teacher)

    @Update
    suspend fun updateTeacher(teacher: Teacher)

    @Transaction
    @Query("""
        SELECT s.* FROM student_table s
        INNER JOIN student_course_cross_ref scr ON s.studentId = scr.studentId
        INNER JOIN course_table c ON scr.courseId = c.courseId
        WHERE c.teacherId = :teacherId AND c.courseId = :courseId
    """)
    suspend fun getStudentsInCourse(teacherId: Int, courseId: Int): List<Student>

    @Transaction
    @Query("""
        SELECT g.* FROM grade_table g
        INNER JOIN assignment_table a ON g.assignmentId = a.assignmentId
        INNER JOIN course_table c ON a.courseId = c.courseId
        WHERE c.teacherId = :teacherId AND c.courseId = :courseId
    """)
    suspend fun getGradesForCourse(teacherId: Int, courseId: Int): List<Grade>

    @Transaction
    @Query("""
        SELECT g.* FROM grade_table g
        INNER JOIN assignment_table a ON g.assignmentId = a.assignmentId
        INNER JOIN course_table c ON a.courseId = c.courseId
        WHERE c.teacherId = :teacherId AND c.courseId = :courseId AND g.studentId = :studentId
    """)
    suspend fun getStudentGradesInCourse(teacherId: Int, courseId: Int, studentId: Int): List<Grade>
}