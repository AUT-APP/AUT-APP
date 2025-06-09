package com.example.autapp.ui.theme

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.autapp.AUTApplication
import com.example.autapp.ui.admin.AdminDashboardScreen
import com.example.autapp.ui.booking.BookingDetailsScreen
import com.example.autapp.ui.booking.BookingsScreen
import com.example.autapp.ui.booking.BookingViewModel
import com.example.autapp.ui.calendar.CalendarScreen
import com.example.autapp.ui.calendar.CalendarViewModel
import com.example.autapp.ui.calendar.ManageEventsScreen
import com.example.autapp.ui.chat.ChatScreen
import com.example.autapp.ui.chat.ChatViewModel
import com.example.autapp.ui.StudentDashboard
import com.example.autapp.ui.DashboardViewModel
import com.example.autapp.ui.login.ChangePasswordScreen
import com.example.autapp.ui.login.LoginScreen
import com.example.autapp.ui.notification.NotificationScreen
import com.example.autapp.ui.settings.SettingsScreen
import com.example.autapp.ui.teacher.TeacherDashboard
import com.example.autapp.ui.transport.TransportScreen
import com.example.autapp.ui.transport.TransportViewModel
import com.example.autapp.ui.components.AUTTopAppBar
import com.example.autapp.ui.components.AUTBottomBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.autapp.data.firebase.FirebaseUser
import com.example.autapp.data.firebase.FirebaseTimetableEntry
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.dialog
import com.example.autapp.data.datastores.SettingsDataStore
import com.example.autapp.data.models.CourseMaterial
import com.example.autapp.ui.login.LoginScreen
import com.example.autapp.ui.login.LoginViewModel
import com.example.autapp.ui.notification.NotificationViewModel
import com.example.autapp.ui.settings.SettingsViewModel
import com.example.autapp.ui.teacher.TeacherDashboardViewModel
import com.example.autapp.ui.material.CourseMaterialScreen
import com.example.autapp.ui.material.CourseMaterialViewModel

class MainActivity : ComponentActivity() {
    private var currentStudentId by mutableStateOf<String?>(null)
    private var currentTeacherId by mutableStateOf<String?>(null)
    private var isTeacher by mutableStateOf(false)
    private var currentUserRole by mutableStateOf<String?>(null)

    private val loginViewModel: LoginViewModel by viewModels { LoginViewModel.Factory }
    private val dashboardViewModel: DashboardViewModel by viewModels { DashboardViewModel.Factory }
    private val teacherDashboardViewModel: TeacherDashboardViewModel by viewModels { TeacherDashboardViewModel.Factory }
    private val calendarViewModel: CalendarViewModel by viewModels { CalendarViewModel.Factory }
    private val bookingViewModel: BookingViewModel by viewModels { BookingViewModel.Factory }
    private val notificationViewModel: NotificationViewModel by viewModels { NotificationViewModel.Factory }
    private val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }
    private val chatViewModel: ChatViewModel by viewModels()
    private val transportViewModel: TransportViewModel by viewModels { TransportViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate started")

        val settingsDataStore = SettingsDataStore(applicationContext)
        val coroutineScope = CoroutineScope(Dispatchers.Main)

        var isDarkTheme by mutableStateOf(false)

        coroutineScope.launch {
            settingsDataStore.isDarkMode.collect {
                isDarkTheme = it
            }
        }

        setContent {
            AUTAPPTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                AppContent(
                    loginViewModel = loginViewModel,
                    dashboardViewModel = dashboardViewModel,
                    teacherDashboardViewModel = teacherDashboardViewModel,
                    notificationViewModel = notificationViewModel,
                    calendarViewModel = calendarViewModel,
                    bookingViewModel = bookingViewModel,
                    chatViewModel = chatViewModel,
                    transportViewModel = transportViewModel,
                    settingsViewModel = settingsViewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = {
                        val newTheme = !isDarkTheme
                        isDarkTheme = newTheme
                        coroutineScope.launch {
                            settingsDataStore.setDarkMode(newTheme)
                        }
                    },
                    currentStudentId = currentStudentId,
                    onStudentIdChange = { currentStudentId = it },
                    currentTeacherId = currentTeacherId,
                    onTeacherIdChange = { currentTeacherId = it },
                    isTeacher = isTeacher,
                    onIsTeacherChange = { isTeacher = it },
                    application = application as AUTApplication,
                    currentUserRole = currentUserRole,
                    onRoleChange = { role -> currentUserRole = role }
                )
            }
        }
    }
}

@Composable
fun AppContent(
    loginViewModel: LoginViewModel,
    dashboardViewModel: DashboardViewModel,
    teacherDashboardViewModel: TeacherDashboardViewModel,
    calendarViewModel: CalendarViewModel,
    bookingViewModel: BookingViewModel,
    chatViewModel: ChatViewModel,
    transportViewModel: TransportViewModel,
    notificationViewModel: NotificationViewModel,
    settingsViewModel: SettingsViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    currentStudentId: String?,
    onStudentIdChange: (String?) -> Unit,
    currentTeacherId: String?,
    onTeacherIdChange: (String?) -> Unit,
    isTeacher: Boolean,
    onIsTeacherChange: (Boolean) -> Unit,
    application: AUTApplication,
    currentUserRole: String?,
    onRoleChange: (String?) -> Unit
) {
    val currentUserState: State<FirebaseUser?> = loginViewModel.currentUser.collectAsState(initial = null)
    val currentUser: FirebaseUser? = currentUserState.value

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            Log.d("AppContent", "Current user: ${currentUser.username}, role: ${currentUser.role}, isFirstLogin: ${currentUser.isFirstLogin}")
            val teacher = currentUser.role == "Teacher"
            onIsTeacherChange(teacher)
            if (teacher) {
                onTeacherIdChange(currentUser.id)
                teacherDashboardViewModel.initialize(currentUser.id)
            } else {
                onTeacherIdChange(null)
                // dashboardViewModel initialization happens in the dashboard composable
            }
            onRoleChange(currentUser.role)
        } else {
            Log.d("AppContent", "No current user")
            onIsTeacherChange(false)
            onTeacherIdChange(null)
            onStudentIdChange(null)
        }
    }

    val navController = rememberNavController()
    // Always start at login to ensure LoginScreen handles navigation
    val startDestination = "login"

    val courseMaterialViewModel: CourseMaterialViewModel = viewModel(factory = CourseMaterialViewModel.Factory)


    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            val coroutineScope = rememberCoroutineScope()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { userId, role ->
                    Log.d("AppContent", "onLoginSuccess: userId=$userId, role=$role")
                    coroutineScope.launch {
                        when (role) {
                            "Admin" -> {
                                onRoleChange("Admin")
                                onIsTeacherChange(false)
                                onStudentIdChange(null)
                                onTeacherIdChange(null)
                                navController.navigate("admin_dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                            "Teacher" -> {
                                onIsTeacherChange(true)
                                onStudentIdChange(null)
                                onTeacherIdChange(userId)
                                navController.navigate("teacherDashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                            "Student" -> {
                                val student = application.studentRepository.getById(userId)
                                if (student != null) {
                                    val studentId = student.studentId
                                    Log.d("AppContent", "Fetched studentId: $studentId for userId: $userId")
                                    onStudentIdChange(studentId)
                                    onIsTeacherChange(false)
                                    onTeacherIdChange(null)
                                    dashboardViewModel.initialize(studentId)
                                    navController.navigate("dashboard/$studentId") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    Log.e("AppContent", "Student document not found for userId: $userId")
                                    // Handle case where student document is not found (e.g., show error message)
                                }
                            }
                            else -> {
                                onIsTeacherChange(false)
                                onStudentIdChange(userId)
                                onTeacherIdChange(null)
                                navController.navigate("dashboard/$userId") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }
                    }
                },
                navController = navController,
                isDarkTheme = isDarkTheme
            )
        }
        composable("admin_dashboard") {
            AdminDashboardScreen(
                studentRepository = application.studentRepository,
                teacherRepository = application.teacherRepository,
                courseRepository = application.courseRepository,
                departmentRepository = application.departmentRepository,
                navController = navController,
                isDarkTheme = isDarkTheme,
                timetableEntryRepository = application.timetableEntryRepository
            )
        }
        composable("teacherDashboard") {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val userId = currentTeacherId

            if (userId != null && userId.isNotBlank()) {
                Scaffold(
                    topBar = {
                        AUTTopAppBar(
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            title = "Teacher Dashboard",
                            showBackButton = false,
                            currentRoute = currentRoute,
                            currentUserId = userId,
                            isTeacher = true,
                            currentUserRole = currentUserRole
                        )
                    },
                    bottomBar = {
                        AUTBottomBar(
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            calendarViewModel = calendarViewModel,
                            currentRoute = currentRoute,
                            currentUserId = userId,
                            isTeacher = true,
                            onClick = { /* Handle click if needed */ },
                            currentUserRole = currentUserRole
                        )
                    }
                ) { paddingValues ->
                    TeacherDashboard(
                        viewModel = teacherDashboardViewModel,
                        courseMaterialViewModel = courseMaterialViewModel,
                        departmentRepository = application.departmentRepository,
                        teacherId = userId,
                        paddingValues = paddingValues,
                        navController = navController
                    )
                }
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
        }
        composable(
            route = "dashboard/{userId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            if (userId.isNotBlank()) {
                if (!isTeacher && (currentStudentId == null || currentStudentId != userId)) {
                    onStudentIdChange(userId)
                }
                if (isTeacher && (currentTeacherId == null || currentTeacherId != userId)) {
                    onTeacherIdChange(userId)
                }
                LaunchedEffect(userId, isTeacher) {
                    if (!isTeacher && dashboardViewModel.studentId != userId) {
                        dashboardViewModel.initialize(userId)
                    }
                    if (isTeacher && teacherDashboardViewModel.teacherId != userId) {
                        teacherDashboardViewModel.initialize(userId)
                    }
                }
                Scaffold(
                    topBar = {
                        AUTTopAppBar(
                            title = if (isTeacher) "Teacher Dashboard" else "Dashboard",
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            showBackButton = false,
                            currentRoute = "dashboard",
                            currentUserId = userId,
                            isTeacher = isTeacher,
                            currentUserRole = currentUserRole
                        )
                    },
                    bottomBar = {
                        AUTBottomBar(
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            calendarViewModel = calendarViewModel,
                            currentRoute = "dashboard",
                            currentUserId = userId,
                            isTeacher = isTeacher,
                            onClick = {
                                Log.d("AUTBottomBar", "Home button clicked. currentUserId: $userId, isTeacher: $isTeacher")
                                if (userId.isNotBlank()) {
                                    navController.navigate("dashboard/$userId")
                                }
                            },
                            currentUserRole = currentUserRole
                        )
                    }
                ) { paddingValues ->
                    TeacherDashboard(
                        viewModel = teacherDashboardViewModel,
                        courseMaterialViewModel = courseMaterialViewModel,
                        modifier = Modifier.fillMaxSize(),
                        teacherId = userId,
                        paddingValues = paddingValues,
                        departmentRepository = application.departmentRepository,
                        navController = navController
                    )
                    if (isTeacher) {
                        TeacherDashboard(
                            viewModel = teacherDashboardViewModel,
                            departmentRepository = application.departmentRepository,
                            courseMaterialViewModel = courseMaterialViewModel,
                            modifier = Modifier.fillMaxSize(),
                            teacherId = userId,
                            paddingValues = paddingValues,
                            navController = navController
                        )
                    } else {
                        val studentTimetableEntries by dashboardViewModel.timetableEntries.collectAsState(initial = emptyList<FirebaseTimetableEntry>())
                        StudentDashboard(
                            viewModel = dashboardViewModel,
                            paddingValues = paddingValues,
                            isDarkTheme = isDarkTheme,
                            timetableEntries = studentTimetableEntries,
                            navController = navController
                        )
                    }
                }
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
        }
        composable(
            route = "calendar/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            LaunchedEffect(userId, isTeacher) {
                if (userId.isNotBlank()) {
                    calendarViewModel.initialize(userId, isTeacher)
                }
            }
            val snackbarHostState = remember { SnackbarHostState() }
            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Calendar",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true,
                        currentRoute = "calendar",
                        currentUserId = userId,
                        isTeacher = isTeacher,
                        currentUserRole = currentUserRole
                    )
                },
                bottomBar = {
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        currentRoute = "calendar",
                        currentUserId = userId,
                        isTeacher = isTeacher,
                        onClick = {
                            Log.d("AUTBottomBar", "Home button clicked. currentUserId: $userId, isTeacher: $isTeacher")
                            if (userId.isNotBlank()) {
                                navController.navigate("dashboard/$userId")
                            }
                        },
                        currentUserRole = currentUserRole
                    )
                },
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(16.dp) // Ensure visibility
                    )
                }
            ) { paddingValues ->
                CalendarScreen(
                    viewModel = calendarViewModel,
                    paddingValues = paddingValues,
                    onNavigateToManageEvents = {
                        navController.navigate("manage_events/$userId")
                    },
                    snackbarHostState = snackbarHostState
                )
            }
        }
        composable(
            route = "manage_events/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            LaunchedEffect(userId, isTeacher) {
                if (userId.isNotBlank()) {
                    calendarViewModel.initialize(userId, isTeacher)
                }
            }
            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Manage Events",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true,
                        currentRoute = "manage_events",
                        currentUserId = userId,
                        isTeacher = isTeacher,
                        currentUserRole = currentUserRole
                    )
                },
                bottomBar = {
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        currentRoute = "manage_events",
                        currentUserId = userId,
                        isTeacher = isTeacher,
                        onClick = {
                            Log.d("AUTBottomBar", "Home button clicked. currentUserId: $userId, isTeacher: $isTeacher")
                            if (userId.isNotBlank()) {
                                navController.navigate("dashboard/$userId")
                            }
                        },
                        currentUserRole = currentUserRole
                    )
                }
            ) { paddingValues ->
                ManageEventsScreen(
                    viewModel = calendarViewModel,
                    paddingValues = paddingValues
                )
            }
        }
        composable(
            route = "bookings/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            if (userId.isNotBlank()) {
                if (!isTeacher && (currentStudentId == null || currentStudentId != userId)) {
                    onStudentIdChange(userId)
                }
                Scaffold(
                    topBar = {
                        AUTTopAppBar(
                            title = "Bookings",
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            showBackButton = true,
                            currentRoute = "bookings",
                            currentUserId = userId,
                            isTeacher = isTeacher,
                            currentUserRole = currentUserRole
                        )
                    },
                    bottomBar = {
                        AUTBottomBar(
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            calendarViewModel = calendarViewModel,
                            currentRoute = "bookings",
                            currentUserId = userId,
                            isTeacher = isTeacher,
                            onClick = {
                                Log.d("AUTBottomBar", "Home button clicked. currentUserId: $userId, isTeacher: $isTeacher")
                                if (userId.isNotBlank()) {
                                    navController.navigate("dashboard/$userId")
                                }
                            },
                            currentUserRole = currentUserRole
                        )
                    }
                ) { paddingValues ->
                    BookingsScreen(
                        viewModel = bookingViewModel,
                        navController = navController,
                        studentId = userId,
                        isDarkTheme = isDarkTheme,
                        paddingValues = paddingValues
                    )
                }
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
        }
        composable(
            route = "booking_details/{spaceId}/{level}/{date}/{timeSlot}/{userId}/{campus}/{building}",
            arguments = listOf(
                navArgument("spaceId") { type = NavType.StringType },
                navArgument("level") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType },
                navArgument("timeSlot") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType },
                navArgument("campus") { type = NavType.StringType },
                navArgument("building") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            if (userId.isNotBlank()) {
                if (!isTeacher && (currentStudentId == null || currentStudentId != userId)) {
                    onStudentIdChange(userId)
                }
                val spaceId = backStackEntry.arguments?.getString("spaceId") ?: ""
                val level = backStackEntry.arguments?.getString("level") ?: ""
                val date = backStackEntry.arguments?.getString("date") ?: ""
                val timeSlot = backStackEntry.arguments?.getString("timeSlot") ?: ""
                val campus = backStackEntry.arguments?.getString("campus") ?: ""
                val building = backStackEntry.arguments?.getString("building") ?: ""
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    topBar = {
                        AUTTopAppBar(
                            title = "Booking Details",
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            showBackButton = true,
                            currentRoute = "booking_details",
                            currentUserId = userId,
                            isTeacher = isTeacher,
                            currentUserRole = currentUserRole
                        )
                    },
                    bottomBar = {
                        AUTBottomBar(
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            calendarViewModel = calendarViewModel,
                            currentRoute = "booking_details",
                            currentUserId = userId,
                            isTeacher = isTeacher,
                            onClick = {
                                Log.d("AUTBottomBar", "Home button clicked. currentUserId: $userId, isTeacher: $isTeacher")
                                if (userId.isNotBlank()) {
                                    navController.navigate("dashboard/$userId")
                                }
                            },
                            currentUserRole = currentUserRole
                        )
                    },
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                ) { paddingValues ->
                    BookingDetailsScreen(
                        viewModel = bookingViewModel,
                        navController = navController,
                        spaceId = spaceId,
                        level = level,
                        date = date,
                        timeSlot = timeSlot,
                        studentId = userId,
                        campus = campus,
                        building = building,
                        isDarkTheme = isDarkTheme,
                        paddingValues = paddingValues,
                        snackbarHostState = snackbarHostState
                    )
                }
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
        }
        composable(
            route = "transport/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            if (userId.isNotBlank()) {
                if (!isTeacher && (currentStudentId == null || currentStudentId != userId)) {
                    onStudentIdChange(userId)
                }
                if (isTeacher && (currentTeacherId == null || currentTeacherId != userId)) {
                    onTeacherIdChange(userId)
                }
                Scaffold(
                    topBar = {
                        AUTTopAppBar(
                            title = "Shuttle Bus",
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            showBackButton = true,
                            currentRoute = "transport",
                            currentUserId = userId,
                            isTeacher = isTeacher,
                            currentUserRole = currentUserRole
                        )
                    },
                    bottomBar = {
                        AUTBottomBar(
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            calendarViewModel = calendarViewModel,
                            currentRoute = "transport",
                            currentUserId = userId,
                            isTeacher = isTeacher,
                            onClick = {
                                Log.d("AUTBottomBar", "Home button clicked. currentUserId: $userId, isTeacher: $isTeacher")
                                if (userId.isNotBlank()) {
                                    navController.navigate("dashboard/$userId")
                                }
                            },
                            currentUserRole = currentUserRole
                        )
                    }
                ) { padding ->
                    TransportScreen(
                        viewModel = transportViewModel,
                        paddingValues = padding
                    )
                }
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
        }
        composable("chat") {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val userId = if (isTeacher) currentTeacherId else currentStudentId

            if (userId != null && userId.isNotBlank()) {
                Scaffold(
                    topBar = {
                        AUTTopAppBar(
                            title = "AI Chat",
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            showBackButton = true,
                            currentRoute = currentRoute,
                            currentUserId = userId,
                            isTeacher = isTeacher,
                            currentUserRole = currentUserRole
                        )
                    },
                    bottomBar = {
                        AUTBottomBar(
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            calendarViewModel = calendarViewModel,
                            currentRoute = currentRoute,
                            currentUserId = userId,
                            isTeacher = isTeacher,
                            onClick = {
                                Log.d("AUTBottomBar", "Home button clicked. currentUserId: $userId, isTeacher: $isTeacher")
                                if (userId.isNotBlank()) {
                                    navController.navigate("dashboard/$userId")
                                }
                            },
                            currentUserRole = currentUserRole
                        )
                    }
                ) { paddingValues ->
                    ChatScreen(
                        viewModel = chatViewModel,
                        navController = navController,
                        paddingValues = paddingValues
                    )
                }
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
        }
        composable("settings") {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val userId = if (isTeacher) currentTeacherId else currentStudentId

            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Settings",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true,
                        currentRoute = currentRoute,
                        currentUserId = userId,
                        isTeacher = isTeacher,
                        currentUserRole = currentUserRole
                    )
                },
                bottomBar = {
                    if (currentUserRole != "Admin") {
                        AUTBottomBar(
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            calendarViewModel = calendarViewModel,
                            currentRoute = currentRoute,
                            currentUserId = userId,
                            isTeacher = isTeacher,
                            onClick = {
                                Log.d("AUTBottomBar", "Home button clicked. currentUserId: $userId, isTeacher: $isTeacher")
                                if (userId?.isNotBlank() == true) {
                                    navController.navigate("dashboard/$userId")
                                }
                            },
                            currentUserRole = currentUserRole
                        )
                    }
                }
            ) { paddingValues ->
                SettingsScreen(
                    viewModel = settingsViewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    paddingValues = paddingValues
                )
            }
        }
        composable(
            route = "notification/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            LaunchedEffect(userId, isTeacher) {
                if (userId.isNotBlank()) {
                    notificationViewModel.initialize(userId, isTeacher)
                }
            }
            val snackbarHostState = remember { SnackbarHostState() }
            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Notifications",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true,
                        currentRoute = "notification",
                        currentUserId = userId,
                        isTeacher = isTeacher,
                        currentUserRole = currentUserRole
                    )
                },
                bottomBar = {
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        currentRoute = "notification",
                        currentUserId = userId,
                        isTeacher = isTeacher,
                        onClick = {
                            Log.d("AUTBottomBar", "Home button clicked. currentUserId: $userId, isTeacher: $isTeacher")
                            if (userId.isNotBlank()) {
                                navController.navigate("dashboard/$userId")
                            }
                        },
                        currentUserRole = currentUserRole
                    )
                },
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            ) { paddingValues ->
                NotificationScreen(
                    viewModel = notificationViewModel,
                    paddingValues = paddingValues,
                    snackbarHostState = snackbarHostState
                )
            }
        }
        composable(
            route = "change_password/{username}/{role}/{userId}",
            arguments = listOf(
                navArgument("username") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val role = backStackEntry.arguments?.getString("role") ?: ""
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ChangePasswordScreen(
                viewModel = loginViewModel,
                navController = navController,
                username = username,
                role = role,
                userId = userId,
                isDarkTheme = isDarkTheme,
                onPasswordChanged = {
                    when (role) {
                        "Admin" -> navController.navigate("admin_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                        "Student", "Teacher" -> navController.navigate("dashboard/$userId") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            route = "materials/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "myapp://materials/{courseId}" })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""

            // Initialize the ViewModel using its factory
            val viewModel: CourseMaterialViewModel = viewModel(factory = CourseMaterialViewModel.Factory)


            // Load materials for the given course ID
            LaunchedEffect(courseId) {
                viewModel.loadMaterialsForCourse(courseId)
            }

            var materialToDelete by remember { mutableStateOf<CourseMaterial?>(null) }
            var showDeleteDialog by remember { mutableStateOf(false) }

            // Main UI layout structure
            Scaffold(
                topBar = {
                    // Custom top app bar with back button and route context
                    AUTTopAppBar(
                        title = "Course Materials",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true,
                        currentRoute = "materials",
                        currentUserRole = currentUserRole,
                        currentUserId = if (isTeacher) currentTeacherId else currentStudentId,
                        isTeacher = isTeacher,
                    )
                },
                bottomBar = {
                    // Custom bottom navigation bar
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        currentRoute = "materials",
                        currentUserRole = currentUserRole,
                        currentUserId = if (isTeacher) currentTeacherId else currentStudentId,
                        isTeacher = isTeacher,
                        onClick = {
                            val userId = if (isTeacher) currentTeacherId else currentStudentId
                            if (userId != null) {
                                navController.navigate("dashboard/$userId")
                            }
                        }
                    )
                }
            ) { padding ->
                // Content area displaying the list of course materials
                CourseMaterialScreen(
                    viewModel = viewModel,
                    paddingValues = padding,
                    courseId = courseId,
                    isTeacher = isTeacher,
                    onEditMaterial = { /* We'll handle this in a later step */ },
                    onDeleteMaterial = { materialToDelete = it }
                )
                materialToDelete?.let { material ->
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Material") },
                            text = { Text("Are you sure you want to delete this material?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.deleteMaterial(material)
                                    showDeleteDialog = false
                                    materialToDelete = null
                                }) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showDeleteDialog = false
                                    materialToDelete = null
                                }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }

            }
        }

    }
}