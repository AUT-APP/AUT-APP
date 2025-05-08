package com.example.autapp.data.datastores

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for accessing the DataStore instance from Context
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val CLASS_REMINDERS_ENABLED = booleanPreferencesKey("class_reminders_enabled")
    }

    val isNotificationsEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[NOTIFICATIONS_ENABLED] ?: true }

    val isClassRemindersEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[CLASS_REMINDERS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setClassRemindersEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[CLASS_REMINDERS_ENABLED] = enabled }
    }
}
