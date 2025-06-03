package com.example.autapp.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseEventRepository : BaseFirebaseRepository<FirebaseEvent>("events") {

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseEvent {
        return FirebaseEvent(
            eventId = documentId,
            title = document["title"] as? String ?: "",
            date = (document["date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
            startTime = (document["startTime"] as? com.google.firebase.Timestamp)?.toDate(),
            endTime = (document["endTime"] as? com.google.firebase.Timestamp)?.toDate(),
            location = document["location"] as? String,
            details = document["details"] as? String,
            isToDoList = document["isToDoList"] as? Boolean ?: false,
            frequency = document["frequency"] as? String,
            studentId = document["studentId"] as? String ?: "",
            teacherId = document["teacherId"] as? String,
            isTeacherEvent = document["isTeacherEvent"] as? Boolean ?: false
        )
    }

    override fun objectToDocument(obj: FirebaseEvent): Map<String, Any?> {
        return mapOf(
            "title" to obj.title,
            "date" to obj.date,
            "startTime" to obj.startTime,
            "endTime" to obj.endTime,
            "location" to obj.location,
            "details" to obj.details,
            "isToDoList" to obj.isToDoList,
            "frequency" to obj.frequency,
            "studentId" to obj.studentId,
            "teacherId" to obj.teacherId,
            "isTeacherEvent" to obj.isTeacherEvent
        )
    }

    /**
     * Get event by event ID
     */
    suspend fun getEventByEventId(eventId: String): FirebaseEvent? {
        return try {
            val result = collection.whereEqualTo("eventId", eventId).get().await()
            result.documents.firstOrNull()?.data?.let { documentToObject(eventId, it) }
        } catch (e: Exception) {
            throw FirebaseException("Error getting event by ID", e)
        }
    }

    /**
     * Get all events for a student
     */
    suspend fun getStudentEvents(studentId: String): List<FirebaseEvent> {
        return try {
            val result = collection.whereEqualTo("studentId", studentId)
                .whereEqualTo("isTeacherEvent", false)
                .get().await()
            result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } }
        } catch (e: Exception) {
            throw FirebaseException("Error getting student events", e)
        }
    }

    /**
     * Get all events for a teacher
     */
    suspend fun getTeacherEvents(teacherId: String): List<FirebaseEvent> {
        return try {
            val result = collection.whereEqualTo("teacherId", teacherId)
                .whereEqualTo("isTeacherEvent", true)
                .get().await()
            result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } }
        } catch (e: Exception) {
            throw FirebaseException("Error getting teacher events", e)
        }
    }

    /**
     * Get events by date range
     */
    suspend fun getEventsByDateRange(startDate: Date, endDate: Date): List<FirebaseEvent> {
        return try {
            val result = collection.whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get().await()
            result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } }
        } catch (e: Exception) {
            throw FirebaseException("Error getting events by date range", e)
        }
    }

    /**
     * Get todo list items for a student
     */
    suspend fun getStudentTodoList(studentId: String): List<FirebaseEvent> {
        return try {
            val result = collection.whereEqualTo("studentId", studentId)
                .whereEqualTo("isToDoList", true)
                .get().await()
            result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } }
        } catch (e: Exception) {
            throw FirebaseException("Error getting student todo list", e)
        }
    }

    /**
     * Get todo list items for a teacher
     */
    suspend fun getTeacherTodoList(teacherId: String): List<FirebaseEvent> {
        return try {
            val result = collection.whereEqualTo("teacherId", teacherId)
                .whereEqualTo("isToDoList", true)
                .get().await()
            result.documents.mapNotNull { doc -> doc.data?.let { documentToObject(doc.id, it) } }
        } catch (e: Exception) {
            throw FirebaseException("Error getting teacher todo list", e)
        }
    }

    /**
     * Update event details
     */
    suspend fun updateEventDetails(
        eventId: String,
        title: String,
        details: String?,
        location: String?,
        startTime: Date?,
        endTime: Date?
    ) {
        try {
            val event = getById(eventId) ?: throw FirebaseException("Event not found")
            update(eventId, event.copy(
                title = title,
                details = details,
                location = location,
                startTime = startTime,
                endTime = endTime
            ))
        } catch (e: Exception) {
            throw FirebaseException("Error updating event details", e)
        }
    }

    /**
     * Update event frequency
     */
    suspend fun updateEventFrequency(eventId: String, frequency: String?) {
        try {
            val event = getById(eventId) ?: throw FirebaseException("Event not found")
            update(eventId, event.copy(frequency = frequency))
        } catch (e: Exception) {
            throw FirebaseException("Error updating event frequency", e)
        }
    }

    /**
     * Delete event
     */
    suspend fun deleteEvent(eventId: String) {
        try {
            val event = getById(eventId) ?: throw FirebaseException("Event not found")
            delete(eventId)
        } catch (e: Exception) {
            throw FirebaseException("Error deleting event", e)
        }
    }
} 