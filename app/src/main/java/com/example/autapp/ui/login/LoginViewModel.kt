package com.example.autapp.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.repository.*
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var loginResult by mutableStateOf<String?>(null)
        private set

    fun updateUsername(newUsername: String) {
        username = newUsername
    }

    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    fun login(onSuccess: (Int) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val isValid = userRepository.checkUser(username, password)
                if (!isValid) {
                    loginResult = "Invalid credentials"
                    onFailure("Invalid credentials")
                    return@launch
                }

                val student = studentRepository.getStudentByUsername(username)
                if (student?.studentId != null) {
                    loginResult = "Login successful"
                    onSuccess(student.studentId)
                } else {
                    loginResult = "Error: Student not found"
                    onFailure("Error: Student not found")
                }
            } catch (e: Exception) {
                loginResult = "Login error: ${e.message}"
                onFailure("Login error: ${e.message}")
            }
        }
    }

    fun reset() {
        username = ""
        password = ""
        loginResult = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as AUTApplication

                LoginViewModel(
                    application.userRepository,
                    application.studentRepository,
                )
            }
        }
    }
}