package com.example.autapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.autapp.ui.notification.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    isNotificationsEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    isClassRemindersEnabled: Boolean,
    onToggleClassReminders: (Boolean) -> Unit,
    paddingValues: PaddingValues
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Settings", "MazeMap")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> SettingsContent(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                isNotificationsEnabled = isNotificationsEnabled,
                onToggleNotifications = onToggleNotifications,
                isClassRemindersEnabled = isClassRemindersEnabled,
                onToggleClassReminders = onToggleClassReminders
            )
            1 -> MazeMapView()
        }
    }
}

@Composable
private fun SettingsContent(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    isNotificationsEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    isClassRemindersEnabled: Boolean,
    onToggleClassReminders: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dark Mode")
            Switch(
                checked = isDarkTheme,
                onCheckedChange = { onToggleTheme() }
            )
        }

        HorizontalDivider()

        // Notification settings
        Text("Notifications", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Notifications")
            Switch(checked = isNotificationsEnabled, onCheckedChange = onToggleNotifications)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Class Reminders")
            Switch(checked = isClassRemindersEnabled, onCheckedChange = onToggleClassReminders)
        }
    }
}