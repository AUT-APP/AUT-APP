package com.example.autapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    paddingValues: PaddingValues
) {
    val isNotificationsEnabled  by viewModel.isNotificationsEnabled.collectAsState(initial = false)
    val isRemindersEnabled  by viewModel.isRemindersEnabled.collectAsState(initial = false)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Settings", "MazeMap")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        PrimaryTabRow(
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
                onToggleNotifications = viewModel::setNotificationsEnabled,
                isRemindersEnabled = isRemindersEnabled,
                onToggleReminders = viewModel::setRemindersEnabled
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
    isRemindersEnabled: Boolean,
    onToggleReminders: (Boolean) -> Unit
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

        SettingToggleWithInfo(
            title = "Dark Mode",
            infoText = "Enable a darker theme for low-light environments or personal preference.",
            checked = isDarkTheme,
            onCheckedChange = { onToggleTheme() }
        )

        HorizontalDivider()

        // Notification settings
        Text("Notifications", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))

        SettingToggleWithInfo(
            title = "Enable Notifications",
            infoText = "Toggle general notifications from the app on or off.",
            checked = isNotificationsEnabled,
            onCheckedChange = onToggleNotifications
        )

        SettingToggleWithInfo(
            title = "Enable Reminders",
            infoText = "Toggle reminder notifications for calendar events on or off.",
            checked = isRemindersEnabled,
            onCheckedChange = onToggleReminders
        )
    }
}

@Composable
fun SettingToggleWithInfo(
    title: String,
    infoText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title)
            IconButton(onClick = { showInfoDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "$title Info",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text(title) },
            text = { Text(infoText) }
        )
    }
}
