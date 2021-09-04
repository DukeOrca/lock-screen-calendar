package com.duke.orca.android.kotlin.lockscreencalendar.calendar.model

import com.duke.orca.android.kotlin.lockscreencalendar.calendar.VISIBLE_INSTANCE_COUNT
import java.util.*
import kotlin.collections.ArrayList

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

class CalItem2(
    val timeInMillis2: Long,
    val nextKey: Int,
    val instances: ArrayList<Instance> = arrayListOf(),
    val visibleInstances: Array<Instance?> = arrayOfNulls(VISIBLE_INSTANCE_COUNT)
) {
    val calendar = Calendar.getInstance().apply {
        this.timeInMillis = timeInMillis2
    }
    val date = calendar.get(Calendar.DATE)
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val month = calendar.get(Calendar.MONTH)
    val year = calendar.get(Calendar.YEAR)

    val yearMonthDay = year * 10000 + month * 100 + date
}