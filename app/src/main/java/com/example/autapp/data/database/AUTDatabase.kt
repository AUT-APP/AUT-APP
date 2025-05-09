package com.example.autapp.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.autapp.data.dao.*
import com.example.autapp.data.models.*

@Database(
    entities = [
        User::class,
        Student::class,
        Teacher::class,
        Course::class,
        StudentCourseCrossRef::class,
        Grade::class,
        Assignment::class,
        TimetableEntry::class,
        TimetableNotificationPreference::class,
        Event::class,
        Booking::class,
        StudySpace::class,
        Notification::class
    ],
    version = 24,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AUTDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun studentDao(): StudentDao
    abstract fun teacherDao(): TeacherDao
    abstract fun courseDao(): CourseDao
    abstract fun gradeDao(): GradeDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun timetableEntryDao(): TimetableEntryDao
    abstract fun eventDao(): EventDao
    abstract fun bookingDao(): BookingDao
    abstract fun studySpaceDao(): StudySpaceDao
    abstract fun notificationDao(): NotificationDao
    abstract fun timetableNotificationPreferenceDao(): TimetableNotificationPreferenceDao


    companion object {
        @Volatile
        private var INSTANCE: AUTDatabase? = null
        fun getDatabase(context: Context): AUTDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d("AUTDatabase", "Building new database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AUTDatabase::class.java,
                    "AUT_database_v24" // Change to Notification model
                )
                    .fallbackToDestructiveMigration()
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            Log.d("AUTDatabase", "Database created")
                            // Enable foreign key constraints
                            db.execSQL("PRAGMA foreign_keys = ON;")
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            Log.d("AUTDatabase", "Database opened")
                            // Enable foreign key constraints
                            db.execSQL("PRAGMA foreign_keys = ON;")
                            // Reset auto-increment counters by clearing sqlite_sequence
                            db.execSQL("DELETE FROM sqlite_sequence")
                            Log.d("AUTDatabase", "Auto-increment counters reset")
                            // Log current tables for debugging
                            val tables =
                                db.query("SELECT name FROM sqlite_master WHERE type='table'")
                            tables.use {
                                while (it.moveToNext()) {
                                    Log.d("AUTDatabase", "Table on open: ${it.getString(0)}")
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun resetInstance() {
            INSTANCE = null
            Log.d("AUTDatabase", "Database instance reset")
        }
    }
}