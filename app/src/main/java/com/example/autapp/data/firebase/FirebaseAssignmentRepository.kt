package com.example.autapp.data.firebase

import com.example.autapp.data.models.Assignment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date
import com.google.firebase.Timestamp

class FirebaseAssignmentRepository : BaseFirebaseRepository<FirebaseAssignment>("assignments") {
    private val firestore = FirebaseFirestore.getInstance()

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseAssignment {
        return FirebaseAssignment(
            assignmentId = documentId,
            name = document["name"] as? String ?: "",
            location = document["location"] as? String ?: "",
            due = (document["due"] as? Timestamp)?.toDate() ?: Date(),
            weight = (document["weight"] as? Number)?.toDouble() ?: 0.0,
            maxScore = (document["maxScore"] as? Number)?.toDouble() ?: 0.0,
            type = document["type"] as? String ?: "",
            courseId = document["courseId"] as? String ?: ""
        )
    }

    override fun objectToDocument(obj: FirebaseAssignment): Map<String, Any?> {
        return mapOf(
            "name" to obj.name,
            "location" to obj.location,
            "due" to obj.due,
            "weight" to obj.weight,
            "maxScore" to obj.maxScore,
            "type" to obj.type,
            "courseId" to obj.courseId
        )
    }

    suspend fun getAssignmentsByCourse(courseId: String): List<FirebaseAssignment> {
        return try {
            val snapshot = collection
                .whereEqualTo("courseId", courseId)
                .orderBy("due", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUpcomingAssignments(courseId: String, fromDate: Date): List<FirebaseAssignment> {
        return try {
            val snapshot = collection
                .whereEqualTo("courseId", courseId)
                .whereGreaterThanOrEqualTo("due", fromDate)
                .orderBy("due", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAssignmentsByType(courseId: String, type: String): List<FirebaseAssignment> {
        return try {
            val snapshot = collection
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("type", type)
                .orderBy("due", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }
} 