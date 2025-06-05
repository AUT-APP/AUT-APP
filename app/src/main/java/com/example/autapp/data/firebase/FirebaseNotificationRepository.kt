package com.example.autapp.data.firebase

import com.example.autapp.data.models.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseNotificationRepository : BaseFirebaseRepository<FirebaseNotification>("notifications") {
    private val firestore = FirebaseFirestore.getInstance()

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseNotification {
        return FirebaseNotification(
            notificationId = documentId,
            userId = document["userId"] as? String ?: "",
            title = document["title"] as? String ?: "",
            text = document["text"] as? String ?: "",
            isTeacher = document["isTeacher"] as? Boolean == true,
            notificationType = document["notificationType"] as? String ?: "",
            relatedItemId = document["relatedItemId"] as? String ?: "",
            scheduledDeliveryTime = (document["scheduledDeliveryTime"] as? com.google.firebase.Timestamp)?.toDate()
                ?: Date(),
        )
    }

    override fun objectToDocument(obj: FirebaseNotification): Map<String, Any?> {
        return mapOf(
            "userId" to obj.userId,
            "title" to obj.title,
            "text" to obj.text,
            "isTeacher" to obj.isTeacher,
            "notificationType" to obj.notificationType,
            "relatedItemId" to obj.relatedItemId,
            "scheduledDeliveryTime" to obj.scheduledDeliveryTime
        )
    }


    suspend fun getRecentNotificationsForUser(limit: Int, userId: String, isTeacher: Boolean): List<FirebaseNotification> {
        return try {
            val snapshot = collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isTeacher", isTeacher)
                .orderBy("scheduledDeliveryTime", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            snapshot.toObjects(FirebaseNotification::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteNotification(notificationId: String): Boolean {
        return try {
            val query = collection.whereEqualTo("notificationId", notificationId).get().await()
            if (query.isEmpty) return false
            
            val doc = query.documents.first()
            doc.reference.delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteNotificationsForUser(userId: String, isTeacher: Boolean): Boolean {
        return try {
            val query = collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isTeacher", isTeacher)
                .get().await()
            val batch = firestore.batch()
            query.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}