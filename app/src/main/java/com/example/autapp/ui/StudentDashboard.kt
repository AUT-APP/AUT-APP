package com.example.autapp.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autapp.data.dao.GradeDao
import com.example.autapp.data.models.Assignment
import com.example.autapp.data.models.Course
import java.util.*
import com.example.autapp.ui.DashboardViewModel
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import com.example.autapp.data.models.Grade
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.autapp.data.models.TimetableEntry

data class AssignmentGradeDisplay(
    val assignmentName: String,
    val grade: String,
    val score: Double,
    val maxScore: Double,
    val due: Date,
    val feedback: String?
)

@Composable
fun StudentDashboard(
    viewModel: DashboardViewModel,
    paddingValues: PaddingValues,
    isDarkTheme: Boolean,
    timetableEntries: List<TimetableEntry>
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

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
            Text(
                text = "Upcoming...",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                viewModel.courses.take(2).forEach { course ->
                    ClassCard(
                        course = course,
                        timetableEntries = timetableEntries,
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
            val gradesByCourse = viewModel.grades.groupBy { gradeWithAssignment ->
                viewModel.courses.find { it.courseId == gradeWithAssignment.assignment.courseId }?.name ?: "Unknown"
            }
            val assignmentsByCourse = viewModel.courses.associateWith { course ->
                viewModel.grades.filter { it.assignment.courseId == course.courseId }
            }
            assignmentsByCourse.forEach { (course, gradeList) ->
                if (gradeList.isNotEmpty()) {
                    // Calculate overall grade (weighted average)
                    val totalWeight = gradeList.sumOf { it.assignment.weight }
                    val weightedScore = gradeList.sumOf { it.grade.score * it.assignment.weight }
                    val overallScore = if (totalWeight > 0) weightedScore / totalWeight else 0.0
                    val overallGrade = Grade(assignmentId = 0, studentId = 0, _score = overallScore, grade = "").grade
                    CourseGradeCard(
                        courseName = course.title,
                        overallGrade = overallGrade,
                        assignments = gradeList.map {
                            AssignmentGradeDisplay(
                                assignmentName = it.assignment.name,
                                grade = it.grade.grade,
                                score = it.grade.score,
                                maxScore = it.assignment.maxScore,
                                due = it.assignment.due,
                                feedback = it.grade.feedback
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

@Composable
fun ClassCard(
    course: Course,
    timetableEntries: List<TimetableEntry>,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val rawToday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val today = if (rawToday == Calendar.SUNDAY) 7 else rawToday - 1
    val hasScheduleToday = timetableEntries.any {
        it.courseId == course.courseId && it.dayOfWeek == today
    }

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
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun CourseDetailsDialog(
    course: Course,
    timetableEntries: List<TimetableEntry>,
    onDismiss: () -> Unit
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
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = courseName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Overall Grade: $overallGrade",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                assignments.forEach { assignment ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "${assignment.assignmentName}: ${assignment.grade}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Score: %.1f / %.1f".format(assignment.score, assignment.maxScore),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Due: ${SimpleDateFormat("dd MMM yyyy").format(assignment.due)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        assignment.feedback?.let { feedback ->
                            Text(
                                text = "Feedback: $feedback",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("WeekBasedYear")
@Composable
fun AssignmentCard(
    assignment: Assignment,
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


