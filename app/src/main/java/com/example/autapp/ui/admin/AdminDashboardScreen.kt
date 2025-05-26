package com.example.autapp.ui.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.autapp.data.models.*
import com.example.autapp.data.repository.*
import com.example.autapp.ui.components.AUTTopAppBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

// Helper function to parse enrollmentDate (String) to LocalDate for sorting
fun parseEnrollmentDate(date: String): LocalDate {
    return try {
        LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: DateTimeParseException) {
        LocalDate.of(1970, 1, 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    studentRepository: StudentRepository,
    teacherRepository: TeacherRepository,
    courseRepository: CourseRepository,
    departmentRepository: DepartmentRepository,
    navController: NavController,
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
    val tabs = listOf("Students", "Teachers", "Departments", "Activity")
    var showCreateStudentDialog by remember { mutableStateOf(false) }
    var showEditStudentDialog by remember { mutableStateOf<Student?>(null) }
    var showDeleteStudentDialog by remember { mutableStateOf<List<Student>?>(null) }
    var showCreateTeacherDialog by remember { mutableStateOf(false) }
    var showEditTeacherDialog by remember { mutableStateOf<Teacher?>(null) }
    var showDeleteTeacherDialog by remember { mutableStateOf<List<Teacher>?>(null) }
    var showCreateDepartmentDialog by remember { mutableStateOf(false) }
    var showEditDepartmentDialog by remember { mutableStateOf<Department?>(null) }
    var showDeleteDepartmentDialog by remember { mutableStateOf<Department?>(null) }
    var showBulkEnrollDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AUTTopAppBar(
                title = "Admin Dashboard",
                isDarkTheme = false,
                navController = navController,
                showBackButton = false, // Admin dashboard is top-level
                currentRoute = "admin_dashboard",
                currentStudentId = null,
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
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
                    onEditStudent = { showEditStudentDialog = it },
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
                2 -> DepartmentsTab(
                    departments = departments,
                    onCreateDepartment = { showCreateDepartmentDialog = true },
                    onEditDepartment = { showEditDepartmentDialog = it },
                    onDeleteDepartment = { showDeleteDepartmentDialog = it }
                )
                3 -> ActivityTab(
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
                            firstName, lastName, role, enrollmentDate,
                            majorId, minorId, yearOfStudy, 0.0, dob, selectedCourses, 2025, 1
                        )
                        showCreateStudentDialog = false
                    }
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
                                majorId = majorId,
                                minorId = minorId,
                                yearOfStudy = yearOfStudy,
                                gpa = student.gpa,
                                dob = dob
                            ),
                            selectedCourses,
                            2025,
                            1
                        )
                        showEditStudentDialog = null
                    }
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
                        viewModel.bulkEnroll(studentIds, courseId, year, semester)
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
                            firstName, lastName, role, departmentId,
                            officeHours, courses, dob
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
                                departmentId = departmentId,
                                officeHours = officeHours,
                                courses = courses.toMutableList(),
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

            if (showCreateDepartmentDialog) {
                DepartmentFormDialog(
                    onDismiss = { showCreateDepartmentDialog = false },
                    onSave = { name, type, description ->
                        viewModel.createDepartment(name, type, description)
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
                            department.copy(name = name, type = type, description = description)
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

// Rest of the file (StudentsTab, TeachersTab, etc.) remains unchanged
@Composable
fun StudentsTab(
    students: List<Student>,
    courses: List<Course>,
    departments: List<Department>,
    onCreateStudent: () -> Unit,
    onEditStudent: (Student) -> Unit,
    onDeleteStudents: (List<Student>) -> Unit,
    onBulkEnroll: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("name") }
    var sortAscending by remember { mutableStateOf(true) }
    var selectedStudentIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
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
                        "id" -> compareBy<Student> { it.studentId }
                        "enrollment" -> compareBy<Student> { parseEnrollmentDate(it.enrollmentDate) }
                        else -> compareBy<Student> { "${it.firstName} ${it.lastName}" }
                    }.let { comparator ->
                        if (sortAscending) comparator else compareByDescending { comparator.compare(it, it) }
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
                        onClick = { sortBy = "name"; sortExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("ID") },
                        onClick = { sortBy = "id"; sortExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Enrollment Date") },
                        onClick = { sortBy = "enrollment"; sortExpanded = false }
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
                        onCheckedChange = { isChecked ->
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
                        onEdit = { onEditStudent(student) },
                        onDelete = { onDeleteStudents(listOf(student)) }
                    )
                }
            }
        }
    }
}

@Composable
fun TeachersTab(
    teachers: List<Teacher>,
    departments: List<Department>,
    onCreateTeacher: () -> Unit,
    onEditTeacher: (Teacher) -> Unit,
    onDeleteTeachers: (List<Teacher>) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("name") }
    var sortAscending by remember { mutableStateOf(true) }
    var selectedTeacherIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
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
                        "id" -> compareBy<Teacher> { it.teacherId }
                        else -> compareBy<Teacher> { "${it.firstName} ${it.lastName}" }
                    }.let { comparator ->
                        if (sortAscending) comparator else compareByDescending { comparator.compare(it, it) }
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
                        onClick = { sortBy = "name"; sortExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("ID") },
                        onClick = { sortBy = "id"; sortExpanded = false }
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
                        onCheckedChange = { isChecked ->
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
    departments: List<Department>,
    onCreateDepartment: () -> Unit,
    onEditDepartment: (Department) -> Unit,
    onDeleteDepartment: (Department) -> Unit
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
    activities: List<ActivityLog>
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
                        "Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(activity.timestamp))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun StudentItem(
    student: Student,
    courses: List<Course>,
    departments: List<Department>,
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
    teacher: Teacher,
    departments: List<Department>,
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
    department: Department,
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
    student: Student? = null,
    studentRepository: StudentRepository,
    courses: List<Course>,
    departments: List<Department>,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int, Int?, Int, String, List<Triple<Int, Int, Int>>) -> Unit
) {
    var firstName by remember { mutableStateOf(student?.firstName ?: "") }
    var lastName by remember { mutableStateOf(student?.lastName ?: "") }
    var role by remember { mutableStateOf(student?.role ?: "Student") }
    var enrollmentDate by remember { mutableStateOf(student?.enrollmentDate ?: SimpleDateFormat("yyyy-MM-dd").format(Date())) }
    var majorId by remember { mutableStateOf(student?.majorId ?: 0) }
    var minorId by remember { mutableStateOf(student?.minorId) }
    var yearOfStudy by remember { mutableStateOf(student?.yearOfStudy?.toString() ?: "") }
    var dob by remember { mutableStateOf(student?.dob ?: "") }
    var selectedCourses by remember { mutableStateOf<List<Triple<Int, Int, Int>>>(emptyList()) }
    var majorExpanded by remember { mutableStateOf(false) }
    var minorExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(student, isEditing) {
        if (isEditing && student != null) {
            try {
                val enrolledCourses = studentRepository.getStudentCoursesWithEnrollmentInfo(student.studentId)
                selectedCourses = enrolledCourses.map { Triple(it.course.courseId, it.year, it.semester) }
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
                            dob = SimpleDateFormat("yyyy-MM-dd").format(Date(millis))
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
                                }
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
                            }
                        )
                        departments.filter { it.type == "Minor" }.forEach { department ->
                            DropdownMenuItem(
                                text = { Text(department.name) },
                                onClick = {
                                    minorId = department.departmentId
                                    minorExpanded = false
                                }
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
                    var isChecked by remember { mutableStateOf(courseState != null) }
                    var year by remember { mutableStateOf(courseState?.second?.toString() ?: "2025") }
                    var semester by remember { mutableStateOf(courseState?.third?.toString() ?: "1") }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
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
                                    selectedCourses = selectedCourses.map {
                                        if (it.first == course.courseId) Triple(it.first, newYear.toIntOrNull() ?: it.second, it.third)
                                        else it
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
                                    selectedCourses = selectedCourses.map {
                                        if (it.first == course.courseId) Triple(it.first, it.second, newSemester.toIntOrNull() ?: it.third)
                                        else it
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
                            majorId == 0 || yearOfStudy.isBlank() || dob.isBlank()
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
    students: List<Student>,
    courses: List<Course>,
    onDismiss: () -> Unit,
    onSave: (List<Int>, Int, Int, Int) -> Unit
) {
    var selectedStudentIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var courseId by remember { mutableStateOf(0) }
    var year by remember { mutableStateOf("2025") }
    var semester by remember { mutableStateOf("1") }
    var courseExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
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
                                }
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
                                onCheckedChange = { checked ->
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
                        if (selectedStudentIds.isEmpty() || courseId == 0 || year.isBlank() || semester.isBlank()) {
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
    teacher: Teacher? = null,
    departments: List<Department>,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, String, List<String>, String) -> Unit
) {
    var firstName by remember { mutableStateOf(teacher?.firstName ?: "") }
    var lastName by remember { mutableStateOf(teacher?.lastName ?: "") }
    var role by remember { mutableStateOf(teacher?.role ?: "Teacher") }
    var departmentId by remember { mutableStateOf(teacher?.departmentId ?: 0) }
    var officeHours by remember { mutableStateOf(teacher?.officeHours ?: "") }
    var courses by remember { mutableStateOf(teacher?.courses?.joinToString(", ") ?: "") }
    var dob by remember { mutableStateOf(teacher?.dob ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dob = SimpleDateFormat("yyyy-MM-dd").format(Date(millis))
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
                        departments.filter { it.type == "Department" }.forEach { department ->
                            DropdownMenuItem(
                                text = { Text(department.name) },
                                onClick = {
                                    departmentId = department.departmentId
                                    expanded = false
                                }
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
                        if (firstName.isBlank() || lastName.isBlank() || departmentId == 0 ||
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
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentFormDialog(
    department: Department? = null,
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
                                }
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
                        onSave(name, type, description)
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
        }
    )
}