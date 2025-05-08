package com.example.autapp.ui.transport

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autapp.data.database.BusDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class TransportViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BusDatabase.getDatabase(application)
    private val busScheduleDao = database.busScheduleDao()
    
    val departures: Flow<List<BusSchedule>> = busScheduleDao.getAllSchedules()

    // Define the coordinates for both campuses
    val cityCampus = LatLng(-36.8519, 174.7681) // AUT City Campus
    val southCampus = LatLng(-36.9927, 174.8797) // AUT South Campus
    
    // Calculate the center point for the camera
    val centerPoint = LatLng(
        (cityCampus.latitude + southCampus.latitude) / 2,
        (cityCampus.longitude + southCampus.longitude) / 2
    )

    init {
        viewModelScope.launch {
            val schedules = listOf(
                // Morning departures
                BusSchedule(departureTime = LocalTime.of(7, 0), arrivalTime = LocalTime.of(7, 45)),
                BusSchedule(departureTime = LocalTime.of(8, 15), arrivalTime = LocalTime.of(9, 0)),
                BusSchedule(departureTime = LocalTime.of(9, 0), arrivalTime = LocalTime.of(9, 30)),
                BusSchedule(departureTime = LocalTime.of(9, 30), arrivalTime = LocalTime.of(10, 0)),
                BusSchedule(departureTime = LocalTime.of(10, 30), arrivalTime = LocalTime.of(11, 0)),
                BusSchedule(departureTime = LocalTime.of(11, 30), arrivalTime = LocalTime.of(12, 0)),
                // Afternoon departures
                BusSchedule(departureTime = LocalTime.of(12, 30), arrivalTime = LocalTime.of(13, 0)),
                BusSchedule(departureTime = LocalTime.of(13, 30), arrivalTime = LocalTime.of(14, 0)),
                BusSchedule(departureTime = LocalTime.of(14, 30), arrivalTime = LocalTime.of(15, 0)),
                BusSchedule(departureTime = LocalTime.of(15, 15), arrivalTime = LocalTime.of(15, 45)),
                BusSchedule(departureTime = LocalTime.of(16, 30), arrivalTime = LocalTime.of(17, 15)),
                BusSchedule(departureTime = LocalTime.of(17, 15), arrivalTime = LocalTime.of(18, 0)),
                BusSchedule(departureTime = LocalTime.of(18, 30), arrivalTime = LocalTime.of(19, 0))
            )
            busScheduleDao.deleteAll()
            busScheduleDao.insertAll(schedules)
        }
    }
}

@Composable
fun TransportScreen(
    viewModel: TransportViewModel = viewModel(),
    paddingValues: PaddingValues
) {
    val now = remember { mutableStateOf(LocalTime.now()) }
    val schedules by viewModel.departures.collectAsState(initial = emptyList())
    
    LaunchedEffect(Unit) {
        while(true) {
            now.value = LocalTime.now()
            kotlinx.coroutines.delay(60_000)
        }
    }

    val formatter = DateTimeFormatter.ofPattern("hh:mm a")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Map Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(viewModel.centerPoint, 10f)
            }
            
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Add markers for both campuses
                Marker(
                    state = MarkerState(position = viewModel.cityCampus),
                    title = "AUT City Campus",
                    snippet = "55 Wellesley Street East"
                )
                Marker(
                    state = MarkerState(position = viewModel.southCampus),
                    title = "AUT South Campus",
                    snippet = "640 Great South Road"
                )
                
                // Draw a line between the campuses
                Polyline(
                    points = listOf(viewModel.cityCampus, viewModel.southCampus),
                    color = MaterialTheme.colorScheme.primary,
                    width = 5f
                )
            }
        }

        // Schedule List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(schedules) { schedule ->
                val isPast = now.value.isAfter(schedule.departureTime)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPast) 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else 
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "City to South Campus",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Departure",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = schedule.departureTime.format(formatter),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isPast) Color.Gray else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isPast) FontWeight.Normal else FontWeight.Bold
                                    )
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "To",
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(16.dp),
                                tint = if (isPast) Color.Gray else MaterialTheme.colorScheme.primary
                            )
                            
                            Column {
                                Text(
                                    text = "Arrival",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = schedule.arrivalTime.format(formatter),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isPast) Color.Gray else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isPast) FontWeight.Normal else FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
