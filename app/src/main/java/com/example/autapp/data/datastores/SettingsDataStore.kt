package com.example.autapp.data.datastores

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val CLASS_REMINDERS_ENABLED_KEY = booleanPreferencesKey("class_reminders_enabled")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[DARK_MODE_KEY] == true }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = enabled
        }
    }

    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[NOTIFICATIONS_ENABLED_KEY] != false }

    val isRemindersEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[CLASS_REMINDERS_ENABLED_KEY] != false }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit {
            prefs -> prefs[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit {
            prefs -> prefs[CLASS_REMINDERS_ENABLED_KEY] = enabled
        }
    }
}
