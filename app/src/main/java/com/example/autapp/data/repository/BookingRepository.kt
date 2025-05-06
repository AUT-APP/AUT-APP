package com.example.autapp.data.repository
import android.util.Log
import com.example.autapp.data.dao.BookingDao
import com.example.autapp.data.dao.StudySpaceDao
import com.example.autapp.data.models.Booking
import com.example.autapp.data.models.BookingStatus
import java.util.Calendar
import java.util.Date

class BookingRepository(
    private val bookingDao: BookingDao,
    private val studySpaceDao: StudySpaceDao
)

{

    suspend fun insertBooking(booking: Booking) {
        Log.d("BookingRepository", "Attempting to insert booking: $booking")
        // Check maximum bookings per student
        val activeBookings = bookingDao.getBookingsByStudentAndStatus(booking.studentId, BookingStatus.ACTIVE)
        if (activeBookings.size >= 2) {
            Log.e("BookingRepository", "Failed: Cannot create more than 2 active bookings")
            throw IllegalStateException("Cannot create more than 2 active bookings")
        }

        // Check study space availability
        val studySpace = studySpaceDao.getStudySpaceById(booking.roomId)
        if (studySpace == null) {
            Log.e("BookingRepository", "Failed: Study space ${booking.roomId} does not exist")
            throw IllegalStateException("Study space does not exist")
        }
        if (!studySpace.isAvailable) {
            Log.e("BookingRepository", "Failed: Study space ${booking.roomId} is not available")
            throw IllegalStateException("Study space is not available")
        }

        // Check for booking conflicts
        val conflicts = bookingDao.checkBookingConflict(
            roomId = booking.roomId,
            startTime = booking.startTime,
            endTime = booking.endTime
        )
        if (conflicts.isNotEmpty()) {
            Log.e("BookingRepository", "Failed: Time slot conflict with existing bookings: $conflicts")
            throw IllegalStateException("This time slot is already booked")
        }

        // Ensure booking is not in the past
        val now = Date()
        if (booking.startTime.before(now)) {
            Log.e("BookingRepository", "Failed: Cannot book a time slot in the past. StartTime=${booking.startTime}, Now=$now")
            throw IllegalStateException("Cannot book a time slot in the past")
        }

        // Validate end time does not exceed 21:00
        val endOfDay = Calendar.getInstance().apply { time = booking.bookingDate }
        endOfDay.set(Calendar.HOUR_OF_DAY, 21)
        endOfDay.set(Calendar.MINUTE, 0)
        endOfDay.set(Calendar.SECOND, 0)
        endOfDay.set(Calendar.MILLISECOND, 0)
        if (booking.endTime.after(endOfDay.time)) {
            Log.e("BookingRepository", "Failed: Booking end time ${booking.endTime} exceeds 21:00")
            throw IllegalStateException("Booking cannot extend past 21:00")
        }

        // Use transaction to ensure insertion is committed
        bookingDao.withTransaction {
            bookingDao.insertBooking(booking)
            // Verify insertion
            val insertedBooking = bookingDao.getBookingByDetails(
                booking.studentId,
                booking.roomId,
                booking.startTime,
                booking.endTime
            )
            if (insertedBooking == null) {
                Log.e("BookingRepository", "Booking not found after insertion: $booking")
                throw IllegalStateException("Booking insertion failed: not found in database")
            } else {
                Log.d("BookingRepository", "Booking verified in database: $insertedBooking")
            }
        }
        Log.d("BookingRepository", "Booking inserted successfully: $booking")
    }

    suspend fun getBookingById(bookingId: Int): Booking? {
        return bookingDao.getBookingById(bookingId)
    }

    suspend fun getBookingsByStudent(studentId: Int): List<Booking> {
        return bookingDao.getAllBookingsByStudent(studentId)
    }

    suspend fun getActiveBookingsByStudent(studentId: Int): List<Booking> {
        return bookingDao.getBookingsByStudentAndStatus(studentId, BookingStatus.ACTIVE)
    }

    suspend fun checkBookingConflict(roomId: String, startTime: Date, endTime: Date): List<Booking> {
        return bookingDao.checkBookingConflict(roomId, startTime, endTime)
    }

    suspend fun getNextBooking(roomId: String, startTime: Date): Booking? {
        return bookingDao.getNextBooking(roomId, startTime)
    }

    suspend fun getBookingsForRoomAndDate(roomId: String, date: Date): List<Booking> {
        return bookingDao.getBookingsForRoomAndDate(roomId, date)
    }

    suspend fun cancelBooking(booking: Booking) {
        deleteBooking(booking)
    }

    suspend fun deleteBooking(booking: Booking) {
        bookingDao.deleteBooking(booking)
    }

    suspend fun updateBooking(booking: Booking) {
        bookingDao.updateBooking(booking)
    }

    suspend fun deleteAll() {
        bookingDao.deleteAll()
    }

    suspend fun getAllBookings(): List<Booking> {
        return bookingDao.getAllBookings()
    }

    suspend fun deleteCompletedAndCancelledBookings() {
        Log.d("BookingRepository", "Deleting completed and cancelled bookings")
        val currentTime = Date()
        // Log bookings before deletion
        val allBookings = bookingDao.getAllBookings()
        Log.d("BookingRepository", "Bookings before deletion: ${allBookings.size}")
        allBookings.forEach { Log.d("BookingRepository", "Booking: $it") }

        val deletedCompleted = bookingDao.deleteCompletedBookings(currentTime)
        val deletedCancelled = bookingDao.deleteCancelledBookings()
        Log.d("BookingRepository", "Deleted $deletedCompleted completed bookings and $deletedCancelled cancelled bookings")

        // Log bookings after deletion
        val remainingBookings = bookingDao.getAllBookings()
        Log.d("BookingRepository", "Bookings after deletion: ${remainingBookings.size}")
        remainingBookings.forEach { Log.d("BookingRepository", "Booking: $it") }
    }

}

