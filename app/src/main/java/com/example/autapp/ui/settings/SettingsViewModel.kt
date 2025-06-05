package com.example.autapp.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.datastores.SettingsDataStore
import kotlinx.coroutines.launch

class SettingsViewModel(context: Context) : ViewModel() {
    private val settingsDataStore = SettingsDataStore(context)

    val isNotificationsEnabled = settingsDataStore.isNotificationsEnabled
    val isRemindersEnabled = settingsDataStore.isRemindersEnabled

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotificationsEnabled(enabled)
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setRemindersEnabled(enabled)
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
