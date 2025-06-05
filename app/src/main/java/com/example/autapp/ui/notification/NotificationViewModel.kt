package com.example.autapp.ui.notification

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.firebase.FirebaseCourse
import com.example.autapp.data.firebase.FirebaseNotification
import com.example.autapp.data.firebase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


const val TAG = "NotificationViewModel"
class NotificationViewModel(
    private val notificationRepository: FirebaseNotificationRepository
) : ViewModel() {

    private var _userId: String = ""
    private var _isTeacher: Boolean = false

    private val _notifications = MutableStateFlow<List<FirebaseNotification>>(emptyList())
    val notifications: StateFlow<List<FirebaseNotification>> = _notifications.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var courses by mutableStateOf<List<FirebaseCourse>>(emptyList())

    fun initialize(userId: String, isTeacher: Boolean) {
        _userId  = userId
        _isTeacher  = isTeacher
        Log.d(TAG, "Initializing ViewModel for userId ID: $_userId")
        fetchNotificationData()
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotificationsForUser(_userId, _isTeacher)
                _notifications.value = emptyList()
                _errorMessage.value = null
                Log.d(TAG, "All notifications cleared for user: $_userId")
            } catch (e: Exception) {
                _errorMessage.value = "Failed to clear notifications: ${e.message}"
                Log.e(TAG, "Error clearing notifications: ${e.message}", e)
            }
        }
    }

    // Function to clear the error message after it's been consumed by the UI
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun fetchNotificationData() {
        viewModelScope.launch {
            try {
                // Load notifications
                _notifications.value = notificationRepository.getRecentNotificationsForUser(50, _userId, _isTeacher)
                Log.d(TAG, "Fetched notifications: ${_notifications.value.size}")
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error in fetchNotificationData: ${e.message}", e)
                _errorMessage.value = "Error loading notification screen: ${e.message}"
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as AUTApplication
                NotificationViewModel(
                    application.notificationRepository
                )
            }
        }
    }
}