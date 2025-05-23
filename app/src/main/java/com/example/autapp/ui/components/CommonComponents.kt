package com.example.autapp.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.autapp.R
import com.example.autapp.ui.calendar.CalendarViewModel
import kotlin.String

// Define the TopAppBar composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AUTTopAppBar(
    isDarkTheme: Boolean,
    navController: NavController,
    title: String,
    showBackButton: Boolean,
    currentRoute: String?,
    currentStudentId: Int?,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFF2F7A78)
    val titleTextColor = if (isDarkTheme) Color.White else Color.White
    val actionIconColor = if (isDarkTheme) Color.White else Color.White
    val autLabelBackground = if (isDarkTheme) Color.White else Color.Black
    val autLabelTextColor = if (isDarkTheme) Color.Black else Color.White
    val profileBackground = if (isDarkTheme) Color.DarkGray else Color.White
    val profileIconColor = if (isDarkTheme) Color.White else Color.Black

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentRoute ?: navBackStackEntry?.destination?.route
    val currentStudentId = currentStudentId ?: navBackStackEntry?.arguments?.getString("studentId")?.toIntOrNull()

    val expanded = remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                if (showBackButton) {
                    IconButton(onClick = {
                        if (currentRoute?.startsWith("dashboard") != true) {
                            navController.navigateUp()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = actionIconColor
                        )
                    }
                }
                Text(
                    text = "AUT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = autLabelTextColor,
                    modifier = Modifier
                        .background(autLabelBackground)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(id = R.drawable.chatbot_assistant_icon),
                    contentDescription = "AI Chat",
                    tint = actionIconColor,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { navController.navigate("chat") }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = actionIconColor,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { navController.navigate("notification/${currentStudentId}") }
                )

                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(profileBackground)
                        .clickable { expanded.value = true }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Profile",
                        tint = profileIconColor,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                    DropdownMenu(
                        expanded = expanded.value,
                        onDismissRequest = { expanded.value = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                expanded.value = false
                                if (currentRoute?.startsWith("settings") != true) {
                                    navController.navigate("settings")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                expanded.value = false
                                // TODO: Add proper user sessions
                                if (currentRoute?.startsWith("login") != true) {
                                    navController.navigate("login")
                                }
                            }
                        )
                    }
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = titleTextColor,
            actionIconContentColor = actionIconColor
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// Define the Bottom NavigationBar composable
@Composable
fun AUTBottomBar(
    isDarkTheme: Boolean,
    navController: NavController,
    calendarViewModel: CalendarViewModel,
    currentRoute: String?,
    currentStudentId: Int?
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color.White
    val iconTint = if (isDarkTheme) Color.White else Color.Black
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentRoute ?: navBackStackEntry?.destination?.route
    val currentStudentId = currentStudentId ?: navBackStackEntry?.arguments?.getString("studentId")?.toIntOrNull()

    NavigationBar(
        containerColor = backgroundColor,
        contentColor = iconTint
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Home", tint = iconTint) },
            label = { Text("Home") },
            selected = currentRoute?.startsWith("dashboard") == true,
            onClick = {
                if (currentStudentId != null && currentRoute?.startsWith("dashboard") != true) {
                    navController.navigate("dashboard/$currentStudentId") {
                        popUpTo("dashboard/$currentStudentId") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.DateRange, contentDescription = "Calendar", tint = iconTint) },
            label = { Text("Calendar") },
            selected = currentRoute?.startsWith("calendar") == true,
            onClick = {
                Log.d("MainActivity", "Calendar icon clicked")
                if (currentRoute?.startsWith("calendar") != true) {
                    currentStudentId?.let { studentId ->
                        Log.d("MainActivity", "Navigating to calendar with student ID: $studentId")
                        calendarViewModel.initialize(studentId)
                        navController.navigate("calendar/$studentId") {
                            popUpTo("dashboard/$studentId") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Event, contentDescription = "Bookings", tint = iconTint) },
            label = { Text("Bookings") },
            selected = currentRoute?.startsWith("bookings") == true,
            onClick = {
                if (currentRoute?.startsWith("bookings") != true) {
                    currentStudentId?.let { studentId ->
                        navController.navigate("bookings/$studentId") {
                            popUpTo("dashboard/$studentId") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_directions),
                    contentDescription = "Transport",
                    tint = iconTint
                )
            },
            label = { Text("Transport") },
            selected = currentRoute?.startsWith("transport") == true,
            onClick = {
                if (currentStudentId != null && currentRoute?.startsWith("transport") != true) {
                    navController.navigate("transport/$currentStudentId") {
                        popUpTo("dashboard/$currentStudentId") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Menu, contentDescription = "More", tint = iconTint) },
            label = { Text("More") },
            selected = currentRoute == "settings",
            onClick = {
                if (currentRoute?.startsWith("settings") != true) {
                    navController.navigate("settings")
                }
            }
        )
    }
}