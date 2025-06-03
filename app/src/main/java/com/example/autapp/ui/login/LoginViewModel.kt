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
import com.example.autapp.data.firebase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

class LoginViewModel(
    private val userRepository: FirebaseUserRepository,
    private val studentRepository: FirebaseStudentRepository,
    private val teacherRepository: FirebaseTeacherRepository
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

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    fun updateUsername(newUsername: String) {
        username = newUsername
    }

    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    fun login(onSuccess: (String, String, Boolean, String?) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Hardcoded admin login
                if (username == "admin" && password == "admin123") {
                    loginResult = "Login successful"
                    currentUserRole = "Admin"
                    _currentUser.value = null // No user object for admin
                    Log.d("LoginViewModel", "Admin login successful")
                    onSuccess("0", "Admin", false, null)
                    return@launch
                }

                // Use Firebase Authentication to sign in
                val user = userRepository.signIn(username, password)

                if (user != null) {
                    _currentUser.value = user
                    when (user.role) {
                        "Student" -> {
                            // Get student details using the authenticated user's ID
                            val student = studentRepository.getById(user.id)
                            if (student != null) {
                                loginResult = "Login successful"
                                dob = student.dob
                                currentUserRole = "Student"
                                Log.d("LoginViewModel", "Student login successful: ${user.username}, isFirstLogin: ${user.isFirstLogin}")
                                onSuccess(user.id, "Student", user.isFirstLogin, student.dob)
                            } else {
                                loginResult = "Error: Student profile not found"
                                _currentUser.value = null
                                Log.d("LoginViewModel", "Student profile not found for user ID: ${user.id}")
                                onFailure("Error: Student profile not found")
                            }
                        }
                        "Teacher" -> {
                            // Get teacher details using the authenticated user's ID
                            val teacher = teacherRepository.getById(user.id)
                            if (teacher != null) {
                                loginResult = "Login successful"
                                dob = teacher.dob
                                currentUserRole = "Teacher"
                                Log.d("LoginViewModel", "Teacher login successful: ${user.username}, isFirstLogin: ${user.isFirstLogin}")
                                onSuccess(user.id, "Teacher", user.isFirstLogin, teacher.dob)
                            } else {
                                loginResult = "Error: Teacher profile not found"
                                _currentUser.value = null
                                Log.d("LoginViewModel", "Teacher profile not found for user ID: ${user.id}")
                                onFailure("Error: Teacher profile not found")
                            }
                        }
                        else -> {
                            loginResult = "Error: Invalid user role"
                            _currentUser.value = null
                            Log.d("LoginViewModel", "Invalid user role for username: ${user.username}")
                            onFailure("Error: Invalid user role")
                        }
                    }
                }

            } catch (e: Exception) {
                loginResult = "Login error: ${e.message}"
                _currentUser.value = null
                Log.e("LoginViewModel", "Login error: ${e.message}", e)
                withContext(Dispatchers.Main) { onFailure("Login error: ${e.message}") }
            }
        }
    }

    fun updateUserPassword(username: String, newPassword: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Find the user by username first to get their ID
                val user = userRepository.getUserByUsername(username)
                if (user != null) {
                    // Update password using Firebase Authentication
                    userRepository.updatePassword(newPassword)
                    // Update isFirstLogin flag in the Firestore user document
                    userRepository.updateProfile(user.copy(isFirstLogin = false))
                    Log.d("LoginViewModel", "Password updated for: $username, isFirstLogin set to false")
                    onSuccess()
                } else {
                    Log.d("LoginViewModel", "User not found for password update: $username")
                    onFailure("User not found")
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error updating password: ${e.message}", e)
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
        Log.d("LoginViewModel", "State reset")
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