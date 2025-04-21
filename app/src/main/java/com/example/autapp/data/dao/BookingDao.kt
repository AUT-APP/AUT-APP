package com.example.autapp.data.dao
import androidx.room.*
import com.example.autapp.data.models.Booking
import com.example.autapp.data.models.BookingStatus
import java.util.*@Dao

interface BookingDao {
    @Insert
    suspend fun insertBooking(booking: Booking)

    @Query("SELECT * FROM booking_table WHERE bookingId = :bookingId")
    suspend fun getBookingById(bookingId: Int): Booking?

    @Query("SELECT * FROM booking_table WHERE studentId = :studentId")
    suspend fun getAllBookingsByStudent(studentId: Int): List<Booking>

    @Query("SELECT * FROM booking_table WHERE studentId = :studentId AND status = :status")
    suspend fun getBookingsByStudentAndStatus(studentId: Int, status: String): List<Booking>

    @Query(
        "SELECT * FROM booking_table WHERE roomId = :roomId AND " +
                "DATE(bookingDate / 1000, 'unixepoch') = DATE(:bookingDate / 1000, 'unixepoch')"
    )
    suspend fun getBookingsForRoomAndDate(roomId: String, bookingDate: Date): List<Booking>

    @Query("SELECT * FROM booking_table WHERE roomId = :roomId AND (:startTime < endTime AND :endTime > startTime)")
    suspend fun checkBookingConflict(roomId: String, startTime: Date, endTime: Date): List<Booking>

    @Query("SELECT * FROM booking_table WHERE roomId = :roomId AND startTime > :startTime ORDER BY startTime LIMIT 1")
    suspend fun getNextBooking(roomId: String, startTime: Date): Booking?

    @Query("DELETE FROM booking_table WHERE endTime < :currentTime AND status = :status")
    suspend fun deleteCompletedBookings(currentTime: Date, status: String = BookingStatus.ACTIVE): Int

    @Query("DELETE FROM booking_table WHERE status = :status")
    suspend fun deleteCancelledBookings(status: String = BookingStatus.CANCELED): Int

    @Query("SELECT * FROM booking_table")
    suspend fun getAllBookings(): List<Booking>

    @Query("SELECT * FROM booking_table WHERE studentId = :studentId AND roomId = :roomId AND startTime = :startTime AND endTime = :endTime LIMIT 1")
    suspend fun getBookingByDetails(studentId: Int, roomId: String, startTime: Date, endTime: Date): Booking?

    @Update
    suspend fun updateBooking(booking: Booking)

    @Delete
    suspend fun deleteBooking(booking: Booking)

    @Query("DELETE FROM booking_table")
    suspend fun deleteAll()

    @Transaction
    suspend fun withTransaction(block: suspend () -> Unit) {
        block()
    }

}

