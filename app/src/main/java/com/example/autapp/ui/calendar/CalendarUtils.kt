package com.example.autapp.ui.calendar

import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.DayOfWeek
import org.threeten.bp.ZoneId
import java.util.*
import java.text.SimpleDateFormat

fun Date.format(): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(this)
}

fun Date.toLocalDate(): LocalDate {
    return org.threeten.bp.Instant.ofEpochMilli(time)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

fun LocalDate.toDate(): Date {
    return Date(this.atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli())
}

fun generateDaysForMonth(yearMonth: YearMonth): List<LocalDate?> {
    val firstOfMonth = yearMonth.atDay(1)
    // Adjust for Sunday start (Sunday = 7 in ThreeTenABP, we want it to be 0)
    val firstDayOfWeek = if (firstOfMonth.dayOfWeek == DayOfWeek.SUNDAY) 0 else firstOfMonth.dayOfWeek.value

    val days = mutableListOf<LocalDate?>()

    // Add empty spaces for days before the first of the month
    repeat(firstDayOfWeek) {
        days.add(null)
    }

    // Add all days of the month
    for (i in 1..yearMonth.lengthOfMonth()) {
        days.add(yearMonth.atDay(i))
    }

    // Add empty spaces to complete the last week if needed
    while (days.size % 7 != 0) {
        days.add(null)
    }

    return days
} 