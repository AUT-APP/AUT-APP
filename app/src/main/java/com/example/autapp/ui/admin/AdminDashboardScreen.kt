package com.example.autapp.ui.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.autapp.ui.components.AUTTopAppBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import androidx.compose.ui.platform.LocalContext
import com.example.autapp.data.firebase.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateListOf

// Helper function to parse enrollmentDate (String) to LocalDate for sorting
fun parseEnrollmentDate(date: String): LocalDate {
    return try {
        LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: DateTimeParseException) {
        LocalDate.of(1970, 1, 1)
    }
}

// Define a simple data class for the form's timetable entry state
data class TimetableEntryFormData(
    var dayOfWeek: Int = 1, // 1 = Monday, 7 = Sunday
    var startTime: String = "", // Use String for time input initially
    var endTime: String = "",   // Use String for time input initially
    var room: String = "",
    var type: String = "" // Lecture, Lab, Tutorial
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    studentRepository: FirebaseStudentRepository,
    teacherRepository: FirebaseTeacherRepository,
    courseRepository: FirebaseCourseRepository,
    departmentRepository: FirebaseDepartmentRepository,
    timetableEntryRepository: FirebaseTimetableRepository,
    navController: NavController,
    isDarkTheme: Boolean = false
) {
    val viewModel: AdminDashboardViewModel = viewModel(
        factory = AdminDashboardViewModel.Factory
    )
    val students by viewModel.students.collectAsState()
    val teachers by viewModel.teachers.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Students", "Teachers", "Courses", "Departments", "Activity")
    var showCreateStudentDialog by remember { mutableStateOf(false) }
    var showEditStudentDialog by remember { mutableStateOf<FirebaseStudent?>(null) }
    var showDeleteStudentDialog by remember { mutableStateOf<List<FirebaseStudent>?>(null) }
    var showCreateTeacherDialog by remember { mutableStateOf(false) }
    var showEditTeacherDialog by remember { mutableStateOf<FirebaseTeacher?>(null) }
    var showDeleteTeacherDialog by remember { mutableStateOf<List<FirebaseTeacher>?>(null) }
    var showCreateCourseDialog by remember { mutableStateOf(false) }
    var showEditCourseDialog by remember { mutableStateOf<FirebaseCourse?>(null) }
    var showDeleteCourseDialog by remember { mutableStateOf<List<FirebaseCourse>?>(null) }
    var showCreateDepartmentDialog by remember { mutableStateOf(false) }
    var showEditDepartmentDialog by remember { mutableStateOf<FirebaseDepartment?>(null) }
    var showDeleteDepartmentDialog by remember { mutableStateOf<FirebaseDepartment?>(null) }
    var showBulkEnrollDialog by remember { mutableStateOf(false) }
    var showMigrationDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AUTTopAppBar(
                title = "Admin Dashboard",
                isDarkTheme = isDarkTheme,
                navController = navController,
                showBackButton = false,
                currentRoute = "admin_dashboard",
                currentUserId = null,
                isTeacher = false,
                currentUserRole = null
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(8.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> StudentsTab(
                    students = students,
                    courses = courses,
                    departments = departments,
                    onCreateStudent = { showCreateStudentDialog = true },
                    onEditClickStudent = { showEditStudentDialog = it },
                    onDeleteStudents = { showDeleteStudentDialog = it },
                    onBulkEnroll = { showBulkEnrollDialog = true }
                )
                1 -> TeachersTab(
                    teachers = teachers,
                    departments = departments,
                    onCreateTeacher = { showCreateTeacherDialog = true },
                    onEditTeacher = { showEditTeacherDialog = it },
                    onDeleteTeachers = { showDeleteTeacherDialog = it }
                )
                2 -> CoursesTab(
                    courses = courses,
                    teachers = teachers,
                    departments = departments,
                    onCreateCourse = { showCreateCourseDialog = true },
                    onEditCourse = { showEditCourseDialog = it },
                    onDeleteCourses = { showDeleteCourseDialog = it },
                    studentRepository = studentRepository,
                    courseRepository = courseRepository,
                    timetableEntryRepository = timetableEntryRepository
                )
                3 -> DepartmentsTab(
                    departments = departments,
                    onCreateDepartment = { showCreateDepartmentDialog = true },
                    onEditDepartment = { showEditDepartmentDialog = it },
                    onDeleteDepartment = { showDeleteDepartmentDialog = it }
                )
                4 -> ActivityTab(
                    activities = activities
                )
            }

            LaunchedEffect(successMessage, errorMessage) {
                successMessage?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearMessages()
                }
                errorMessage?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearMessages()
                }
            }

            if (showCreateStudentDialog) {
                StudentFormDialog(
                    studentRepository = studentRepository,
                    courses = courses,
                    departments = departments,
                    isEditing = false,
                    onDismiss = { showCreateStudentDialog = false },
                    onSave = { firstName, lastName, role, enrollmentDate, majorId, minorId, yearOfStudy, dob, selectedCourses ->
                        viewModel.createStudent(
                            firstName = firstName,
                            lastName = lastName,
                            role = role,
                            enrollmentDate = enrollmentDate,
                            majorId = majorId.toString(),
                            minorId = minorId?.toString(),
                            yearOfStudy = yearOfStudy,
                            gpa = 0.0,
                            dob = dob,
                            selectedCourses = selectedCourses,
                            enrollmentYear = 2025,
                            semester = 1
                        )
                        showCreateStudentDialog = false
                    },
                    courseRepository = courseRepository
                )
            }

            showEditStudentDialog?.let { student ->
                StudentFormDialog(
                    student = student,
                    studentRepository = studentRepository,
                    courses = courses,
                    departments = departments,
                    isEditing = true,
                    onDismiss = { showEditStudentDialog = null },
                    onSave = { firstName, lastName, role, enrollmentDate, majorId, minorId, yearOfStudy, dob, selectedCourses ->
                        viewModel.updateStudent(
                            student.copy(
                                firstName = firstName,
                                lastName = lastName,
                                role = role,
                                enrollmentDate = enrollmentDate,
                                majorId = majorId.toString(),
                                minorId = minorId?.toString() ?: "",
                                yearOfStudy = yearOfStudy,
                                gpa = student.gpa,
                                dob = dob
                            ),
                            enrollmentYear = 2025,
                            semester = 1,
                            courseEnrollments = selectedCourses
                        )
                        showEditStudentDialog = null
                    },
                    courseRepository = courseRepository
                )
            }

            showDeleteStudentDialog?.let { studentsToDelete ->
                AlertDialog(
                    onDismissRequest = { showDeleteStudentDialog = null },
                    title = { Text("Confirm Deletion") },
                    text = {
                        Text(
                            if (studentsToDelete.size == 1)
                                "Are you sure you want to delete ${studentsToDelete[0].firstName} ${studentsToDelete[0].lastName}?"
                            else
                                "Are you sure you want to delete ${studentsToDelete.size} students?"
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteStudents(studentsToDelete)
                            showDeleteStudentDialog = null
                        }) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteStudentDialog = null }) { Text("Cancel") }
                    }
                )
            }

            if (showBulkEnrollDialog) {
                BulkEnrollDialog(
                    students = students,
                    courses = courses,
                    onDismiss = { showBulkEnrollDialog = false },
                    onSave = { studentIds, courseId, year, semester ->
                        viewModel.bulkEnroll(
                            studentIds = studentIds.map { it.toString() },
                            courseId = courseId.toString(),
                            year = year,
                            semester = semester
                        )
                        showBulkEnrollDialog = false
                    }
                )
            }

            if (showCreateTeacherDialog) {
                TeacherFormDialog(
                    departments = departments,
                    isEditing = false,
                    onDismiss = { showCreateTeacherDialog = false },
                    onSave = { firstName, lastName, role, departmentId, officeHours, courses, dob ->
                        viewModel.createTeacher(
                            firstName = firstName,
                            lastName = lastName,
                            role = role,
                            departmentId = departmentId.toString(),
                            title = "",
                            officeNumber = officeHours,
                            email = "",
                            phoneNumber = "",
                            dob = dob,
                            courseAssignments = courses.map { Triple(it, 0, 0) }
                        )
                        showCreateTeacherDialog = false
                    }
                )
            }

            showEditTeacherDialog?.let { teacher ->
                TeacherFormDialog(
                    teacher = teacher,
                    departments = departments,
                    isEditing = true,
                    onDismiss = { showEditTeacherDialog = null },
                    onSave = { firstName, lastName, role, departmentId, officeHours, courses, dob ->
                        viewModel.updateTeacher(
                            teacher.copy(
                                firstName = firstName,
                                lastName = lastName,
                                role = role,
                                departmentId = departmentId.toString(),
                                officeHours = officeHours,
                                courses = courses,
                                dob = dob
                            )
                        )
                        showEditTeacherDialog = null
                    }
                )
            }

            showDeleteTeacherDialog?.let { teachersToDelete ->
                AlertDialog(
                    onDismissRequest = { showDeleteTeacherDialog = null },
                    title = { Text("Confirm Deletion") },
                    text = {
                        Text(
                            if (teachersToDelete.size == 1)
                                "Are you sure you want to delete ${teachersToDelete[0].firstName} ${teachersToDelete[0].lastName}?"
                            else
                                "Are you sure you want to delete ${teachersToDelete.size} teachers?"
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteTeachers(teachersToDelete)
                            showDeleteTeacherDialog = null
                        }) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteTeacherDialog = null }) { Text("Cancel") }
                    }
                )
            }

            if (showCreateCourseDialog) {
                CourseFormDialog(
                    teachers = teachers,
                    departments = departments,
                    isEditing = false,
                    onDismiss = { showCreateCourseDialog = false },
                    onSave = { name, title, description, location, teacherId, objectives, departmentId, timetableEntries ->
                        viewModel.createCourse(
                            name = title,
                            code = name,
                            credits = 0,
                            departmentId = departmentId,
                            description = description,
                            prerequisites = emptyList(),
                            timetableEntries = timetableEntries
                        )
                        showCreateCourseDialog = false
                    },
                    studentRepository = studentRepository,
                    courseRepository = courseRepository,
                    timetableEntryRepository = timetableEntryRepository
                )
            }

            showEditCourseDialog?.let { course ->
                CourseFormDialog(
                    course = course,
                    teachers = teachers,
                    departments = departments,
                    isEditing = true,
                    onDismiss = { showEditCourseDialog = null },
                    onSave = { name, title, description, location, teacherId, objectives, departmentId, timetableEntries ->
                        viewModel.updateCourse(
                            course.copy(
                                name = name,
                                title = title,
                                description = description,
                                location = location,
                                teacherId = teacherId,
                                objectives = objectives,
                                departmentId = departmentId,
                            ),
                            timetableEntries = timetableEntries
                        )
                        showEditCourseDialog = null
                    },
                    studentRepository = studentRepository,
                    courseRepository = courseRepository,
                    timetableEntryRepository = timetableEntryRepository
                )
            }

            showDeleteCourseDialog?.let { coursesToDelete ->
                AlertDialog(
                    onDismissRequest = { showDeleteCourseDialog = null },
                    title = { Text("Confirm Deletion") },
                    text = {
                        Text(
                            if (coursesToDelete.size == 1)
                                "Are you sure you want to delete ${coursesToDelete[0].name}?"
                            else
                                "Are you sure you want to delete ${coursesToDelete.size} courses?"
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteCourses(coursesToDelete)
                            showDeleteCourseDialog = null
                        }) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteCourseDialog = null }) { Text("Cancel") }
                    }
                )
            }

            if (showCreateDepartmentDialog) {
                DepartmentFormDialog(
                    onDismiss = { showCreateDepartmentDialog = false },
                    onSave = { name, type, description ->
                        viewModel.createDepartment(
                            name = name,
                            code = "",
                            type = type,
                            description = description.toString(),
                            headTeacherId = null
                        )
                        showCreateDepartmentDialog = false
                    }
                )
            }

            showEditDepartmentDialog?.let { department ->
                DepartmentFormDialog(
                    department = department,
                    onDismiss = { showEditDepartmentDialog = null },
                    onSave = { name, type, description ->
                        viewModel.updateDepartment(
                            department.copy(
                                name = name,
                                type = type,
                                description = description,
                                departmentId = department.departmentId
                            )
                        )
                        showEditDepartmentDialog = null
                    }
                )
            }

            showDeleteDepartmentDialog?.let { department ->
                AlertDialog(
                    onDismissRequest = { showDeleteDepartmentDialog = null },
                    title = { Text("Confirm Deletion") },
                    text = { Text("Are you sure you want to delete ${department.name}?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteDepartment(department)
                            showDeleteDepartmentDialog = null
                        }) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDepartmentDialog = null }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
fun CoursesTab(
    courses: List<FirebaseCourse>,
    teachers: List<FirebaseTeacher>,
    departments: List<FirebaseDepartment>,
    onCreateCourse: () -> Unit,
    onEditCourse: (FirebaseCourse) -> Unit,
    onDeleteCourses: (List<FirebaseCourse>) -> Unit,
    studentRepository: FirebaseStudentRepository,
    courseRepository: FirebaseCourseRepository,
    timetableEntryRepository: FirebaseTimetableRepository
) {
    var searchText by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("name") }
    var sortAscending by remember { mutableStateOf(true) }
    var selectedCourseIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sortExpanded by remember { mutableStateOf(false) }

    val sortedAndFilteredCourses by remember(courses, searchText, sortBy, sortAscending) {
        derivedStateOf {
            courses
                .filter {
                    searchText.isEmpty() ||
                            it.name.contains(searchText, ignoreCase = true) ||
                            it.title.contains(searchText, ignoreCase = true)
                }
                .sortedWith(
                    when (sortBy) {
                        "id" -> compareBy<FirebaseCourse> { it.courseId }
                        else -> compareBy<FirebaseCourse> { it.name }
                    }.let { comparator ->
                        if (sortAscending) comparator else comparator.reversed()
                    }
                )
        }
    }

    Column {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search Courses") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = when (sortBy) {
                        "name" -> "Name"
                        "id" -> "ID"
                        else -> "Name"
                    },
                    onValueChange = {},
                    label = { Text("Sort By") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sortExpanded = true },
                    readOnly = true
                )
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Name") },
                        onClick = { sortBy = "name"; sortExpanded = false },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("ID") },
                        onClick = { sortBy = "id"; sortExpanded = false },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            IconButton(
                onClick = { sortAscending = !sortAscending },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = "Toggle sort order"
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onCreateCourse,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text("Create Course")
            }
            Button(
                onClick = {
                    val coursesToDelete = courses.filter { selectedCourseIds.contains(it.courseId) }
                    if (coursesToDelete.isNotEmpty()) {
                        onDeleteCourses(coursesToDelete)
                    }
                },
                enabled = selectedCourseIds.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text("Delete Selected")
            }
        }
        LazyColumn {
            items(sortedAndFilteredCourses) { course ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Checkbox(
                        checked = selectedCourseIds.contains(course.courseId),
                        onCheckedChange = { isChecked: Boolean ->
                            selectedCourseIds = if (isChecked) {
                                selectedCourseIds + course.courseId
                            } else {
                                selectedCourseIds - course.courseId
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    CourseItem(
                        course = course,
                        teachers = teachers,
                        departments = departments,
                        onEdit = { onEditCourse(course) },
                        onDelete = { onDeleteCourses(listOf(course)) }
                    )
                }
            }
        }
    }
}

@Composable
fun CourseItem(
    course: FirebaseCourse,
    teachers: List<FirebaseTeacher>,
    departments: List<FirebaseDepartment>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    course.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Row {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
            if (expanded) {
                Text("ID: ${course.courseId}", style = MaterialTheme.typography.bodyMedium)
                Text("Title: ${course.title}", style = MaterialTheme.typography.bodyMedium)
                Text("Description: ${course.description}", style = MaterialTheme.typography.bodyMedium)
                Text("Objectives: ${course.objectives ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                Text("Location: ${course.location ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Teacher: ${teachers.find { it.teacherId == course.teacherId }?.let { "${it.firstName} ${it.lastName}" } ?: "Unassigned"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseFormDialog(
    course: FirebaseCourse? = null,
    teachers: List<FirebaseTeacher>,
    departments: List<FirebaseDepartment>,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, String, String, String, List<TimetableEntryFormData>) -> Unit,
    studentRepository: FirebaseStudentRepository,
    courseRepository: FirebaseCourseRepository,
    timetableEntryRepository: FirebaseTimetableRepository
) {
    var name by remember { mutableStateOf(course?.name ?: "") }
    var title by remember { mutableStateOf(course?.title ?: "") }
    var description by remember { mutableStateOf(course?.description ?: "") }
    var objectives by remember { mutableStateOf(course?.objectives ?: "") }
    var location by remember { mutableStateOf(course?.location ?: "") }
    var teacherId by remember { mutableStateOf(course?.teacherId ?: "") }
    var teacherExpanded by remember { mutableStateOf(false) }
    var departmentId by remember { mutableStateOf(course?.departmentId ?: "") }
    var departmentExpanded by remember { mutableStateOf(false) }

    // State to hold timetable entries for the form
    // Initialize with existing entries if editing (requires fetching them first)
    // For now, start with an empty list for new courses, or an empty list for editing until fetch is implemented
    var timetableEntries = remember { mutableStateListOf<TimetableEntryFormData>() }

    // State for adding a new timetable entry
    var newEntryDayOfWeek by remember { mutableStateOf(1) }
    var newEntryStartTime by remember { mutableStateOf("") }
    var newEntryEndTime by remember { mutableStateOf("") }
    var newEntryType by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current // To show time picker

    // Options for Day of Week dropdown
    val daysOfWeek = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    // Options for Entry Type dropdown
    val entryTypes = listOf("Lecture", "Lab", "Tutorial", "Other")

    LaunchedEffect(course, isEditing) {
        if (isEditing && course != null) {
            try {
                // Ensure repositories are accessed within the coroutine scope if needed
                val enrollments = studentRepository.getEnrollmentsByStudent(course.courseId)
                val enrolledCoursesData = enrollments.mapNotNull { enrollment ->
                    val course = courseRepository.getCourseByCourseId(enrollment.courseId)
                    val semesterInt = enrollment.semester.toIntOrNull() ?: return@mapNotNull null
                    course?.let { Triple(it.courseId, enrollment.year, semesterInt) }
                }
                // Clear existing entries and add new ones to the mutable state list
                timetableEntries.clear()
                enrolledCoursesData.map { (courseId, year, semester) ->
                    TimetableEntryFormData(
                        dayOfWeek = 1, // Default to Monday
                        startTime = "",
                        endTime = "",
                        room = "",
                        type = "" // Lecture, Lab, Tutorial
                    )
                }.forEach { entry -> timetableEntries.add(entry) }

            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to load timetable entries: ${e.message}")
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Course" else "Create Course") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Code *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = objectives,
                    onValueChange = { objectives = it },
                    label = { Text("Objectives (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Default Room") }, // Changed label to reflect its use as default
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedTextField(
                        value = teachers.find { it.teacherId == teacherId }?.let { "${it.firstName} ${it.lastName}" } ?: "Select Teacher *",
                        onValueChange = {},
                        label = { Text("Teacher") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { teacherExpanded = true },
                        readOnly = true
                    )
                    DropdownMenu(
                        expanded = teacherExpanded,
                        onDismissRequest = { teacherExpanded = false }
                    ) {
                        teachers.forEach { teacher ->
                            DropdownMenuItem(
                                text = { Text("${teacher.firstName} ${teacher.lastName}") },
                                onClick = {
                                    teacherId = teacher.teacherId
                                    teacherExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Box {
                    OutlinedTextField(
                        value = departments.find { it.departmentId == departmentId }?.name ?: "Select Department *",
                        onValueChange = {},
                        label = { Text("Department") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { departmentExpanded = true },
                        readOnly = true
                    )
                    DropdownMenu(
                        expanded = departmentExpanded,
                        onDismissRequest = { departmentExpanded = false }
                    ) {
                        departments.forEach { department ->
                            DropdownMenuItem(
                                text = { Text(department.name) },
                                onClick = {
                                    departmentId = department.departmentId
                                    departmentExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Timetable Entries Section
                Text("Timetable Entries", style = MaterialTheme.typography.titleMedium)

                // Display existing timetable entries
                timetableEntries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${daysOfWeek[entry.dayOfWeek - 1]} ${entry.startTime}-${entry.endTime}")
                            Text("${entry.type} in ${entry.room}")
                        }
                        IconButton(onClick = { timetableEntries.removeAt(index) }) {
                            Icon(Icons.Default.Delete, "Remove Entry")
                        }
                    }
                    if (index < timetableEntries.lastIndex) {
                        Divider()
                    }
                }

                // Add new timetable entry form
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Add New Entry", style = MaterialTheme.typography.titleSmall)
                    // Day of Week Dropdown
                    var dayOfWeekExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = daysOfWeek[newEntryDayOfWeek - 1],
                            onValueChange = { },
                            label = { Text("Day") },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dayOfWeekExpanded = true },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Select Day") }
                        )
                        DropdownMenu(
                            expanded = dayOfWeekExpanded,
                            onDismissRequest = { dayOfWeekExpanded = false }
                        ) {
                            daysOfWeek.forEachIndexed { index, day ->
                                DropdownMenuItem(
                                    text = { Text(day) },
                                    onClick = {
                                        newEntryDayOfWeek = index + 1
                                        dayOfWeekExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Time Pickers Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Time Picker
                        OutlinedTextField(
                            value = newEntryStartTime,
                            onValueChange = { newEntryStartTime = it },
                            label = { Text("Start Time (HH:mm)") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("e.g., 09:00") }
                            // Consider adding a visual transformation or input filter for time format
                        )
                        // End Time Picker
                        OutlinedTextField(
                            value = newEntryEndTime,
                            onValueChange = { newEntryEndTime = it },
                            label = { Text("End Time (HH:mm)") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("e.g., 10:00") }
                            // Consider adding a visual transformation or input filter for time format
                        )
                    }

                    // Room and Type Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                         OutlinedTextField(
                            value = newEntryType, // Use newEntryType state
                            onValueChange = { newEntryType = it },
                            label = { Text("Type") },
                            modifier = Modifier.weight(1f),
                             placeholder = { Text("e.g., Lecture") }
                            // Consider adding a dropdown for Type
                        )
                        OutlinedTextField(
                            value = location.ifEmpty { "Enter Room" }, // Use the main location field as default/source
                            onValueChange = { location = it }, // Allow editing the main location field here too? Or make this a separate field? Let's make it a separate field for the entry.
                             label = { Text("Room") }, // Changed label to reflect room for entry
                             modifier = Modifier.weight(1f),
                             placeholder = { Text("e.g., Room 101") }
                         )
                    }

                    // Button to add entry
                    Button(
                        onClick = {
                            if (newEntryStartTime.isNotBlank() && newEntryEndTime.isNotBlank() && newEntryType.isNotBlank() && location.isNotBlank()) { // Use main location for room for now
                                // Basic time format validation (HH:mm)
                                val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
                                try {
                                    timeFormat.parse(newEntryStartTime)
                                    timeFormat.parse(newEntryEndTime)
                                    timetableEntries.add(
                                        TimetableEntryFormData(
                                            dayOfWeek = newEntryDayOfWeek,
                                            startTime = newEntryStartTime,
                                            endTime = newEntryEndTime,
                                            room = location, // Use the main location field for room
                                            type = newEntryType
                                        )
                                    )
                                    // Clear new entry fields
                                    newEntryDayOfWeek = 1
                                    newEntryStartTime = ""
                                    newEntryEndTime = ""
                                    newEntryType = ""
                                    // Keep location as is, or clear it? Let's keep it.
                                } catch (e: Exception) {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Invalid time format (use HH:mm)") }
                                }
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Please fill all new entry fields") }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Add Entry")
                    }
                }

                Text(
                    "* indicates required field",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        if (name.isBlank() || title.isBlank() || description.isBlank() || teacherId.isBlank() || departmentId.isBlank()) {
                            throw IllegalArgumentException("Please fill all required fields")
                        }
                        // Pass the list of timetable entries to onSave
                        onSave(name, title, description, location.takeIf { it.isNotBlank() }, teacherId, objectives, departmentId, timetableEntries.toList())
                    } catch (e: Exception) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(e.message ?: "Invalid input")
                        }
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        modifier = Modifier.width(600.dp)
    )
}

@Composable
fun StudentsTab(
    students: List<FirebaseStudent>,
    courses: List<FirebaseCourse>,
    departments: List<FirebaseDepartment>,
    onCreateStudent: () -> Unit,
    onEditClickStudent: (FirebaseStudent) -> Unit,
    onDeleteStudents: (List<FirebaseStudent>) -> Unit,
    onBulkEnroll: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("name") }
    var sortAscending by remember { mutableStateOf(true) }
    var selectedStudentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sortExpanded by remember { mutableStateOf(false) }

    val sortedAndFilteredStudents by remember(students, searchText, sortBy, sortAscending) {
        derivedStateOf {
            students
                .filter {
                    searchText.isEmpty() ||
                            it.firstName.contains(searchText, ignoreCase = true) ||
                            it.lastName.contains(searchText, ignoreCase = true)
                }
                .sortedWith(
                    when (sortBy) {
                        "id" -> compareBy<FirebaseStudent> { it.studentId }
                        "enrollment" -> compareBy<FirebaseStudent> { parseEnrollmentDate(it.enrollmentDate) }
                        else -> compareBy<FirebaseStudent> { "${it.firstName} ${it.lastName}" }
                    }.let { comparator ->
                        if (sortAscending) comparator else comparator.reversed()
                    }
                )
        }
    }

    Column {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search Students") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = when (sortBy) {
                        "name" -> "Name"
                        "id" -> "ID"
                        "enrollment" -> "Enrollment Date"
                        else -> "Name"
                    },
                    onValueChange = {},
                    label = { Text("Sort By") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sortExpanded = true },
                    readOnly = true
                )
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Name") },
                        onClick = { sortBy = "name"; sortExpanded = false },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("ID") },
                        onClick = { sortBy = "id"; sortExpanded = false },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("Enrollment Date") },
                        onClick = { sortBy = "enrollment"; sortExpanded = false },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            IconButton(
                onClick = { sortAscending = !sortAscending },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = "Toggle sort order"
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onCreateStudent,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text("Create Student")
            }
            Button(
                onClick = onBulkEnroll,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text("Bulk Enroll")
            }
            Button(
                onClick = {
                    val studentsToDelete = students.filter { selectedStudentIds.contains(it.studentId) }
                    if (studentsToDelete.isNotEmpty()) {
                        onDeleteStudents(studentsToDelete)
                    }
                },
                enabled = selectedStudentIds.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text("Delete Selected")
            }
        }
        LazyColumn {
            items(sortedAndFilteredStudents) { student ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Checkbox(
                        checked = selectedStudentIds.contains(student.studentId),
                        onCheckedChange = { isChecked: Boolean ->
                            selectedStudentIds = if (isChecked) {
                                selectedStudentIds + student.studentId
                            } else {
                                selectedStudentIds - student.studentId
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    StudentItem(
                        student = student,
                        courses = courses,
                        departments = departments,
                        onEdit = { onEditClickStudent(student) },
                        onDelete = { onDeleteStudents(listOf(student)) }
                    )
                }
            }
        }
    }
}

@Composable
fun TeachersTab(
    teachers: List<FirebaseTeacher>,
    departments: List<FirebaseDepartment>,
    onCreateTeacher: () -> Unit,
    onEditTeacher: (FirebaseTeacher) -> Unit,
    onDeleteTeachers: (List<FirebaseTeacher>) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("name") }
    var sortAscending by remember { mutableStateOf(true) }
    var selectedTeacherIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sortExpanded by remember { mutableStateOf(false) }

    val sortedAndFilteredTeachers by remember(teachers, searchText, sortBy, sortAscending) {
        derivedStateOf {
            teachers
                .filter {
                    searchText.isEmpty() ||
                            it.firstName.contains(searchText, ignoreCase = true) ||
                            it.lastName.contains(searchText, ignoreCase = true)
                }
                .sortedWith(
                    when (sortBy) {
                        "id" -> compareBy<FirebaseTeacher> { it.teacherId }
                        else -> compareBy<FirebaseTeacher> { "${it.firstName} ${it.lastName}" }
                    }.let { comparator ->
                        if (sortAscending) comparator else comparator.reversed()
                    }
                )
        }
    }

    Column {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search Teachers") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = when (sortBy) {
                        "name" -> "Name"
                        "id" -> "ID"
                        else -> "Name"
                    },
                    onValueChange = {},
                    label = { Text("Sort By") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sortExpanded = true },
                    readOnly = true
                )
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Name") },
                        onClick = { sortBy = "name"; sortExpanded = false },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("ID") },
                        onClick = { sortBy = "id"; sortExpanded = false },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            IconButton(
                onClick = { sortAscending = !sortAscending },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = "Toggle sort order"
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onCreateTeacher,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text("Create Teacher")
            }
            Button(
                onClick = {
                    val teachersToDelete = teachers.filter { selectedTeacherIds.contains(it.teacherId) }
                    if (teachersToDelete.isNotEmpty()) {
                        onDeleteTeachers(teachersToDelete)
                    }
                },
                enabled = selectedTeacherIds.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text("Delete Selected")
            }
        }
        LazyColumn {
            items(sortedAndFilteredTeachers) { teacher ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Checkbox(
                        checked = selectedTeacherIds.contains(teacher.teacherId),
                        onCheckedChange = { isChecked: Boolean ->
                            selectedTeacherIds = if (isChecked) {
                                selectedTeacherIds + teacher.teacherId
                            } else {
                                selectedTeacherIds - teacher.teacherId
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    TeacherItem(
                        teacher = teacher,
                        departments = departments,
                        onEdit = { onEditTeacher(teacher) },
                        onDelete = { onDeleteTeachers(listOf(teacher)) }
                    )
                }
            }
        }
    }
}

@Composable
fun DepartmentsTab(
    departments: List<FirebaseDepartment>,
    onCreateDepartment: () -> Unit,
    onEditDepartment: (FirebaseDepartment) -> Unit,
    onDeleteDepartment: (FirebaseDepartment) -> Unit
) {
    Column {
        Button(
            onClick = onCreateDepartment,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Create Department/Major/Minor")
        }
        LazyColumn {
            items(departments) { department ->
                DepartmentItem(
                    department = department,
                    onEdit = { onEditDepartment(department) },
                    onDelete = { onDeleteDepartment(department) }
                )
            }
        }
    }
}

@Composable
fun ActivityTab(
    activities: List<FirebaseActivityLog>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        items(activities) { activity ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        activity.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(activity.timestamp))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun StudentItem(
    student: FirebaseStudent,
    courses: List<FirebaseCourse>,
    departments: List<FirebaseDepartment>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${student.firstName} ${student.lastName}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
            if (expanded) {
                Text("ID: ${student.studentId}", style = MaterialTheme.typography.bodyMedium)
                Text("Username: ${student.username}", style = MaterialTheme.typography.bodyMedium)
                Text("Role: ${student.role}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Major: ${departments.find { it.departmentId == student.majorId }?.name ?: "Unknown"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Minor: ${student.minorId?.let { departments.find { d -> d.departmentId == it }?.name } ?: "None"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Enrollment: ${student.enrollmentDate}", style = MaterialTheme.typography.bodyMedium)
                Text("Year: ${student.yearOfStudy}", style = MaterialTheme.typography.bodyMedium)
                Text("GPA: ${student.gpa}", style = MaterialTheme.typography.bodyMedium)
                Text("DOB: ${student.dob}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun TeacherItem(
    teacher: FirebaseTeacher,
    departments: List<FirebaseDepartment>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${teacher.firstName} ${teacher.lastName}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
            if (expanded) {
                Text("ID: ${teacher.teacherId}", style = MaterialTheme.typography.bodyMedium)
                Text("Username: ${teacher.username}", style = MaterialTheme.typography.bodyMedium)
                Text("Role: ${teacher.role}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Department: ${departments.find { it.departmentId == teacher.departmentId }?.name ?: "Unknown"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Office Hours: ${teacher.officeHours}", style = MaterialTheme.typography.bodyMedium)
                Text("Courses: ${teacher.courses.joinToString()}", style = MaterialTheme.typography.bodyMedium)
                Text("DOB: ${teacher.dob}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun DepartmentItem(
    department: FirebaseDepartment,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${department.name} (${department.type})", style = MaterialTheme.typography.bodyLarge)
                department.description?.let { Text("Description: $it", style = MaterialTheme.typography.bodyMedium) }
            }
            Row {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFormDialog(
    student: FirebaseStudent? = null,
    studentRepository: FirebaseStudentRepository,
    courses: List<FirebaseCourse>,
    departments: List<FirebaseDepartment>,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String?, Int, String, List<Triple<String, Int, Int>>) -> Unit,
    courseRepository: FirebaseCourseRepository
) {
    var firstName by remember { mutableStateOf(student?.firstName ?: "") }
    var lastName by remember { mutableStateOf(student?.lastName ?: "") }
    var role by remember { mutableStateOf(student?.role ?: "Student") }
    var enrollmentDate by remember { mutableStateOf(student?.enrollmentDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var majorId by remember { mutableStateOf(student?.majorId ?: "") }
    var minorId by remember { mutableStateOf(student?.minorId) }
    var yearOfStudy by remember { mutableStateOf(student?.yearOfStudy?.toString() ?: "") }
    var dob by remember { mutableStateOf(student?.dob ?: "") }
    var selectedCourses by remember { mutableStateOf<List<Triple<String, Int, Int>>>(emptyList()) }
    var majorExpanded by remember { mutableStateOf(false) }
    var minorExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Initialize selectedCourses when student data is available
    LaunchedEffect(student, isEditing) {
        if (isEditing && student != null) {
            try {
                val enrollments = studentRepository.getEnrollmentsByStudent(student.studentId)
                selectedCourses = enrollments.mapNotNull { enrollment ->
                    val course = courseRepository.getCourseByCourseId(enrollment.courseId)
                    val semesterInt = enrollment.semester.toIntOrNull() ?: return@mapNotNull null
                    course?.let { Triple(it.courseId, enrollment.year, semesterInt) }
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to load courses: ${e.message}")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dob = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Student" else "Create Student") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditing
                )
                OutlinedTextField(
                    value = enrollmentDate,
                    onValueChange = { enrollmentDate = it },
                    label = { Text("Enrollment Date (yyyy-MM-dd) *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedTextField(
                        value = departments.find { it.departmentId == majorId }?.name ?: "Select Major",
                        onValueChange = {},
                        label = { Text("Major *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { majorExpanded = true },
                        readOnly = true
                    )
                    DropdownMenu(
                        expanded = majorExpanded,
                        onDismissRequest = { majorExpanded = false }
                    ) {
                        departments.filter { it.type == "Major" }.forEach { department ->
                            DropdownMenuItem(
                                text = { Text(department.name) },
                                onClick = {
                                    majorId = department.departmentId
                                    majorExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Box {
                    OutlinedTextField(
                        value = minorId?.let { departments.find { it.departmentId == minorId }?.name } ?: "Select Minor (Optional)",
                        onValueChange = {},
                        label = { Text("Minor") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { minorExpanded = true },
                        readOnly = true
                    )
                    DropdownMenu(
                        expanded = minorExpanded,
                        onDismissRequest = { minorExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                minorId = null
                                minorExpanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                        departments.filter { it.type == "Minor" }.forEach { department ->
                            DropdownMenuItem(
                                text = { Text(department.name) },
                                onClick = {
                                    minorId = department.departmentId
                                    minorExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = yearOfStudy,
                    onValueChange = { yearOfStudy = it },
                    label = { Text("Year of Study *") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isEditing) {
                    OutlinedTextField(
                        value = student?.gpa?.toString() ?: "0.0",
                        onValueChange = {},
                        label = { Text("GPA (Calculated)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )
                }
                OutlinedTextField(
                    value = dob,
                    onValueChange = {},
                    label = { Text("Date of Birth *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    readOnly = true
                )
                Text(
                    "Select Courses",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
                courses.forEach { course ->
                    val courseState = selectedCourses.find { it.first == course.courseId }
                    var isChecked by remember(courseState) { mutableStateOf(courseState != null) }
                    var year by remember(courseState) { mutableStateOf(courseState?.second?.toString() ?: "2025") }
                    var semester by remember(courseState) { mutableStateOf(courseState?.third?.toString() ?: "1") }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked: Boolean ->
                                isChecked = checked
                                selectedCourses = if (checked) {
                                    selectedCourses + Triple(course.courseId, year.toIntOrNull() ?: 2025, semester.toIntOrNull() ?: 1)
                                } else {
                                    selectedCourses.filter { it.first != course.courseId }
                                }
                            }
                        )
                        Text(course.name, modifier = Modifier.weight(1f))
                        if (isChecked) {
                            OutlinedTextField(
                                value = year,
                                onValueChange = { newYear ->
                                    year = newYear
                                    selectedCourses = selectedCourses.map { courseEnrollment ->
                                        if (courseEnrollment.first == course.courseId) {
                                            Triple(courseEnrollment.first, newYear.toIntOrNull() ?: courseEnrollment.second, courseEnrollment.third)
                                        } else {
                                            courseEnrollment
                                        }
                                    }
                                },
                                label = { Text("Year") },
                                modifier = Modifier.width(100.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = semester,
                                onValueChange = { newSemester ->
                                    semester = newSemester
                                    selectedCourses = selectedCourses.map { courseEnrollment ->
                                        if (courseEnrollment.first == course.courseId) {
                                            Triple(courseEnrollment.first, courseEnrollment.second, newSemester.toIntOrNull() ?: courseEnrollment.third)
                                        } else {
                                            courseEnrollment
                                        }
                                    }
                                },
                                label = { Text("Sem") },
                                modifier = Modifier.width(80.dp)
                            )
                        }
                    }
                }
                Text(
                    "* indicates required field",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        if (firstName.isBlank() || lastName.isBlank() || enrollmentDate.isBlank() ||
                            majorId.isBlank() || yearOfStudy.isBlank() || dob.isBlank()
                        ) {
                            throw IllegalArgumentException("Please fill all required fields")
                        }

                        if (selectedCourses.any { it.second <= 0 || it.third <= 0 }) {
                            throw IllegalArgumentException("Year and semester must be positive")
                        }
                        onSave(
                            firstName,
                            lastName,
                            role,
                            enrollmentDate,
                            majorId,
                            minorId,
                            yearOfStudy.toInt(),
                            dob,
                            selectedCourses
                        )
                    } catch (e: Exception) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(e.message ?: "Invalid input")
                        }
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        modifier = Modifier.width(600.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkEnrollDialog(
    students: List<FirebaseStudent>,
    courses: List<FirebaseCourse>,
    onDismiss: () -> Unit,
    onSave: (List<String>, String, Int, Int) -> Unit
) {
    var selectedStudentIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var courseId by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2025") }
    var semester by remember { mutableStateOf("1") }
    var courseExpanded by remember { mutableStateOf(false) }
    var snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Enroll Students") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box {
                    OutlinedTextField(
                        value = courses.find { it.courseId == courseId }?.name ?: "Select Course",
                        onValueChange = {},
                        label = { Text("Course *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { courseExpanded = true },
                        readOnly = true
                    )
                    DropdownMenu(
                        expanded = courseExpanded,
                        onDismissRequest = { courseExpanded = false }
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text(course.name) },
                                onClick = {
                                    courseId = course.courseId
                                    courseExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Year *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = semester,
                    onValueChange = { semester = it },
                    label = { Text("Semester *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Select Students",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(students) { student ->
                        var isChecked by remember { mutableStateOf(false) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked: Boolean ->
                                    isChecked = checked
                                    selectedStudentIds = if (checked) {
                                        selectedStudentIds + student.studentId
                                    } else {
                                        selectedStudentIds - student.studentId
                                    }
                                }
                            )
                            Text("${student.firstName} ${student.lastName}")
                        }
                    }
                }
                Text(
                    "* indicates required field",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        if (selectedStudentIds.isEmpty() || courseId.isBlank() || year.isBlank() || semester.isBlank()) {
                            throw IllegalArgumentException("Please select at least one student, a course, and specify year and semester")
                        }
                        val yearInt = year.toIntOrNull() ?: throw IllegalArgumentException("Invalid year")
                        val semesterInt = semester.toIntOrNull() ?: throw IllegalArgumentException("Invalid semester")
                        if (yearInt <= 0 || semesterInt <= 0) {
                            throw IllegalArgumentException("Year and semester must be positive")
                        }
                        onSave(selectedStudentIds, courseId, yearInt, semesterInt)
                    } catch (e: Exception) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(e.message ?: "Invalid input")
                        }
                    }
                }
            ) { Text("Enroll") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        modifier = Modifier.width(600.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherFormDialog(
    teacher: FirebaseTeacher? = null,
    departments: List<FirebaseDepartment>,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, List<String>, String) -> Unit
) {
    var firstName by remember { mutableStateOf(teacher?.firstName ?: "") }
    var lastName by remember { mutableStateOf(teacher?.lastName ?: "") }
    var role by remember { mutableStateOf(teacher?.role ?: "Teacher") }
    var departmentId by remember { mutableStateOf(teacher?.departmentId ?: "") }
    var officeHours by remember { mutableStateOf(teacher?.officeHours ?: "") }
    var courses by remember { mutableStateOf(teacher?.courses?.joinToString(", ") ?: "") }
    var dob by remember { mutableStateOf(teacher?.dob ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dob = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Teacher" else "Create Teacher") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditing
                )
                Box {
                    OutlinedTextField(
                        value = departments.find { it.departmentId == departmentId }?.name ?: "Select Department",
                        onValueChange = {},
                        label = { Text("Department *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true },
                        readOnly = true
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        departments.forEach { department ->
                            DropdownMenuItem(
                                text = { Text(department.name) },
                                onClick = {
                                    departmentId = department.departmentId
                                    expanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = officeHours,
                    onValueChange = { officeHours = it },
                    label = { Text("Office Hours *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = courses,
                    onValueChange = { courses = it },
                    label = { Text("Courses (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dob,
                    onValueChange = {},
                    label = { Text("Date of Birth *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    readOnly = true
                )
                Text(
                    "* indicates required field",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        if (firstName.isBlank() || lastName.isBlank() || departmentId.isBlank() ||
                            officeHours.isBlank() || dob.isBlank()
                        ) {
                            throw IllegalArgumentException("Please fill all required fields")
                        }
                        onSave(
                            firstName,
                            lastName,
                            role,
                            departmentId,
                            officeHours,
                            courses.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            dob
                        )
                    } catch (e: Exception) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(e.message ?: "Invalid input")
                        }
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        modifier = Modifier.width(600.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentFormDialog(
    department: FirebaseDepartment? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf(department?.name ?: "") }
    var type by remember { mutableStateOf(department?.type ?: "Department") }
    var description by remember { mutableStateOf(department?.description ?: "") }
    var expanded by remember { mutableStateOf(false) }
    val types = listOf("Department", "Major", "Minor")
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (department == null) "Create Department/Major/Minor" else "Edit Department/Major/Minor") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        label = { Text("Type *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true },
                        readOnly = true
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    type = t
                                    expanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "* indicates required field",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        if (name.isBlank() || type.isBlank()) {
                            throw IllegalArgumentException("Please fill all required fields")
                        }
                        onSave(name, type, description.takeIf { it.isNotBlank() })
                    } catch (e: Exception) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(e.message ?: "Invalid input")
                        }
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        modifier = Modifier.width(600.dp)
    )
}