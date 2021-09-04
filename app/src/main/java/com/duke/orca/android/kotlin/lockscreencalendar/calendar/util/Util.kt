package com.duke.orca.android.kotlin.lockscreencalendar.calendar.util

import java.util.*

fun Long.toDate(): Int {
    return Calendar.getInstance().also {
        it.timeInMillis = this
    }.get(Calendar.DATE)
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
        set(Calendar.DATE, 1)
    }[Calendar.DAY_OF_WEEK]
}

fun Calendar.getYearMonthDay() = get(Calendar.YEAR) * 10000 + get(Calendar.MONTH) * 100 + get(Calendar.DATE)