package com.example.autapp.ui.transport

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// 1) A simple ViewModel that emits a fixed schedule:
class TransportViewModel : ViewModel() {
    // Replace with your real data source / DAO if you like
    private val rawTimes = listOf("08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00")
    private val fmt = DateTimeFormatter.ofPattern("HH:mm")

    // Expose them as a sorted list of LocalTime
    val departures: List<LocalTime> = rawTimes
        .map { LocalTime.parse(it, fmt) }
        .sorted()
}

// 2) The composable
@Composable
fun TransportScreen(
    viewModel: TransportViewModel = viewModel(),
    paddingValues: PaddingValues
) {
    val now = remember { mutableStateOf(LocalTime.now()) }
    // update “now” every minute or so:
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
        items(viewModel.departures) { departure ->
            val isPast = now.value.isAfter(departure)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = departure.format(formatter),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (isPast) Color.Gray else MaterialTheme.colorScheme.onBackground,
                        fontWeight = if (isPast) FontWeight.Normal else FontWeight.Bold
                    )
                )
            }
        }
    }
}
