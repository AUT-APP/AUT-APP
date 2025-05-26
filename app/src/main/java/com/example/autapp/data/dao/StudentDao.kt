package com.example.autapp.data.dao

import androidx.room.*
import com.example.autapp.data.models.CourseWithEnrollmentInfo
import com.example.autapp.data.models.Student
import com.example.autapp.data.models.StudentCourseCrossRef
import com.example.autapp.data.models.StudentWithCourses

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Query("SELECT * FROM student_table WHERE username = :username")
    suspend fun getStudentByUsername(username: String): Student?

    @Query("SELECT * FROM student_table WHERE id = :studentId")
    suspend fun getStudentById(studentId: Int): Student?

    @Query("SELECT * FROM student_table WHERE studentId = :studentId")
    suspend fun getStudentByStudentId(studentId: Int): Student?

    @Query("SELECT * FROM student_table")
    suspend fun getAllStudents(): List<Student>

    @Delete
    suspend fun deleteStudent(student: Student)

    @Update
    suspend fun updateStudent(student: Student)

    @Transaction
    @Query("SELECT * FROM student_table WHERE studentId = :studentId")
    suspend fun getStudentWithCourses(studentId: Int): StudentWithCourses?

    @Transaction
    @Query("""
        SELECT course_table.courseId, course_table.name, course_table.title, course_table.description,
               course_table.location, student_course_cross_ref.year, student_course_cross_ref.semester
        FROM course_table
        INNER JOIN student_course_cross_ref
        ON course_table.courseId = student_course_cross_ref.courseId
        WHERE student_course_cross_ref.studentId = :studentId
    """)
    suspend fun getStudentCoursesWithEnrollmentInfo(studentId: Int): List<CourseWithEnrollmentInfo>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStudentCourseCrossRef(crossRef: StudentCourseCrossRef)

    @Query("DELETE FROM student_table")
    suspend fun deleteAll()

    @Query("DELETE FROM student_course_cross_ref")
    suspend fun deleteAllCrossRefs()

    @Query("DELETE FROM student_course_cross_ref WHERE studentId = :studentId")
    suspend fun deleteCrossRefsByStudentId(studentId: Int)

    @Query("SELECT * FROM student_course_cross_ref WHERE studentId = :studentId AND courseId = :courseId AND year = :year AND semester = :semester")
    suspend fun getCrossRef(studentId: Int, courseId: Int, year: Int, semester: Int): StudentCourseCrossRef?
}