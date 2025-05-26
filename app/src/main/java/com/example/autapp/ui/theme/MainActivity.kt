package com.example.autapp.ui.theme

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.autapp.ui.admin.AdminDashboardScreen
import com.example.autapp.ui.booking.BookingDetailsScreen
import com.example.autapp.ui.booking.BookingScreen
import com.example.autapp.ui.booking.BookingViewModel
import com.example.autapp.ui.booking.MyBookingsScreen
import com.example.autapp.ui.calendar.CalendarScreen
import com.example.autapp.ui.calendar.CalendarViewModel
import com.example.autapp.ui.calendar.ManageEventsScreen
import com.example.autapp.ui.chat.ChatScreen
import com.example.autapp.ui.chat.ChatViewModel
import com.example.autapp.ui.DashboardViewModel
import com.example.autapp.ui.settings.SettingsScreen
import com.example.autapp.ui.components.AUTTopAppBar
import com.example.autapp.ui.components.AUTBottomBar
import com.example.autapp.ui.transport.TransportScreen
import com.example.autapp.ui.transport.TransportViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.example.autapp.ui.notification.NotificationScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.navDeepLink
import com.example.autapp.data.datastores.SettingsDataStore
import com.example.autapp.ui.login.LoginScreen
import com.example.autapp.ui.login.LoginViewModel
import com.example.autapp.ui.notification.NotificationViewModel
import com.example.autapp.ui.settings.SettingsViewModel
import com.example.autapp.util.TestDataInitializer
import com.example.autapp.AUTApplication
import com.example.autapp.ui.login.ChangePasswordScreen

class MainActivity : ComponentActivity() {
    private var currentStudentId by mutableStateOf<Int?>(null)

    private val loginViewModel: LoginViewModel by viewModels { LoginViewModel.Factory }
    private val dashboardViewModel: DashboardViewModel by viewModels { DashboardViewModel.Factory }
    private val calendarViewModel: CalendarViewModel by viewModels { CalendarViewModel.Factory }
    private val bookingViewModel: BookingViewModel by viewModels { BookingViewModel.Factory }
    private val notificationViewModel: NotificationViewModel by viewModels { NotificationViewModel.Factory }
    private val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }
    private val chatViewModel: ChatViewModel by viewModels()
    private val transportViewModel: TransportViewModel by viewModels()

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
                    application = application as AUTApplication
                )
            }
        }
    }
}

@Composable
fun AppContent(
    loginViewModel: LoginViewModel,
    dashboardViewModel: DashboardViewModel,
    calendarViewModel: CalendarViewModel,
    bookingViewModel: BookingViewModel,
    chatViewModel: ChatViewModel,
    transportViewModel: TransportViewModel,
    notificationViewModel: NotificationViewModel,
    settingsViewModel: SettingsViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    currentStudentId: Int?,
    onStudentIdChange: (Int) -> Unit,
    application: AUTApplication
) {
    val navController = rememberNavController()

    // Define the BookingsScreen composable
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BookingsScreen(
        viewModel: BookingViewModel,
        navController: NavController,
        studentId: Int,
        isDarkTheme: Boolean,
        paddingValues: PaddingValues
    ) {
        val tabs = listOf("Create Booking", "Manage Bookings")
        val pagerState = rememberPagerState(pageCount = { tabs.size })
        val coroutineScope = rememberCoroutineScope()
        val textColor = if (isDarkTheme) Color.White else Color(0xFF333333)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(if (isDarkTheme) Color(0xFF121212) else Color.White)
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = if (isDarkTheme) Color(0xFF121212) else Color.White,
                contentColor = textColor
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title, color = textColor) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> BookingScreen(
                        viewModel = viewModel,
                        navController = navController,
                        studentId = studentId,
                        isDarkTheme = isDarkTheme,
                        paddingValues = PaddingValues(0.dp)
                    )

                    1 -> MyBookingsScreen(
                        viewModel = viewModel,
                        navController = navController,
                        studentId = studentId,
                        isDarkTheme = isDarkTheme,
                        paddingValues = PaddingValues(0.dp)
                    )
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { userId, role ->
                    when (role) {
                        "Admin" -> {
                            navController.navigate("admin_dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                        "Student" -> {
                            onStudentIdChange(userId)
                            dashboardViewModel.initialize(userId)
                            navController.navigate("dashboard/$userId") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                        "Teacher" -> {
                            onStudentIdChange(userId)
                            dashboardViewModel.initialize(userId)
                            navController.navigate("dashboard/$userId") {
                                popUpTo("login") { inclusive = true }
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
                navController = navController
            )
        }
        composable(
            route = "dashboard/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.IntType }),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "myapp://dashboard/{studentId}"
                }
            )
        ) { backStackEntry ->
            val studentId = currentStudentId ?: backStackEntry.arguments?.getInt("studentId") ?: 0
            onStudentIdChange(studentId)
            Log.d("MainActivity", "Entering dashboard with student ID: $studentId")
            LaunchedEffect(studentId) {
                dashboardViewModel.initialize(studentId)
            }
            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Dashboard",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = false,
                        currentRoute = null,
                        currentStudentId = studentId
                    )
                },
                bottomBar = {
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        currentRoute = null,
                        currentStudentId = studentId
                    )
                }
            ) { paddingValues ->
                StudentDashboard(
                    viewModel = dashboardViewModel,
                    paddingValues = paddingValues,
                    isDarkTheme = isDarkTheme,
                    timetableEntries = dashboardViewModel.timetableEntries
                )
            }
        }
        composable(
            route = "calendar/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.IntType })
        ) { backStackEntry ->
            val studentId = currentStudentId ?: backStackEntry.arguments?.getInt("studentId") ?: 0
            onStudentIdChange(studentId)
            LaunchedEffect(studentId) {
                calendarViewModel.initialize(studentId)
            }
            Log.d("MainActivity", "Entering calendar with student ID: $studentId")

            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Calendar",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true,
                        currentRoute = null,
                        currentStudentId = studentId
                    )
                },
                bottomBar = {
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        currentRoute = null,
                        currentStudentId = studentId
                    )
                }
            ) { paddingValues ->
                CalendarScreen(
                    viewModel = calendarViewModel,
                    paddingValues = paddingValues,
                    onNavigateToManageEvents = {
                        navController.navigate("manage_events/$studentId")
                    }
                )
            }
        }
        composable(
            route = "manage_events/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.IntType })
        ) { backStackEntry ->
            val studentId = currentStudentId ?: backStackEntry.arguments?.getInt("studentId") ?: 0
            onStudentIdChange(studentId)
            LaunchedEffect(studentId) {
                if (calendarViewModel.studentId != studentId) {
                    calendarViewModel.initialize(studentId)
                }
            }

            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Manage Events",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true,
                        currentRoute = null,
                        currentStudentId = studentId
                    )
                },
                bottomBar = {
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        currentRoute = null,
                        currentStudentId = studentId
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
            route = "bookings/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.IntType })
        ) { backStackEntry ->
            val studentId = currentStudentId ?: backStackEntry.arguments?.getInt("studentId") ?: 0
            onStudentIdChange(studentId)
            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Bookings",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true,
                        currentRoute = null,
                        currentStudentId = studentId
                    )
                },
                bottomBar = {
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        currentRoute = null,
                        currentStudentId = studentId
                    )
                }
            ) { paddingValues ->
                BookingsScreen(
                    viewModel = bookingViewModel,
                    navController = navController,
                    studentId = studentId,
                    isDarkTheme = isDarkTheme,
                    paddingValues = paddingValues
                )
            }
        }
        composable(
            route = "booking_details/{spaceId}/{level}/{date}/{timeSlot}/{studentId}/{campus}/{building}",
            arguments = listOf(
                navArgument("spaceId") { type = NavType.StringType },
                navArgument("level") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType },
                navArgument("timeSlot") { type = NavType.StringType },
                navArgument("studentId") { type = NavType.IntType },
                navArgument("campus") { type = NavType.StringType },
                navArgument("building") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val spaceId = backStackEntry.arguments?.getString("spaceId") ?: ""
            val level = backStackEntry.arguments?.getString("level") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val timeSlot = backStackEntry.arguments?.getString("timeSlot") ?: ""
            val studentId = currentStudentId ?: backStackEntry.arguments?.getInt("studentId") ?: 0
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
                        currentRoute = null,
                        currentStudentId = studentId
                    )
                },
                bottomBar = {
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        currentRoute = null,
                        currentStudentId = studentId
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
                    studentId = studentId,
                    campus = campus,
                    building = building,
                    isDarkTheme = isDarkTheme,
                    paddingValues = paddingValues,
                    snackbarHostState = snackbarHostState
                )
            }
        }
        composable(
            route = "transport/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.IntType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getInt("studentId") ?: 0
            onStudentIdChange(studentId)
            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Shuttle Bus",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true,
                        currentRoute = "transport",
                        currentStudentId = studentId
                    )
                },
                bottomBar = {
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        currentRoute = "transport",
                        currentStudentId = studentId
                    )
                }
            ) { padding ->
                TransportScreen(
                    viewModel = transportViewModel,
                    paddingValues = padding
                )
            }
        }
        composable("chat") {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val currentStudentId = currentStudentId
            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Chatbot",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true,
                        currentRoute = currentRoute,
                        currentStudentId = currentStudentId
                    )
                },
                bottomBar = {
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        currentRoute = currentRoute,
                        currentStudentId = currentStudentId
                    )
                }
            ) { paddingValues ->
                ChatScreen(
                    viewModel = chatViewModel,
                    navController = navController,
                    paddingValues = paddingValues
                )
            }
        }
        composable("settings") {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val currentStudentId = currentStudentId
            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Settings",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true,
                        currentRoute = currentRoute,
                        currentStudentId = currentStudentId
                    )
                },
                bottomBar = {
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        currentRoute = currentRoute,
                        currentStudentId = currentStudentId
                    )
                }
            ) { paddingValues ->
                SettingsScreen(
                    viewModel = settingsViewModel,
                    isDarkTheme = isDarkTheme,
                    isNotificationsEnabled = settingsViewModel.isNotificationsEnabled.collectAsState(initial = true).value,
                    onToggleNotifications = { settingsViewModel.setNotificationsEnabled(it) },
                    isClassRemindersEnabled = settingsViewModel.isClassRemindersEnabled.collectAsState(initial = true).value,
                    onToggleClassReminders = { settingsViewModel.setClassRemindersEnabled(it) },
                    onToggleTheme = onToggleTheme,
                    paddingValues = paddingValues
                )
            }
        }
        composable(
            route = "notification/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.IntType })
        ) { backStackEntry ->
            val studentId = currentStudentId ?: backStackEntry.arguments?.getInt("studentId") ?: 0
            val snackbarHostState = remember { SnackbarHostState() }
            onStudentIdChange(studentId)
            LaunchedEffect(studentId) {
                notificationViewModel.initialize(studentId)
            }
            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Notifications",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true,
                        currentRoute = null,
                        currentStudentId = studentId
                    )
                },
                bottomBar = {
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        calendarViewModel = calendarViewModel,
                        currentRoute = null,
                        currentStudentId = studentId
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
                navArgument("userId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val role = backStackEntry.arguments?.getString("role") ?: ""
            val userId = backStackEntry.arguments?.getInt("userId") ?: 0
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
    }
}