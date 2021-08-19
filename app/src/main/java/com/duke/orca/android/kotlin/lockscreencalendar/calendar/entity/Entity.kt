package com.duke.orca.android.kotlin.lockscreencalendar.calendar.entity

import androidx.annotation.ColorInt

sealed class Entity {
    data class Calendar(
        val id: Long,
        val name: String
    ) : Entity()

    data class Event(
        val id: Long,
        val DTSTART: Long
    ): Entity()

    data class Instance (
        val allDay: Boolean,
        val begin: Long,
        @ColorInt
        val calendarColor: Int,
        val calendarDisplayName: String,
        val calendarId: Long,
        val end: Long,
        val eventId: Long,
        val id: Long,
        val month: Int,
        val title: String
    ) : Entity()
}