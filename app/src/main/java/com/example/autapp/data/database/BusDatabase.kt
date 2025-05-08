package com.example.autapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.autapp.data.dao.BusScheduleDao
import com.example.autapp.ui.transport.BusSchedule

/**
 * Main database class for the bus schedule application.
 * Uses Room persistence library to create and manage the SQLite database.
 */
@Database(entities = [BusSchedule::class], version = 1)
@TypeConverters(Converters::class)
abstract class BusDatabase : RoomDatabase() {
    // Abstract method to get the DAO (Data Access Object) for bus schedules
    abstract fun busScheduleDao(): BusScheduleDao

    companion object {
        // Volatile annotation ensures that the INSTANCE value is always up to date
        @Volatile
        private var INSTANCE: BusDatabase? = null

        fun getDatabase(context: Context): BusDatabase {
            return INSTANCE ?: synchronized(this) {
                // Create database instance if it doesn't exist
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BusDatabase::class.java,
                    "bus_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 