package com.duke.orca.android.kotlin.lockscreencalendar.calendar.model

import android.text.style.BackgroundColorSpan
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
    val duration: Int,
    val isAllDay: Boolean,
    val startDay: Int,
    val title: String,
    var isTransparent: Boolean = false,
    var isVisible: Boolean = true,
    var spanCount: Int = 1
) {
    private val beginCalendar = Calendar.getInstance().apply {
        timeInMillis = begin
    }

    private val endCalendar = Calendar.getInstance().apply {
        timeInMillis = end
    }

    val beginYearMonthDay = beginCalendar.getYearMonthDay()
    val endYearMonthDay = endCalendar.getYearMonthDay()

    fun isFillBackgroundColor() = when {
        isAllDay -> true
        spanCount > 1 -> true
        else -> false
    }

    fun copy(deep: Boolean = true): Instance {
        return if (deep) {
            Instance(
                begin = begin,
                calendarColor = calendarColor,
                end = end,
                endDay = endDay,
                duration = duration,
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