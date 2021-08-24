package com.duke.orca.android.kotlin.lockscreencalendar.calendar.model

import androidx.annotation.ColorInt
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.VISIBLE_INSTANCE_COUNT

sealed class Model {
    data class Calendar(
        val year: Int,
        val month: Int,
        val items: Array<CalendarItem?>,
        val indexOfFirstDayOfMonth: Int,
        val indexOfLastDayOfMonth: Int
    )

//    data class Calendar(
//        val id: Long,
//        val name: String
//    ) : Model()

    data class Event(
        val id: Long,
        val DTSTART: Long
    ): Model()

    data class Instance (
        val isAllDay: Boolean,
        val begin: Long,
        @ColorInt
        val calendarColor: Int,
        val calendarDisplayName: String,
        val calendarId: Long,
        val end: Long,
        val eventId: Long,
        val id: Long,
        val month: Int,
        val title: String,
        var beginDayOfMonth: Int,
        var duration: Int,
        val endDayOfMonth: Int,
        val fillBackgroundColor: Boolean,
        var isVisible: Boolean = true,
    ) : Model() {
        fun deepCopy() = Model.Instance(
            isAllDay = isAllDay,
            begin = begin,
            calendarColor = calendarColor,
            calendarDisplayName = calendarDisplayName,
            calendarId = calendarId,
            end = end,
            eventId = eventId,
            id = id,
            month = month,
            title = title,
            beginDayOfMonth = beginDayOfMonth,
            duration = duration,
            endDayOfMonth = endDayOfMonth,
            fillBackgroundColor = fillBackgroundColor,
            isVisible = isVisible
        )

        fun getColumnCount() = if (isAllDay) 1 else duration.inc()
    }

    sealed class CalendarItem {
        abstract val year: Int
        abstract val month: Int
        abstract val date: Int
        abstract val instances: ArrayList<Instance>
        abstract val position: Int
        abstract val visibleInstances: Array<Instance?>

        open class DayOfMonth(
            override val year: Int,
            override val month: Int,
            override val date: Int,
            override val instances: ArrayList<Instance> = arrayListOf(),
            override val position: Int,
            override val visibleInstances: Array<Instance?> = arrayOfNulls(
                VISIBLE_INSTANCE_COUNT
            )
        ) : CalendarItem()

        class DayOfPreviousMonth(
            override val year: Int,
            override val month: Int,
            override val date: Int,
            override val instances: ArrayList<Model.Instance> = arrayListOf(),
            override val position: Int,
            override val visibleInstances: Array<Model.Instance?> = arrayOfNulls(
                VISIBLE_INSTANCE_COUNT
            )
        ) : CalendarItem()

        class DayOfNextMonth(
            override val year: Int,
            override val month: Int,
            override val date: Int,
            override val instances: ArrayList<Instance> = arrayListOf(),
            override val position: Int,
            override val visibleInstances: Array<Instance?> = arrayOfNulls(
                VISIBLE_INSTANCE_COUNT
            )
        ) : CalendarItem()
    }
}