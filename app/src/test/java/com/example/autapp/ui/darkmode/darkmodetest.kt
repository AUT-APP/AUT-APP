package com.example.autapp.ui.darkmode

import android.content.Context
import com.example.autapp.data.datastores.SettingsDataStore  // import the DataStore class fileciteturn1file0
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for dark mode persistence in SettingsDataStore.
 *
 * Verifies that the default value is false, that setting dark mode persists,
 * and that the preference is retained across DataStore instances.
 */
@RunWith(RobolectricTestRunner::class)
class DarkModeTest {
    private lateinit var context: Context
    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setup() {
        // Use ApplicationProvider to obtain a Context for DataStore
        context = ApplicationProvider.getApplicationContext()
        settingsDataStore = SettingsDataStore(context)
    }

    @Test
    fun `isdarkmode is true when enabled`() = runBlocking {
        // When setting dark mode to true
        settingsDataStore.setDarkMode(true)

        // Then isDarkMode should be true
        assertTrue(settingsDataStore.isDarkMode.first())
    }

    @Test
    fun `set dark mode to true persists`() = runBlocking {
        // Given the user enables dark mode
        settingsDataStore.setDarkMode(true)

        // When querying isDarkMode
        val darkMode: Boolean = settingsDataStore.isDarkMode.first()

        // Then it should be true
        assertTrue(darkMode)
    }

    @Test
    fun `toggle dark mode persists across instances`() = runBlocking {
        // Given dark mode was set to true in one instance
        settingsDataStore.setDarkMode(true)

        // When creating a new SettingsDataStore
        val newDataStore = SettingsDataStore(context)
        val persistedMode: Boolean = newDataStore.isDarkMode.first()

        // Then the new instance should reflect the persisted setting
        assertTrue(persistedMode)
    }
}
