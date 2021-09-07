package com.duke.orca.android.kotlin.lockscreencalendar.calendar.model

import androidx.annotation.ColorInt
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.VISIBLE_INSTANCE_COUNT

sealed class Model {


    data class Event(
        val id: Long,
        val DTSTART: Long
    ): Model()

    data class Instance (
        val isAllDay: Boolean,
        val begin: Long,
        @ColorInt
        val calendarColor: Int,
        val calendarId: Long,
        val end: Long,
        val endDay: Int,
        val eventId: Long,
        val id: Long,
        val month: Int,
        val startDay: Int,
        val title: String,
        var beginDayOfMonth: Int,
        var duration: Int,
        val endDayOfMonth: Int,
        val fillBackgroundColor: Boolean,
        var isVisible: Boolean = true,
    ) : Model() {
        fun deepCopy() = Instance(
            isAllDay = isAllDay,
            begin = begin,
            calendarColor = calendarColor,
            calendarId = calendarId,
            end = end,
            endDay = endDay,
            eventId = eventId,
            id = id,
            month = month,
            startDay = startDay,
            title = title,
            beginDayOfMonth = beginDayOfMonth,
            duration = duration,
            endDayOfMonth = endDayOfMonth,
            fillBackgroundColor = fillBackgroundColor,
            isVisible = isVisible
        )

        fun getColumnCount() = if (isAllDay) 1 else duration.inc()
    }
}