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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.autapp.data.models.User

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

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

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
                    _currentUser.value = null // No user object for admin
                    onSuccess(0, "Admin", false, null)
                    return@launch
                }

                // Check user credentials
                val user = userRepository.getUserByUsername(username)
                val isValid = user != null && userRepository.checkUser(username, password)

                if (!isValid) {
                    loginResult = "Invalid credentials"
                    _currentUser.value = null
                    withContext(Dispatchers.Main) { onFailure("Invalid credentials") }
                    return@launch
                }

                if (user != null) {
                    _currentUser.value = user
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
                                _currentUser.value = null
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
                                _currentUser.value = null
                                onFailure("Error: Teacher not found")
                            }
                        }
                        else -> {
                            loginResult = "Error: Invalid user role"
                            _currentUser.value = null
                            onFailure("Error: Invalid user role")
                        }
                    }
                }

            } catch (e: Exception) {
                loginResult = "Login error: ${e.message}"
                _currentUser.value = null
                withContext(Dispatchers.Main) { onFailure("Login error: ${e.message}") }
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
        _currentUser.value = null
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