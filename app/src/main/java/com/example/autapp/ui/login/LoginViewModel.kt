package com.example.autapp.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.repository.StudentRepository
import com.example.autapp.data.repository.TeacherRepository
import com.example.autapp.data.repository.UserRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var loginResult by mutableStateOf<String?>(null)
        private set

    var dob by mutableStateOf<String?>(null)
        private set

    private var currentUserRole: String? = null

    fun updateUsername(newUsername: String) {
        username = newUsername
    }

    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    fun login(onSuccess: (Int, String, Boolean, String?) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Hardcoded admin login
                if (username == "admin" && password == "admin123") {
                    loginResult = "Login successful"
                    currentUserRole = "Admin"
                    onSuccess(0, "Admin", false, null)
                    return@launch
                }

                // Check user credentials
                val user = userRepository.getUserByUsername(username)
                if (user == null || user.password != password) {
                    loginResult = "Invalid credentials"
                    onFailure("Invalid credentials")
                    return@launch
                }

                when (user.role) {
                    "Student" -> {
                        val student = studentRepository.getStudentByUsername(username)
                        if (student?.studentId != null) {
                            loginResult = "Login successful"
                            dob = student.dob
                            currentUserRole = "Student"
                            onSuccess(student.studentId, "Student", user.isFirstLogin, student.dob)
                        } else {
                            loginResult = "Error: Student not found"
                            onFailure("Error: Student not found")
                        }
                    }
                    "Teacher" -> {
                        val teacher = teacherRepository.getTeacherByUsername(username)
                        if (teacher?.teacherId != null) {
                            loginResult = "Login successful"
                            dob = teacher.dob
                            currentUserRole = "Teacher"
                            onSuccess(teacher.teacherId, "Teacher", user.isFirstLogin, teacher.dob)
                        } else {
                            loginResult = "Error: Teacher not found"
                            onFailure("Error: Teacher not found")
                        }
                    }
                    "Admin" -> {
                        loginResult = "Login successful"
                        currentUserRole = "Admin"
                        onSuccess(0, "Admin", false, null)
                    }
                    else -> {
                        loginResult = "Error: Invalid user role"
                        onFailure("Error: Invalid user role")
                    }
                }
            } catch (e: Exception) {
                loginResult = "Login error: ${e.message}"
                onFailure("Login error: ${e.message}")
            }
        }
    }

    fun updateUserPassword(username: String, newPassword: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val user = userRepository.getUserByUsername(username)
                if (user != null) {
                    val updatedUser = user.copy(password = newPassword, isFirstLogin = false)
                    userRepository.updateUser(updatedUser)
                    // Update password in Student or Teacher table if applicable
                    when (user.role) {
                        "Student" -> {
                            val student = studentRepository.getStudentByUsername(username)
                            if (student != null) {
                                studentRepository.updateStudent(student.copy(password = newPassword))
                            }
                        }
                        "Teacher" -> {
                            val teacher = teacherRepository.getTeacherByUsername(username)
                            if (teacher != null) {
                                teacherRepository.updateTeacher(teacher.copy(password = newPassword))
                            }
                        }
                    }
                    onSuccess()
                } else {
                    onFailure("User not found")
                }
            } catch (e: Exception) {
                onFailure("Error updating password: ${e.message}")
            }
        }
    }

    fun getUserRole(): String? {
        return currentUserRole
    }

    fun reset() {
        username = ""
        password = ""
        loginResult = null
        dob = null
        currentUserRole = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AUTApplication
                LoginViewModel(
                    userRepository = application.userRepository,
                    studentRepository = application.studentRepository,
                    teacherRepository = application.teacherRepository
                )
            }
        }
    }
}