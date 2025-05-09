package com.example.autapp.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.datastores.SettingsDataStore
import com.example.autapp.ui.booking.BookingViewModel
import kotlinx.coroutines.launch

class SettingsViewModel(context: Context) : ViewModel() {
    private val settingsDataStore = SettingsDataStore(context)

    val isNotificationsEnabled = settingsDataStore.isNotificationsEnabled
    val isClassRemindersEnabled = settingsDataStore.isClassRemindersEnabled

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotificationsEnabled(enabled)
        }
    }

    fun setClassRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setClassRemindersEnabled(enabled)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as AUTApplication
                val context = application.applicationContext
                SettingsViewModel(context)
            }
        }
    }
}
