package com.example.autapp.data.repository

import com.example.autapp.data.dao.EventDao
import com.example.autapp.data.models.Event
import java.util.Date

class EventRepository(private val eventDao: EventDao) {
    suspend fun insertEvent(event: Event) {
        eventDao.insertEvent(event)
    }

    suspend fun updateEvent(event: Event) {
        eventDao.updateEvent(event)
    }

    suspend fun deleteEvent(event: Event) {
        eventDao.deleteEvent(event)
    }

    suspend fun getEventById(eventId: Int): Event? {
        return eventDao.getEventById(eventId)
    }

    suspend fun getEventsByStudent(studentId: Int): List<Event> {
        return eventDao.getEventsByStudent(studentId)
    }

    suspend fun getEventsByDate(studentId: Int, date: Date): List<Event> {
        return eventDao.getEventsByDate(studentId, date)
    }

    suspend fun getEventsByType(studentId: Int, isToDoList: Boolean): List<Event> {
        return eventDao.getEventsByType(studentId, isToDoList)
    }

    suspend fun getEventsBetweenDates(studentId: Int, startDate: Date, endDate: Date): List<Event> {
        return eventDao.getEventsBetweenDates(studentId, startDate, endDate)
    }

    suspend fun deleteAllEventsByStudent(studentId: Int) {
        eventDao.deleteAllEventsByStudent(studentId)
    }

    suspend fun getEventsByTitleAndDate(title: String, date: Date): List<Event> {
        return eventDao.getEventsByTitleAndDate(title, date)
    }
} 