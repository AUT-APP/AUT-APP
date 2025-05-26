package com.example.autapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.autapp.data.repository.ActivityLogRepository
import com.example.autapp.data.repository.CourseRepository
import com.example.autapp.data.repository.DepartmentRepository
import com.example.autapp.data.repository.StudentRepository
import com.example.autapp.data.repository.TeacherRepository

class AdminDashboardViewModelFactory(
    private val studentRepository: StudentRepository,
    private val teacherRepository: TeacherRepository,
    private val courseRepository: CourseRepository,
    private val departmentRepository: DepartmentRepository,
    private val activityLogRepository: ActivityLogRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminDashboardViewModel(
                studentRepository,
                teacherRepository,
                courseRepository,
                departmentRepository,
                activityLogRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}