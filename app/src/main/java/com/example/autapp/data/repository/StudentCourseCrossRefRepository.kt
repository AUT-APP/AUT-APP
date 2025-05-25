package com.example.autapp.data.repository

import com.example.autapp.data.dao.StudentCourseCrossRefDao
import com.example.autapp.data.models.StudentCourseCrossRef

class StudentCourseCrossRefRepository(
    private val studentCourseCrossRefDao: StudentCourseCrossRefDao
) {
    suspend fun insert(crossRef: StudentCourseCrossRef) {
        studentCourseCrossRefDao.insert(crossRef)
    }

    suspend fun delete(crossRef: StudentCourseCrossRef) {
        studentCourseCrossRefDao.delete(crossRef)
    }

    suspend fun getByStudentId(studentId: Int): List<StudentCourseCrossRef> {
        return studentCourseCrossRefDao.getByStudentId(studentId)
    }

    suspend fun getByCourseId(courseId: Int): List<StudentCourseCrossRef> {
        return studentCourseCrossRefDao.getByCourseId(courseId)
    }

}