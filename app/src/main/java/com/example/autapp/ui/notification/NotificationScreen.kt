    package com.example.autapp.ui.notification

    import android.Manifest
    import android.app.Activity
    import android.app.AlarmManager
    import android.content.Context
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.os.Build
    import android.provider.Settings
    import android.util.Log
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.PaddingValues
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.defaultMinSize
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.layout.width
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.material3.Button
    import androidx.compose.material3.ButtonDefaults
    import androidx.compose.material3.Card
    import androidx.compose.material3.CardDefaults
    import androidx.compose.material3.Icon
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.PrimaryTabRow
    import androidx.compose.material3.SnackbarDuration
    import androidx.compose.material3.SnackbarHostState
    import androidx.compose.material3.Tab
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.collectAsState
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableIntStateOf
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.rememberCoroutineScope
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.core.content.ContextCompat
    import androidx.core.net.toUri
    import com.example.autapp.data.models.TimetableEntry
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.launch
    import org.threeten.bp.DayOfWeek
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale
    import org.threeten.bp.format.TextStyle as ADBTextStyle
    import com.example.autapp.data.firebase.FirebaseTimetableEntry

    @Composable
    fun NotificationScreen(
        viewModel: NotificationViewModel,
        paddingValues: PaddingValues,
        snackbarHostState: SnackbarHostState
    ) {
        val tag = "NotificationScreen"
        Log.d(tag, "NotificationScreen composed")

        val colorScheme = MaterialTheme.colorScheme

        val courses = viewModel.courses
        var selectedTabIndex = remember { mutableIntStateOf(0) }

        val coroutineScope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (courses.isNotEmpty()) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex.intValue,
                    containerColor = colorScheme.surface,
                    contentColor = colorScheme.onSurface
                ) {
                    courses.forEachIndexed { index, course ->
                        Tab(
                            selected = selectedTabIndex.intValue == index,
                            onClick = { selectedTabIndex.intValue = index },
                            text = { Text(course.name, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Show notifications related to the selected course
                    val selectedCourse = courses[selectedTabIndex.intValue]
                    CourseNotificationsTab(viewModel,
                        selectedCourse.courseId,
                        selectedCourse.name,
                        snackbarHostState,
                        coroutineScope
                    )
                }
            } else {
                // Loading or no courses
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading courses or no courses found")
                }
            }
        }

    }

    @Composable
    fun CourseNotificationsTab(
        viewModel: NotificationViewModel,
        courseId: String,
        courseName: String,
        snackbarHostState: SnackbarHostState,
        coroutineScope: CoroutineScope
    ) {
        val notificationPrefs = viewModel.notificationPrefs // Map of classSessionId to minutesBefore
        val timetableEntries = viewModel.timetableEntries.filter { it.courseId == courseId }
        val context = androidx.compose.ui.platform.LocalContext.current

        // State to track notification permission
        var hasNotificationsPermission by remember {
            mutableStateOf(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true // Permission is automatically granted on versions below Android 13
                }
            )
        }

        // Activity result launcher for permission request
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted -> hasNotificationsPermission = isGranted }
        )

        // Request permission only once when the Composable is first launched
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationsPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (timetableEntries.isEmpty()) {
            Text("No classes in this course.")
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                timetableEntries
                    .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
                    .forEach { timetableEntry ->
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimetableEntryCard(
                                timetableEntry = timetableEntry.toTimetableEntry(),
                                notificationPrefs = notificationPrefs,
                                context = context,
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState,
                                coroutineScope = coroutineScope,
                                courseName = courseName,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun FirebaseTimetableEntry.toTimetableEntry(): TimetableEntry {
        return TimetableEntry(
            entryId = this.entryId.toIntOrNull() ?: 0,
            courseId = this.courseId.toIntOrNull() ?: 0,
            dayOfWeek = this.dayOfWeek,
            startTime = this.startTime,
            endTime = this.endTime,
            room = this.room,
            type = this.type
        )
    }

    private fun Date.format(): String {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        return formatter.format(this)
    }

    fun getDayAbbreviation(dayOfWeek: Int): String {
        return DayOfWeek.of(dayOfWeek).getDisplayName(ADBTextStyle.SHORT, Locale.ENGLISH)
    }

    fun requestExactAlarmPermissionIfNeeded(activity: Activity) {
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


    @Composable
    fun TimetableEntryCard(
        timetableEntry: TimetableEntry,
        notificationPrefs: Map<Int, Int>,
        context: Context,
        viewModel: NotificationViewModel,
        snackbarHostState: SnackbarHostState,
        coroutineScope: CoroutineScope,
        courseName: String,
        modifier: Modifier = Modifier
    ) {
        val notificationsEnabled: Boolean by viewModel.notificationsEnabled.collectAsState(initial = true)
        val remindersEnabled: Boolean by viewModel.remindersEnabled.collectAsState(initial = true)

        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timetableEntry.type,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${getDayAbbreviation(timetableEntry.dayOfWeek)} ${timetableEntry.startTime.format()}-${timetableEntry.endTime.format()}",
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Notify me when class starts in...",
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth(), // Takes full width to allow centering
                    ) {
                        listOf(60, 30, 15, 0).forEach { minutes ->
                            val selected = notificationPrefs[timetableEntry.entryId] == minutes
                            val label = when (minutes) {
                                0 -> "Start"
                                60 -> "1 hour"
                                else -> "$minutes min"
                            }

                            Button(
                                shape = RoundedCornerShape(5.dp),
                                onClick = {
                                    coroutineScope.launch {
                                        val isCurrentlySet =
                                            notificationPrefs[timetableEntry.entryId] == minutes
                                        if (isCurrentlySet) {
                                            viewModel.deleteNotificationPreference(
                                                context,
                                                studentId = viewModel.studentId,
                                                classSessionId = timetableEntry.entryId.toString()
                                            )
                                            // Dismiss any currently showing snackbar
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(
                                                message = "$courseName ${timetableEntry.type} notification disabled",
                                                duration = SnackbarDuration.Short
                                            )
                                        } else {
                                            // Request permission before setting the notification
                                            val activity = context as? Activity
                                            activity?.let {
                                                requestExactAlarmPermissionIfNeeded(it)
                                            }
                                            // Call ViewModel function and get the scheduled time
                                            val scheduledTimeMillis =
                                                viewModel.setNotificationPreference(
                                                    context = context,
                                                    studentId = viewModel.studentId,
                                                    classSessionId = timetableEntry.entryId.toString(),
                                                    minutesBefore = minutes,
                                                    courseName = courseName
                                                )
                                            // Dismiss any currently showing snackbar
                                            snackbarHostState.currentSnackbarData?.dismiss()
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
                                                "$courseName ${timetableEntry.type} notification scheduled for $scheduledTimeString"
                                            } else {
                                                "Failed to schedule notification for $courseName ${timetableEntry.type}"
                                            }

                                            snackbarHostState.showSnackbar(
                                                message = "$baseMessage$warning",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .defaultMinSize(minWidth = 0.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_dialog_map),
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timetableEntry.room,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }