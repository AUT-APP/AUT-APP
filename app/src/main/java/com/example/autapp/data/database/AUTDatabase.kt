package com.example.autapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.autapp.data.dao.*
import com.example.autapp.data.models.*

@Database(
    entities = [User::class, Student::class, Teacher::class, Course::class, StudentCourseCrossRef::class],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AUTDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun studentDao(): StudentDao
    abstract fun teacherDao(): TeacherDao
    abstract fun courseDao(): CourseDao

    companion object {
        @Volatile
        private var INSTANCE: AUTDatabase? = null

        fun getDatabase(context: Context): AUTDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AUTDatabase::class.java,
                    "AUT_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}