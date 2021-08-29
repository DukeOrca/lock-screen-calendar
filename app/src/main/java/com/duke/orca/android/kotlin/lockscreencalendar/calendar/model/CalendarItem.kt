package com.duke.orca.android.kotlin.lockscreencalendar.calendar.model

import com.duke.orca.android.kotlin.lockscreencalendar.calendar.VISIBLE_INSTANCE_COUNT

sealed class CalendarItem {
    abstract val year: Int
    abstract val month: Int
    abstract val date: Int
    abstract val instances: ArrayList<Model.Instance>
    abstract val position: Int
    abstract val visibleInstances: Array<Model.Instance?>

    open class DayOfMonth(
        override val year: Int,
        override val month: Int,
        override val date: Int,
        override val instances: ArrayList<Model.Instance> = arrayListOf(),
        override val position: Int,
        override val visibleInstances: Array<Model.Instance?> = arrayOfNulls(
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
        override val instances: ArrayList<Model.Instance> = arrayListOf(),
        override val position: Int,
        override val visibleInstances: Array<Model.Instance?> = arrayOfNulls(
            VISIBLE_INSTANCE_COUNT
        )
    ) : CalendarItem()
}