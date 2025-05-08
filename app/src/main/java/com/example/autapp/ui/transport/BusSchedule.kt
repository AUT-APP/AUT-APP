package com.example.autapp.ui.transport

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(tableName = "bus_schedules")
data class BusSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val departureTime: LocalTime,
    val arrivalTime: LocalTime,
    val route: String = "City to South Campus" // can expand this later for different routes
) 