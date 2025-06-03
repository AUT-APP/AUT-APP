package com.example.autapp.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseTimetableNotificationPreferenceRepository :
    BaseFirebaseRepository<FirebaseTimetableNotificationPreference>("timetablePreferences") {

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseTimetableNotificationPreference {
        return FirebaseTimetableNotificationPreference(
            id = documentId,
            studentId = document["studentId"] as? String,
            teacherId = document["teacherId"] as? String,
            isTeacher = document["isTeacher"] as? Boolean ?: false,
            classSessionId = document["classSessionId"] as? String ?: "",
            notificationTime = (document["notificationTime"] as? Long)?.toInt() ?: 15,
            isEnabled = document["isEnabled"] as? Boolean ?: true
        )
    }

    override fun objectToDocument(obj: FirebaseTimetableNotificationPreference): Map<String, Any?> {
        return mapOf(
            "studentId" to obj.studentId,
            "teacherId" to obj.teacherId,
            "isTeacher" to obj.isTeacher,
            "classSessionId" to obj.classSessionId,
            "notificationTime" to obj.notificationTime,
            "isEnabled" to obj.isEnabled
        )
    }

    suspend fun getPreferencesByUser(userId: String, isTeacher: Boolean): List<FirebaseTimetableNotificationPreference> {
        return try {
            val field = if (isTeacher) "teacherId" else "studentId"
            val result = collection
                .whereEqualTo(field, userId)
                .get().await()
            result.documents.mapNotNull { doc ->
                doc.data?.let { documentToObject(doc.id, it) }
            }
        } catch (e: Exception) {
            throw FirebaseException("Error fetching timetable preferences", e)
        }
    }


    suspend fun insertOrUpdatePreference(entry: FirebaseTimetableNotificationPreference) {
        val userId = entry.studentId ?: entry.teacherId
        ?: throw IllegalArgumentException("Either studentId or teacherId must be provided")
        val id = entry.id.ifBlank { "${userId}_${entry.classSessionId}" }

        try {
            collection.document(id).set(objectToDocument(entry.copy(id = id))).await()
        } catch (e: Exception) {
            throw FirebaseException("Error inserting or updating timetable preference", e)
        }
    }



    suspend fun deletePreference(userId: String, classSessionId: String) {
        try {
            val id = "${userId}_$classSessionId"
            delete(id)
        } catch (e: Exception) {
            throw FirebaseException("Error deleting timetable preference", e)
        }
    }

    suspend fun getPreference(userId: String, classSessionId: String, isTeacher: Boolean): FirebaseTimetableNotificationPreference? {
        return try {
            val field = if (isTeacher) "teacherId" else "studentId"
            val result = collection
                .whereEqualTo(field, userId)
                .whereEqualTo("classSessionId", classSessionId)
                .limit(1)
                .get().await()

            result.documents.firstOrNull()?.data?.let {
                documentToObject(result.documents.first().id, it)
            }
        } catch (e: Exception) {
            throw FirebaseException("Error getting timetable preference", e)
        }
    }
}
