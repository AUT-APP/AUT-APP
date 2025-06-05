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
            notificationId = documentId.toInt(),
            iconResId = (document["iconResId"] as? Number)?.toInt() ?: 0,
            title = document["title"] as? String ?: "",
            text = document["text"] as? String ?: "",
            priority = (document["priority"] as? Number)?.toInt() ?: 0,
            deepLinkUri = document["deepLinkUri"] as? String,
            channelId = document["channelId"] as? String ?: "",
            timestamp = (document["timestamp"] as? Number)?.toLong() ?: 0L
        )
    }

    override fun objectToDocument(obj: FirebaseNotification): Map<String, Any?> {
        return mapOf(
            "iconResId" to obj.iconResId,
            "title" to obj.title,
            "text" to obj.text,
            "priority" to obj.priority,
            "deepLinkUri" to obj.deepLinkUri,
            "channelId" to obj.channelId,
            "timestamp" to obj.timestamp
        )
    }

    suspend fun getNotificationsByChannel(channelId: String): List<FirebaseNotification> {
        return try {
            val snapshot = collection
                .whereEqualTo("channelId", channelId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getNotificationsByPriority(priority: Int): List<FirebaseNotification> {
        return try {
            val snapshot = collection
                .whereEqualTo("priority", priority)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRecentNotifications(limit: Int): List<FirebaseNotification> {
        return try {
            val snapshot = collection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteNotification(notificationId: Int): Boolean {
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

    suspend fun deleteNotificationsByChannel(channelId: String): Boolean {
        return try {
            val query = collection.whereEqualTo("channelId", channelId).get().await()
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