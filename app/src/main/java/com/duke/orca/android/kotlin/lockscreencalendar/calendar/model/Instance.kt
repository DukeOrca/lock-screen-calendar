package com.duke.orca.android.kotlin.lockscreencalendar.calendar.model

import androidx.annotation.ColorInt
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.getYearMonthDay
import java.util.*

data class Instance(
    val begin: Long,
    @ColorInt
    val calendarColor: Int,
    val end: Long,
    val endDay: Int,
    val eventId: Long,
    val isAllDay: Boolean,
    val startDay: Int,
    val title: String,
    var isVisible: Boolean = true
) {
    private val beginCalendar = Calendar.getInstance().apply {
        timeInMillis = begin
    }

    private val endCalendar = Calendar.getInstance().apply {
        timeInMillis = end
    }

    val beginYearMonthDay = beginCalendar.getYearMonthDay()
    val endYearMonthDay = endCalendar.getYearMonthDay()

    var spanCount = 1

    fun copy(deep: Boolean = true): Instance {
        return if (deep) {
            Instance(
                begin = begin,
                calendarColor = calendarColor,
                end = end,
                endDay = endDay,
                startDay = startDay,
                eventId = eventId,
                isAllDay = isAllDay,
                isVisible = isVisible,
                title = title
            )
        } else {
            this
        }
    }
}