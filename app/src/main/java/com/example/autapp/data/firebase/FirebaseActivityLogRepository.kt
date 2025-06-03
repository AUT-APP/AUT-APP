package com.example.autapp.data.firebase

import com.example.autapp.data.models.ActivityLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseActivityLogRepository : BaseFirebaseRepository<FirebaseActivityLog>("activity_logs") {
    private val firestore = FirebaseFirestore.getInstance()

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseActivityLog {
        return FirebaseActivityLog(
            id = documentId,
            description = document["description"] as? String ?: "",
            timestamp = (document["timestamp"] as? Number)?.toLong() ?: 0L
        )
    }

    override fun objectToDocument(obj: FirebaseActivityLog): Map<String, Any?> {
        return mapOf(
            "description" to obj.description,
            "timestamp" to obj.timestamp
        )
    }

    suspend fun getRecentActivityLogs(limit: Int): List<FirebaseActivityLog> {
        return try {
            val snapshot = collection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            snapshot.documents.mapNotNull { document ->
                document.data?.let { documentToObject(document.id, it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getActivityLogsByDateRange(startDate: Date, endDate: Date): List<FirebaseActivityLog> {
        return try {
            val snapshot = collection
                .whereGreaterThanOrEqualTo("timestamp", startDate.time)
                .whereLessThanOrEqualTo("timestamp", endDate.time)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document ->
                document.data?.let { documentToObject(document.id, it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteActivityLogsOlderThan(date: Date): Boolean {
        return try {
            val query = collection
                .whereLessThan("timestamp", date.time)
                .get()
                .await()
            
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