package com.example.autapp.data.firebase

import com.example.autapp.data.models.StudySpace
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseStudySpaceRepository : BaseFirebaseRepository<FirebaseStudySpace>("study_spaces") {
    private val firestore = FirebaseFirestore.getInstance()

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseStudySpace {
        return FirebaseStudySpace(
            documentId = documentId,
            spaceId = document["spaceId"] as? String ?: "",
            building = document["building"] as? String ?: "",
            campus = document["campus"] as? String ?: "",
            level = document["level"] as? String ?: "",
            capacity = (document["capacity"] as? Number)?.toInt() ?: 0,
            isAvailable = document["isAvailable"] as? Boolean ?: false
        )
    }

    override fun objectToDocument(obj: FirebaseStudySpace): Map<String, Any?> {
        return mapOf(
            "spaceId" to obj.spaceId,
            "building" to obj.building,
            "campus" to obj.campus,
            "level" to obj.level,
            "capacity" to obj.capacity,
            "isAvailable" to obj.isAvailable
        )
    }

    suspend fun getStudySpacesByBuilding(building: String): List<FirebaseStudySpace> {
        return try {
            val snapshot = collection
                .whereEqualTo("building", building)
                .orderBy("spaceId", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStudySpacesByCampus(campus: String): List<FirebaseStudySpace> {
        return try {
            val snapshot = collection
                .whereEqualTo("campus", campus)
                .orderBy("building", Query.Direction.ASCENDING)
                .orderBy("spaceId", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAvailableStudySpaces(): List<FirebaseStudySpace> {
        return try {
            val snapshot = collection
                .whereEqualTo("isAvailable", true)
                .orderBy("building", Query.Direction.ASCENDING)
                .orderBy("spaceId", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStudySpacesByCapacity(minCapacity: Int): List<FirebaseStudySpace> {
        return try {
            val snapshot = collection
                .whereGreaterThanOrEqualTo("capacity", minCapacity)
                .orderBy("capacity", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateAvailability(spaceId: String, isAvailable: Boolean): Boolean {
        return try {
            val query = collection.whereEqualTo("spaceId", spaceId).get().await()
            if (query.isEmpty) return false
            
            val doc = query.documents.first()
            doc.reference.update("isAvailable", isAvailable).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getStudySpaceById(spaceId: String): FirebaseStudySpace? {
        return try {
            val query = collection.whereEqualTo("spaceId", spaceId).get().await()
            query.documents.firstOrNull()?.data?.let { documentToObject(query.documents.first().id, it) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStudySpacesByName(name: String): List<FirebaseStudySpace> {
        return try {
            val snapshot = collection
                .whereEqualTo("spaceId", name)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStudySpacesByCampusAndBuilding(campus: String, building: String): List<FirebaseStudySpace> {
        return try {
            val conditions = listOf(
                QueryCondition("campus", QueryOperator.EQUAL_TO, campus),
                QueryCondition("building", QueryOperator.EQUAL_TO, building)
            )
            query(conditions)
        } catch (e: Exception) {
            emptyList()
        }
    }
} 