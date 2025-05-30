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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.example.autapp.data.firebase.FirebaseBusSchedule
import com.example.autapp.data.firebase.FirebaseBusScheduleRepository
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date // Import Date
import java.util.Calendar // Import Calendar for Date conversion
import androidx.lifecycle.ViewModelStoreOwner

class TransportViewModel(application: Application, private val busScheduleRepository: FirebaseBusScheduleRepository) : AndroidViewModel(application) {
    private val _departures = MutableStateFlow<List<FirebaseBusSchedule>>(emptyList())
    val departures: StateFlow<List<FirebaseBusSchedule>> = _departures.asStateFlow()

    // Define the coordinates for both campuses
    val cityCampus = GeoPoint(-36.8519, 174.7681) // AUT City Campus
    val southCampus = GeoPoint(-36.9927, 174.8797) // AUT South Campus

    // Calculate the center point for the camera
    val centerPoint = GeoPoint(
        (cityCampus.latitude + southCampus.latitude) / 2,
        (cityCampus.longitude + southCampus.longitude) / 2
    )

    init {
        // Initialize OpenStreetMap configuration
        Configuration.getInstance().userAgentValue = application.packageName

        viewModelScope.launch {
            // Check if schedules exist in Firebase, if not, insert the hardcoded data
            val existingSchedules = busScheduleRepository.getAllSchedules()
            if (existingSchedules.isEmpty()) {
                val schedulesToInsert = listOf(
                    // Morning departures
                    FirebaseBusSchedule(departureTime = LocalTime.of(7, 0).toDate(), arrivalTime = LocalTime.of(7, 45).toDate()),
                    FirebaseBusSchedule(departureTime = LocalTime.of(8, 15).toDate(), arrivalTime = LocalTime.of(9, 0).toDate()),
                    FirebaseBusSchedule(departureTime = LocalTime.of(9, 0).toDate(), arrivalTime = LocalTime.of(9, 30).toDate()),
                    FirebaseBusSchedule(departureTime = LocalTime.of(9, 30).toDate(), arrivalTime = LocalTime.of(10, 0).toDate()),
                    FirebaseBusSchedule(
                        departureTime = LocalTime.of(10, 30).toDate(),
                        arrivalTime = LocalTime.of(11, 0).toDate()
                    ),
                    FirebaseBusSchedule(
                        departureTime = LocalTime.of(11, 30).toDate(),
                        arrivalTime = LocalTime.of(12, 0).toDate()
                    ),
                    // Afternoon departures
                    FirebaseBusSchedule(
                        departureTime = LocalTime.of(12, 30).toDate(),
                        arrivalTime = LocalTime.of(13, 0).toDate()
                    ),
                    FirebaseBusSchedule(
                        departureTime = LocalTime.of(13, 30).toDate(),
                        arrivalTime = LocalTime.of(14, 0).toDate()
                    ),
                    FirebaseBusSchedule(
                        departureTime = LocalTime.of(14, 30).toDate(),
                        arrivalTime = LocalTime.of(15, 0).toDate()
                    ),
                    FirebaseBusSchedule(
                        departureTime = LocalTime.of(15, 15).toDate(),
                        arrivalTime = LocalTime.of(15, 45).toDate()
                    ),
                    FirebaseBusSchedule(
                        departureTime = LocalTime.of(16, 30).toDate(),
                        arrivalTime = LocalTime.of(17, 15).toDate()
                    ),
                    FirebaseBusSchedule(
                        departureTime = LocalTime.of(17, 15).toDate(),
                        arrivalTime = LocalTime.of(18, 0).toDate()
                    ),
                    FirebaseBusSchedule(departureTime = LocalTime.of(18, 30).toDate(), arrivalTime = LocalTime.of(19, 0).toDate())
                )
                busScheduleRepository.insertAll(schedulesToInsert)
            }

            _departures.value = busScheduleRepository.getAllSchedules()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as AUTApplication
                TransportViewModel(
                    application = application,
                    busScheduleRepository = application.busScheduleRepository
                )
            }
        }
    }
}

@Composable
fun TransportScreen(
    viewModel: TransportViewModel = viewModel(
        viewModelStoreOwner = LocalLifecycleOwner.current as ViewModelStoreOwner,
        factory = TransportViewModel.Factory
    ),
    paddingValues: PaddingValues
) {
    val now = remember { mutableStateOf(LocalTime.now()) }
    val schedules by viewModel.departures.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        while (true) {
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
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)

                        // Set initial view
                        controller.setZoom(10.0)
                        controller.setCenter(viewModel.centerPoint)

                        // Add markers
                        val cityMarker = Marker(this).apply {
                            position = viewModel.cityCampus
                            title = "AUT City Campus"
                            snippet = "55 Wellesley Street East"
                        }
                        val southMarker = Marker(this).apply {
                            position = viewModel.southCampus
                            title = "AUT South Campus"
                            snippet = "640 Great South Road"
                        }
                        overlays.add(cityMarker)
                        overlays.add(southMarker)

                        // Add route line
                        val routeLine = Polyline().apply {
                            outlinePaint.color = Color.Blue.toArgb()
                            outlinePaint.strokeWidth = 5f
                            setPoints(listOf(viewModel.cityCampus, viewModel.southCampus))
                        }
                        overlays.add(routeLine)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    // Update map if needed
                }
            )
        }

        // Schedule List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(schedules) { schedule ->
                val isPast = now.value.isAfter(schedule.departureTime.toLocalTime()) // Convert Date to LocalTime
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
                                    text = schedule.departureTime.toLocalTime().format(formatter), // Convert Date to LocalTime
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
                                    text = schedule.arrivalTime.toLocalTime().format(formatter), // Convert Date to LocalTime
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

private fun Date.toLocalTime(): LocalTime { // Add extension function to convert Date to LocalTime
    return LocalTime.of(hours, minutes)
}

private fun LocalTime.toDate(): Date { // Add extension function to convert LocalTime to Date
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    // Set date to a fixed date, e.g., start of epoch, as only time is relevant for bus schedules
    calendar.set(Calendar.YEAR, 1970)
    calendar.set(Calendar.MONTH, Calendar.JANUARY)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    return calendar.time
}
