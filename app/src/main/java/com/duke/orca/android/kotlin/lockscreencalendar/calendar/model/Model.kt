package com.duke.orca.android.kotlin.lockscreencalendar.calendar.model

import androidx.annotation.ColorInt
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.ONE
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.VISIBLE_INSTANCE_COUNT
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

    sealed class CalendarItem {
        abstract val dayOfMonth: Int
        abstract val instances: ArrayList<Model.Instance>
        abstract val position: Int
        abstract val visibleInstances: Array<Model.Instance?>

        open class DayOfMonth(
            override val dayOfMonth: Int,
            override val instances: ArrayList<Model.Instance> = arrayListOf(),
            override val position: Int,
            override val visibleInstances: Array<Model.Instance?> = arrayOfNulls(
                VISIBLE_INSTANCE_COUNT
            )
        ) : CalendarItem()

        class DayOfPreviousMonth(
            override val dayOfMonth: Int,
            override val instances: ArrayList<Model.Instance> = arrayListOf(),
            override val position: Int,
            override val visibleInstances: Array<Model.Instance?> = arrayOfNulls(
                VISIBLE_INSTANCE_COUNT
            )
        ) : CalendarItem()

        class DayOfNextMonth(
            override val dayOfMonth: Int,
            override val instances: ArrayList<Model.Instance> = arrayListOf(),
            override val position: Int,
            override val visibleInstances: Array<Model.Instance?> = arrayOfNulls(
                VISIBLE_INSTANCE_COUNT
            )
        ) : CalendarItem()
    }
}