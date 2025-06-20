package com.example.autapp.data.firebase

import com.example.autapp.data.models.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.lang.Exception
import android.util.Log

class FirebaseNotificationRepository : BaseFirebaseRepository<FirebaseNotification>("notifications"),
    NotificationRepository {
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
            channelId = document["channelId"] as? String ?: "default_channel",
            iconResId = (document["iconResId"] as? Number)?.toInt() ?: 0
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
            "scheduledDeliveryTime" to obj.scheduledDeliveryTime,
            "channelId" to obj.channelId,
            "iconResId" to obj.iconResId
        )
    }


    override suspend fun getRecentNotificationsForUser(limit: Int, userId: String, isTeacher: Boolean): List<FirebaseNotification> {
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

    override suspend fun deleteNotificationsForUser(userId: String, isTeacher: Boolean): Boolean {
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

    fun getNotificationsRealtime(onUpdate: (List<FirebaseNotification>) -> Unit): ListenerRegistration {
        return collection
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationRepo", "Error listening for notifications: ${error.message}")
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.data?.let { data ->
                            FirebaseNotification(
                                notificationId = doc.id,
                                iconResId = (data["iconResId"] as? Number)?.toInt() ?: 0,
                                title = data["title"] as? String ?: "",
                                text = data["text"] as? String ?: "",
                                channelId = data["channelId"] as? String ?: "",
                                priority = (data["priority"] as? Number)?.toInt() ?: 0,
                                deepLinkUri = data["deepLinkUri"] as? String,
                                timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                userId = data["userId"] as? String ?: "",
                                isTeacher = data["isTeacher"] as? Boolean == true,
                                notificationType = data["notificationType"] as? String ?: "",
                                relatedItemId = data["relatedItemId"] as? String ?: "",
                                scheduledDeliveryTime = (data["scheduledDeliveryTime"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationRepo", "Error parsing notification: ${e.message}")
                        null
                    }
                } ?: emptyList()

                onUpdate(notifications)
            }
    }
}