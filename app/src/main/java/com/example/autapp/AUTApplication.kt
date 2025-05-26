package com.example.autapp

import android.app.Application
import com.example.autapp.data.database.AUTDatabase
import com.example.autapp.data.repository.*
import com.example.autapp.util.NotificationHelper
import com.jakewharton.threetenabp.AndroidThreeTen

class AUTApplication : Application() {
    // Initialize Room database (version 30 with migrations for Teacher role and ActivityLog)
    val database: AUTDatabase by lazy {
        AUTDatabase.getDatabase(this)
    }

    // Initialize repositories
    val userRepository by lazy { UserRepository(database.userDao()) }
    val studentRepository by lazy { StudentRepository(database.studentDao(), database.userDao()) }
    val teacherRepository by lazy { TeacherRepository(database.teacherDao(), database.userDao()) }
    val adminRepository by lazy { AdminRepository(database.adminDao(), database.userDao()) }
    val courseRepository by lazy { CourseRepository(database.courseDao()) }
    val departmentRepository by lazy { DepartmentRepository(database.departmentDao()) }
    val assignmentRepository by lazy { AssignmentRepository(database.assignmentDao()) }
    val gradeRepository by lazy { GradeRepository(database.gradeDao(), assignmentRepository) }
    val timetableEntryRepository by lazy { TimetableEntryRepository(database.timetableEntryDao()) }
    val notificationRepository by lazy {
        NotificationRepository(
            database.notificationDao(),
            database.timetableNotificationPreferenceDao()
        )
    }
    val eventRepository by lazy { EventRepository(database.eventDao()) }
    val bookingRepository by lazy {
        BookingRepository(database.bookingDao(), database.studySpaceDao())
    }
    val studySpaceRepository by lazy {
        StudySpaceRepository(database.studySpaceDao())
    }
    val timetableNotificationPreferenceRepository by lazy {
        TimetableNotificationPreferenceRepository(database.timetableNotificationPreferenceDao())
    }
    val activityLogRepository by lazy { ActivityLogRepositoryImpl(database.activityLogDao()) }

    override fun onCreate() {
        super.onCreate()
        // Initialize ThreeTenABP for date/time handling
        AndroidThreeTen.init(this)
        // Create notification channels for the app
        NotificationHelper.createNotificationChannels(this)
        android.util.Log.d("AUTApplication", "Application initialized")
    }
}