package com.example.autapp.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.repository.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.autapp.data.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun updateUsername(newUsername: String) {
        username = newUsername
    }

    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    fun login(onSuccess: (Int) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
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

                    val student = studentRepository.getStudentByUsername(username)
                    if (student?.studentId != null) {
                        loginResult = "Login successful"
                        withContext(Dispatchers.Main) { onSuccess(student.studentId) }
                    } else if (user.role == "Teacher") {
                        loginResult = "Teacher login successful"
                        withContext(Dispatchers.Main) { onSuccess(user.id) }
                    } else {
                        loginResult = "Login successful, but not a recognized role"
                        withContext(Dispatchers.Main) { onSuccess(user.id) }
                    }
                }

            } catch (e: Exception) {
                loginResult = "Login error: ${e.message}"
                _currentUser.value = null
                withContext(Dispatchers.Main) { onFailure("Login error: ${e.message}") }
            }
        }
    }

    fun reset() {
        username = ""
        password = ""
        loginResult = null
        _currentUser.value = null
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