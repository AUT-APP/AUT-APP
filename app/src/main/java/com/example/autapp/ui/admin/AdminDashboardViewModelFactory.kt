package com.example.autapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.autapp.data.firebase.*

class AdminDashboardViewModelFactory(
    private val studentRepository: FirebaseStudentRepository,
    private val teacherRepository: FirebaseTeacherRepository,
    private val courseRepository: FirebaseCourseRepository,
    private val departmentRepository: FirebaseDepartmentRepository,
    private val activityLogRepository: FirebaseActivityLogRepository,
    private val userRepository: FirebaseUserRepository,
    private val timetableEntryRepository: FirebaseTimetableRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminDashboardViewModel(
                studentRepository,
                teacherRepository,
                courseRepository,
                departmentRepository,
                activityLogRepository,
                userRepository,
                timetableEntryRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}