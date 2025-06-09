package com.example.autapp.ui.calendar

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.autapp.data.firebase.FirebaseBooking
import com.example.autapp.data.firebase.FirebaseEvent
import com.example.autapp.data.firebase.FirebaseTimetableEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun rememberReminderScheduler (
    snackbarHostState: SnackbarHostState,
    viewModel: CalendarViewModel,
    uiState: CalendarUiState,
    notificationsEnabled: Boolean,
    remindersEnabled: Boolean
): (Any, Int) -> Unit {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? Activity

    // States for tracking permissions
    var hasNotificationsPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var hasExactAlarmPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else true
        )
    }

    // State to hold a pending reminder request if permissions are needed
    var pendingReminderRequest by remember {
        mutableStateOf<Pair<Any, Int>?>(null)
    }

    // Activity result launcher for permission request
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasNotificationsPermission = isGranted }
    )

    fun requestExactAlarmPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:${activity.packageName}".toUri()
                }
                activity.startActivity(intent)
            }
        }
    }

    // Shared onSetReminder logic
    val onSetReminder: (Any, Int) -> Unit = { item, minutes ->
        coroutineScope.launch {
            if (activity == null) {
                return@launch
            }

            pendingReminderRequest = null

            // Prevent scheduling notification for a time before the current time
            val itemStartTimeMillis = when (item) {
                is FirebaseEvent -> item.startTime?.time
                is FirebaseBooking -> item.startTime.time
                else -> null
            }
            if (itemStartTimeMillis != null && itemStartTimeMillis < System.currentTimeMillis()) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Cannot set reminder for a past item.")
                return@launch
            }

            // Check notification permission before proceeding
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationsPermission) {
                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )

                // Store the request for retry
                pendingReminderRequest = Pair(item, minutes)

                snackbarHostState.currentSnackbarData?.dismiss()
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = "Please enable notification permission to set reminders.",
                    actionLabel = "Enable",
                    duration = SnackbarDuration.Indefinite
                )

                when (snackbarResult) {
                    SnackbarResult.ActionPerformed -> {
                        // User clicked "Enable" button
                        if (shouldShowRationale) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val intent = Intent().apply {
                                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                    }
                    SnackbarResult.Dismissed -> {
                        pendingReminderRequest = null
                    }
                }

                return@launch
            }

            // Check and redirect if exact alarm permission is missing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission) {
                // Store the request for retry
                pendingReminderRequest = Pair(item, minutes)

                snackbarHostState.currentSnackbarData?.dismiss()
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = "Please enable exact alarms to set reminders.",
                    actionLabel = "Enable", // Use "Enable" as action label
                    duration = SnackbarDuration.Indefinite
                )
                when (snackbarResult) {
                    SnackbarResult.ActionPerformed -> {
                        requestExactAlarmPermission(activity)
                    }
                    SnackbarResult.Dismissed -> {
                        pendingReminderRequest = null
                    }
                }
                return@launch
            }

            // Proceed with scheduling if permissions are granted
            val scheduledTimeMillis = when (item) {
                is FirebaseTimetableEntry -> viewModel.updateReminder(context, item, minutes)
                is FirebaseEvent -> viewModel.updateReminder(context, item, minutes)
                is FirebaseBooking -> viewModel.updateReminder(context, item, minutes)
                else -> null
            }

            snackbarHostState.currentSnackbarData?.dismiss()

            val reminderMessage = formatReminderMessage(
                item = item,
                scheduledTimeMillis = scheduledTimeMillis,
                uiState = uiState,
                notificationsEnabled = notificationsEnabled,
                remindersEnabled = remindersEnabled
            )
            snackbarHostState.showSnackbar(
                message = reminderMessage,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Observe lifecycle to check permission when returning from settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Update permission states
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    hasExactAlarmPermission = alarmManager.canScheduleExactAlarms()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationsPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
                // Retry pending reminder request if permissions are now granted
                if (pendingReminderRequest != null) {
                    coroutineScope.launch {
                        val (item, minutes) = pendingReminderRequest!!
                        pendingReminderRequest = null
                        onSetReminder(item, minutes)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return onSetReminder
}

private fun formatReminderMessage(
    item: Any,
    scheduledTimeMillis: Long?,
    uiState: CalendarUiState,
    notificationsEnabled: Boolean,
    remindersEnabled: Boolean
): String {
    val warning = when {
        !notificationsEnabled -> " (Notifications disabled in settings)"
        !remindersEnabled -> " (Reminders disabled in settings)"
        else -> ""
    }
    val baseMessage = if (scheduledTimeMillis != null) {
        // Format the scheduled time into a readable string
        val scheduledDate = Date(scheduledTimeMillis)
        val formatter = SimpleDateFormat("MMM dd 'at' h:mm a", Locale.getDefault())
        val scheduledTimeString = formatter.format(scheduledDate)
        // Construct the message
        when (item) {
            is FirebaseTimetableEntry -> {
                val courseName =
                    uiState.courses.find { it.courseId == item.courseId }?.name ?: item.courseId
                "$courseName ${item.type} notification scheduled for $scheduledTimeString"
            }

            is FirebaseEvent -> "${item.title} event notification scheduled for $scheduledTimeString"
            is FirebaseBooking -> "${item.roomId} booking notification scheduled for $scheduledTimeString"
            else -> "Failed to schedule notification: Unknown item type"
        }
    } else {
        when (item) {
            is FirebaseTimetableEntry -> {
                val courseName =
                    uiState.courses.find { it.courseId == item.courseId }?.name ?: item.courseId
                "Failed to schedule $courseName ${item.type} notification"
            }

            is FirebaseEvent -> "Failed to schedule ${item.title} event notification"
            is FirebaseBooking -> "Failed to schedule ${item.roomId} booking notification "
            else -> "Failed to schedule notification."
        }
    }
    return "$baseMessage$warning"
}