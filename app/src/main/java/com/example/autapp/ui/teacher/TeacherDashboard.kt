package com.example.autapp.ui.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.autapp.data.firebase.*
import java.util.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import android.app.DatePickerDialog
import android.app.TimePickerDialog as AndroidTimePickerDialog
import androidx.compose.ui.platform.LocalContext
import com.example.autapp.data.firebase.FirebaseDepartmentRepository
import kotlinx.coroutines.launch
import android.util.Log

private const val TAG = "TeacherDashboard"

@Composable
fun TeacherDashboard(
    viewModel: TeacherDashboardViewModel,
    departmentRepository: FirebaseDepartmentRepository,
    modifier: Modifier = Modifier,
    teacherId: String,
    paddingValues: PaddingValues
) {
    var showAddAssignmentDialog by remember { mutableStateOf(false) }
    var showAddGradeDialog by remember { mutableStateOf(false) }
    var selectedCourseForAssignment by remember { mutableStateOf<FirebaseCourse?>(null) }
    var selectedAssignmentForGrade by remember { mutableStateOf<FirebaseAssignment?>(null) }

    // When a course is selected, load students
    val selectedCourse by viewModel.selectedCourse.collectAsState()
    val studentsInCourse by viewModel.studentsInSelectedCourse.collectAsState()

    // State for department name
    var departmentName by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch department name when teacher is loaded
    viewModel.teacher?.let { teacher ->
        LaunchedEffect(teacher.departmentId) {
            coroutineScope.launch {
                val department = departmentRepository.getDepartmentByDepartmentId(teacher.departmentId.toString())
                departmentName = department?.name ?: "Unknown"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header with Add Course button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Teacher Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Teacher Info Card
        viewModel.teacher?.let { teacher ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "${teacher.firstName} ${teacher.lastName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Department: ${departmentName ?: "Loading..."}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Office Hours: ${teacher.officeHours}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // My Courses Section
        Text(
            text = "My Courses",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            viewModel.courses.forEach { course ->
                TeacherCourseCard(
                    viewModel = viewModel,
                    course = course,
                    onAddAssignment = {
                        selectedCourseForAssignment = course
                        showAddAssignmentDialog = true
                    },
                    onClick = { viewModel.selectCourse(course) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Activity Section (Assignments and Grades)
        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Assignments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (viewModel.assignments.isEmpty()) {
                    Text("No assignments added yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    viewModel.assignments.take(5).forEach { assignment ->
                        Text(
                            text = "${assignment.name} for Course ${viewModel.courses.find { it.courseId == assignment.courseId }?.name ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Grades",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (viewModel.assignments.isEmpty()) {
                    Text("No grades added yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val allStudents = viewModel.allStudents
                    viewModel.grades.take(5).forEach { grade ->
                        val student = allStudents.find { it.studentId == grade.studentId }
                        val studentName = if (student != null) "${student.firstName} ${student.lastName}" else "Student ${grade.studentId}"
                        val assignment = viewModel.assignments.find { it.assignmentId == grade.assignmentId }
                        val assignmentName = assignment?.name ?: "Unknown"
                        val courseName = viewModel.courses.find { it.courseId == assignment?.courseId }?.name ?: "Unknown"
                        Text(
                            text = "$studentName received a grade of ${grade.grade} for $assignmentName in $courseName",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error Message
        viewModel.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        selectedCourse?.let { course ->
            LaunchedEffect(course) {
                viewModel.loadStudentsForCourse(course)
            }
            StudentsInCourseSection(
                students = studentsInCourse,
                course = course
            )
        }
    }

    // Add Assignment Dialog
    if (showAddAssignmentDialog && selectedCourseForAssignment != null) {
        AddAssignmentDialog(
            course = selectedCourseForAssignment!!,
            onDismiss = { showAddAssignmentDialog = false },
            onConfirm = { name, location, dueDate, weight, maxScore, type, courseId ->
                viewModel.createAssignment(
                    FirebaseAssignment(
                        name = name,
                        location = location,
                        due = dueDate,
                        weight = weight,
                        maxScore = maxScore,
                        type = type,
                        courseId = courseId
                    )
                )
                showAddAssignmentDialog = false
            }
        )
    }

    // Add Grade Dialog
    if (showAddGradeDialog && selectedAssignmentForGrade != null) {
        AddGradeDialog(
            assignment = selectedAssignmentForGrade!!,
            onDismiss = { showAddGradeDialog = false },
            onConfirm = { studentId, score ->
                viewModel.addGrade(
                    FirebaseGrade(
                        assignmentId = selectedAssignmentForGrade!!.assignmentId,
                        studentId = studentId,
                        _score = score,
                        grade = "", // Grade will be calculated automatically based on score
                        feedback = null
                    )
                )
                showAddGradeDialog = false
            }
        )
    }
}

@Composable
fun TeacherCourseCard(
    viewModel: TeacherDashboardViewModel,
    course: FirebaseCourse,
    onAddAssignment: () -> Unit,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showEditDescriptionDialog by remember { mutableStateOf(false) }
    var showEditGradesDialog by remember { mutableStateOf(false) }
    var selectedAssignment by remember { mutableStateOf<FirebaseAssignment?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Pair<Boolean, FirebaseAssignment?>>(false to null) }

    val studentsInCourse by viewModel.studentsInSelectedCourse.collectAsState()
    val gradesForAssignment by viewModel.gradesForAssignment.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row {
                    IconButton(onClick = { showEditDescriptionDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Description")
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Description: ${course.description}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Location: ${course.location}",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Display Assignments for this course
                val courseAssignments = viewModel.assignments.filter { it.courseId == course.courseId }
                if (courseAssignments.isNotEmpty()) {
                    Text("Assignments:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    courseAssignments.forEach { assignment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(assignment.name, style = MaterialTheme.typography.bodyMedium)
                            Row {
                                Button(onClick = {
                                    Log.d(TAG, "View/Add Grades button clicked for assignment: ${assignment.name}")
                                    viewModel.loadStudentsForCourse(course)
                                    viewModel.loadGradesForAssignment(assignment.assignmentId)
                                    selectedAssignment = assignment
                                    showEditGradesDialog = true
                                }) {
                                    Text("View/Add Grades")
                                }
                                IconButton(onClick = {
                                    showDeleteDialog = true to assignment
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Assignment")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = onAddAssignment) {
                    Text("Add Assignment")
                }
            }
        }
    }

    if (showEditDescriptionDialog) {
        EditCourseDescriptionDialog(
            course = course,
            onDismiss = { showEditDescriptionDialog = false },
            onConfirm = { title, summary, objectives ->
                viewModel.updateCourseDescription(
                    FirebaseCourse(
                        courseId = course.courseId,
                        name = course.name,
                        title = title,
                        description = "$summary\n\nCourse Objectives:\n$objectives"
                    )
                )
                showEditDescriptionDialog = false
            }
        )
    }

    if (showEditGradesDialog && selectedAssignment != null) {
        EditGradesDialog(
            students = studentsInCourse,
            grades = gradesForAssignment,
            assignment = selectedAssignment!!,
            onDismiss = { showEditGradesDialog = false },
            onSave = { updatedGrades ->
                Log.d(TAG, "Save button clicked in EditGradesDialog")
                viewModel.saveGradesForAssignment(updatedGrades)
                viewModel.loadAllGrades()
                showEditGradesDialog = false
            }
        )
    }

    // Delete assignment confirmation dialog
    if (showDeleteDialog.first && showDeleteDialog.second != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false to null },
            title = { Text("Delete Assignment") },
            text = { Text("Are you sure you want to delete the assignment '${showDeleteDialog.second!!.name}'?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteAssignment(showDeleteDialog.second!!)
                    showDeleteDialog = false to null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false to null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AddCourseDialog(
    onDismiss: () -> Unit,
    onConfirm: (courseId: String, name: String, title: String, description: String, location: String?) -> Unit
) {
    var courseId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Course") },
        text = {
            Column {
                OutlinedTextField(
                    value = courseId,
                    onValueChange = { courseId = it },
                    label = { Text("Course ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(courseId, name, title, description, location.takeIf { it.isNotBlank() }) },
                enabled = courseId.isNotBlank() && name.isNotBlank() && title.isNotBlank() && description.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddAssignmentDialog(
    course: FirebaseCourse,
    onDismiss: () -> Unit,
    onConfirm: (name: String, location: String, dueDate: Date, weight: Double, maxScore: Double, type: String, courseId: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(Date()) }
    var weight by remember { mutableStateOf("") }
    var maxScore by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Assignment") },
        text = {
            Column {
                Text("Course: ${course.name} - ${course.title}")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Assignment Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (0.0-1.0)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = maxScore,
                    onValueChange = { maxScore = it },
                    label = { Text("Maximum Score") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Assignment Type (e.g., Quiz, Essay)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Due Date Picker
                Text("Due Date: ${dateFormat.format(dueDate)}", modifier = Modifier.padding(bottom = 4.dp))
                Row {
                    Button(onClick = { showDatePicker = true }) {
                        Text("Pick Date")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { showTimePicker = true }) {
                        Text("Pick Time")
                    }
                }
                if (showDatePicker) {
                    DatePickerDialog(
                        initialDate = dueDate,
                        onDateSelected = { selectedDate ->
                            // Keep the time from the old dueDate
                            val cal = Calendar.getInstance().apply { time = dueDate }
                            val hour = cal.get(Calendar.HOUR_OF_DAY)
                            val minute = cal.get(Calendar.MINUTE)
                            cal.time = selectedDate
                            cal.set(Calendar.HOUR_OF_DAY, hour)
                            cal.set(Calendar.MINUTE, minute)
                            dueDate = cal.time
                            showDatePicker = false
                        },
                        onDismissRequest = { showDatePicker = false }
                    )
                }
                if (showTimePicker) {
                    TimePickerDialog(
                        initialTime = dueDate,
                        onTimeSelected = { hour, minute ->
                            val cal = Calendar.getInstance().apply { time = dueDate }
                            cal.set(Calendar.HOUR_OF_DAY, hour)
                            cal.set(Calendar.MINUTE, minute)
                            dueDate = cal.time
                            showTimePicker = false
                        },
                        onDismissRequest = { showTimePicker = false }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weightValue = weight.toDoubleOrNull() ?: 0.0
                    val maxScoreValue = maxScore.toDoubleOrNull() ?: 0.0
                    onConfirm(name, location, dueDate, weightValue, maxScoreValue, type, course.courseId)
                },
                enabled = name.isNotBlank() && location.isNotBlank() && weight.isNotBlank() && maxScore.isNotBlank() && type.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddGradeDialog(
    assignment: FirebaseAssignment,
    onDismiss: () -> Unit,
    onConfirm: (studentId: String, score: Double) -> Unit
) {
    var studentId by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Grade for ${assignment.name}") },
        text = {
            Column {
                Text("Assignment: ${assignment.name}")
                Text("Maximum Score: ${assignment.maxScore}")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text("Student ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = score,
                    onValueChange = { score = it },
                    label = { Text("Score") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(studentId, score.toDoubleOrNull() ?: 0.0) },
                enabled = studentId.isNotBlank() && score.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditCourseDescriptionDialog(
    course: FirebaseCourse,
    onDismiss: () -> Unit,
    onConfirm: (title: String, summary: String, objectives: String) -> Unit
) {
    var title by remember { mutableStateOf(course.title) }
    var summary by remember { mutableStateOf(course.description) }
    var objectives by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Course Description") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Course Summary") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = objectives,
                    onValueChange = { objectives = it },
                    label = { Text("Course Objectives") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, summary, objectives) },
                enabled = title.isNotBlank() && summary.isNotBlank() && objectives.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditGradesDialog(
    students: List<FirebaseStudent>,
    grades: List<FirebaseGrade>,
    assignment: FirebaseAssignment,
    onDismiss: () -> Unit,
    onSave: (List<FirebaseGrade>) -> Unit
) {
    // Map studentId to grade and feedback for easy editing
    val gradeMap = remember { mutableStateMapOf<String, String>() }
    val feedbackMap = remember { mutableStateMapOf<String, String>() }
    students.forEach { student ->
        val grade = grades.find { it.studentId == student.studentId }
        gradeMap.putIfAbsent(student.studentId, grade?.score?.toString() ?: "")
        feedbackMap.putIfAbsent(student.studentId, grade?.feedback ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Grades for ${assignment.name}") },
        text = {
            Column {
                students.forEach { student ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${student.firstName} ${student.lastName}", modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = gradeMap[student.studentId] ?: "",
                            onValueChange = { gradeMap[student.studentId] = it },
                            label = { Text("Score") },
                            modifier = Modifier.width(100.dp)
                        )
                    }
                    OutlinedTextField(
                        value = feedbackMap[student.studentId] ?: "",
                        onValueChange = { feedbackMap[student.studentId] = it },
                        label = { Text("Feedback") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedGrades = students.map { student ->
                    val score = gradeMap[student.studentId]?.toDoubleOrNull() ?: 0.0
                    val feedback = feedbackMap[student.studentId]
                    // Find existing grade or create new one
                    grades.find { it.studentId == student.studentId }?.copy(
                        _score = score,
                        feedback = feedback
                    ) ?: FirebaseGrade(
                        assignmentId = assignment.assignmentId,
                        studentId = student.studentId,
                        _score = score,
                        grade = "", // Grade will be calculated automatically based on score
                        feedback = feedback
                    )
                }
                onSave(updatedGrades)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun StudentsInCourseSection(
    students: List<FirebaseStudent>,
    course: FirebaseCourse?
) {
    if (course != null) {
        Text(
            text = "Students in ${course.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        if (students.isEmpty()) {
            Text("No students enrolled.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                students.forEach { student ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${student.firstName} ${student.lastName}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "ID: ${student.studentId}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DatePickerDialog(
    initialDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { time = initialDate }
    DisposableEffect(Unit) {
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                // Keep the time from initialDate
                cal.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                cal.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                onDateSelected(cal.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.setOnDismissListener { onDismissRequest() }
        dialog.show()
        onDispose { dialog.dismiss() }
    }
}

@Composable
fun TimePickerDialog(
    initialTime: Date,
    onTimeSelected: (Int, Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { time = initialTime }
    DisposableEffect(Unit) {
        val dialog = AndroidTimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onTimeSelected(hourOfDay, minute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        dialog.setOnDismissListener { onDismissRequest() }
        dialog.show()
        onDispose { dialog.dismiss() }
    }
} 