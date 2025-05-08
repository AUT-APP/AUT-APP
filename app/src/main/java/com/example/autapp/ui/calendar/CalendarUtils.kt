package com.example.autapp.ui.calendar

import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.DayOfWeek
import org.threeten.bp.ZoneId
import java.util.*
import java.text.SimpleDateFormat

// Formats a Date object to a string representation like "h:mm a" (e.g., "9:30 AM")
fun Date.format(): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(this)
}

// Converts a java.util.Date object to a org.threeten.bp.LocalDate object.
// This is useful for working with the ThreeTenABP library for date and time.
fun Date.toLocalDate(): LocalDate {
    return org.threeten.bp.Instant.ofEpochMilli(time)
        .atZone(ZoneId.systemDefault()) // Use the system's default time zone
        .toLocalDate()
}

// Converts a org.threeten.bp.LocalDate object back to a java.util.Date object.
// Assumes the time is the start of the day (midnight) in the system's default time zone.
fun LocalDate.toDate(): Date {
    return Date(this.atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli())
}

/**
 * Generates a list of LocalDate objects for a given month, including nulls for empty cells
 * in a calendar grid that starts on Sunday.
 *
 * @param yearMonth The month to generate days for
 * @return List of LocalDate? where null represents empty calendar cells
 */
fun generateDaysForMonth(yearMonth: YearMonth): List<LocalDate?> {
    val firstOfMonth = yearMonth.atDay(1) // Get the first day of the specified month.
    /**
     * Determine the day of the week for the first day of the month.
     * DayOfWeek.SUNDAY is 7 in ThreeTenABP; we adjust it to 0 for a Sunday-start week.
     * Otherwise, DayOfWeek.MONDAY (1) to DayOfWeek.SATURDAY (6) are used as is.
     */
    val firstDayOfWeek = if (firstOfMonth.dayOfWeek == DayOfWeek.SUNDAY) 0 else firstOfMonth.dayOfWeek.value

    val days = mutableListOf<LocalDate?>()

    // Add nulls for the days of the week before the first day of the month.
    // This creates empty cells at the beginning of the calendar grid if the month doesn't start on Sunday.
    repeat(firstDayOfWeek) {
        days.add(null)
    }

    // Add all actual days of the month to the list.
    for (i in 1..yearMonth.lengthOfMonth()) {
        days.add(yearMonth.atDay(i))
    }

    // Add nulls to complete the last week of the calendar grid if it doesn't end on Saturday.
    // This ensures the grid has a multiple of 7 cells.
    while (days.size % 7 != 0) {
        days.add(null)
    }

    return days
} 