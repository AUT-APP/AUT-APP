package com.example.autapp

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import com.example.autapp.util.NotificationHelper
import com.example.autapp.data.firebase.FirebaseBusScheduleRepository

class AUTApplication : Application() {
    // Initialize Firebase repositories
    val userRepository by lazy { com.example.autapp.data.firebase.FirebaseUserRepository() }
    val teacherRepository by lazy { com.example.autapp.data.firebase.FirebaseTeacherRepository() }
    val courseRepository by lazy { com.example.autapp.data.firebase.FirebaseCourseRepository(teacherRepository) }
    val studentRepository by lazy { com.example.autapp.data.firebase.FirebaseStudentRepository(courseRepository) }
    val adminRepository by lazy { com.example.autapp.data.firebase.FirebaseAdminRepository() }
    val departmentRepository by lazy { com.example.autapp.data.firebase.FirebaseDepartmentRepository() }
    val assignmentRepository by lazy { com.example.autapp.data.firebase.FirebaseAssignmentRepository() }
    val gradeRepository by lazy { com.example.autapp.data.firebase.FirebaseGradeRepository() }
    val timetableEntryRepository by lazy { com.example.autapp.data.firebase.FirebaseTimetableRepository() }
    val studySpaceRepository by lazy { com.example.autapp.data.firebase.FirebaseStudySpaceRepository() }
    val bookingRepository by lazy { com.example.autapp.data.firebase.FirebaseBookingRepository() }
    val notificationRepository by lazy { com.example.autapp.data.firebase.FirebaseNotificationRepository() }
    val activityLogRepository by lazy { com.example.autapp.data.firebase.FirebaseActivityLogRepository() }
    val eventRepository by lazy { com.example.autapp.data.firebase.FirebaseEventRepository() }
    val timetableNotificationPreferenceRepository by lazy { com.example.autapp.data.firebase.FirebaseTimetableNotificationPreferenceRepository() }
    val bookingNotificationPreferenceRepository by lazy { com.example.autapp.data.firebase.FirebaseBookingNotificationPreferenceRepository() }
    val eventNotificationPreferenceRepository by lazy { com.example.autapp.data.firebase.FirebaseEventNotificationPreferenceRepository() }
    val busScheduleRepository by lazy { FirebaseBusScheduleRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance()) }

    override fun onCreate() {
        super.onCreate()
        // Initialize ThreeTenABP for date/time handling
        AndroidThreeTen.init(this)
        // Create notification channels for the app
        NotificationHelper.createNotificationChannels(this)
        android.util.Log.d("AUTApplication", "Application initialized")
    }
}