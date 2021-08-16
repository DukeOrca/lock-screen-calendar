package com.duke.orca.android.kotlin.lockscreencalendar.calendar.model

import androidx.annotation.ColorInt
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.ONE
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.entity.Entity

sealed class Model {
    data class Instance (
        val entity: Entity.Instance,
        val allDay: Boolean = entity.allDay,
        val beginDayOfMonth: Int,
        @ColorInt
        val calendarColor: Int = entity.calendarColor,
        val duration: Int,
        val endDayOfMonth: Int,
        val fillBackground: Boolean,
        val isVisible: Boolean = true,
        val month: Int = entity.month,
        val title: String = entity.title
    ) : Model() {
        fun columnCount() = if (entity.allDay)
            ONE
        else
            duration.inc()
    }
}