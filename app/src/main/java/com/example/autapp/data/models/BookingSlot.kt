package com.example.autapp.data.models

data class BookingSlot(
    val roomId: String,
    val building: String,
    val campus: String,
    val level: String,
    val timeSlot: String, // e.g., "09:00"
    val status: SlotStatus
)
enum class SlotStatus {
    AVAILABLE,
    BOOKED,
    IN_USE,
    MY_BOOKING,
    PAST
}