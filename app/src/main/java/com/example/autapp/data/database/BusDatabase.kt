package com.example.autapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.autapp.data.dao.BusScheduleDao
import com.example.autapp.ui.transport.BusSchedule

@Database(entities = [BusSchedule::class], version = 1)
@TypeConverters(Converters::class)
abstract class BusDatabase : RoomDatabase() {
    abstract fun busScheduleDao(): BusScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: BusDatabase? = null

        fun getDatabase(context: Context): BusDatabase {
            return INSTANCE ?: synchronized(this) {
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