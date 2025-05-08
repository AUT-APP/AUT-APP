package com.example.autapp.data.repository

import com.example.autapp.data.dao.CourseDao
import com.example.autapp.data.models.Course
import com.example.autapp.data.models.TimetableEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CourseRepository(private val courseDao: CourseDao) {

    suspend fun insertCourse(course: Course) {
        courseDao.insertCourse(course)
    }

    suspend fun getCourseById(courseId: Int): Course? {
        return courseDao.getCourseById(courseId)
    }

    suspend fun getCourseByName(name: String): Course? {
        return courseDao.getCourseByName(name)
    }

    suspend fun getAllCourses(): List<Course> {
        return courseDao.getAllCourses()
    }

    suspend fun getTimetableForCourse(courseId: Int): List<TimetableEntry> {
        return withContext(Dispatchers.IO) {
            courseDao.getTimetableForCourse(courseId)
        }
    }

    suspend fun deleteCourse(course: Course) {
        courseDao.deleteCourse(course)
    }

    suspend fun updateCourse(course: Course) {
        courseDao.updateCourse(course)
    }

    suspend fun deleteAll() {
        courseDao.deleteAll()
    }
}