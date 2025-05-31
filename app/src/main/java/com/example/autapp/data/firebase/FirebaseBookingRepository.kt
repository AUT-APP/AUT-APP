package com.example.autapp.data.firebase

import com.example.autapp.data.models.Booking
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date
import com.google.firebase.Timestamp
import android.util.Log

class FirebaseBookingRepository : BaseFirebaseRepository<FirebaseBooking>("bookings") {
    private val firestore = FirebaseFirestore.getInstance()

    override fun documentToObject(documentId: String, document: Map<String, Any?>): FirebaseBooking {
        return FirebaseBooking(
            id = documentId,
            studentId = document["studentId"] as? String ?: "",
            roomId = document["roomId"] as? String ?: "",
            building = document["building"] as? String ?: "",
            campus = document["campus"] as? String ?: "",
            level = document["level"] as? String ?: "",
            bookingDate = (document["bookingDate"] as? Timestamp)?.toDate() ?: Date(),
            startTime = (document["startTime"] as? Timestamp)?.toDate() ?: Date(),
            endTime = (document["endTime"] as? Timestamp)?.toDate() ?: Date(),
            status = document["status"] as? String ?: ""
        )
    }

    override fun objectToDocument(obj: FirebaseBooking): Map<String, Any?> {
        return mapOf(
            "studentId" to obj.studentId,
            "roomId" to obj.roomId,
            "building" to obj.building,
            "campus" to obj.campus,
            "level" to obj.level,
            "bookingDate" to obj.bookingDate,
            "startTime" to obj.startTime,
            "endTime" to obj.endTime,
            "status" to obj.status
        )
    }

    suspend fun getBookingsByStudent(studentId: String): List<FirebaseBooking> {
        return try {
            val snapshot = collection
                .whereEqualTo("studentId", studentId)
                .orderBy("bookingDate", Query.Direction.DESCENDING)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()
            Log.d("FirebaseBookingRepository", "Fetched \${snapshot.documents.size} documents for studentId: $studentId")
            snapshot.documents.mapNotNull { document ->
                val booking = document.data?.let { data -> documentToObject(document.id, data as Map<String, Any?>) }
                Log.d("FirebaseBookingRepository", "Converted document to booking: $booking")
                booking
            }
        } catch (e: Exception) {
            Log.e("FirebaseBookingRepository", "Error fetching bookings for studentId $studentId: ${e.message}")
            emptyList()
        }
    }

    suspend fun getBookingsByRoom(roomId: String): List<FirebaseBooking> {
        return try {
            val snapshot = collection
                .whereEqualTo("roomId", roomId)
                .orderBy("bookingDate", Query.Direction.ASCENDING)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getActiveBookings(): List<FirebaseBooking> {
        return try {
            val snapshot = collection
                .whereEqualTo("status", "ACTIVE")
                .orderBy("bookingDate", Query.Direction.ASCENDING)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getBookingsByDate(date: Date): List<FirebaseBooking> {
        val startOfDay = Date(date.time - (date.time % (24 * 60 * 60 * 1000)))
        val endOfDay = Date(startOfDay.time + (24 * 60 * 60 * 1000 - 1))
        
        return try {
            val snapshot = collection
                .whereGreaterThanOrEqualTo("bookingDate", startOfDay)
                .whereLessThanOrEqualTo("bookingDate", endOfDay)
                .orderBy("bookingDate", Query.Direction.ASCENDING)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun checkRoomAvailability(roomId: String, date: Date, startTime: Date, endTime: Date): Boolean {
        return try {
            val snapshot = collection
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("status", "ACTIVE")
                .whereLessThanOrEqualTo("startTime", endTime)
                .whereGreaterThanOrEqualTo("endTime", startTime)
                .get()
                .await()
            snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: String): Boolean {
        return try {
            val docRef = collection.document(bookingId)
            docRef.update("status", status).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteCompletedAndCancelledBookings() {
        try {
            val completedSnapshot = collection.whereEqualTo("status", "COMPLETED").get().await()
            for (doc in completedSnapshot.documents) {
                doc.reference.delete().await()
            }
            val cancelledSnapshot = collection.whereEqualTo("status", "CANCELLED").get().await()
            for (doc in cancelledSnapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            // Log or handle exception
        }
    }

    suspend fun getNextBooking(roomId: String, startTime: Date): FirebaseBooking? {
        return try {
            val snapshot = collection
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("status", "ACTIVE")
                .whereGreaterThan("startTime", startTime)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.data?.let { documentToObject(snapshot.documents.first().id, it) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkBookingConflict(roomId: String, startTime: Date, endTime: Date): List<FirebaseBooking> {
        return try {
            val snapshot = collection
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("status", "ACTIVE")
                .whereLessThan("startTime", endTime)
                .whereGreaterThan("endTime", startTime)
                .get()
                .await()
            snapshot.documents.mapNotNull { document -> document.data?.let { documentToObject(document.id, it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Member function to convert Booking to FirebaseBooking
    fun fromBooking(booking: Booking): FirebaseBooking {
        return FirebaseBooking(
            studentId = booking.studentId.toString(), // Convert Int studentId to String
            roomId = booking.roomId,
            building = booking.building,
            campus = booking.campus,
            level = booking.level,
            bookingDate = booking.bookingDate,
            startTime = booking.startTime,
            endTime = booking.endTime,
            status = booking.status
            // Firebase ID and createdAt will be handled by Firestore
        )
    }
} 