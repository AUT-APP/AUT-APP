package com.example.autapp.ui.booking

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.autapp.data.models.BookingSlot
import com.example.autapp.data.models.SlotStatus
import com.example.autapp.data.models.StudySpace
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    viewModel: BookingViewModel,
    navController: NavController,
    studentId: Int,
    isDarkTheme: Boolean,
    paddingValues: PaddingValues
) {
    val availableSlots by viewModel.availableSlots.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val campuses by viewModel.campuses.collectAsState()
    val buildings by viewModel.buildings.collectAsState()
    val studySpaces by viewModel.studySpaces.collectAsState()
    val allLevels by viewModel.allLevels.collectAsState()
    val selectedCampus by viewModel.selectedCampus.collectAsState()
    val selectedBuilding by viewModel.selectedBuilding.collectAsState()
    val selectedSpaceId by viewModel.selectedSpaceId.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val containerColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F5)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF333333)

    LaunchedEffect(campuses) {
        if (campuses.isNotEmpty() && selectedCampus !in campuses) {
            viewModel.updateFilters(campuses.first(), "", "All", allLevels.firstOrNull() ?: "", selectedDate)
            viewModel.fetchBuildings(campuses.first())
        }
    }

    LaunchedEffect(selectedCampus) {
        if (selectedCampus.isNotEmpty()) {
            viewModel.fetchBuildings(selectedCampus)
            viewModel.fetchAllLevels(selectedCampus, selectedBuilding)
            if (selectedBuilding !in buildings) {
                viewModel.updateFilters(selectedCampus, "", "All", allLevels.firstOrNull() ?: "", selectedDate)
            }
        }
    }

    LaunchedEffect(selectedBuilding) {
        if (selectedBuilding.isNotEmpty()) {
            viewModel.fetchStudySpaces(selectedCampus, selectedBuilding, selectedLevel)
            viewModel.fetchAllLevels(selectedCampus, selectedBuilding)
            if (selectedSpaceId != "All" && studySpaces.none { it.spaceId == selectedSpaceId }) {
                viewModel.updateFilters(selectedCampus, selectedBuilding, "All", allLevels.firstOrNull() ?: selectedLevel, selectedDate)
            }
        }
    }

    LaunchedEffect(selectedLevel) {
        if (selectedBuilding.isNotEmpty()) {
            viewModel.fetchStudySpaces(selectedCampus, selectedBuilding, selectedLevel)
            if (selectedSpaceId != "All" && studySpaces.none { it.spaceId == selectedSpaceId }) {
                viewModel.updateFilters(selectedCampus, selectedBuilding, "All", selectedLevel, selectedDate)
            }
        }
    }

    LaunchedEffect(selectedCampus, selectedBuilding, selectedSpaceId, selectedLevel, selectedDate) {
        if (selectedCampus.isNotEmpty() && selectedBuilding.isNotEmpty()) {
            viewModel.fetchAvailableSlots(
                spaceId = if (selectedSpaceId == "All") null else selectedSpaceId,
                building = selectedBuilding,
                campus = selectedCampus,
                level = if (selectedLevel.isEmpty()) null else selectedLevel,
                date = selectedDate,
                studentId = studentId
            )
        }
    }

    LaunchedEffect(Unit) {
        if (campuses.isEmpty() && !isLoading) {
            viewModel.fetchCampuses()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor)
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = textColor)
            }
        }

        FilterBar(
            selectedCampus = selectedCampus,
            onCampusSelected = { campus ->
                viewModel.updateFilters(campus, "", "All", allLevels.firstOrNull() ?: "", selectedDate)
            },
            selectedBuilding = selectedBuilding,
            onBuildingSelected = { building ->
                viewModel.updateFilters(selectedCampus, building, "All", allLevels.firstOrNull() ?: "", selectedDate)
            },
            selectedSpaceId = selectedSpaceId,
            onSpaceSelected = { spaceId ->
                viewModel.updateFilters(selectedCampus, selectedBuilding, spaceId, selectedLevel, selectedDate)
            },
            selectedLevel = selectedLevel,
            onLevelSelected = { level ->
                viewModel.updateFilters(selectedCampus, selectedBuilding, "All", level, selectedDate)
            },
            selectedDate = selectedDate,
            onDateSelected = { date ->
                viewModel.updateFilters(selectedCampus, selectedBuilding, selectedSpaceId, selectedLevel, date)
            },
            campuses = campuses,
            buildings = buildings,
            studySpaces = studySpaces,
            allLevels = allLevels,
            isDarkTheme = isDarkTheme
        )

        Spacer(modifier = Modifier.height(16.dp))

        BookingLegend(isDarkTheme = isDarkTheme)

        Spacer(modifier = Modifier.height(16.dp))

        if (availableSlots.isEmpty() && !isLoading) {
            EmptyBookingSlotsState(isDarkTheme = isDarkTheme)
        } else if (!isLoading) {
            val levels = availableSlots.map { it.level }.distinct()
            RoomBookingContent(
                levels = levels,
                bookingSlots = availableSlots,
                navController = navController,
                selectedDate = selectedDate,
                studentId = studentId,
                isDarkTheme = isDarkTheme
            )
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun BookingLegend(isDarkTheme: Boolean) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF333333)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(color = Color(0xFF6ABF69), text = "Available", textColor = textColor)
        LegendItem(color = Color(0xFFEF5350), text = "Booked", textColor = textColor)
        LegendItem(color = Color(0xFF4285F4), text = "In Use", textColor = textColor)
        LegendItem(color = Color(0xFF26A69A), text = "My Booking", textColor = textColor)
        LegendItem(color = Color.Gray, text = "Past", textColor = textColor)
    }
}

@Composable
fun LegendItem(color: Color, text: String, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, color = textColor)
    }
}

@Composable
fun RoomBookingContent(
    levels: List<String>,
    bookingSlots: List<BookingSlot>,
    navController: NavController,
    selectedDate: Date,
    studentId: Int,
    isDarkTheme: Boolean
) {
    val expandedLevels = remember { mutableStateMapOf<String, Boolean>().apply { levels.forEach { put(it, true) } } }
    val textColor = if (isDarkTheme) Color.White else Color(0xFF333333)

    LazyColumn {
        levels.forEach { level ->
            item {
                LevelHeader(
                    level = level,
                    expanded = expandedLevels[level] ?: true,
                    onToggle = { expandedLevels[level] = !(expandedLevels[level] ?: true) },
                    isDarkTheme = isDarkTheme
                )
            }
            if (expandedLevels[level] == true) {
                item {
                    BookingTable(
                        bookingSlots = bookingSlots.filter { it.level == level },
                        navController = navController,
                        selectedDate = selectedDate,
                        studentId = studentId,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}

@Composable
fun LevelHeader(level: String, expanded: Boolean, onToggle: () -> Unit, isDarkTheme: Boolean) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF333333)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray.copy(alpha = 0.3f))
            .padding(12.dp)
            .clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(level, fontWeight = FontWeight.Bold, color = textColor)
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = textColor
        )
    }
}

@Composable
fun BookingTable(
    bookingSlots: List<BookingSlot>,
    navController: NavController,
    selectedDate: Date,
    studentId: Int,
    isDarkTheme: Boolean
) {
    val timeSlots = listOf(
        "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
        "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30",
        "16:00", "16:30", "17:00", "17:30", "18:00", "18:30", "19:00", "19:30",
        "20:00", "20:30"
    )
    val spaces = bookingSlots.map { it.roomId }.distinct()
    val textColor = if (isDarkTheme) Color.White else Color(0xFF333333)
    val scrollState = rememberScrollState()

    val timeSlotSections = listOf(
        "Morning (8:00 - 12:00)" to timeSlots.subList(0, 8),
        "Afternoon (12:00 - 17:00)" to timeSlots.subList(8, 18),
        "Evening (17:00 - 21:00)" to timeSlots.subList(18, timeSlots.size)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        timeSlotSections.forEach { (sectionTitle, sectionTimeSlots) ->
            Text(
                text = sectionTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = textColor,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDarkTheme) Color(0xFF1C1C1C) else Color(0xFFF0F0F0))
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(50.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Space",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textColor
                    )
                }
                Row(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                ) {
                    sectionTimeSlots.forEach { timeSlot ->
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(50.dp)
                                .padding(horizontal = 2.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = timeSlot,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(spaces) { spaceId ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(50.dp)
                                .padding(4.dp)
                                .border(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                spaceId,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            modifier = Modifier
                                .horizontalScroll(scrollState)
                        ) {
                            sectionTimeSlots.forEach { timeSlot ->
                                val slot = bookingSlots.find { it.roomId == spaceId && it.timeSlot == timeSlot }
                                BookingSlotItem(
                                    slot = slot,
                                    onClick = {
                                        if (slot?.status == SlotStatus.AVAILABLE) {
                                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate)
                                            navController.navigate(
                                                "booking_details/${slot.roomId}/${slot.level}/$dateStr/$timeSlot/$studentId/${slot.campus}/${slot.building}"
                                            )
                                        }
                                    },
                                    isDarkTheme = isDarkTheme,
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(50.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingSlotItem(
    slot: BookingSlot?,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (slot?.status) {
        SlotStatus.AVAILABLE -> Color(0xFF6ABF69)
        SlotStatus.BOOKED -> Color(0xFFEF5350)
        SlotStatus.IN_USE -> Color(0xFF4285F4)
        SlotStatus.MY_BOOKING -> Color(0xFF26A69A)
        SlotStatus.PAST -> Color.Gray
        null -> Color.Gray.copy(alpha = 0.2f)
    }
    val statusText = when (slot?.status) {
        SlotStatus.AVAILABLE -> "Available"
        SlotStatus.BOOKED -> "Booked"
        SlotStatus.IN_USE -> "In Use"
        SlotStatus.MY_BOOKING -> "My Booking"
        SlotStatus.PAST -> "Past"
        null -> ""
    }
    val textColor = if (isDarkTheme) Color.White else Color.White

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(0.5.dp, Color.Gray.copy(alpha = 0.3f))
            .clickable(enabled = slot?.status == SlotStatus.AVAILABLE) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = statusText,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FilterBar(
    selectedCampus: String,
    onCampusSelected: (String) -> Unit,
    selectedBuilding: String,
    onBuildingSelected: (String) -> Unit,
    selectedSpaceId: String,
    onSpaceSelected: (String) -> Unit,
    selectedLevel: String,
    onLevelSelected: (String) -> Unit,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    campuses: List<String>,
    buildings: List<String>,
    studySpaces: List<StudySpace>,
    allLevels: List<String>,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val textColor = if (isDarkTheme) Color.White else Color(0xFF333333)
    var campusExpanded by remember { mutableStateOf(false) }
    var buildingExpanded by remember { mutableStateOf(false) }
    var spaceExpanded by remember { mutableStateOf(false) }
    var levelExpanded by remember { mutableStateOf(false) }

    // Remove "All" from levelOptions
    val levelOptions = allLevels.sorted()
    val spaceOptions = listOf("All") + studySpaces
        .filter { selectedLevel.isEmpty() || it.level == selectedLevel }
        .map { it.spaceId }
        .sorted()

    val isBuildingEnabled = selectedCampus.isNotEmpty() && campuses.contains(selectedCampus)
    val isLevelEnabled = isBuildingEnabled && selectedBuilding.isNotEmpty() && buildings.contains(selectedBuilding)
    val isSpaceEnabled = isLevelEnabled && selectedLevel.isNotEmpty() && levelOptions.contains(selectedLevel)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                OutlinedTextField(
                    value = selectedCampus.ifEmpty { "Select Campus" },
                    onValueChange = { },
                    label = { Text("Campus", color = textColor) },
                    readOnly = true,
                    enabled = campuses.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            if (campuses.isEmpty()) "Loading..." else "Select Campus",
                            color = textColor.copy(alpha = 0.5f)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = if (campusExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle Campus Dropdown",
                            tint = textColor
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        disabledTextColor = textColor.copy(alpha = 0.5f),
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = textColor,
                        disabledLabelColor = textColor.copy(alpha = 0.5f),
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        disabledBorderColor = textColor.copy(alpha = 0.3f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                )
                DropdownMenu(
                    expanded = campusExpanded,
                    onDismissRequest = { campusExpanded = false },
                    modifier = Modifier
                        .background(if (isDarkTheme) Color(0xFF242424) else Color.White)
                        .width(200.dp)
                ) {
                    if (campuses.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Loading...", color = textColor, fontSize = 14.sp) },
                            onClick = { },
                            enabled = false,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        campuses.forEach { campus ->
                            DropdownMenuItem(
                                text = { Text(campus, color = textColor, fontSize = 14.sp) },
                                onClick = {
                                    onCampusSelected(campus)
                                    campusExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(enabled = campuses.isNotEmpty()) { campusExpanded = true }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                OutlinedTextField(
                    value = selectedBuilding.ifEmpty { if (isBuildingEnabled) "Select Building" else "Select Campus First" },
                    onValueChange = { },
                    label = { Text("Building", color = textColor) },
                    readOnly = true,
                    enabled = isBuildingEnabled && buildings.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            if (!isBuildingEnabled) "Select Campus" else if (buildings.isEmpty()) "Loading..." else "Select Building",
                            color = textColor.copy(alpha = 0.5f)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = if (buildingExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle Building Dropdown",
                            tint = textColor
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        disabledTextColor = textColor.copy(alpha = 0.5f),
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = textColor,
                        disabledLabelColor = textColor.copy(alpha = 0.5f),
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        disabledBorderColor = textColor.copy(alpha = 0.3f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                )
                DropdownMenu(
                    expanded = buildingExpanded,
                    onDismissRequest = { buildingExpanded = false },
                    modifier = Modifier
                        .background(if (isDarkTheme) Color(0xFF242424) else Color.White)
                        .width(200.dp)
                ) {
                    if (!isBuildingEnabled || buildings.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(if (!isBuildingEnabled) "Select Campus First" else "Loading...", color = textColor, fontSize = 14.sp) },
                            onClick = { },
                            enabled = false,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        buildings.forEach { building ->
                            DropdownMenuItem(
                                text = { Text(building, color = textColor, fontSize = 14.sp) },
                                onClick = {
                                    onBuildingSelected(building)
                                    buildingExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(enabled = isBuildingEnabled && buildings.isNotEmpty()) { buildingExpanded = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                OutlinedTextField(
                    value = if (selectedLevel.isEmpty()) {
                        if (isLevelEnabled) "Select Level" else "Select Building"
                    } else {
                        selectedLevel
                    },
                    onValueChange = { },
                    label = { Text("Level", color = textColor) },
                    readOnly = true,
                    enabled = isLevelEnabled && levelOptions.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            if (!isLevelEnabled) "Select Building" else if (levelOptions.isEmpty()) "Loading..." else "Select Level",
                            color = textColor.copy(alpha = 0.5f)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = if (levelExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle Level Dropdown",
                            tint = textColor
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        disabledTextColor = textColor.copy(alpha = 0.5f),
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = textColor,
                        disabledLabelColor = textColor.copy(alpha = 0.5f),
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        disabledBorderColor = textColor.copy(alpha = 0.3f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                )
                DropdownMenu(
                    expanded = levelExpanded,
                    onDismissRequest = { levelExpanded = false },
                    modifier = Modifier
                        .background(if (isDarkTheme) Color(0xFF242424) else Color.White)
                        .width(200.dp)
                ) {
                    if (!isLevelEnabled || levelOptions.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(if (!isLevelEnabled) "Select Building First" else "Loading...", color = textColor, fontSize = 14.sp) },
                            onClick = { },
                            enabled = false,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        levelOptions.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level, color = textColor, fontSize = 14.sp) },
                                onClick = {
                                    onLevelSelected(level)
                                    levelExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(enabled = isLevelEnabled && levelOptions.isNotEmpty()) { levelExpanded = true }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                OutlinedTextField(
                    value = selectedSpaceId.ifEmpty { if (isSpaceEnabled) "Select Space" else "Select Level First" },
                    onValueChange = { },
                    label = { Text("Space", color = textColor) },
                    readOnly = true,
                    enabled = isSpaceEnabled && spaceOptions.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            if (!isSpaceEnabled) "Select Level First" else if (spaceOptions.isEmpty()) "Loading..." else "Select Space",
                            color = textColor.copy(alpha = 0.5f)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = if (spaceExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle Space Dropdown",
                            tint = textColor
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        disabledTextColor = textColor.copy(alpha = 0.5f),
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = textColor,
                        disabledLabelColor = textColor.copy(alpha = 0.5f),
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        disabledBorderColor = textColor.copy(alpha = 0.3f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                )
                DropdownMenu(
                    expanded = spaceExpanded,
                    onDismissRequest = { spaceExpanded = false },
                    modifier = Modifier
                        .background(if (isDarkTheme) Color(0xFF242424) else Color.White)
                        .width(200.dp)
                ) {
                    if (!isSpaceEnabled || spaceOptions.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(if (!isSpaceEnabled) "Select Level First" else "Loading...", color = textColor, fontSize = 14.sp) },
                            onClick = { },
                            enabled = false,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        spaceOptions.forEach { space ->
                            DropdownMenuItem(
                                text = { Text(space, color = textColor, fontSize = 14.sp) },
                                onClick = {
                                    onSpaceSelected(space)
                                    spaceExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(enabled = isSpaceEnabled && spaceOptions.isNotEmpty()) { spaceExpanded = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Old Button for Date Selection
        Button(
            onClick = {
                val calendar = Calendar.getInstance().apply { time = selectedDate }
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        onDateSelected(calendar.time)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkTheme) Color(0xFF006060) else Color(0xFF006B6B),
                contentColor = textColor
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Date: ${SimpleDateFormat("dd MMM yyyy", Locale.US).format(selectedDate)}",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun EmptyBookingSlotsState(isDarkTheme: Boolean) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF333333)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EventBusy,
                contentDescription = "No Slots",
                tint = textColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No booking slots available",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Try adjusting the filters or selecting a different date.",
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}