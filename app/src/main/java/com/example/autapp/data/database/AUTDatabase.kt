package com.example.autapp.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.autapp.data.dao.*
import com.example.autapp.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        Notification::class,
        Admin::class,
        Department::class,
        ActivityLog::class
    ],
    version = 20,
    exportSchema = true
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
    abstract fun adminDao(): AdminDao
    abstract fun departmentDao(): DepartmentDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: AUTDatabase? = null

        // Migration from 28 to 29 (no-op, as per original)
        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("AUTDatabase", "Migrating from version 28 to 29")
                // No schema changes; preserves existing data
            }
        }

        // Migration from 29 to 30: Add activity_log table
        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("AUTDatabase", "Migrating from version 29 to 30")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS activity_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        description TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AUTDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d("AUTDatabase", "Building new database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AUTDatabase::class.java,
                    "database"
                )
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Log.d("AUTDatabase", "Database created")
                            db.execSQL("PRAGMA foreign_keys = ON;")
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            Log.d("AUTDatabase", "Database opened")
                            db.execSQL("PRAGMA foreign_keys = ON;")
                            // Log current tables for debugging
                            val tables = db.query("SELECT name FROM sqlite_master WHERE type='table'")
                            tables.use {
                                while (it.moveToNext()) {
                                    Log.d("AUTDatabase", "Table on open: ${it.getString(0)}")
                                }
                            }
                        }
                    })
                    .addMigrations(MIGRATION_28_29, MIGRATION_29_30)
                    .fallbackToDestructiveMigration() // Wipe database if migrations are missing
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}