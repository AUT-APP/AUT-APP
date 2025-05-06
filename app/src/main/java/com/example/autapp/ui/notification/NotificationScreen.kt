package com.example.autapp.ui.notification

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.autapp.R
import com.example.autapp.ui.AUTTopAppBar

@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel,
    navController: NavController
) {
    val tag = "NotificationScreen"
    Log.d(tag, "NotificationScreen composed")

    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val courses = viewModel.courses
    var selectedTabIndex = remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        AUTTopAppBar(
            title = "Notifications",
            navController = navController,
            showBackButton = true,
            modifier = Modifier.statusBarsPadding()
        )

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
                CourseNotificationsTab(viewModel, selectedCourse.courseId)
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
fun CourseNotificationsTab(viewModel: NotificationViewModel, courseId: Int) {
    val courseNotifications = viewModel.notifications
        //.filter { it.courseId == courseId }

    if (courseNotifications.isEmpty()) {
        Text("No notifications for this course.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            courseNotifications.forEach { notification ->
                Text(
                    text = notification.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }
        }
    }
}

