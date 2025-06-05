package com.example.autapp.data.firebase

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import com.google.firebase.Timestamp

data class FirebaseBusSchedule(
    @DocumentId
    val id: String = "",
    val departureTime: Date = Date(),
    val arrivalTime: Date = Date(),
    val route: String = "City to South Campus"
)

class FirebaseBusScheduleRepository(private val firestore: FirebaseFirestore) : BaseFirebaseRepository<FirebaseBusSchedule>("bus_schedules") {

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseBusSchedule {
        return FirebaseBusSchedule(
            id = documentId,
            departureTime = (document["departureTime"] as? Timestamp)?.toDate() ?: Date(),
            arrivalTime = (document["arrivalTime"] as? Timestamp)?.toDate() ?: Date(),
            route = document["route"] as? String ?: ""
        )
    }

    override fun objectToDocument(obj: FirebaseBusSchedule): Map<String, Any?> {
        return mapOf(
            "departureTime" to obj.departureTime,
            "arrivalTime" to obj.arrivalTime,
            "route" to obj.route
        )
    }

    suspend fun getAllSchedules(): List<FirebaseBusSchedule> {
        return try {
            val snapshot = collection.get().await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun insertAll(schedules: List<FirebaseBusSchedule>) {
        try {
            val batch = firestore.batch()
            schedules.forEach { schedule ->
                val docRef = collection.document()
                batch.set(docRef, objectToDocument(schedule))
            }
            batch.commit().await()
        } catch (e: Exception) {
            // Handle exceptions, e.g., log or throw
            println("Error inserting bus schedules: ${e.message}")
        }
    }
} 