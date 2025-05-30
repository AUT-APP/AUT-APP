package com.example.autapp.data.models

import java.util.Calendar
import java.util.Date
import com.example.autapp.data.firebase.*

data class Booking(
    val bookingId: Int = 0,
    val studentId: String,
    val roomId: String,
    val building: String,
    val campus: String,
    val level: String,
    val bookingDate: Date,
    val startTime: Date,
    val endTime: Date,
    val status: String = BookingStatus.ACTIVE,
    val documentId: String? = null
) {
    init {
        val durationMinutes = (endTime.time - startTime.time) / (1000 * 60)
        require(durationMinutes >= 30) { "Booking duration must be at least 30 minutes" }
        require(durationMinutes <= 120) { "Booking duration cannot exceed 2 hours" }
        require(startTime.before(endTime)) { "Start time must be before end time" }
        // Ensure bookingDate is the same day as startTime
        val calendarBooking = Calendar.getInstance().apply { time = bookingDate }
        val calendarStart = Calendar.getInstance().apply { time = startTime }
        require(
            calendarBooking.get(Calendar.YEAR) == calendarStart.get(Calendar.YEAR) &&
                    calendarBooking.get(Calendar.DAY_OF_YEAR) == calendarStart.get(Calendar.DAY_OF_YEAR)
        ) { "Booking date must match start time date" }
        // Prevent bookings more than 30 days in the future
        val maxFutureDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 30) }.time
        require(bookingDate.before(maxFutureDate)) { "Booking cannot be more than 30 days in the future" }
    }

    val isActive: Boolean
        get() = status == BookingStatus.ACTIVE && Date().before(endTime)

    val isUpcoming: Boolean
        get() = status == BookingStatus.ACTIVE && startTime.after(Date())

    val isCompleted: Boolean
        get() = status == BookingStatus.CANCELED || Date().after(endTime)

    override fun toString(): String {
        return "Booking(bookingId=$bookingId, studentId=$studentId, roomId='$roomId', building='$building', " +
                "campus='$campus', level='$level', bookingDate=$bookingDate, startTime=$startTime, endTime=$endTime, " +
                "status='$status', documentId='$documentId')"
    }

}
object BookingStatus {
    const val ACTIVE = "ACTIVE"
    const val CANCELED = "CANCELED"
}