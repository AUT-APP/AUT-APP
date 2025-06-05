package com.example.autapp.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseEventNotificationPreferenceRepository :
    BaseFirebaseRepository<FirebaseEventNotificationPreference>("eventPreferences") {

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseEventNotificationPreference {
        return FirebaseEventNotificationPreference(
            id = documentId,
            studentId = document["studentId"] as? String ?: "",
            eventId = document["eventId"] as? String ?: "",
            notificationTime = (document["notificationTime"] as? Long)?.toInt() ?: 15,
            isEnabled = document["isEnabled"] as? Boolean ?: true
        )
    }

    override fun objectToDocument(obj: FirebaseEventNotificationPreference): Map<String, Any?> {
        return mapOf(
            "studentId" to obj.studentId,
            "eventId" to obj.eventId,
            "notificationTime" to obj.notificationTime,
            "isEnabled" to obj.isEnabled
        )
    }

    suspend fun getPreferencesByStudent(studentId: String): List<FirebaseEventNotificationPreference> {
        return try {
            val result = collection.whereEqualTo("studentId", studentId).get().await()
            result.documents.mapNotNull { it.data?.let { data -> documentToObject(it.id, data) } }
        } catch (e: Exception) {
            throw FirebaseException("Error fetching event preferences", e)
        }
    }

    suspend fun insertOrUpdatePreference(entry: FirebaseEventNotificationPreference) {
        val id = entry.id.ifBlank { "${entry.studentId}_${entry.eventId}" }

        try {
            collection.document(id).set(objectToDocument(entry.copy(id = id))).await()
        } catch (e: Exception) {
            throw FirebaseException("Error inserting or updating event preference", e)
        }
    }

    suspend fun deletePreference(studentId: String, eventId: String) {
        val id = "${studentId}_$eventId"
        try {
            delete(id)
        } catch (e: Exception) {
            throw FirebaseException("Error deleting event preference", e)
        }
    }

    suspend fun getPreference(studentId: String, eventId: String): FirebaseEventNotificationPreference? {
        return try {
            val result = collection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get().await()

            result.documents.firstOrNull()?.data?.let {
                documentToObject(result.documents.first().id, it)
            }
        } catch (e: Exception) {
            throw FirebaseException("Error getting event preference", e)
        }
    }
}
