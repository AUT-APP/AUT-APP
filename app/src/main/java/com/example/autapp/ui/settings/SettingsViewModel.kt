package com.example.autapp.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autapp.data.datastores.SettingsDataStore
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
}
