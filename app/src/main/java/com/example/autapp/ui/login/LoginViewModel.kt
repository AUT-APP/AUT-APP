package com.example.autapp.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autapp.data.models.Student
import com.example.autapp.data.models.User
import com.example.autapp.data.repository.StudentRepository
import com.example.autapp.data.repository.UserRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    var username by mutableStateOf("")  // Observable state
        private set

    var password by mutableStateOf("")  // Observable state
        private set

    var loginResult by mutableStateOf<String?>(null)  // Observable state
        private set

    fun updateUsername(newUsername: String) {  // Public setter
        username = newUsername
    }

    fun updatePassword(newPassword: String) {  // Public setter
        password = newPassword
    }

    fun checkLogin() {
        viewModelScope.launch {
            try {
                val isValid = userRepository.checkUser(username, password)
                loginResult = if (isValid) "Login successful" else "Invalid credentials"
                if (isValid) {
                    val student = studentRepository.getStudentByUsername(username)
                    if (student != null) {
                        val studentWithCourses = studentRepository.getStudentWithCourses(student.studentId)
                        loginResult += " - Courses: ${studentWithCourses?.courses?.joinToString { it.name }}"
                    }
                }
            } catch (e: Exception) {
                loginResult = "Login error: ${e.message}"
            }
        }
    }

    fun insertTestData() {
        viewModelScope.launch {
            try {
                val testUser = User("John", "Doe", 0, "Student", "johndoe", "12345")
                val testStudent = Student("John", "Doe", 0, "johndoe", "12345", 1001, "2023-01-01", "CS", 1, 3.5)
                userRepository.insertUser(testUser)
                studentRepository.insertStudent(testStudent)
            } catch (e: Exception) {
                loginResult = "Test data insertion failed: ${e.message}"
            }
        }
    }
}