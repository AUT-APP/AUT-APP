package com.example.autapp.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.example.autapp.AUTApplication
import com.example.autapp.data.models.Department
import com.example.autapp.data.models.Student
import com.example.autapp.data.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TestDataInitializer {
    private const val TAG = "TestDataInitializer"
    private const val PREFS_NAME = "AUTAppPrefs"
    private const val KEY_INITIALIZED = "is_initialized"

    // Get SharedPreferences
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Check if database has been initialized
    private fun isDatabaseInitialized(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_INITIALIZED, false)
    }

    // Mark database as initialized
    private fun setDatabaseInitialized(context: Context) {
        getSharedPreferences(context).edit().putBoolean(KEY_INITIALIZED, true).apply()
    }

    suspend fun initializeDatabase(context: Context) {
        if (isDatabaseInitialized(context)) {
            Log.d(TAG, "Database already initialized, skipping data insertion")
            return
        }

        val app = context.applicationContext as AUTApplication
        val userRepository = app.userRepository
        val studentRepository = app.studentRepository
        val departmentRepository = app.departmentRepository

        try {
            withContext(Dispatchers.IO) {
                // Insert an admin user for AdminDashboardScreen access
                val adminUser = User(
                    id = 0, // Auto-generated
                    firstName = "Admin",
                    lastName = "User",
                    username = "admin",
                    password = "admin123",
                    role = "Admin"
                )
                userRepository.insertUser(adminUser)
                Log.d(TAG, "Inserted admin user: ${adminUser.username}")

                // Insert a test student
                val testStudent = Student(
                    id = 0,
                    firstName = "Test",
                    lastName = "Student",
                    studentId = 1000,
                    username = "teststudent",
                    password = "password123",
                    role = "Student",
                    enrollmentDate = "2024-01-01",
                    majorId = 1, // References Computer Science
                    minorId = null,
                    yearOfStudy = 2,
                    gpa = 0.0,
                    dob = "2004-05-10"
                )
                studentRepository.insertStudent(testStudent)
                Log.d(TAG, "Inserted test student: ${testStudent.firstName} ${testStudent.lastName}")

                // Insert departments (Major, Minor, Department)
                val departments = listOf(
                    Department(
                        departmentId = 0, // Auto-generated
                        name = "Computer Science",
                        type = "Major",
                        description = "Department of Computer Science"
                    ),
                    Department(
                        departmentId = 0,
                        name = "Mathematics",
                        type = "Minor",
                        description = "Department of Mathematics"
                    ),
                    Department(
                        departmentId = 0,
                        name = "Engineering",
                        type = "Major",
                        description = "Department of Engineering"
                    ),
                    Department(
                        departmentId = 0,
                        name = "Academic",
                        type = "Department",
                        description = "General Academic Department"
                    )
                )
                departments.forEach { department ->
                    departmentRepository.insertDepartment(department)
                    Log.d(TAG, "Inserted department: ${department.name} (${department.type})")
                }

                // Verify insertions
                val users = userRepository.getAllUsers()
                val students = studentRepository.getAllStudents()
                val depts = departmentRepository.getAllDepartments()
                Log.d(TAG, "Inserted ${users.size} users: $users")
                Log.d(TAG, "Inserted ${students.size} students: $students")
                Log.d(TAG, "Inserted ${depts.size} departments: $depts")

                if (students.isEmpty() || depts.isEmpty()) {
                    Log.e(TAG, "Failed to insert initial data")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to insert initial data!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Mark database as initialized
                    setDatabaseInitialized(context)
                    Log.d(TAG, "Database initialization completed successfully")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing database: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error initializing database: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}