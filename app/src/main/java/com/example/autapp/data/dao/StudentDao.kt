package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.Student
import com.example.autapp.data.models.StudentCourseCrossRef
import com.example.autapp.data.models.StudentWithCourses

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Query("SELECT * FROM student_table WHERE username = :username")
    suspend fun getStudentByUsername(username: String): Student?

    @Query("SELECT * FROM student_table WHERE studentId = :studentId")
    suspend fun getStudentById(studentId: Int): Student?

    @Query("SELECT * FROM student_table")
    suspend fun getAllStudents(): List<Student>

    @Delete
    suspend fun deleteStudent(student: Student)

    @Update
    suspend fun updateStudent(student: Student)

    @Transaction
    @Query("SELECT * FROM student_table WHERE studentId = :studentId")
    suspend fun getStudentWithCourses(studentId: Int): StudentWithCourses?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentCourseCrossRef(crossRef: StudentCourseCrossRef)

    @Query("DELETE FROM student_table")
    suspend fun deleteAll()

    @Query("DELETE FROM student_course_cross_ref")
    suspend fun deleteAllCrossRefs()
}