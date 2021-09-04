package com.duke.orca.android.kotlin.lockscreencalendar.calendar.repository

interface CalendarRepository {
    fun getInstances(begin: Long, end: Long)
}