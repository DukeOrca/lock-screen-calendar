package com.duke.orca.android.kotlin.lockscreencalendar.calendar.util

import java.util.*

private val calendar = Calendar.getInstance()

fun Long.toDayOfMonth(): Int {
    return calendar.also {
        it.timeInMillis = this
    }.get(Calendar.DAY_OF_MONTH)
}

fun Long.toHourOfDay(): Int {
    return calendar.also {
        it.timeInMillis = this
    }.get(Calendar.HOUR_OF_DAY)
}

fun Long.toMinute(): Int {
    return calendar.also {
        it.timeInMillis = this
    }.get(Calendar.MINUTE)
}

fun getFirstDayOfWeekOfMonth(year: Int, month: Int): Int {
    return calendar.apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }[Calendar.DAY_OF_WEEK]
}