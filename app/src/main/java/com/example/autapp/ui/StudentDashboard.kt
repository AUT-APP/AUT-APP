package com.example.autapp.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Intent
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.example.autapp.data.firebase.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.example.autapp.data.models.Notification
import com.example.autapp.data.firebase.FirebaseNotification
import com.example.autapp.util.NotificationHelper
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.SharedPreferences

data class AssignmentGradeDisplay(
    val assignmentName: String,
    val grade: String,
    val score: Double,
    val maxScore: Double,
    val due: Date,
    val feedback: String?,
    val courseId: String
)

@Composable
fun StudentDashboard(
    viewModel: DashboardViewModel,
    paddingValues: PaddingValues,
    isDarkTheme: Boolean,
    navController: NavController,
    timetableEntries: List<FirebaseTimetableEntry>
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val context = LocalContext.current
    val sharedPrefs = remember {
        context.getSharedPreferences("material_notifications", Context.MODE_PRIVATE)
    }
    val lastShownTimestampKey = "last_shown_material_notification"

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = viewModel.isRefreshing)
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    val materialNotifications = viewModel.notifications.filter {
        it.deepLinkUri?.startsWith("myapp://materials/") == true
    }

    // Track the last notification IDs to avoid duplicate system notifications
    var lastNotificationIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Request notification permission
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasNotificationPermission = granted }
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Only show system notification for new material notifications
    LaunchedEffect(viewModel.notifications) {
        val lastShown = sharedPrefs.getLong(lastShownTimestampKey, 0L)
        val newNotifications = viewModel.notifications.filter {
            it.deepLinkUri?.startsWith("myapp://materials/") == true &&
            (it.timestamp ?: 0L) > lastShown
        }
        if (newNotifications.isNotEmpty()) {
            // Show only the most recent new notification
            val latest = newNotifications.maxByOrNull { it.timestamp ?: 0L }
            latest?.let { notif ->
                NotificationHelper.pushNotification(
                    context,
                    notif.toNotification()
                )
                // Update last shown timestamp
                sharedPrefs.edit().putLong(lastShownTimestampKey, notif.timestamp ?: 0L).apply()
            }
        }
    }

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = { viewModel.fetchDashboardData() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Notifications Section
                if (materialNotifications.isNotEmpty()) {
                    Text(
                        text = "Recent Notifications",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )

                    materialNotifications.take(5).forEach { notification ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    notification.deepLinkUri?.let { uri ->
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e("StudentDashboard", "Error opening deep link: ${e.message}")
                                        }
                                    }
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = notification.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notification.text,
                                    fontSize = 14.sp,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dateFormat.format(Date(notification.timestamp)),
                                    fontSize = 12.sp,
                                    color = textColor.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Upcoming...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    viewModel.courses.take(2).forEach { course ->
                        ClassCard(
                            course = course,
                            timetableEntries = timetableEntries,
                            navController = navController,
                            modifier = Modifier
                                .weight(1f)
                                .height(180.dp)
                        )
                    }
                    if (viewModel.courses.size < 2) {
                        repeat(2 - viewModel.courses.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Text(
                    text = "Grades",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )

                GPASummaryCard(gpa = viewModel.studentGpa)

                Spacer(modifier = Modifier.height(16.dp))

                // Group grades by course
                val gradesByCourse = viewModel.grades.groupBy { it.courseId }
                val assignmentsByCourse = viewModel.courses.associateWith { course ->
                    viewModel.grades.filter { it.courseId == course.courseId }
                }
                assignmentsByCourse.forEach { (course, gradeList) ->
                    if (gradeList.isNotEmpty()) {
                        // Calculate overall grade (weighted average)
                        val totalWeight = gradeList.sumOf { it.maxScore }
                        val weightedScore = gradeList.sumOf { it.score }
                        val overallScore = if (totalWeight > 0) weightedScore / totalWeight else 0.0
                        val overallGrade = if (overallScore >= 0.9) "A" else if (overallScore >= 0.8) "B" else if (overallScore >= 0.7) "C" else if (overallScore >= 0.6) "D" else "F"
                        CourseGradeCard(
                            courseName = course.title,
                            overallGrade = overallGrade,
                            assignments = gradeList.map {
                                AssignmentGradeDisplay(
                                    assignmentName = it.assignmentName,
                                    grade = it.grade,
                                    score = it.score,
                                    maxScore = it.maxScore,
                                    due = it.due,
                                    feedback = it.feedback,
                                    courseId = it.courseId
                                )
                            }
                        )
                    }
                }

                Text(
                    text = "Upcoming Assignments",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )

                val today = Calendar.getInstance().time
                val upcomingAssignments = viewModel.assignments.filter { it.due.after(today) }
                upcomingAssignments.forEach { assignment ->
                    val courseName = viewModel.courses.find { it.courseId == assignment.courseId }?.name ?: "Unknown"
                    val courseTitle = viewModel.courses.find { it.courseId == assignment.courseId }?.title ?: "Untitled"
                    AssignmentCard(
                        assignment = assignment,
                        courseName = courseName,
                        courseTitle = courseTitle,
                        formatDate = viewModel::formatDate,
                        formatTime = viewModel::formatTime
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (viewModel.courses.size > 2) {
                    Text(
                        text = "All Courses",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )

                    viewModel.courses.drop(2).chunked(2).forEach { coursePair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            coursePair.forEach { course ->
                                ClassCard(
                                    course = course,
                                    timetableEntries = timetableEntries,
                                    navController = navController,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(120.dp)
                                )
                            }
                            if (coursePair.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                viewModel.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun NavigationButton(location: String?) {
    val context = LocalContext.current

    Button(
        onClick = {
            val encodedLocation = java.net.URLEncoder.encode(location ?: "", "UTF-8")
            val mazeMapUrl = "https://use.mazemap.com/#v=1&campusid=103&zlevel=1&center=174.765877,-36.853388&zoom=16&search=$encodedLocation"
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(mazeMapUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_dialog_map),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Navigate to Location", color = MaterialTheme.colorScheme.onSecondary)
    }
}

@Composable
fun ClassCard(
    course: FirebaseCourse,
    timetableEntries: List<FirebaseTimetableEntry>,
    modifier: Modifier = Modifier,
    navController: NavController,
    ) {
    var showDialog by remember { mutableStateOf(false) }

    val rawToday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val today = if (rawToday == Calendar.SUNDAY) 7 else rawToday - 1
    val hasScheduleToday = timetableEntries.any { it.courseId == course.courseId && it.dayOfWeek == today }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = course.name,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = course.title,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (hasScheduleToday) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (hasScheduleToday) {
                    Text(
                        text = "Class Today",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDialog) {
        CourseDetailsDialog(
            course = course,
            timetableEntries = timetableEntries.filter { it.courseId == course.courseId },
            onDismiss = { showDialog = false },
            navController = navController
        )
    }
}

@Composable
fun CourseDetailsDialog(
    course: FirebaseCourse,
    timetableEntries: List<FirebaseTimetableEntry>,
    onDismiss: () -> Unit,
    navController: NavController
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "${course.name} - ${course.title}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                course.description?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                course.location?.let {
                    Text(
                        text = "Location: $it",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Button
                NavigationButton(location = course.location)

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button
                Button(
                    onClick = {
                        onDismiss()
                        navController.navigate("materials/${course.courseId}") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_upload),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Materials", color = MaterialTheme.colorScheme.onPrimary)
                }

                if (timetableEntries.isNotEmpty()) {
                    Text(
                        text = "Schedule:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    timetableEntries.sortedBy { it.dayOfWeek }.forEach { entry ->
                        Text(
                            text = "${getDayOfWeek(entry.dayOfWeek)}: ${formatTime(entry.startTime)} - ${formatTime(entry.endTime)} (${entry.room}, ${entry.type})",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun GPASummaryCard(gpa: Double?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Current GPA",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (gpa != null) String.format("%.2f", gpa) else "N/A",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun CourseGradeCard(
    courseName: String,
    overallGrade: String,
    assignments: List<AssignmentGradeDisplay>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = courseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Overall Grade: $overallGrade",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                assignments.forEach { assignmentGrade ->
                    Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                        Text(
                            text = assignmentGrade.assignmentName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Score: ${assignmentGrade.score}/${assignmentGrade.maxScore}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Grade: ${assignmentGrade.grade}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        assignmentGrade.feedback?.let { feedback ->
                            Text(
                                text = "Feedback: $feedback",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "Due: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(assignmentGrade.due)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("WeekBasedYear")
@Composable
fun AssignmentCard(
    assignment: FirebaseAssignment,
    courseName: String,
    courseTitle: String,
    formatDate: (Date) -> String,
    formatTime: (Date) -> String
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(
                text = formatDate(assignment.due),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true },
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = assignment.name,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Due ${formatTime(assignment.due)}",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            // Dialog content surface with rounded corners and elevation
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.6f)
            ) {
                //scrollable content column
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Top row with assignment title and close icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Assignment name header
                        Text(
                            text = assignment.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        // Close dialog button
                        IconButton(onClick = { showDialog = false }) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                contentDescription = "Close"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp)) // Adds vertical spacing

                    // Assignment details
                    Text("Type: ${assignment.type}")
                    Text("Course ID: ${assignment.courseId}")
                    Text("Course Name: $courseName")
                    Text("Course Title: $courseTitle")
                    Text("Due: ${formatDate(assignment.due)} at ${formatTime(assignment.due)}")
                    Text("Location: ${assignment.location}")
                    Text("Weight: ${assignment.weight * 100}%")
                    Text("Max Score: ${assignment.maxScore}")


                    Spacer(modifier = Modifier.height(16.dp)) // Adds more spacing

                    // Close button at the bottom of the dialog
                    Button(onClick = { showDialog = false }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}


@SuppressLint("SimpleDateFormat")
fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy")
    return formatter.format(date)
}

@SuppressLint("SimpleDateFormat")
fun formatTime(date: Date): String {
    val formatter = SimpleDateFormat("hh:mm a")
    return formatter.format(date)
}

fun getDayOfWeek(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> ""
    }
}


