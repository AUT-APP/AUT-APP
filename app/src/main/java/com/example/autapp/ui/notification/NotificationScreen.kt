    package com.example.autapp.ui.notification

    import android.Manifest
    import android.app.Activity
    import android.content.Context
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.os.Build
    import android.util.Log
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.foundation.background
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.foundation.text.KeyboardOptions
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.material3.*
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.text.TextStyle
    import androidx.compose.ui.text.font.Font
    import androidx.compose.ui.text.font.FontFamily
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.input.KeyboardType
    import androidx.compose.ui.text.input.PasswordVisualTransformation
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.text.style.TextDecoration
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.core.content.ContextCompat
    import androidx.lifecycle.viewmodel.compose.viewModel
    import androidx.navigation.NavController
    import com.example.autapp.R
    import com.example.autapp.data.models.Course
    import com.example.autapp.data.models.TimetableEntry
    import com.example.autapp.ui.components.AUTTopAppBar
    import com.example.autapp.ui.theme.ClassCard
    import org.threeten.bp.LocalDate
    import org.threeten.bp.ZoneId
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale
    import org.threeten.bp.DayOfWeek
    import org.threeten.bp.format.TextStyle as ADBTextStyle
    import android.app.AlarmManager
    import android.net.Uri
    import android.provider.Settings
    import androidx.compose.runtime.rememberCoroutineScope
    import androidx.core.net.toUri
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.launch

    @Composable
    fun NotificationScreen(
        viewModel: NotificationViewModel,
        navController: NavController,
        paddingValues: PaddingValues,
        snackbarHostState: SnackbarHostState
    ) {
        val tag = "NotificationScreen"
        Log.d(tag, "NotificationScreen composed")

        val colorScheme = MaterialTheme.colorScheme
        val typography = MaterialTheme.typography

        val courses = viewModel.courses
        var selectedTabIndex = remember { mutableStateOf(0) }

        val coroutineScope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (courses.isNotEmpty()) {
                TabRow(
                    selectedTabIndex = selectedTabIndex.value,
                    containerColor = colorScheme.surface,
                    contentColor = colorScheme.onSurface
                ) {
                    courses.forEachIndexed { index, course ->
                        Tab(
                            selected = selectedTabIndex.value == index,
                            onClick = { selectedTabIndex.value = index },
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
                    val selectedCourse = courses[selectedTabIndex.value]
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
        courseId: Int,
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
                timetableEntries.forEach { timetableEntry ->
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimetableEntryCard(
                                timetableEntry = timetableEntry,
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
                                    val isCurrentlySet = notificationPrefs[timetableEntry.entryId] == minutes
                                    if (isCurrentlySet) {
                                        viewModel.deleteNotificationPreference(
                                            studentId = viewModel.studentId,
                                            classSessionId = timetableEntry.entryId
                                        )
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "$courseName ${timetableEntry.type} notification disabled",
                                                actionLabel = "OK"
                                            )
                                        }
                                    } else {
                                        // Request permission before setting the notification
                                        val activity = context as? Activity
                                        activity?.let {
                                            requestExactAlarmPermissionIfNeeded(it)
                                        }
                                        viewModel.setNotificationPreference(
                                            context = context,
                                            studentId = viewModel.studentId,
                                            classSessionId = timetableEntry.entryId,
                                            minutesBefore = minutes,
                                            courseName = courseName
                                        )
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "$courseName ${timetableEntry.type} notification set for $label",
                                                actionLabel = "OK"
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