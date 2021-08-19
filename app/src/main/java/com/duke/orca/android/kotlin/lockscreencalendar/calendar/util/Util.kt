package com.duke.orca.android.kotlin.lockscreencalendar.calendar.util

import java.util.*

fun Long.toDayOfMonth(): Int {
    return Calendar.getInstance().also {
        it.timeInMillis = this
    }.get(Calendar.DAY_OF_MONTH)
}

fun Long.toHourOfDay(): Int {
    return Calendar.getInstance().also {
        it.timeInMillis = this
    }.get(Calendar.HOUR_OF_DAY)
}

fun Long.toMinute(): Int {
    return Calendar.getInstance().also {
        it.timeInMillis = this
    }.get(Calendar.MINUTE)
}

fun Long.toMonth(): Int {
    return Calendar.getInstance().also {
        it.timeInMillis = this
    }.get(Calendar.MONTH)
}

fun Long.toYear(): Int {
    return Calendar.getInstance().also {
        it.timeInMillis = this
    }.get(Calendar.YEAR)
}

fun getFirstDayOfWeekOfMonth(year: Int, month: Int): Int {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }[Calendar.DAY_OF_WEEK]
}