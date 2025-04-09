package com.example.autapp.data.models
import java.util.Date

class Booking(
    var id: Int,
    var location: String,
    var startTime: Date,
    var endTime: Date,
    var bookedBy: String,
    var bookingType: String
) {
    fun getId() = id
    fun getLocation() = location
    fun getStartTime() = startTime
    fun getEndTime() = endTime
    fun getBookedBy() = bookedBy
    fun getBookingType() = bookingType

    fun setLocation(loc: String) { location = loc }
    fun setStartTime(time: Date) { startTime = time }
    fun setEndTime(time: Date) { endTime = time }
    fun setBookedBy(user: String) { bookedBy = user }
    fun setBookingType(type: String) { bookingType = type }

    fun updateBooking(loc: String, start: Date, end: Date, type: String) {
        location = loc
        startTime = start
        endTime = end
        bookingType = type
    }
}
