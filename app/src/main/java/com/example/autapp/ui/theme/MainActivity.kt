package com.example.autapp.ui.theme

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.autapp.R
import com.example.autapp.data.database.AUTDatabase
import com.example.autapp.data.repository.*
import com.example.autapp.ui.chat.ChatScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.example.autapp.ui.DashboardViewModel
import com.example.autapp.ui.settings.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.autapp.ui.calendar.CalendarViewModel
import com.example.autapp.ui.calendar.CalendarScreen


class MainActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(
            userRepository = UserRepository(AUTDatabase.getDatabase(this).userDao()),
            studentRepository = StudentRepository(
                studentDao = AUTDatabase.getDatabase(this).studentDao(),
                userDao = AUTDatabase.getDatabase(this).userDao()
            ),
            courseRepository = CourseRepository(AUTDatabase.getDatabase(this).courseDao()),
            assignmentRepository = AssignmentRepository(AUTDatabase.getDatabase(this).assignmentDao()),
            gradeRepository = GradeRepository(
                AUTDatabase.getDatabase(this).gradeDao(),
                AssignmentRepository(AUTDatabase.getDatabase(this).assignmentDao())
            ),
            timetableEntryRepository = TimetableEntryRepository(AUTDatabase.getDatabase(this).timetableEntryDao())
        )
    }

    private val dashboardViewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(
            studentRepository = StudentRepository(
                studentDao = AUTDatabase.getDatabase(this).studentDao(),
                userDao = AUTDatabase.getDatabase(this).userDao()
            ),
            courseRepository = CourseRepository(AUTDatabase.getDatabase(this).courseDao()),
            gradeRepository = GradeRepository(
                AUTDatabase.getDatabase(this).gradeDao(),
                AssignmentRepository(AUTDatabase.getDatabase(this).assignmentDao())
            ),
            assignmentRepository = AssignmentRepository(AUTDatabase.getDatabase(this).assignmentDao())
        )
    }

    private val calendarViewModel: CalendarViewModel by viewModels {
        CalendarViewModelFactory(
            timetableEntryRepository = TimetableEntryRepository(AUTDatabase.getDatabase(this).timetableEntryDao()),
            studentRepository = StudentRepository(
                studentDao = AUTDatabase.getDatabase(this).studentDao(),
                userDao = AUTDatabase.getDatabase(this).userDao()
            ),
            eventRepository = EventRepository(AUTDatabase.getDatabase(this).eventDao())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate started")

        // Initialize database
        AUTDatabase.resetInstance()
        val db = AUTDatabase.getDatabase(applicationContext)
        Log.d("MainActivity", "Database initialized")

        // Initialize repositories
        val userRepository = UserRepository(db.userDao())
        val studentRepository = StudentRepository(
            studentDao = db.studentDao(),
            userDao = db.userDao()
        )
        val courseRepository = CourseRepository(db.courseDao())
        val assignmentRepository = AssignmentRepository(db.assignmentDao())
        val gradeRepository = GradeRepository(db.gradeDao(), assignmentRepository)
        val timetableEntryRepository = TimetableEntryRepository(db.timetableEntryDao())
        val eventRepository = EventRepository(db.eventDao())
        Log.d("MainActivity", "Repositories initialized")

        // Initialize ViewModels
        val loginViewModelFactory = LoginViewModelFactory(
            userRepository,
            studentRepository,
            courseRepository,
            assignmentRepository,
            gradeRepository,
            timetableEntryRepository
        )
        val loginViewModel =
            ViewModelProvider(this, loginViewModelFactory)[LoginViewModel::class.java]

        val dashboardViewModelFactory = DashboardViewModelFactory(
            studentRepository, courseRepository, gradeRepository, assignmentRepository
        )
        val dashboardViewModel =
            ViewModelProvider(this, dashboardViewModelFactory)[DashboardViewModel::class.java]

        // Insert test data
        loginViewModel.insertTestData()

        val themeManager = ThemePreferenceManager(applicationContext)
        val coroutineScope = CoroutineScope(Dispatchers.Main)

        var isDarkTheme by mutableStateOf(false)

        // Load saved theme preference
        coroutineScope.launch {
            themeManager.isDarkMode.collect {
                isDarkTheme = it
            }
        }

        setContent {
            AUTAPPTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                AppContent(
                    loginViewModel = loginViewModel,
                    dashboardViewModel = dashboardViewModel,
                    calendarViewModel = calendarViewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = {
                        val newTheme = !isDarkTheme
                        isDarkTheme = newTheme
                        coroutineScope.launch {
                            themeManager.setDarkMode(newTheme)
                        }
                    }
                )
            }
        }
    }


    @Composable
    fun AppContent(
        loginViewModel: LoginViewModel,
        dashboardViewModel: DashboardViewModel,
        calendarViewModel: CalendarViewModel,
        isDarkTheme: Boolean,
        onToggleTheme: () -> Unit
    ) {
        val navController = rememberNavController()

        // Define the TopAppBar composable
        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun AUTTopAppBar(
            isDarkTheme: Boolean,
            navController: NavController,
            title: String,
            showBackButton: Boolean
        ) {
            val containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFF2F7A78)
            val titleTextColor = if (isDarkTheme) Color.White else Color.White
            val actionIconColor = if (isDarkTheme) Color.White else Color.White
            val autLabelBackground = if (isDarkTheme) Color.White else Color.Black
            val autLabelTextColor = if (isDarkTheme) Color.Black else Color.White
            val profileBackground = if (isDarkTheme) Color.DarkGray else Color.White
            val profileIconColor = if (isDarkTheme) Color.White else Color.Black

            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
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
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(profileBackground)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Profile",
                                tint = profileIconColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                },
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
        fun AUTBottomBar(isDarkTheme: Boolean, navController: NavController) {
            val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color.White
            val iconTint = if (isDarkTheme) Color.White else Color.Black
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val currentStudentId = navBackStackEntry?.arguments?.getString("studentId")?.toIntOrNull()

            NavigationBar(
                containerColor = backgroundColor,
                contentColor = iconTint
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Home", tint = iconTint) },
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
                    selected = currentRoute?.startsWith("calendar") == true,
                    onClick = {
                        Log.d("MainActivity", "Calendar icon clicked")
                        if (currentStudentId != null && currentRoute?.startsWith("calendar") != true) {
                            Log.d("MainActivity", "Navigating to calendar with student ID: $currentStudentId")
                            calendarViewModel.initialize(currentStudentId)
                            navController.navigate("calendar/$currentStudentId") {
                                popUpTo("dashboard/$currentStudentId") { inclusive = false }
                                launchSingleTop = true
                            }
                        } else {
                            Log.e("MainActivity", "Cannot navigate: currentStudentId is null")
                        }
                    }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_camera),
                            contentDescription = "Camera",
                            tint = iconTint
                        )
                    },
                    selected = false,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_directions),
                            contentDescription = "Transport",
                            tint = iconTint
                        )
                    },
                    selected = false,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "More",
                            tint = iconTint
                        )
                    },
                    selected = false,
                    onClick = { navController.navigate("settings") }
                )
            }
        }

        NavHost(navController = navController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = { studentId ->
                        Log.d("MainActivity", "Login successful with student ID: $studentId")
                        dashboardViewModel.initialize(studentId)
                        navController.navigate("dashboard/$studentId") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme
                )
            }
            composable(
                route = "dashboard/{studentId}",
                arguments = listOf(
                    navArgument("studentId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val studentId = backStackEntry.arguments?.getString("studentId")?.toIntOrNull() ?: 0
                Log.d("MainActivity", "Entering dashboard with student ID: $studentId")
                Scaffold(
                    topBar = {
                        AUTTopAppBar(
                            title = "Dashboard",
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            showBackButton = false
                        )
                    },
                    bottomBar = { AUTBottomBar(isDarkTheme = isDarkTheme, navController = navController) }
                ) { innerPadding ->
                    StudentDashboard(
                        viewModel = dashboardViewModel,
                        paddingValues = innerPadding,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            composable(
                route = "calendar/{studentId}",
                arguments = listOf(
                    navArgument("studentId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val studentId = backStackEntry.arguments?.getString("studentId")?.toIntOrNull() ?: 0
                Log.d("MainActivity", "Entering calendar with student ID: $studentId")
                Scaffold(
                    topBar = {
                        AUTTopAppBar(
                            title = "Calendar",
                            isDarkTheme = isDarkTheme,
                            navController = navController,
                            showBackButton = true
                        )
                    },
                    bottomBar = {
                        AUTBottomBar(
                            isDarkTheme = isDarkTheme,
                            navController = navController
                        )
                    }
                ) { paddingValues ->
                    CalendarScreen(
                        viewModel = calendarViewModel.apply {
                            if (this.studentId != studentId) {
                                initialize(studentId)
                            }
                        },
                        paddingValues = paddingValues
                    )
                }
            }
            composable("chat") {
                ChatScreen(
                    navController = navController
                )
            }
            composable("settings") {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme
                )
            }
        }
    }

    class LoginViewModelFactory(
        private val userRepository: UserRepository,
        private val studentRepository: StudentRepository,
        private val courseRepository: CourseRepository,
        private val assignmentRepository: AssignmentRepository,
        private val gradeRepository: GradeRepository,
        private val timetableEntryRepository: TimetableEntryRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(
                    userRepository,
                    studentRepository,
                    courseRepository,
                    assignmentRepository,
                    gradeRepository,
                    timetableEntryRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    class DashboardViewModelFactory(
        private val studentRepository: StudentRepository,
        private val courseRepository: CourseRepository,
        private val gradeRepository: GradeRepository,
        private val assignmentRepository: AssignmentRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(
                    studentRepository,
                    courseRepository,
                    gradeRepository,
                    assignmentRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    class CalendarViewModelFactory(
        private val timetableEntryRepository: TimetableEntryRepository,
        private val studentRepository: StudentRepository,
        private val eventRepository: EventRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CalendarViewModel(
                    timetableEntryRepository,
                    studentRepository,
                    eventRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}