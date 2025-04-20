package com.example.autapp.ui.theme

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
import com.example.autapp.ui.DashboardViewModel
import java.util.*


@Composable
fun StudentDashboard(
    viewModel: DashboardViewModel,
    paddingValues: PaddingValues,
    isDarkTheme: Boolean
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

            viewModel.grades.chunked(2).forEach { gradePair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    gradePair.forEach { gradeWithAssignment ->
                        GradeCard(
                            gradeWithAssignment = gradeWithAssignment,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (gradePair.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Assignments",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            viewModel.assignments.forEach { assignment ->
                AssignmentCard(
                    assignment = assignment,
                    formatDate = { viewModel.formatDate(it) },
                    formatTime = { viewModel.formatTime(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Click for More...",
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
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
                    text = course.name,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = course.title,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    text = course.location ?: "TBD",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
fun GradeCard(
    gradeWithAssignment: GradeDao.GradeWithAssignment,
    modifier: Modifier = Modifier
) {
    val grade = gradeWithAssignment.grade
    val assignment = gradeWithAssignment.assignment
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = assignment.name,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = assignment.type,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Score: ${grade.score}",
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = grade.grade,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}


@Composable
fun GPASummaryCard(gpa: Double?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Current GPA",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Semester 1, 2025",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (gpa != null) String.format("%.3f / 9.0", gpa) else "N/A",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@SuppressLint("WeekBasedYear")
@Composable
fun AssignmentCard(
    assignment: Assignment,
    formatDate: (Date) -> String,
    formatTime: (Date) -> String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { },
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
            modifier = Modifier.fillMaxWidth(),
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
}
