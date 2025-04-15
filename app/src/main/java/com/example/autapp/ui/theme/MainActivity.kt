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


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate started")

        // Initialize database
        AUTDatabase.resetInstance()
        val db = AUTDatabase.getDatabase(applicationContext)
        Log.d("MainActivity", "Database initialized")

        // Initialize repositories
        val userRepository = UserRepository(db.userDao())
        val studentRepository = StudentRepository(db.studentDao())
        val courseRepository = CourseRepository(db.courseDao())
        val assignmentRepository = AssignmentRepository(db.assignmentDao())
        val gradeRepository = GradeRepository(db.gradeDao(), assignmentRepository)
        Log.d("MainActivity", "Repositories initialized")

        // Initialize ViewModels
        val loginViewModelFactory = LoginViewModelFactory(
            userRepository, studentRepository, courseRepository, assignmentRepository, gradeRepository
        )
        val loginViewModel = ViewModelProvider(this, loginViewModelFactory)[LoginViewModel::class.java]

        val dashboardViewModelFactory = DashboardViewModelFactory(
            studentRepository, courseRepository, gradeRepository, assignmentRepository
        )
        val dashboardViewModel = ViewModelProvider(this, dashboardViewModelFactory)[DashboardViewModel::class.java]

        // Insert test data
        loginViewModel.insertTestData()

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            AUTAPPTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                AppContent(
                    loginViewModel = loginViewModel,
                    dashboardViewModel = dashboardViewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }

        Log.d("MainActivity", "Content set")
    }
}

@Composable
fun AppContent(
    loginViewModel: LoginViewModel,
    dashboardViewModel: DashboardViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()

    // Define the TopAppBar composable
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AUTTopAppBar(isDarkTheme: Boolean, navController: NavController) {
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
                        painter = painterResource(id = R.drawable.ic_chatbot),
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
    fun AUTBottomBar(isDarkTheme: Boolean) {
        val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color.White
        val iconTint = if (isDarkTheme) Color.White else Color.Black

        NavigationBar(
            containerColor = backgroundColor,
            contentColor = iconTint
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Outlined.Home, contentDescription = "Home", tint = iconTint) },
                selected = true,
                onClick = { }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Outlined.DateRange, contentDescription = "Calendar", tint = iconTint) },
                selected = false,
                onClick = { }
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
                icon = { Icon(Icons.Default.Menu, contentDescription = "More", tint = iconTint) },
                selected = false,
                onClick = { }
            )
        }
    }


    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { studentId ->
                    dashboardViewModel.initialize(studentId)
                    navController.navigate("dashboard/$studentId") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )

        }
        composable("dashboard/{studentId}") { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId")?.toIntOrNull() ?: 0
            Scaffold(
                topBar = { AUTTopAppBar(isDarkTheme = isDarkTheme, navController = navController) },
                bottomBar = { AUTBottomBar(isDarkTheme = isDarkTheme) },
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
                StudentDashboard(
                    viewModel = dashboardViewModel,
                    paddingValues = paddingValues,
                    isDarkTheme = isDarkTheme
                )
            }
        }
        composable("chat") {
            ChatScreen()
        }
    }
}

class LoginViewModelFactory(
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val courseRepository: CourseRepository,
    private val assignmentRepository: AssignmentRepository,
    private val gradeRepository: GradeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(userRepository, studentRepository, courseRepository, assignmentRepository, gradeRepository) as T
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
            return DashboardViewModel(studentRepository, courseRepository, gradeRepository, assignmentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}