package com.example.autapp.ui.transport

import java.time.LocalTime

data class BusSchedule(
    val id: Int = 0,
    val departureTime: LocalTime,
    val arrivalTime: LocalTime,
    val route: String = "City to South Campus" // can expand this later for different routes
) 