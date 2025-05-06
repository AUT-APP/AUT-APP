package com.example.autapp.ui.booking
import androidx.compose.foundation.background
import com.example.autapp.ui.booking.BookingScreen
import com.example.autapp.ui.booking.BookingViewModel
import com.example.autapp.ui.booking.MyBookingsScreen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

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

