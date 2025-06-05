package com.example.autapp.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseBookingNotificationPreferenceRepository :
    BaseFirebaseRepository<FirebaseBookingNotificationPreference>("bookingPreferences") {

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseBookingNotificationPreference {
        return FirebaseBookingNotificationPreference(
            id = documentId,
            studentId = document["studentId"] as? String ?: "",
            teacherId = document["teacherId"] as? String ?: "",
            isTeacher = document["isTeacher"] as? Boolean ?: false,
            bookingId = document["bookingId"] as? String ?: "",
            notificationTime = (document["notificationTime"] as? Long)?.toInt() ?: 15,
            isEnabled = document["isEnabled"] as? Boolean ?: true
        )
    }

    override fun objectToDocument(obj: FirebaseBookingNotificationPreference): Map<String, Any?> {
        return mapOf(
            "studentId" to obj.studentId,
            "teacherId" to obj.teacherId,
            "isTeacher" to obj.isTeacher,
            "bookingId" to obj.bookingId,
            "notificationTime" to obj.notificationTime,
            "isEnabled" to obj.isEnabled
        )
    }

    suspend fun getPreferencesByUser(userId: String, isTeacher: Boolean): List<FirebaseBookingNotificationPreference> {
        return try {
            val field = if (isTeacher) "teacherId" else "studentId"
            val result = collection.whereEqualTo(field, userId).get().await()
            result.documents.mapNotNull { it.data?.let { data -> documentToObject(it.id, data) } }
        } catch (e: Exception) {
            throw FirebaseException("Error fetching booking preferences", e)
        }
    }

    suspend fun insertOrUpdatePreference(entry: FirebaseBookingNotificationPreference) {
        val userId = if (entry.isTeacher) entry.teacherId else entry.studentId
        val id = entry.id.ifBlank { "${userId}_${entry.bookingId}" }

        try {
            collection.document(id).set(objectToDocument(entry.copy(id = id))).await()
        } catch (e: Exception) {
            throw FirebaseException("Error inserting or updating booking preference", e)
        }
    }

    suspend fun deletePreference(userId: String, bookingId: String) {
        val id = "${userId}_$bookingId"
        try {
            delete(id)
        } catch (e: Exception) {
            throw FirebaseException("Error deleting booking preference", e)
        }
    }

    suspend fun getPreference(userId: String, bookingId: String, isTeacher: Boolean): FirebaseBookingNotificationPreference? {
        return try {
            val field = if (isTeacher) "teacherId" else "studentId"
            val result = collection
                .whereEqualTo(field, userId)
                .whereEqualTo("bookingId", bookingId)
                .limit(1)
                .get().await()

            result.documents.firstOrNull()?.data?.let {
                documentToObject(result.documents.first().id, it)
            }
        } catch (e: Exception) {
            throw FirebaseException("Error getting booking preference", e)
        }
    }
}
