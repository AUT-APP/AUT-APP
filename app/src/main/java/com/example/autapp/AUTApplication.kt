package com.example.autapp

import android.app.Application
import com.example.autapp.data.database.AUTDatabase
import com.example.autapp.data.models.StudySpace
import com.example.autapp.data.repository.AssignmentRepository
import com.example.autapp.data.repository.BookingRepository
import com.example.autapp.data.repository.CourseRepository
import com.example.autapp.data.repository.EventRepository
import com.example.autapp.data.repository.GradeRepository
import com.example.autapp.data.repository.MaterialRepository
import com.example.autapp.data.repository.NotificationRepository
import com.example.autapp.data.repository.StudentRepository
import com.example.autapp.data.repository.StudySpaceRepository
import com.example.autapp.data.repository.TimetableEntryRepository
import com.example.autapp.data.repository.TimetableNotificationPreferenceRepository
import com.example.autapp.data.repository.UserRepository
import com.example.autapp.util.NotificationHelper
import com.jakewharton.threetenabp.AndroidThreeTen

class AUTApplication : Application() {

    // Initialize Room database
    val database: AUTDatabase by lazy {
        AUTDatabase.resetInstance()
        AUTDatabase.getDatabase(this)
    }

    // Initialize repositories
    val userRepository by lazy { UserRepository(database.userDao()) }
    val studentRepository by lazy { StudentRepository(database.studentDao(), database.userDao()) }
    val materialRepository by lazy { MaterialRepository(database.materialDao()) }
    val courseRepository by lazy { CourseRepository(database.courseDao()) }
    val assignmentRepository by lazy { AssignmentRepository(database.assignmentDao()) }
    val gradeRepository by lazy { GradeRepository(database.gradeDao(), assignmentRepository) }
    val timetableEntryRepository by lazy { TimetableEntryRepository(database.timetableEntryDao()) }
    val notificationRepository by lazy {
        NotificationRepository(database.notificationDao(),
        database.timetableNotificationPreferenceDao())
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

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        // Initialize Notification channels
        NotificationHelper.createNotificationChannels(this)
    }
}