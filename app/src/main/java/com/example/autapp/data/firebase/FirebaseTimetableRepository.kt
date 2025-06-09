package com.example.autapp.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date
import com.google.firebase.Timestamp

class FirebaseTimetableRepository : BaseFirebaseRepository<FirebaseTimetableEntry>("timetable_entries") {
    private val firestore = FirebaseFirestore.getInstance()

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseTimetableEntry {
        return FirebaseTimetableEntry(
            entryId = documentId,
            courseId = document["courseId"] as? String ?: "",
            dayOfWeek = (document["dayOfWeek"] as? Number)?.toInt() ?: 0,
            startTime = (document["startTime"] as? Timestamp)?.toDate() ?: Date(),
            endTime = (document["endTime"] as? Timestamp)?.toDate() ?: Date(),
            room = document["room"] as? String ?: "",
            type = document["type"] as? String ?: ""
        )
    }

    override fun objectToDocument(obj: FirebaseTimetableEntry): Map<String, Any?> {
        return mapOf(
            "courseId" to obj.courseId,
            "dayOfWeek" to obj.dayOfWeek,
            "startTime" to obj.startTime,
            "endTime" to obj.endTime,
            "room" to obj.room,
            "type" to obj.type
        )
    }

    suspend fun getTimetableByCourse(courseId: String): List<FirebaseTimetableEntry> {
        return try {
            val snapshot = collection
                .whereEqualTo("courseId", courseId)
                .orderBy("dayOfWeek", Query.Direction.ASCENDING)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTimetableByDay(dayOfWeek: Int): List<FirebaseTimetableEntry> {
        return try {
            val snapshot = collection
                .whereEqualTo("dayOfWeek", dayOfWeek)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTimetableByType(type: String): List<FirebaseTimetableEntry> {
        return try {
            val snapshot = collection
                .whereEqualTo("type", type)
                .orderBy("dayOfWeek", Query.Direction.ASCENDING)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTimetableByRoom(room: String): List<FirebaseTimetableEntry> {
        return try {
            val snapshot = collection
                .whereEqualTo("room", room)
                .orderBy("dayOfWeek", Query.Direction.ASCENDING)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun checkRoomAvailability(room: String, dayOfWeek: Int, startTime: Date, endTime: Date): Boolean {
        return try {
            val snapshot = collection
                .whereEqualTo("room", room)
                .whereEqualTo("dayOfWeek", dayOfWeek)
                .whereLessThanOrEqualTo("startTime", endTime)
                .whereGreaterThanOrEqualTo("endTime", startTime)
                .get()
                .await()
            snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }
} 