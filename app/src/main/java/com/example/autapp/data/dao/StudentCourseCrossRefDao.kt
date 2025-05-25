package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.StudentCourseCrossRef

@Dao
interface StudentCourseCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: StudentCourseCrossRef)

    @Delete
    suspend fun delete(crossRef: StudentCourseCrossRef)

    @Query("SELECT * FROM student_course_cross_ref WHERE studentId = :studentId")
    suspend fun getByStudentId(studentId: Int): List<StudentCourseCrossRef>

    @Query("SELECT * FROM student_course_cross_ref WHERE courseId = :courseId")
    suspend fun getByCourseId(courseId: Int): List<StudentCourseCrossRef>

}