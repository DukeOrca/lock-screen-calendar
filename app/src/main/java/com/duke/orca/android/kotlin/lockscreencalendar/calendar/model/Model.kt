package com.duke.orca.android.kotlin.lockscreencalendar.calendar.model

import androidx.annotation.ColorInt

sealed class Model {
    data class Calendar(
        val id: Long,
        val name: String
    ) : Model()

    data class Instance (
        val allDay: Boolean,
        val begin: Long,
        val beginDayOfMonth: Int,
        @ColorInt
        val calendarColor: Int,
        val calendarDisplayName: String,
        val calendarId: Long,
        val end: Long,
        val endDayOfMonth: Int,
        val eventId: Long,
        val id: Long,
        val month: Int,
        val period: Int,
        val title: String
    ) : Model()
}