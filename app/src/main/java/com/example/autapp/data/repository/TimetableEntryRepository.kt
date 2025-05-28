package com.example.autapp.data.repository

import com.example.autapp.data.dao.TimetableEntryDao
import com.example.autapp.data.models.TimetableEntry

class TimetableEntryRepository(private val timetableEntryDao: TimetableEntryDao) {

    suspend fun insertTimetableEntry(entry: TimetableEntry) {
        timetableEntryDao.insertTimetableEntry(entry)
    }

    suspend fun getTimetableEntryById(entryId: Int): TimetableEntry? {
        return timetableEntryDao.getTimetableEntryById(entryId)
    }

    suspend fun getTimetableEntriesByCourse(courseId: Int): List<TimetableEntry> {
        return timetableEntryDao.getTimetableEntriesByCourse(courseId)
    }

    suspend fun getTimetableEntriesByDay(dayOfWeek: Int): List<TimetableEntry> {
        return timetableEntryDao.getTimetableEntriesByDay(dayOfWeek)
    }

    suspend fun getAllTimetableEntries(): List<TimetableEntry> {
        return timetableEntryDao.getAllTimetableEntries()
    }

    suspend fun deleteTimetableEntry(entry: TimetableEntry) {
        timetableEntryDao.deleteTimetableEntry(entry)
    }

    suspend fun updateTimetableEntry(entry: TimetableEntry) {
        timetableEntryDao.updateTimetableEntry(entry)
    }

    suspend fun getTimetableEntriesWithCourseByDay(dayOfWeek: Int): List<TimetableEntryDao.TimetableEntryWithCourse> {
        return timetableEntryDao.getTimetableEntriesWithCourseByDay(dayOfWeek)
    }

    suspend fun getTimetableEntriesWithCourseByTeacherDay(teacherId: Int, dayOfWeek: Int): List<TimetableEntryDao.TimetableEntryWithCourse> {
        return timetableEntryDao.getTimetableEntriesWithCourseByTeacherDay(teacherId, dayOfWeek)
    }
}