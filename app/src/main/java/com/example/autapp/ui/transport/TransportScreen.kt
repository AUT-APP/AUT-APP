package com.example.autapp.ui.transport

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
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

class TransportViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BusDatabase.getDatabase(application)
    private val busScheduleDao = database.busScheduleDao()
    
    val departures: Flow<List<BusSchedule>> = busScheduleDao.getAllSchedules()

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
    
    // update "now" every minute or so:
    LaunchedEffect(Unit) {
        while(true) {
            now.value = LocalTime.now()
            kotlinx.coroutines.delay(60_000)
        }
    }

    val formatter = DateTimeFormatter.ofPattern("hh:mm a")
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        items(schedules) { schedule ->
            val isPast = now.value.isAfter(schedule.departureTime)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "${schedule.departureTime.format(formatter)} → ${schedule.arrivalTime.format(formatter)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (isPast) Color.Gray else MaterialTheme.colorScheme.onBackground,
                        fontWeight = if (isPast) FontWeight.Normal else FontWeight.Bold
                    )
                )
            }
        }
    }
}
