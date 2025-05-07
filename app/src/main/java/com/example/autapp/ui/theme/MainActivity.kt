package com.example.autapp.ui.theme

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.autapp.R
import com.example.autapp.data.database.AUTDatabase
import com.example.autapp.data.models.*
import com.example.autapp.data.repository.*
import com.example.autapp.ui.AUTTopAppBar
import com.example.autapp.ui.booking.BookingDetailsScreen
import com.example.autapp.ui.booking.BookingScreen
import com.example.autapp.ui.booking.BookingViewModel
import com.example.autapp.ui.booking.BookingViewModelFactory
import com.example.autapp.ui.booking.MyBookingsScreen
import com.example.autapp.ui.calendar.CalendarScreen
import com.example.autapp.ui.calendar.CalendarViewModel
import com.example.autapp.ui.calendar.ManageEventsScreen
import com.example.autapp.ui.chat.ChatScreen
import com.example.autapp.ui.chat.ChatViewModel
import com.example.autapp.ui.DashboardViewModel
import com.example.autapp.ui.settings.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : ComponentActivity() {
    private var currentStudentId by mutableStateOf<Int?>(null)
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
            eventRepository = EventRepository(AUTDatabase.getDatabase(this).eventDao()),
            bookingRepository = BookingRepository(
                bookingDao = AUTDatabase.getDatabase(this).bookingDao(),
                studySpaceDao = AUTDatabase.getDatabase(this).studySpaceDao()
            )
        )
    }

    private val bookingViewModel: BookingViewModel by viewModels {
        BookingViewModelFactory(
            bookingRepository = BookingRepository(
                bookingDao = AUTDatabase.getDatabase(this).bookingDao(),
                studySpaceDao = AUTDatabase.getDatabase(this).studySpaceDao()
            ),
            studySpaceRepository = StudySpaceRepository(AUTDatabase.getDatabase(this).studySpaceDao())
        )
    }

    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate started")

        // Initialize database
        AUTDatabase.resetInstance()
        val db = AUTDatabase.getDatabase(applicationContext)
        Log.d("MainActivity", "Database initialized")

        // Initialize repositories
        val userRepository = UserRepository(db.userDao())
        val studentRepository = StudentRepository(db.studentDao(), db.userDao())
        val courseRepository = CourseRepository(db.courseDao())
        val assignmentRepository = AssignmentRepository(db.assignmentDao())
        val gradeRepository = GradeRepository(db.gradeDao(), assignmentRepository)
        val timetableEntryRepository = TimetableEntryRepository(db.timetableEntryDao())
        val eventRepository = EventRepository(db.eventDao())
        val bookingRepository = BookingRepository(db.bookingDao(), db.studySpaceDao())
        val studySpaceRepository = StudySpaceRepository(db.studySpaceDao())
        Log.d("MainActivity", "Repositories initialized")

        // Insert test data
        runBlocking {
            try {
                // Clear database to prevent duplicates
                withContext(Dispatchers.IO) {
                    db.clearAllTables()
                    Log.d("MainActivity", "Database cleared")
                }

                // Insert test students
                val students = listOf(
                    Student(
                        firstName = "Test",
                        lastName = "Student",
                        studentId = 1000,
                        username = "teststudent",
                        password = "password123",
                        role = "Student",
                        enrollmentDate = "2024-01-01",
                        major = "Computer Science",
                        yearOfStudy = 2,
                        gpa = 0.0
                    ),
                    Student(
                        firstName = "John",
                        lastName = "Doe",
                        studentId = 1,
                        username = "john_doe",
                        password = "password123",
                        enrollmentDate = "2023-09-01",
                        major = "Computer Science",
                        yearOfStudy = 2,
                        gpa = 3.5
                    ),
                    Student(
                        firstName = "Jane",
                        lastName = "Smith",
                        studentId = 2,
                        username = "jane_smith",
                        password = "password123",
                        enrollmentDate = "2022-09-01",
                        major = "Engineering",
                        yearOfStudy = 3,
                        gpa = 3.8
                    ),
                    Student(
                        firstName = "Alex",
                        lastName = "Johnson",
                        studentId = 3,
                        username = "alex_johnson",
                        password = "password123",
                        enrollmentDate = "2024-02-01",
                        major = "Business",
                        yearOfStudy = 1,
                        gpa = 3.2
                    ),
                    Student(
                        firstName = "Emma",
                        lastName = "Brown",
                        studentId = 4,
                        username = "emma_brown",
                        password = "password123",
                        enrollmentDate = "2023-02-01",
                        major = "Psychology",
                        yearOfStudy = 2,
                        gpa = 3.6
                    )
                )

                withContext(Dispatchers.IO) {
                    students.forEach { student ->
                        studentRepository.insertStudent(student)
                        Log.d("MainActivity", "Inserted student: ${student.firstName} ${student.lastName} (ID: ${student.studentId})")
                    }
                }

                // Insert test courses
                val courses = listOf(
                    Course(
                        courseId = 1,
                        name = "COMP101",
                        title = "Introduction to Programming",
                        description = "Basics of programming in Python",
                        location = "WG-301"
                    ),
                    Course(
                        courseId = 2,
                        name = "ENG201",
                        title = "Engineering Mechanics",
                        description = "Fundamentals of statics and dynamics",
                        location = "WA-402"
                    ),
                    Course(
                        courseId = 3,
                        name = "BUS301",
                        title = "Business Management",
                        description = "Principles of management and leadership",
                        location = "WZ-503"
                    )
                )

                withContext(Dispatchers.IO) {
                    courses.forEach { course ->
                        courseRepository.insertCourse(course)
                        Log.d("MainActivity", "Inserted course: ${course.name} (ID: ${course.courseId})")
                    }
                }

                // Insert student-course relationships
                val studentCourseCrossRefs = listOf(
                    StudentCourseCrossRef(studentId = 1000, courseId = 1), // Test student enrolled in COMP101
                    StudentCourseCrossRef(studentId = 1, courseId = 1),
                    StudentCourseCrossRef(studentId = 1, courseId = 2),
                    StudentCourseCrossRef(studentId = 2, courseId = 2),
                    StudentCourseCrossRef(studentId = 3, courseId = 3),
                    StudentCourseCrossRef(studentId = 4, courseId = 1)
                )

                withContext(Dispatchers.IO) {
                    studentCourseCrossRefs.forEach { crossRef ->
                        studentRepository.insertStudentCourseCrossRef(crossRef)
                        Log.d("MainActivity", "Inserted student-course: studentId=${crossRef.studentId}, courseId=${crossRef.courseId}")
                    }
                }

                // Insert test assignments
                val calendar = Calendar.getInstance()
                val assignments = listOf(
                    Assignment(
                        assignmentId = 1,
                        name = "Python Project",
                        location = "WG-301",
                        due = calendar.apply { add(Calendar.DAY_OF_MONTH, 7) }.time,
                        weight = 0.4,
                        maxScore = 100.0,
                        type = "Project",
                        courseId = 1
                    ),
                    Assignment(
                        assignmentId = 2,
                        name = "Quiz 1",
                        location = "WG-301",
                        due = calendar.apply { add(Calendar.DAY_OF_MONTH, 3) }.time,
                        weight = 0.2,
                        maxScore = 50.0,
                        type = "Quiz",
                        courseId = 1
                    ),
                    Assignment(
                        assignmentId = 3,
                        name = "Mechanics Lab",
                        location = "WA-402",
                        due = calendar.apply { add(Calendar.DAY_OF_MONTH, 5) }.time,
                        weight = 0.3,
                        maxScore = 80.0,
                        type = "Lab",
                        courseId = 2
                    ),
                    Assignment(
                        assignmentId = 4,
                        name = "Case Study",
                        location = "WZ-503",
                        due = calendar.apply { add(Calendar.DAY_OF_MONTH, 10) }.time,
                        weight = 0.25,
                        maxScore = 60.0,
                        type = "Case Study",
                        courseId = 3
                    )
                )

                withContext(Dispatchers.IO) {
                    assignments.forEach { assignment ->
                        assignmentRepository.insertAssignment(assignment)
                        Log.d("MainActivity", "Inserted assignment: ${assignment.name} (ID: ${assignment.assignmentId})")
                    }
                }

                // Insert test grades
                val grades = listOf(
                    Grade(
                        assignmentId = 1,
                        studentId = 1000, // Test student grade
                        _score = 90.0,
                        grade = "A",
                        feedback = "Excellent project submission."
                    ),
                    Grade(
                        assignmentId = 1,
                        studentId = 1,
                        _score = 85.0,
                        grade = "A",
                        feedback = "Great work on the project!"
                    ),
                    Grade(
                        assignmentId = 2,
                        studentId = 1,
                        _score = 45.0,
                        grade = "A+",
                        feedback = "Excellent quiz performance."
                    ),
                    Grade(
                        assignmentId = 3,
                        studentId = 2,
                        _score = 60.0,
                        grade = "B+",
                        feedback = "Good lab report, improve calculations."
                    ),
                    Grade(
                        assignmentId = 4,
                        studentId = 3,
                        _score = 45.0,
                        grade = "C+",
                        feedback = "Adequate analysis, needs more depth."
                    ),
                    Grade(
                        assignmentId = 1,
                        studentId = 4,
                        _score = 88.0,
                        grade = "A",
                        feedback = "Solid project submission."
                    )
                )

                withContext(Dispatchers.IO) {
                    grades.forEach { grade ->
                        gradeRepository.insertGrade(grade)
                        Log.d("MainActivity", "Inserted grade: studentId=${grade.studentId}, assignmentId=${grade.assignmentId}, score=${grade.score}")
                    }
                }

                // Verify insertions
                val users = userRepository.getAllUsers()
                val retrievedStudents = withContext(Dispatchers.IO) { studentRepository.getAllStudents() }
                Log.d("MainActivity", "Inserted ${users.size} users: $users")
                Log.d("MainActivity", "Inserted ${retrievedStudents.size} students: $retrievedStudents")

                if (retrievedStudents.isEmpty()) {
                    Log.e("MainActivity", "No students were inserted!")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Failed to insert test students!", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error inserting test data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error inserting test data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        val themeManager = ThemePreferenceManager(applicationContext)
        val coroutineScope = CoroutineScope(Dispatchers.Main)

        var isDarkTheme by mutableStateOf(false)

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
                    bookingViewModel = bookingViewModel,
                    chatViewModel = chatViewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = {
                        val newTheme = !isDarkTheme
                        isDarkTheme = newTheme
                        coroutineScope.launch {
                            themeManager.setDarkMode(newTheme)
                        }
                    },
                    currentStudentId = currentStudentId,
                    onStudentIdChange = { currentStudentId = it }
                )
            }
        }
    }
}

// Rest of the file (composables and ViewModel factories) remains unchanged
@Composable
fun AppContent(
    loginViewModel: LoginViewModel,
    dashboardViewModel: DashboardViewModel,
    calendarViewModel: CalendarViewModel,
    bookingViewModel: BookingViewModel,
    chatViewModel: ChatViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    currentStudentId: Int?,
    onStudentIdChange: (Int) -> Unit
) {
    val navController = rememberNavController()

    // Define the TopAppBar composable
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AUTTopAppBar(
        isDarkTheme: Boolean,
        navController: NavController,
        title: String,
        showBackButton: Boolean,
        actions: @Composable RowScope.() -> Unit = {}
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
                    if (showBackButton) {
                        IconButton(onClick = { navController.navigateUp() }) {
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
                    currentStudentId?.let { studentId ->
                        Log.d("MainActivity", "Navigating to calendar with student ID: $studentId")
                        calendarViewModel.initialize(studentId)
                        navController.navigate("calendar/$studentId") {
                            popUpTo("dashboard/$studentId") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Outlined.Event, contentDescription = "Bookings", tint = iconTint) },
                label = { Text("Bookings") },
                selected = currentRoute?.startsWith("bookings") == true,
                onClick = {
                    currentStudentId?.let { studentId ->
                        navController.navigate("bookings/$studentId") {
                            popUpTo("dashboard/$studentId") { inclusive = false }
                            launchSingleTop = true
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
                selected = false,
                onClick = { /* TODO: Implement Transport screen */ }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Menu, contentDescription = "More", tint = iconTint) },
                label = { Text("More") },
                selected = currentRoute == "settings",
                onClick = { navController.navigate("settings") }
            )
        }
    }

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
                onLoginSuccess = { studentId ->
                    onStudentIdChange(studentId)
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
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = currentStudentId ?: backStackEntry.arguments?.getString("studentId")?.toIntOrNull() ?: 0
            onStudentIdChange(studentId)
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
                bottomBar = { AUTBottomBar(isDarkTheme = isDarkTheme, navController = navController, currentRoute = null, currentStudentId = studentId) }
            ) { paddingValues ->
                StudentDashboard(
                    viewModel = dashboardViewModel,
                    paddingValues = paddingValues,
                    isDarkTheme = isDarkTheme
                )
            }
        }
        composable(
            route = "calendar/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = currentStudentId ?: backStackEntry.arguments?.getString("studentId")?.toIntOrNull() ?: 0
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
                        showBackButton = true
                    )
                },
                bottomBar = { AUTBottomBar(isDarkTheme = isDarkTheme, navController = navController, currentRoute = null, currentStudentId = studentId) }
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
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = currentStudentId ?: backStackEntry.arguments?.getString("studentId")?.toIntOrNull() ?: 0
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
                        showBackButton = true
                    )
                },
                bottomBar = { AUTBottomBar(isDarkTheme = isDarkTheme, navController = navController, currentRoute = null, currentStudentId = studentId) }
            ) { paddingValues ->
                ManageEventsScreen(
                    viewModel = calendarViewModel,
                    paddingValues = paddingValues
                )
            }
        }
        composable(
            route = "bookings/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = currentStudentId ?: backStackEntry.arguments?.getString("studentId")?.toIntOrNull() ?: 0
            onStudentIdChange(studentId)
            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Bookings",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true
                    )
                },
                bottomBar = { AUTBottomBar(isDarkTheme = isDarkTheme, navController = navController, currentRoute = null, currentStudentId = studentId) }
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
                navArgument("studentId") { type = NavType.StringType },
                navArgument("campus") { type = NavType.StringType },
                navArgument("building") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val spaceId = backStackEntry.arguments?.getString("spaceId") ?: ""
            val level = backStackEntry.arguments?.getString("level") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val timeSlot = backStackEntry.arguments?.getString("timeSlot") ?: ""
            val studentId = currentStudentId ?: backStackEntry.arguments?.getString("studentId")?.toIntOrNull() ?: 0
            val campus = backStackEntry.arguments?.getString("campus") ?: ""
            val building = backStackEntry.arguments?.getString("building") ?: ""
            val snackbarHostState = remember { SnackbarHostState() } // Define here
            Scaffold(
                topBar = {
                    AUTTopAppBar(
                        title = "Booking Details",
                        isDarkTheme = isDarkTheme,
                        navController = navController,
                        showBackButton = true
                    )
                },
                bottomBar = { AUTBottomBar(isDarkTheme = isDarkTheme, navController = navController, currentRoute = null, currentStudentId = studentId) },
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(16.dp) // Ensure visibility
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
                    snackbarHostState = snackbarHostState // Pass to BookingDetailsScreen
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
                        showBackButton = true
                    )
                },
                bottomBar = {
                    AUTBottomBar(
                        isDarkTheme = isDarkTheme,
                        navController = navController,
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
                        showBackButton = true
                    )
                },
                bottomBar = { AUTBottomBar(isDarkTheme = isDarkTheme, navController = navController, currentRoute = currentRoute, currentStudentId = currentStudentId) }
            ) { paddingValues ->
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    paddingValues = paddingValues
                )
            }
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
    private val eventRepository: EventRepository,
    private val bookingRepository: BookingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(
                timetableEntryRepository,
                studentRepository,
                eventRepository,
                bookingRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class BookingViewModelFactory(
    private val bookingRepository: BookingRepository,
    private val studySpaceRepository: StudySpaceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(BookingViewModel::class.java)) {
            val savedStateHandle = extras.createSavedStateHandle()
            @Suppress("UNCHECKED_CAST")
            return BookingViewModel(bookingRepository, studySpaceRepository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}