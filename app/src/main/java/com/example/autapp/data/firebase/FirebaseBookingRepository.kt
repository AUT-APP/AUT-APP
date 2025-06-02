package com.example.autapp.data.firebase

import com.example.autapp.data.models.Booking
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date
import com.google.firebase.Timestamp
import android.util.Log
import java.util.Calendar

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
            "bookingDate" to Timestamp(obj.bookingDate),
            "startTime" to Timestamp(obj.startTime),
            "endTime" to Timestamp(obj.endTime),
            "status" to obj.status,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
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
            Log.d("FirebaseBookingRepository", "Fetched ${snapshot.documents.size} documents for studentId: $studentId")
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
            Log.e("FirebaseBookingRepository", "Error fetching bookings for roomId $roomId: ${e.message}")
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
            Log.e("FirebaseBookingRepository", "Error fetching active bookings: ${e.message}")
            emptyList()
        }
    }

    suspend fun getBookingsByDate(date: Date): List<FirebaseBooking> {
        val calendar = Calendar.getInstance().apply { time = date }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = Timestamp(calendar.time)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = Timestamp(calendar.time)

        return try {
            Log.d("FirebaseBookingRepository", "Querying bookings for date: $date, startOfDay: $startOfDay, endOfDay: $endOfDay")
            val snapshot = collection
                .whereGreaterThanOrEqualTo("bookingDate", startOfDay)
                .whereLessThanOrEqualTo("bookingDate", endOfDay)
                .whereEqualTo("status", "ACTIVE")
                .orderBy("bookingDate", Query.Direction.ASCENDING)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()
            val bookings = snapshot.documents.mapNotNull { document ->
                document.data?.let { documentToObject(document.id, it) }
            }
            Log.d("FirebaseBookingRepository", "Fetched ${bookings.size} bookings for date $date")
            bookings
        } catch (e: Exception) {
            Log.e("FirebaseBookingRepository", "Error fetching bookings for date $date: ${e.message}")
            emptyList()
        }
    }

    suspend fun checkRoomAvailability(roomId: String, date: Date, startTime: Date, endTime: Date): Boolean {
        return try {
            val startTimestamp = Timestamp(startTime)
            val endTimestamp = Timestamp(endTime)
            Log.d("FirebaseBookingRepository", "Checking availability for roomId: $roomId, startTime: $startTime, endTime: $endTime")

            val snapshot = collection
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("status", "ACTIVE")
                .whereLessThan("startTime", endTimestamp)
                .whereGreaterThan("endTime", startTimestamp)
                .get()
                .await()

            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.forEach { doc ->
                    val data = doc.data
                    if (data != null) {
                        val booking = documentToObject(doc.id, data as Map<String, Any?>)
                        Log.d("FirebaseBookingRepository", "Conflict found: Booking ID: ${doc.id}, startTime: ${booking.startTime}, endTime: ${booking.endTime}, data: $data")
                    } else {
                        Log.w("FirebaseBookingRepository", "Document ${doc.id} has no data")
                    }
                }
            } else {
                Log.d("FirebaseBookingRepository", "No conflicts found for roomId $roomId")
            }

            snapshot.isEmpty
        } catch (e: Exception) {
            Log.e("FirebaseBookingRepository", "Error checking availability for roomId $roomId: ${e.message}")
            if (e.message?.contains("requires an index") == true) {
                Log.w("FirebaseBookingRepository", "Index missing, assuming slot is available")
                return true // Temporary workaround until index is created
            }
            return false
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: String): Boolean {
        return try {
            val docRef = collection.document(bookingId)
            docRef.update("status", status).await()
            Log.d("FirebaseBookingRepository", "Updated booking $bookingId status to $status")
            true
        } catch (e: Exception) {
            Log.e("FirebaseBookingRepository", "Error updating booking $bookingId status: ${e.message}")
            false
        }
    }

    suspend fun deleteCompletedAndCancelledBookings() {
        try {
            val completedSnapshot = collection.whereEqualTo("status", "COMPLETED").get().await()
            for (doc in completedSnapshot.documents) {
                doc.reference.delete().await()
                Log.d("FirebaseBookingRepository", "Deleted completed booking: ${doc.id}")
            }
            val cancelledSnapshot = collection.whereEqualTo("status", "CANCELED").get().await()
            for (doc in cancelledSnapshot.documents) {
                doc.reference.delete().await()
                Log.d("FirebaseBookingRepository", "Deleted cancelled booking: ${doc.id}")
            }
        } catch (e: Exception) {
            Log.e("FirebaseBookingRepository", "Error deleting completed/cancelled bookings: ${e.message}")
        }
    }

    suspend fun updateCompletedBookings() {
        try {
            val now = Timestamp.now()
            val snapshot = collection
                .whereEqualTo("status", "ACTIVE")
                .whereLessThan("endTime", now)
                .get()
                .await()
            for (doc in snapshot.documents) {
                doc.reference.update("status", "COMPLETED").await()
                Log.d("FirebaseBookingRepository", "Updated booking ${doc.id} to COMPLETED")
            }
        } catch (e: Exception) {
            Log.e("FirebaseBookingRepository", "Error updating completed bookings: ${e.message}")
        }
    }

    suspend fun getNextBooking(roomId: String, startTime: Date): FirebaseBooking? {
        return try {
            val snapshot = collection
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("status", "ACTIVE")
                .whereGreaterThan("startTime", Timestamp(startTime))
                .orderBy("startTime", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .await()
            val booking = snapshot.documents.firstOrNull()?.data?.let { documentToObject(snapshot.documents.first().id, it) }
            Log.d("FirebaseBookingRepository", "Next booking for roomId $roomId after $startTime: $booking")
            booking
        } catch (e: Exception) {
            Log.e("FirebaseBookingRepository", "Error fetching next booking for roomId $roomId: ${e.message}")
            null
        }
    }

    suspend fun checkBookingConflict(roomId: String, startTime: Date, endTime: Date): List<FirebaseBooking> {
        return try {
            val snapshot = collection
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("status", "ACTIVE")
                .whereLessThan("startTime", Timestamp(endTime))
                .whereGreaterThan("endTime", Timestamp(startTime))
                .get()
                .await()
            val conflicts = snapshot.documents.mapNotNull { document ->
                document.data?.let { documentToObject(document.id, it) }
            }
            Log.d("FirebaseBookingRepository", "Found ${conflicts.size} conflicts for roomId $roomId, startTime: $startTime, endTime: $endTime")
            conflicts
        } catch (e: Exception) {
            Log.e("FirebaseBookingRepository", "Error checking conflicts for roomId $roomId: ${e.message}")
            emptyList()
        }
    }

    suspend fun getActiveBookingsForRoomOnDate(roomId: String, date: Date): List<FirebaseBooking> {
        val calendar = Calendar.getInstance().apply { time = date }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = Timestamp(calendar.time)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = Timestamp(calendar.time)

        return try {
            val snapshot = collection
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("status", "ACTIVE")
                .whereGreaterThanOrEqualTo("bookingDate", startOfDay)
                .whereLessThanOrEqualTo("bookingDate", endOfDay)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()
            val bookings = snapshot.documents.mapNotNull { document ->
                document.data?.let { documentToObject(document.id, it) }
            }
            Log.d("FirebaseBookingRepository", "Fetched ${bookings.size} active bookings for roomId $roomId on $date")
            bookings.forEach {
                Log.d("FirebaseBookingRepository", "Booking ID: ${it.id}, startTime: ${it.startTime}, endTime: ${it.endTime}")
            }
            bookings
        } catch (e: Exception) {
            Log.e("FirebaseBookingRepository", "Error fetching active bookings for roomId $roomId on $date: ${e.message}")
            emptyList()
        }
    }

    suspend fun getActiveBookingsCountForStudent(studentId: String): Int {
        return try {
            val now = Timestamp.now()
            val snapshot = collection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "ACTIVE")
                .whereGreaterThan("endTime", now)
                .get()
                .await()
            Log.d("FirebaseBookingRepository", "Fetched ${snapshot.documents.size} active bookings for studentId: $studentId")
            snapshot.documents.size
        } catch (e: Exception) {
            Log.e("FirebaseBookingRepository", "Error fetching active bookings count for studentId $studentId: ${e.message}")
            0
        }
    }

    fun fromBooking(booking: Booking): FirebaseBooking {
        return FirebaseBooking(
            id = "",
            studentId = booking.studentId.toString(),
            roomId = booking.roomId,
            building = booking.building,
            campus = booking.campus,
            level = booking.level,
            bookingDate = booking.bookingDate,
            startTime = booking.startTime,
            endTime = booking.endTime,
            status = booking.status
        )
    }
}